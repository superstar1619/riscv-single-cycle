package riscvsingle

import chisel3._
import riscvsingle.config.CpuConfig
import riscvsingle.ifu.IFU
import riscvsingle.ieu.IEU
import riscvsingle.lsu.LSU

/** 自包含 CPU 的观察接口；ByteMask/MemWrite 表示实际允许的字节写入。 */
final class RiscvSingleIO(config: CpuConfig) extends Bundle {
  val WriteData = Output(UInt(config.xlen.W))
  val IEUAdr = Output(UInt(config.xlen.W))
  val MemWrite = Output(Bool())
  val MemRW = Output(UInt(2.W))
  val Funct3 = Output(UInt(3.W))
  val ByteMask = Output(UInt(4.W))
}

/** 教材 §7.1/图 7.2 的 RV32 单周期 CPU：IFU、IEU、LSU 共享配置。
  * 参考 CVW wallypipelinedcore 的三单元连接，省去流水线与扩展接口。
  * PC/x0 同步复位；复位期间屏蔽寄存器和 RAM 写入，RAM 内容保持。
  */
final class RiscvSingle(val config: CpuConfig = CpuConfig()) extends RawModule {
  val clk = IO(Input(Clock()))
  val reset = IO(Input(Bool()))
  val io = IO(new RiscvSingleIO(config))

  val PC = Wire(UInt(config.xlen.W))
  val PCPlus4 = Wire(UInt(config.xlen.W))
  val Instr = Wire(UInt(32.W))
  val ReadData = Wire(UInt(config.xlen.W))
  val PCSrc = Wire(Bool())

  val ifu = Module(new IFU(config))
  val ieu = Module(new IEU(config))
  val lsu = Module(new LSU(config))

  ifu.clk := clk
  ifu.reset := reset
  ifu.io.PCSrc := PCSrc
  ifu.io.IEUAdr := ieu.io.IEUAdr
  PC := ifu.io.PC
  PCPlus4 := ifu.io.PCPlus4
  Instr := ifu.io.Instr

  ieu.clk := clk
  ieu.reset := reset
  ieu.io.PC := PC
  ieu.io.PCPlus4 := PCPlus4
  ieu.io.Instr := Instr
  ieu.io.ReadData := ReadData
  PCSrc := ieu.io.PCSrc

  lsu.clk := clk
  // 状态只在上升沿复位，请求则在 reset 有效时立即屏蔽。
  val MemRW = Mux(reset, 0.U(2.W), ieu.io.MemRW)
  lsu.io.MemWrite := ieu.io.MemWrite && !reset
  lsu.io.MemRW := MemRW
  lsu.io.Funct3 := ieu.io.Funct3
  lsu.io.IEUAdr := ieu.io.IEUAdr
  lsu.io.WriteData := ieu.io.WriteData
  ReadData := lsu.io.ReadData

  io.WriteData := ieu.io.WriteData
  io.IEUAdr := ieu.io.IEUAdr
  io.MemRW := MemRW
  io.Funct3 := ieu.io.Funct3
  io.ByteMask := lsu.io.ByteMask
  io.MemWrite := lsu.io.ByteMask.orR
}
