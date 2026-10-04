package riscvsingle.lsu

import chisel3._
import chisel3.util.{MuxLookup, log2Ceil}

/** 原生读取字、字内字节偏移、加载编码，以及扩展后的读取结果。 */
final class SubwordReadIO(dataWidth: Int) extends Bundle {
  val ReadDataWord = Input(UInt(dataWidth.W))
  val ByteOffset = Input(UInt(log2Ceil(dataWidth / 8).W))
  val Funct3 = Input(UInt(3.W))
  val ReadData = Output(UInt(dataWidth.W))
}

/** 子字读取与符号/零扩展，对应教材 §7.1.6、图 7.9。
  * 纯组合、独立支持 32/64 位。大小相关的低偏移位不参与选择；
  * 未对齐读零门控留给 LSU，非法 Funct3 在本模块输出零。
  */
final class SubwordRead(val dataWidth: Int = 32) extends RawModule {
  require(
    dataWidth == 32 || dataWidth == 64,
    "SubwordRead dataWidth must be 32 or 64"
  )

  val io = IO(new SubwordReadIO(dataWidth))

  // 1. 先选 32 位字，再选半字和字节；小端偏移 0 对应最低字节。
  val SelectedWord = Wire(UInt(32.W))
  val SelectedHalfword = Wire(UInt(16.W))
  val SelectedByte = Wire(UInt(8.W))

  if (dataWidth == 64) {
    SelectedWord := Mux(io.ByteOffset(2), io.ReadDataWord(63, 32), io.ReadDataWord(31, 0))
  } else {
    SelectedWord := io.ReadDataWord
  }
  SelectedHalfword := Mux(io.ByteOffset(1), SelectedWord(31, 16), SelectedWord(15, 0))
  SelectedByte := Mux(io.ByteOffset(0), SelectedHalfword(15, 8), SelectedHalfword(7, 0))

  // 2. SInt.pad 复制符号位，UInt.pad 补零；完整字宽的加载保持原位模式。
  private val loadData = Seq(
    0.U(3.W) -> SelectedByte.asSInt.pad(dataWidth).asUInt,     // LB
    1.U(3.W) -> SelectedHalfword.asSInt.pad(dataWidth).asUInt, // LH
    2.U(3.W) -> SelectedWord.asSInt.pad(dataWidth).asUInt,     // LW
    4.U(3.W) -> SelectedByte.pad(dataWidth),                  // LBU
    5.U(3.W) -> SelectedHalfword.pad(dataWidth)               // LHU
  ) ++ (if (dataWidth == 64) Seq(
    3.U(3.W) -> io.ReadDataWord,             // LD
    6.U(3.W) -> SelectedWord.pad(dataWidth)  // LWU
  ) else Seq.empty)

  // 3. 检查全部三位编码；RV32 的 LD/LWU 和两种配置的 111 均输出零。
  io.ReadData := MuxLookup(io.Funct3, 0.U(dataWidth.W))(loadData)
}
