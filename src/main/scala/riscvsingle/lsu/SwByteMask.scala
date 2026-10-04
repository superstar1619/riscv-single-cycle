package riscvsingle.lsu

import chisel3._
import chisel3.util.{MuxLookup, log2Ceil}

/** Store size, byte offset within a native word, and per-byte write enables. */
final class SwByteMaskIO(dataWidth: Int) extends Bundle {
  val Funct3 = Input(UInt(3.W))
  val ByteOffset = Input(UInt(log2Ceil(dataWidth / 8).W))
  val ByteMask = Output(UInt((dataWidth / 8).W))
}

/** Combinational store byte enables, Chapter 7, Section 7.1.6, Figure 7.9.
  * Standalone RV32/RV64 widths; invalid sizes and misalignments produce zero.
  */
final class SwByteMask(val dataWidth: Int = 32) extends RawModule {
  require(dataWidth == 32 || dataWidth == 64,
    "SwByteMask dataWidth must be 32 or 64")

  val io = IO(new SwByteMaskIO(dataWidth))
  private val byteCount = dataWidth / 8

  val AccessBytes = Wire(UInt(4.W))
  val BaseMask = Wire(UInt(byteCount.W))
  val Aligned = Wire(Bool())
  val ByteMask = Wire(UInt(byteCount.W))

  // Decode the complete store Funct3; unsigned load codes are not store sizes.
  val storeSizes = Seq(0 -> 1, 1 -> 2, 2 -> 4) ++
    (if (dataWidth == 64) Seq(3 -> 8) else Seq.empty)
  AccessBytes := MuxLookup(io.Funct3, 0.U(4.W),
    storeSizes.map { case (code, size) => code.U(3.W) -> size.U(4.W) })
  BaseMask := MuxLookup(AccessBytes, 0.U(byteCount.W),
    storeSizes.map { case (_, size) =>
      size.U(4.W) -> ((BigInt(1) << size) - 1).U(byteCount.W)
    })
  // Supported sizes are powers of two. Natural alignment also prevents a
  // supported access from crossing the native word boundary.
  Aligned := (io.ByteOffset & (AccessBytes - 1.U)) === 0.U
  ByteMask := Mux(AccessBytes =/= 0.U && Aligned,
    (BaseMask << io.ByteOffset)(byteCount - 1, 0), 0.U(byteCount.W))
  io.ByteMask := ByteMask
}
