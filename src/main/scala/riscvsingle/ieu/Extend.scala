package riscvsingle.ieu

import chisel3._
import chisel3.util.{Cat, Fill, MuxLookup}

/** Instr(24, 0) corresponds to the book's Instr[31:7]. */
final class ExtendIO(outputWidth: Int) extends Bundle {
  val Instr = Input(UInt(25.W))
  val ImmSrc = Input(UInt(3.W))
  val ImmExt = Output(UInt(outputWidth.W))
}

/** Pure combinational I/S/B/J/U immediate extension, Chapter 7, Section 7.1.2, Table 7.2.
  * A wider output is reusable sign extension, not RV64 CPU support.
  */
final class Extend(val outputWidth: Int = 32) extends RawModule {
  require(outputWidth >= 32, "Extend outputWidth must be >= 32")

  val io = IO(new ExtendIO(outputWidth))

  // Recover architectural bit indices; instruction bits [6:0] are not used.
  val InstrFull = Wire(UInt(32.W))
  InstrFull := Cat(io.Instr, 0.U(7.W))

  val ImmI = Wire(UInt(outputWidth.W))
  val ImmS = Wire(UInt(outputWidth.W))
  val ImmB = Wire(UInt(outputWidth.W))
  val ImmJ = Wire(UInt(outputWidth.W))
  val ImmU = Wire(UInt(outputWidth.W))

  ImmI := Cat(Fill(outputWidth - 12, InstrFull(31)), InstrFull(31, 20))
  ImmS := Cat(Fill(outputWidth - 12, InstrFull(31)), InstrFull(31, 25), InstrFull(11, 7))
  ImmB := Cat(Fill(outputWidth - 12, InstrFull(31)), InstrFull(7),
    InstrFull(30, 25), InstrFull(11, 8), 0.U(1.W))
  ImmJ := Cat(Fill(outputWidth - 20, InstrFull(31)), InstrFull(19, 12),
    InstrFull(20), InstrFull(30, 21), 0.U(1.W))
  ImmU := Cat(Fill(outputWidth - 31, InstrFull(31)), InstrFull(30, 12), 0.U(12.W))

  // Unsupported selectors 5/6/7 deterministically produce zero.
  io.ImmExt := MuxLookup(io.ImmSrc, 0.U(outputWidth.W))(Seq(
    0.U -> ImmI,
    1.U -> ImmS,
    2.U -> ImmB,
    3.U -> ImmJ,
    4.U -> ImmU
  ))
}
