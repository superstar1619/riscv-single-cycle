package riscvsingle.ieu

import chisel3._
import riscvsingle.config.CpuConfig

/** Control, instruction, and memory interfaces from Code Example 2.15. */
final class DatapathIO(config: CpuConfig) extends Bundle {
  val Funct3 = Input(UInt(3.W))
  val ALUResultSrc = Input(Bool())
  val ResultSrc = Input(Bool())
  val ALUSrc = Input(UInt(2.W))
  val RegWrite = Input(Bool())
  val ImmSrc = Input(UInt(3.W))
  val ALUControl = Input(UInt(2.W))
  val Eq = Output(Bool())
  val LT = Output(Bool())
  val LTU = Output(Bool())
  val PC = Input(UInt(config.xlen.W))
  val PCPlus4 = Input(UInt(config.xlen.W))
  val Instr = Input(UInt(32.W))
  val IEUAdr = Output(UInt(config.xlen.W))
  val WriteData = Output(UInt(config.xlen.W))
  val ReadData = Input(UInt(config.xlen.W))
}

/** RV32 datapath, Code Example 2.15, p. 63.
  * Control decoding and PC/memory state remain outside this module.
  * Reset follows the reviewed RegFile: synchronous, active high, x0 only.
  */
final class Datapath(val config: CpuConfig = CpuConfig()) extends RawModule {
  val clk = IO(Input(Clock()))
  val reset = IO(Input(Bool()))
  val io = IO(new DatapathIO(config))

  val ImmExt = Wire(UInt(config.xlen.W))
  val R1 = Wire(UInt(config.xlen.W))
  val R2 = Wire(UInt(config.xlen.W))
  val SrcA = Wire(UInt(config.xlen.W))
  val SrcB = Wire(UInt(config.xlen.W))
  val ALUResult = Wire(UInt(config.xlen.W))
  val IEUResult = Wire(UInt(config.xlen.W))
  val Result = Wire(UInt(config.xlen.W))

  val rf = Module(new RegFile(config.xlen, registerCount = 32))
  val ext = Module(new Extend(config.xlen))
  val cmp = Module(new Cmp(config.xlen))
  val alu = Module(new ALU(config.xlen))

  rf.clk := clk
  rf.reset := reset
  rf.io.WE3 := io.RegWrite
  rf.io.A1 := io.Instr(19, 15)
  rf.io.A2 := io.Instr(24, 20)
  rf.io.A3 := io.Instr(11, 7)
  rf.io.WD3 := Result
  R1 := rf.io.RD1
  R2 := rf.io.RD2

  ext.io.Instr := io.Instr(31, 7)
  ext.io.ImmSrc := io.ImmSrc
  ImmExt := ext.io.ImmExt

  cmp.io.R1 := R1
  cmp.io.R2 := R2
  io.Eq := cmp.io.Eq
  io.LT := cmp.io.LT
  io.LTU := cmp.io.LTU

  SrcA := Mux(io.ALUSrc(1), io.PC, R1)
  SrcB := Mux(io.ALUSrc(0), ImmExt, R2)
  alu.io.SrcA := SrcA
  alu.io.SrcB := SrcB
  alu.io.ALUControl := io.ALUControl
  alu.io.Funct3 := io.Funct3
  ALUResult := alu.io.ALUResult
  io.IEUAdr := alu.io.IEUAdr

  IEUResult := Mux(io.ALUResultSrc, io.PCPlus4, ALUResult)
  Result := Mux(io.ResultSrc, io.ReadData, IEUResult)
  io.WriteData := R2
}
