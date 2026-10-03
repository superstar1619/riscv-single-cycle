package riscvsingle.ieu

import chisel3._
import chisel3.util.{Fill, MuxLookup}

/** Book-compatible ALU operands, controls, and separate address/result outputs. */
final class ALUIO(dataWidth: Int) extends Bundle {
  val SrcA = Input(UInt(dataWidth.W))
  val SrcB = Input(UInt(dataWidth.W))
  val ALUControl = Input(UInt(2.W))
  val Funct3 = Input(UInt(3.W))
  val ALUResult = Output(UInt(dataWidth.W))
  val IEUAdr = Output(UInt(dataWidth.W))
}

/** Combinational add/sub/signed-slt/or/and ALU, Code Example 2.15, pp. 64-65.
  * ALUControl = {Sub, ALUOp}. The controller must enable Sub for signed slt.
  * Unsupported effective function codes produce zero instead of the book's x.
  */
final class ALU(val dataWidth: Int = 32) extends RawModule {
  require(dataWidth > 0, "ALU dataWidth must be > 0")

  val io = IO(new ALUIO(dataWidth))

  val ALUOp = Wire(Bool())
  val Sub = Wire(Bool())
  val CondInvb = Wire(UInt(dataWidth.W))
  val Sum = Wire(UInt(dataWidth.W))
  val Overflow = Wire(Bool())
  val Neg = Wire(Bool())
  val LT = Wire(Bool())
  val SLT = Wire(UInt(dataWidth.W))
  val ALUFunct = Wire(UInt(3.W))

  ALUOp := io.ALUControl(0)
  Sub := io.ALUControl(1)
  CondInvb := Mux(Sub, ~io.SrcB, io.SrcB)
  Sum := io.SrcA + CondInvb + Sub.asUInt
  io.IEUAdr := Sum

  // The book's subtraction overflow correction supports signed slt even
  // when the truncated difference has the wrong sign due to overflow.
  Overflow := (io.SrcA(dataWidth - 1) ^ io.SrcB(dataWidth - 1)) &
    (io.SrcA(dataWidth - 1) ^ Sum(dataWidth - 1))
  Neg := Sum(dataWidth - 1)
  LT := Neg ^ Overflow
  SLT := Mux(LT, 1.U(dataWidth.W), 0.U(dataWidth.W))

  // Loads/stores/branches/jal force addition independently of Funct3 when
  // their controller sets ALUOp=0 and Sub=0.
  ALUFunct := io.Funct3 & Fill(3, ALUOp)
  io.ALUResult := MuxLookup(ALUFunct, 0.U(dataWidth.W))(Seq(
    0.U -> Sum,
    2.U -> SLT,
    6.U -> (io.SrcA | io.SrcB),
    7.U -> (io.SrcA & io.SrcB)
  ))
}
