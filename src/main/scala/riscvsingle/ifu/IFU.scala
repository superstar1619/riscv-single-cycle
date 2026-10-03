package riscvsingle.ifu

import chisel3._
import riscvsingle.config.CpuConfig

/** Next-PC selection inputs and current instruction/address outputs. */
final class IFUIO(config: CpuConfig) extends Bundle {
  val PCSrc = Input(Bool())
  val IEUAdr = Input(UInt(config.xlen.W))
  val Instr = Output(UInt(32.W))
  val PC = Output(UInt(config.xlen.W))
  val PCPlus4 = Output(UInt(config.xlen.W))
}

/** Instruction fetch unit, Code Example 2.15, pp. 61-62.
  * Per review, PC has active-high synchronous reset. A rising clock edge
  * with reset asserted loads the configurable resetVector.
  * Instruction reads remain asynchronous through the reviewed IROM.
  */
final class IFU(val config: CpuConfig = CpuConfig()) extends RawModule {
  val clk = IO(Input(Clock()))
  val reset = IO(Input(Bool()))
  val io = IO(new IFUIO(config))

  val PCNext = Wire(UInt(config.xlen.W))
  val pcreg = withClockAndReset(clk, reset) {
    RegInit(config.resetVector.U(config.xlen.W))
  }
  val irom = Module(new IROM(config))

  io.PC := pcreg
  io.PCPlus4 := pcreg + 4.U(config.xlen.W)
  PCNext := Mux(io.PCSrc, io.IEUAdr, io.PCPlus4)
  pcreg := PCNext
  irom.io.a := pcreg
  io.Instr := irom.io.rd
}
