package riscvsingle.ieu

import chisel3._

/** Register operand comparison interface, matching the book's R1/R2/Eq/LT/LTU names. */
final class CmpIO(dataWidth: Int) extends Bundle {
  val R1 = Input(UInt(dataWidth.W))
  val R2 = Input(UInt(dataWidth.W))
  val Eq = Output(Bool())
  val LT = Output(Bool())
  val LTU = Output(Bool())
}

/** Pure combinational equality and signed/unsigned less-than comparison.
  * Chapter 7, Section 7.1.3.6, Code Example 7.1, pp. 308-311.
  * A configurable operand width makes the comparator reusable independently
  * of the first CPU's RV32-only configuration.
  */
final class Cmp(val dataWidth: Int = 32) extends RawModule {
  require(dataWidth > 0, "Cmp dataWidth must be > 0")

  val io = IO(new CmpIO(dataWidth))

  // Code Example 7.1 with sgnd=1: flip only the MSB before unsigned comparison.
  // XOR with a width-matched mask also handles dataWidth=1 without an empty slice.
  val signMask = (BigInt(1) << (dataWidth - 1)).U(dataWidth.W)
  val af = Wire(UInt(dataWidth.W))
  val bf = Wire(UInt(dataWidth.W))
  af := io.R1 ^ signMask
  bf := io.R2 ^ signMask

  val EqResult = Wire(Bool())
  val SignedLT = Wire(Bool())
  val UnsignedLT = Wire(Bool())
  EqResult := io.R1 === io.R2
  SignedLT := af < bf
  UnsignedLT := io.R1 < io.R2
  io.Eq := EqResult
  io.LT := SignedLT
  io.LTU := UnsignedLT
}
