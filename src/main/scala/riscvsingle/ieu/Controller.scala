package riscvsingle.ieu

import chisel3._
import chisel3.util.{Cat, MuxLookup}

/** Full RV32 function fields, comparison flags, and datapath controls. */
final class ControllerIO extends Bundle {
  val Op = Input(UInt(7.W))
  val Eq = Input(Bool())
  val LT = Input(Bool())
  val LTU = Input(Bool())
  val Funct3 = Input(UInt(3.W))
  val Funct7 = Input(UInt(7.W))
  val ALUResultSrc = Output(Bool())
  val ResultSrc = Output(Bool())
  val MemWrite = Output(Bool())
  val MemRW = Output(UInt(2.W))
  val Jump = Output(Bool())
  val PCSrc = Output(Bool())
  val RegWrite = Output(Bool())
  val ALUSrc = Output(UInt(2.W))
  val ImmSrc = Output(UInt(3.W))
  val ALUControl = Output(UInt(2.W))
}

/** Combinational RV32I controller, Chapter 7, Section 7.1.4, pp. 311-314.
  * Full function legality gates all controls; unsupported instructions produce
  * zero controls. This module does not implement illegal-instruction traps.
  */
final class Controller extends RawModule {
  val io = IO(new ControllerIO)

  val controls = Wire(UInt(13.W))
  val Branch = Wire(Bool())
  val Jump = Wire(Bool())
  val BranchTaken = Wire(Bool())
  val ALUOp = Wire(Bool())
  val SubArith = Wire(Bool())
  val LegalInstr = Wire(Bool())

  val RLegal = io.Funct7 === 0.U ||
    (io.Funct7 === 32.U && (io.Funct3 === 0.U || io.Funct3 === 5.U))
  val ILegal = (io.Funct3 =/= 1.U && io.Funct3 =/= 5.U) ||
    (io.Funct3 === 1.U && io.Funct7 === 0.U) ||
    (io.Funct3 === 5.U && (io.Funct7 === 0.U || io.Funct7 === 32.U))
  val LoadLegal = io.Funct3 === 0.U || io.Funct3 === 1.U ||
    io.Funct3 === 2.U || io.Funct3 === 4.U || io.Funct3 === 5.U
  val StoreLegal = io.Funct3 === 0.U || io.Funct3 === 1.U || io.Funct3 === 2.U
  val BranchLegal = io.Funct3 =/= 2.U && io.Funct3 =/= 3.U
  LegalInstr := MuxLookup(io.Op, false.B)(Seq(
    0x33.U -> RLegal,
    0x13.U -> ILegal,
    0x03.U -> LoadLegal,
    0x23.U -> StoreLegal,
    0x63.U -> BranchLegal,
    0x6f.U -> true.B,
    0x67.U -> (io.Funct3 === 0.U),
    0x37.U -> true.B,
    0x17.U -> true.B
  ))

  // RegWrite_ImmSrc_ALUSrc_ALUOp_ALUResultSrc_MemRW_ResultSrc_Branch_Jump
  // Don't-care controls are zero, including R-type ImmSrc and LUI ALUSrc/ALUOp.
  controls := Mux(LegalInstr, MuxLookup(io.Op, 0.U(13.W))(Seq(
    0x33.U -> "b1000001000000".U(13.W), // R:     1_000_00_1_0_00_0_0_0
    0x13.U -> "b1000011000000".U(13.W), // I:     1_000_01_1_0_00_0_0_0
    0x03.U -> "b1000010010100".U(13.W), // load:  1_000_01_0_0_10_1_0_0
    0x23.U -> "b0001010001000".U(13.W), // store: 0_001_01_0_0_01_0_0_0
    0x63.U -> "b0010110000010".U(13.W), // branch:0_010_11_0_0_00_0_1_0
    0x6f.U -> "b1011110100001".U(13.W), // jal:   1_011_11_0_1_00_0_0_1
    0x67.U -> "b1000010100001".U(13.W), // jalr:  1_000_01_0_1_00_0_0_1
    0x37.U -> "b1100000100000".U(13.W), // lui:   1_100_00_0_1_00_0_0_0
    0x17.U -> "b1100110000000".U(13.W)  // auipc: 1_100_11_0_0_00_0_0_0
  )), 0.U(13.W))

  io.RegWrite := controls(12)
  io.ImmSrc := controls(11, 9)
  io.ALUSrc := controls(8, 7)
  ALUOp := controls(6)
  io.ALUResultSrc := controls(5)
  io.MemRW := controls(4, 3)
  io.MemWrite := io.MemRW(0)
  io.ResultSrc := controls(2)
  Branch := controls(1)
  Jump := controls(0)
  io.Jump := Jump

  val BranchFlag = Wire(Bool())
  BranchFlag := MuxLookup(io.Funct3(2, 1), false.B)(Seq(
    0.U -> io.Eq,
    1.U -> false.B,
    2.U -> io.LT,
    3.U -> io.LTU
  ))
  BranchTaken := BranchFlag ^ io.Funct3(0)
  io.PCSrc := Jump || (Branch && BranchTaken)

  SubArith := ALUOp && (io.Funct3 === 2.U || io.Funct3 === 3.U ||
    (io.Funct3 === 5.U && io.Funct7(5)) ||
    (io.Op === 0x33.U && io.Funct3 === 0.U && io.Funct7(5)))
  io.ALUControl := Cat(SubArith, ALUOp)
}
