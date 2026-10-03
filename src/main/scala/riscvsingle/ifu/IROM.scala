package riscvsingle.ifu

import chisel3._
import chisel3.util.log2Ceil
import chisel3.util.experimental.loadMemoryFromFileInline
import riscvsingle.config.CpuConfig

/** Byte-addressed, combinational instruction read interface. */
final class IROMIO(config: CpuConfig) extends Bundle {
  val a = Input(UInt(config.xlen.W))
  val rd = Output(UInt(32.W))
}

/** Asynchronous instruction ROM, Code Example 2.15, p. 62.
  * Only the word-index bits are decoded: low byte bits and upper address
  * bits are ignored, preserving the book's address aliasing behavior.
  * File contents are loaded at simulation initialization, not reset.
  * Without an initialization file, memory contents are unspecified.
  */
final class IROM(val config: CpuConfig = CpuConfig()) extends RawModule {
  val io = IO(new IROMIO(config))
  private val indexWidth = log2Ceil(config.imemDepth)

  val WordIndex = Wire(UInt(indexWidth.W))
  WordIndex := io.a(indexWidth + 1, 2)

  val ROM = Mem(config.imemDepth, UInt(32.W))
  config.instructionInitFile.foreach(path => loadMemoryFromFileInline(ROM, path))

  // Mem has zero read latency. Its FIRRTL read-port clock is unused;
  // tie it low so the production RawModule needs no clock/reset ports.
  io.rd := ROM.read(WordIndex, 0.B.asClock)
}
