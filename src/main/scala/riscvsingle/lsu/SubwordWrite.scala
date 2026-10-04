package riscvsingle.lsu

import chisel3._
import chisel3.util.{Fill, MuxLookup}

/** 原始写数据、完整存储功能编码，以及复制后的原生字。 */
final class SubwordWriteIO(dataWidth: Int) extends Bundle {
  val WriteData = Input(UInt(dataWidth.W))      // 寄存器原始数据。
  val Funct3 = Input(UInt(3.W))                 // 000/001/010/011：SB/SH/SW/SD。
  val WriteDataWord = Output(UInt(dataWidth.W)) // 每个字节通道使用的写数据。
}

/** 子字存储的纯组合数据处理，对应教材 §7.1.6、图 7.9。
  * 将最低字节、半字或字复制到全部对应位置；实际写入位置由 ByteMask 决定。
  * 独立支持 32/64 位，非法 Funct3 输出零；不含时钟、复位或存储状态。
  */
final class SubwordWrite(val dataWidth: Int = 32) extends RawModule {
  require(
    dataWidth == 32 || dataWidth == 64,
    "SubwordWrite dataWidth must be 32 or 64"
  )

  val io = IO(new SubwordWriteIO(dataWidth))

  // 1. 只取所需的低位，并复制到整个输出字宽；未使用的高位不会参与。
  val ByteCopies = Wire(UInt(dataWidth.W))
  val HalfwordCopies = Wire(UInt(dataWidth.W))
  val WordCopies = Wire(UInt(dataWidth.W))

  ByteCopies := Fill(dataWidth / 8, io.WriteData(7, 0))
  HalfwordCopies := Fill(dataWidth / 16, io.WriteData(15, 0))
  WordCopies := Fill(dataWidth / 32, io.WriteData(31, 0))

  // 2. 按完整三位 Funct3 选择。SD 只在 64 位配置下合法。
  private val storeData = Seq(
    0.U(3.W) -> ByteCopies,     // SB：低 8 位复制 4/8 次。
    1.U(3.W) -> HalfwordCopies, // SH：低 16 位复制 2/4 次。
    2.U(3.W) -> WordCopies     // SW：32 位原样输出，64 位复制两次。
  ) ++ (if (dataWidth == 64) Seq(3.U(3.W) -> io.WriteData) else Seq.empty)

  // 3. 非法编码输出零，不能将 100/101/110 解释为 SB/SH/SW。
  io.WriteDataWord := MuxLookup(io.Funct3, 0.U(dataWidth.W))(storeData)
}
