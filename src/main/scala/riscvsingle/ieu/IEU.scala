package riscvsingle.ieu

import chisel3._
import riscvsingle.config.CpuConfig

/** 教材第 7 章 §7.1、图 7.2、表 7.1 的 RV32 执行接口；PC/PCPlus4/IEUAdr 为字节地址。 */
final class IEUIO(config: CpuConfig) extends Bundle {
  val Instr = Input(UInt(32.W))
  val PC = Input(UInt(config.xlen.W))
  val PCPlus4 = Input(UInt(config.xlen.W))
  val PCSrc = Output(Bool())
  val MemWrite = Output(Bool())
  val MemRW = Output(UInt(2.W))
  val Funct3 = Output(UInt(3.W))
  val IEUAdr = Output(UInt(config.xlen.W))
  val WriteData = Output(UInt(config.xlen.W))
  val ReadData = Input(UInt(config.xlen.W))
}

/** 教材第 7 章 §7.1、图 7.2、表 7.1：RV32 整数执行单元。
  * Controller 组合译码，并接收原始寄存器的相等、有符号和无符号比较结果。
  * PC 和存储器状态由 IFU/LSU 提供；ReadData 已由外部完成子字处理。
  * 显式 clk 和高有效同步 reset 沿用第 2 章接口：x0 清零、非零寄存器保持，复位抑制写入。
  */
final class IEU(val config: CpuConfig = CpuConfig()) extends RawModule {
  val clk = IO(Input(Clock()))
  val reset = IO(Input(Bool()))
  val io = IO(new IEUIO(config))

  val RegWrite = Wire(Bool())
  val Eq = Wire(Bool())
  val LT = Wire(Bool())
  val LTU = Wire(Bool())
  val ALUResultSrc = Wire(Bool())
  val Jump = Wire(Bool())
  val ResultSrc = Wire(Bool())
  val ALUSrc = Wire(UInt(2.W))
  val ImmSrc = Wire(UInt(3.W))
  val ALUControl = Wire(UInt(2.W))
  val MemRW = Wire(UInt(2.W))
  val Funct3 = Wire(UInt(3.W))

  val c = Module(new Controller)
  val dp = Module(new Datapath(config))

  c.io.Op := io.Instr(6, 0)
  Funct3 := io.Instr(14, 12)
  c.io.Funct3 := Funct3
  c.io.Funct7 := io.Instr(31, 25)
  c.io.Eq := Eq
  c.io.LT := LT
  c.io.LTU := LTU
  ALUResultSrc := c.io.ALUResultSrc
  Jump := c.io.Jump
  ResultSrc := c.io.ResultSrc
  ALUSrc := c.io.ALUSrc
  RegWrite := c.io.RegWrite
  ImmSrc := c.io.ImmSrc
  ALUControl := c.io.ALUControl
  MemRW := c.io.MemRW
  io.MemRW := MemRW
  io.Funct3 := Funct3
  io.MemWrite := MemRW(0)
  io.PCSrc := c.io.PCSrc

  dp.clk := clk
  dp.reset := reset
  dp.io.Funct3 := Funct3
  dp.io.ALUResultSrc := ALUResultSrc
  dp.io.Jump := Jump
  dp.io.ResultSrc := ResultSrc
  dp.io.ALUSrc := ALUSrc
  dp.io.RegWrite := RegWrite
  dp.io.ImmSrc := ImmSrc
  dp.io.ALUControl := ALUControl
  Eq := dp.io.Eq
  LT := dp.io.LT
  LTU := dp.io.LTU
  dp.io.PC := io.PC
  dp.io.PCPlus4 := io.PCPlus4
  dp.io.Instr := io.Instr
  dp.io.ReadData := io.ReadData
  io.IEUAdr := dp.io.IEUAdr
  io.WriteData := dp.io.WriteData
}
