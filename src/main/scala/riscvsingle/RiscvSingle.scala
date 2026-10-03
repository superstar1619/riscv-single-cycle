package riscvsingle

import chisel3._
import riscvsingle.config.CpuConfig
import riscvsingle.ifu.IFU
import riscvsingle.ieu.IEU
import riscvsingle.lsu.LSU

/** Book-compatible observation outputs for the self-contained CPU. */
final class RiscvSingleIO(config: CpuConfig) extends Bundle {
  val WriteData = Output(UInt(config.xlen.W))
  val IEUAdr = Output(UInt(config.xlen.W))
  val MemWrite = Output(Bool())
}

/** Simplified RV32 single-cycle CPU, Code Example 2.15, p. 61.
  * The same configuration reaches fetch, execution, and data memory.
  * PC and x0 reset synchronously; data RAM has no reset, as reviewed.
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
  lsu.io.MemWrite := ieu.io.MemWrite
  lsu.io.IEUAdr := ieu.io.IEUAdr
  lsu.io.WriteData := ieu.io.WriteData
  ReadData := lsu.io.ReadData

  io.WriteData := ieu.io.WriteData
  io.IEUAdr := ieu.io.IEUAdr
  io.MemWrite := ieu.io.MemWrite
}
