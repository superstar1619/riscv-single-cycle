package riscvsingle.lsu

import chisel3._
import chisel3.util.{MuxLookup, log2Ceil}

/** 存储尺寸、字内字节偏移与逐字节写使能；ByteMask(0) 对应最低字节通道。 */
final class SwByteMaskIO(dataWidth: Int) extends Bundle {
  val Funct3 = Input(UInt(3.W))
  val ByteOffset = Input(UInt(log2Ceil(dataWidth / 8).W))
  val ByteMask = Output(UInt((dataWidth / 8).W))
}

/** 教材第 7 章 §7.1.6、图 7.9：存储字节掩码。
  * 纯组合，无时钟、复位或状态；独立支持 32/64 位，非法尺寸或未对齐访问输出零。
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

  // 检查完整存储 Funct3；无符号加载编码不能当作存储尺寸。
  val storeSizes = Seq(0 -> 1, 1 -> 2, 2 -> 4) ++
    (if (dataWidth == 64) Seq(3 -> 8) else Seq.empty)
  AccessBytes := MuxLookup(io.Funct3, 0.U(4.W),
    storeSizes.map { case (code, size) => code.U(3.W) -> size.U(4.W) })
  BaseMask := MuxLookup(AccessBytes, 0.U(byteCount.W),
    storeSizes.map { case (_, size) =>
      size.U(4.W) -> ((BigInt(1) << size) - 1).U(byteCount.W)
    })
  // 支持尺寸均为二次幂；自然对齐也保证合法访问不会跨越原生字边界。
  Aligned := (io.ByteOffset & (AccessBytes - 1.U)) === 0.U
  ByteMask := Mux(AccessBytes =/= 0.U && Aligned,
    (BaseMask << io.ByteOffset)(byteCount - 1, 0), 0.U(byteCount.W))
  io.ByteMask := ByteMask
}
