package riscvsingle.lsu

import chisel3._
import chisel3.util.log2Ceil

/** 字节地址、原生字数据与逐字节写使能；ByteMask(0) 对应最低字节。 */
final class DTIMIO(dataWidth: Int) extends Bundle {
  val Adr = Input(UInt(dataWidth.W))
  val MemRead = Input(Bool())
  val MemWrite = Input(Bool())
  val WriteDataWord = Input(UInt(dataWidth.W))
  val ByteMask = Input(UInt((dataWidth / 8).W))
  val ReadDataWord = Output(UInt(dataWidth.W))
}

/** 教材 §7.1.6 的数据 RAM，参考 CVW dtim.sv 的字索引和字节写使能。
  * depth 以 dataWidth 位原生字计；低位偏移与超出容量的地址位被忽略。
  * 使用 Mem 保持单周期组合读、上升沿写；没有复位或初始化，未写字节未定义。
  * MemRead 为零时输出零；MemWrite 独立控制写入，不检查访存对齐。
  */
final class DTIM(val dataWidth: Int = 32, val depth: Int = 64) extends RawModule {
  require(dataWidth == 32 || dataWidth == 64, "DTIM dataWidth must be 32 or 64")
  require(depth >= 2 && (depth & (depth - 1)) == 0,
    "DTIM depth must be a power of two >= 2")
  require(BigInt(depth) * (dataWidth / 8) <= (BigInt(1) << 32),
    "DTIM capacity must not exceed 2^32 bytes")

  val clk = IO(Input(Clock()))
  val io = IO(new DTIMIO(dataWidth))
  private val byteCount = dataWidth / 8
  private val byteOffsetWidth = log2Ceil(byteCount)
  private val indexWidth = log2Ceil(depth)

  // 只取容量内的字索引；不保存读地址，地址变化立即反映到读数据。
  val WordIndex = Wire(UInt(indexWidth.W))
  WordIndex := io.Adr(indexWidth + byteOffsetWidth - 1, byteOffsetWidth)
  val RAM = withClock(clk) { Mem(depth, Vec(byteCount, UInt(8.W))) }
  val ReadBytes = RAM.read(WordIndex, clk)
  io.ReadDataWord := Mux(io.MemRead, ReadBytes.asUInt, 0.U(dataWidth.W))

  // Vec 的第 0 项为最低字节；掩码写直接更新字节通道，无读改写路径。
  val WriteBytes = Wire(Vec(byteCount, UInt(8.W)))
  for (lane <- 0 until byteCount) {
    WriteBytes(lane) := io.WriteDataWord(8 * lane + 7, 8 * lane)
  }
  when(io.MemWrite) {
    RAM.write(WordIndex, WriteBytes, io.ByteMask.asBools, clk)
  }
}
