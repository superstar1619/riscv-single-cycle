package riscvsingle.ieu

import chisel3._
import chisel3.util.log2Ceil

/** Two combinational read ports and one synchronous write port. */
final class RegFileIO(dataWidth: Int, registerCount: Int) extends Bundle {
  private val addressWidth = log2Ceil(registerCount)
  val WE3 = Input(Bool())
  val A1 = Input(UInt(addressWidth.W))
  val A2 = Input(UInt(addressWidth.W))
  val A3 = Input(UInt(addressWidth.W))
  val WD3 = Input(UInt(dataWidth.W))
  val RD1 = Output(UInt(dataWidth.W))
  val RD2 = Output(UInt(dataWidth.W))
}

/** Register file, Code Example 2.15, pp. 63-64.
  * Per review, all N entries are registers. Active-high synchronous reset
  * clears only x0; normal writes exclude x0. x1..xN-1 have no reset value.
  * A write becomes visible through both read ports after the rising edge.
  */
final class RegFile(val dataWidth: Int = 32, val registerCount: Int = 32) extends RawModule {
  require(dataWidth > 0, "RegFile dataWidth must be > 0")
  require(registerCount >= 2 && (registerCount & (registerCount - 1)) == 0,
    "RegFile registerCount must be a power of two >= 2")

  val clk = IO(Input(Clock()))
  val reset = IO(Input(Bool()))
  val io = IO(new RegFileIO(dataWidth, registerCount))

  // Every entry is stateful and uses its architectural register number.
  val rf = withClock(clk) { Reg(Vec(registerCount, UInt(dataWidth.W))) }
  val WriteEnable = Wire(Bool())
  WriteEnable := io.WE3 && (io.A3 =/= 0.U)

  withClock(clk) {
    when(reset) {
      rf(0) := 0.U(dataWidth.W)
    }.elsewhen(WriteEnable) {
      rf(io.A3) := io.WD3
    }
  }

  io.RD1 := rf(io.A1)
  io.RD2 := rf(io.A2)
}
