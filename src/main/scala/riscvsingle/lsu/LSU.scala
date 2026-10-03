package riscvsingle.lsu

import chisel3._
import chisel3.util.log2Ceil
import riscvsingle.config.CpuConfig

/** Word load/store interface from Code Example 2.15. */
final class LSUIO(config: CpuConfig) extends Bundle {
  val MemWrite = Input(Bool())
  val IEUAdr = Input(UInt(config.xlen.W))
  val WriteData = Input(UInt(config.xlen.W))
  val ReadData = Output(UInt(config.xlen.W))
}

/** Asynchronous read, synchronous write data RAM, Code Example 2.15, p. 65.
  * The book's LSU has no reset or initialization; unwritten locations
  * have unspecified contents. Writes occur only on rising clock edges.
  * Byte-offset and upper address bits are ignored, matching the book.
  */
final class LSU(val config: CpuConfig = CpuConfig()) extends RawModule {
  val clk = IO(Input(Clock()))
  val io = IO(new LSUIO(config))
  private val indexWidth = log2Ceil(config.dmemDepth)

  val WordIndex = Wire(UInt(indexWidth.W))
  WordIndex := io.IEUAdr(indexWidth + 1, 2)
  val RAM = withClock(clk) { Mem(config.dmemDepth, UInt(config.xlen.W)) }

  io.ReadData := RAM.read(WordIndex, clk)
  when(io.MemWrite) {
    RAM.write(WordIndex, io.WriteData, clk)
  }
}
