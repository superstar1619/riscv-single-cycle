package riscvsingle.ieu

import chisel3._
import chisel3.util.{Cat, Fill, log2Ceil}

/** Operand, amount, direction, arithmetic control, and shifted result. */
final class ShifterIO(dataWidth: Int) extends Bundle {
  val A = Input(UInt(dataWidth.W))
  val Amt = Input(UInt(log2Ceil(dataWidth).W))
  val Right = Input(Bool())
  val SubArith = Input(Bool())
  val Y = Output(UInt(dataWidth.W))
}

/** Combinational funnel shifter, Chapter 7, Section 7.1.3.3, pp. 306-307.
  * Configurable width supports standalone logical left/right and arithmetic
  * right shifts; the CPU's RV32 configuration is independent.
  */
final class Shifter(val dataWidth: Int = 32) extends RawModule {
  require(dataWidth >= 2 && (dataWidth & (dataWidth - 1)) == 0,
    "Shifter dataWidth must be a power of two >= 2")

  val io = IO(new ShifterIO(dataWidth))

  val Sign = Wire(Bool())
  val Z = Wire(UInt((2 * dataWidth - 1).W))
  val Offset = Wire(UInt(log2Ceil(dataWidth).W))
  val ZShift = Wire(UInt((2 * dataWidth - 1).W))

  Sign := io.A(dataWidth - 1) && io.SubArith
  Z := Mux(io.Right, Cat(Fill(dataWidth - 1, Sign), io.A),
    Cat(io.A, 0.U((dataWidth - 1).W)))
  // For power-of-two widths, complementing Amt gives dataWidth - 1 - Amt.
  Offset := Mux(io.Right, io.Amt, ~io.Amt)
  ZShift := Z >> Offset
  io.Y := ZShift(dataWidth - 1, 0)
}
