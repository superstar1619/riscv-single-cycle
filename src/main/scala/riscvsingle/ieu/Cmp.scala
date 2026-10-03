package riscvsingle.ieu

import chisel3._

/** Register operand equality interface, matching the book's R1/R2/Eq names. */
final class CmpIO(dataWidth: Int) extends Bundle {
  val R1 = Input(UInt(dataWidth.W))
  val R2 = Input(UInt(dataWidth.W))
  val Eq = Output(Bool())
}

/** Pure combinational equality comparison, Code Example 2.15, p. 64.
  * A configurable operand width makes the comparator reusable independently
  * of the first CPU's RV32-only configuration.
  */
final class Cmp(val dataWidth: Int = 32) extends RawModule {
  require(dataWidth > 0, "Cmp dataWidth must be > 0")

  val io = IO(new CmpIO(dataWidth))

  val EqResult = Wire(Bool())
  EqResult := io.R1 === io.R2
  io.Eq := EqResult
}
