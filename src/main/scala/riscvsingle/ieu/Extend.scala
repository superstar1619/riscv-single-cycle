package riscvsingle.ieu

import chisel3._
import chisel3.util.{Cat, Fill, MuxLookup}

/** Instr(24, 0) 对应教材 Instr[31:7]；立即数按指令位型扩展，不转换地址单位。 */
final class ExtendIO(outputWidth: Int) extends Bundle {
  val Instr = Input(UInt(25.W))
  val ImmSrc = Input(UInt(3.W))
  val ImmExt = Output(UInt(outputWidth.W))
}

/** 教材第 7 章 §7.1.2、表 7.2：I/S/B/J/U 立即数扩展。
  * 纯组合，无时钟、复位或状态；较宽输出用于独立符号扩展，不表示整机支持 RV64。
  */
final class Extend(val outputWidth: Int = 32) extends RawModule {
  require(outputWidth >= 32, "Extend outputWidth must be >= 32")

  val io = IO(new ExtendIO(outputWidth))

  // 恢复架构位编号以便核对各格式；指令低七位不参与立即数扩展。
  val InstrFull = Wire(UInt(32.W))
  InstrFull := Cat(io.Instr, 0.U(7.W))

  val ImmI = Wire(UInt(outputWidth.W))
  val ImmS = Wire(UInt(outputWidth.W))
  val ImmB = Wire(UInt(outputWidth.W))
  val ImmJ = Wire(UInt(outputWidth.W))
  val ImmU = Wire(UInt(outputWidth.W))

  ImmI := Cat(Fill(outputWidth - 12, InstrFull(31)), InstrFull(31, 20))
  ImmS := Cat(Fill(outputWidth - 12, InstrFull(31)), InstrFull(31, 25), InstrFull(11, 7))
  ImmB := Cat(Fill(outputWidth - 12, InstrFull(31)), InstrFull(7),
    InstrFull(30, 25), InstrFull(11, 8), 0.U(1.W))
  ImmJ := Cat(Fill(outputWidth - 20, InstrFull(31)), InstrFull(19, 12),
    InstrFull(20), InstrFull(30, 21), 0.U(1.W))
  ImmU := Cat(Fill(outputWidth - 31, InstrFull(31)), InstrFull(30, 12), 0.U(12.W))

  // 未支持的选择编码 5/6/7 输出确定的零。
  io.ImmExt := MuxLookup(io.ImmSrc, 0.U(outputWidth.W))(Seq(
    0.U -> ImmI,
    1.U -> ImmS,
    2.U -> ImmB,
    3.U -> ImmJ,
    4.U -> ImmU
  ))
}
