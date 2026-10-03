package riscvsingle.ieu

import chisel3._
import riscvsingle.config.CpuConfig

/** Instruction execution interface from Code Example 2.15. */
final class IEUIO(config: CpuConfig) extends Bundle {
  val Instr = Input(UInt(32.W))
  val PC = Input(UInt(config.xlen.W))
  val PCPlus4 = Input(UInt(config.xlen.W))
  val PCSrc = Output(Bool())
  val MemWrite = Output(Bool())
  val IEUAdr = Output(UInt(config.xlen.W))
  val WriteData = Output(UInt(config.xlen.W))
  val ReadData = Input(UInt(config.xlen.W))
}

/** RV32 integer execution unit, Code Example 2.15, p. 62.
  * Controller decodes Instr and receives the Datapath's register equality.
  * PC and memory state are supplied by the surrounding IFU and LSU.
  */
final class IEU(val config: CpuConfig = CpuConfig()) extends RawModule {
  val clk = IO(Input(Clock()))
  val reset = IO(Input(Bool()))
  val io = IO(new IEUIO(config))

  val RegWrite = Wire(Bool())
  val Eq = Wire(Bool())
  val ALUResultSrc = Wire(Bool())
  val ResultSrc = Wire(Bool())
  val ALUSrc = Wire(UInt(2.W))
  val ImmSrc = Wire(UInt(2.W))
  val ALUControl = Wire(UInt(2.W))

  val c = Module(new Controller)
  val dp = Module(new Datapath(config))

  c.io.Op := io.Instr(6, 0)
  c.io.Funct3 := io.Instr(14, 12)
  c.io.Funct7b5 := io.Instr(30)
  c.io.Eq := Eq
  ALUResultSrc := c.io.ALUResultSrc
  ResultSrc := c.io.ResultSrc
  ALUSrc := c.io.ALUSrc
  RegWrite := c.io.RegWrite
  ImmSrc := c.io.ImmSrc
  ALUControl := c.io.ALUControl
  io.MemWrite := c.io.MemWrite
  io.PCSrc := c.io.PCSrc

  dp.clk := clk
  dp.reset := reset
  dp.io.Funct3 := io.Instr(14, 12)
  dp.io.ALUResultSrc := ALUResultSrc
  dp.io.ResultSrc := ResultSrc
  dp.io.ALUSrc := ALUSrc
  dp.io.RegWrite := RegWrite
  dp.io.ImmSrc := ImmSrc
  dp.io.ALUControl := ALUControl
  Eq := dp.io.Eq
  dp.io.PC := io.PC
  dp.io.PCPlus4 := io.PCPlus4
  dp.io.Instr := io.Instr
  dp.io.ReadData := io.ReadData
  io.IEUAdr := dp.io.IEUAdr
  io.WriteData := dp.io.WriteData
}
