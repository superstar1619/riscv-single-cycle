package riscvsingle.ieu

import chisel3._
import chisel3.util.{Cat, MuxLookup}

/** Opcode/function fields and book-compatible datapath control signals. */
final class ControllerIO extends Bundle {
  val Op = Input(UInt(7.W))
  val Eq = Input(Bool())
  val Funct3 = Input(UInt(3.W))
  val Funct7b5 = Input(Bool())
  val ALUResultSrc = Output(Bool())
  val ResultSrc = Output(Bool())
  val MemWrite = Output(Bool())
  val PCSrc = Output(Bool())
  val RegWrite = Output(Bool())
  val ALUSrc = Output(UInt(2.W))
  val ImmSrc = Output(UInt(2.W))
  val ALUControl = Output(UInt(2.W))
}

/** Combinational controller, Code Example 2.15, pp. 62-63.
  * The book decodes opcode groups rather than full instruction legality.
  * Undefined opcode controls and R-type's unused ImmSrc are fixed to zero.
  */
final class Controller extends RawModule {
  val io = IO(new ControllerIO)

  val controls = Wire(UInt(11.W))
  val Branch = Wire(Bool())
  val Jump = Wire(Bool())
  val Sub = Wire(Bool())
  val ALUOp = Wire(Bool())

  // RegWrite_ImmSrc_ALUSrc_ALUOp_ALUResultSrc_MemWrite_ResultSrc_Branch_Jump
  controls := MuxLookup(io.Op, 0.U(11.W))(Seq(
    "b0000011".U -> "b10001000100".U(11.W), // lw:         1_00_01_0_0_0_1_0_0
    "b0100011".U -> "b00101001000".U(11.W), // sw:         0_01_01_0_0_1_0_0_0
    "b0110011".U -> "b10000100000".U(11.W), // R-type:     1_00_00_1_0_0_0_0_0
    "b0010011".U -> "b10001100000".U(11.W), // I-type ALU: 1_00_01_1_0_0_0_0_0
    "b1100011".U -> "b01011000010".U(11.W), // beq:        0_10_11_0_0_0_0_1_0
    "b1101111".U -> "b11111010001".U(11.W)  // jal:        1_11_11_0_1_0_0_0_1
  ))

  io.RegWrite := controls(10)
  io.ImmSrc := controls(9, 8)
  io.ALUSrc := controls(7, 6)
  ALUOp := controls(5)
  io.ALUResultSrc := controls(4)
  io.MemWrite := controls(3)
  io.ResultSrc := controls(2)
  Branch := controls(1)
  Jump := controls(0)

  Sub := ALUOp && (((io.Funct3 === 0.U) && io.Funct7b5 && io.Op(5)) ||
    (io.Funct3 === 2.U))
  io.ALUControl := Cat(Sub, ALUOp)
  io.PCSrc := (Branch && io.Eq) || Jump
}
