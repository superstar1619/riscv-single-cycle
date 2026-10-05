package riscvsingle.ieu

import chisel3._

/** 寄存器比较接口，保留教材 R1/R2/Eq/LT/LTU 名称；输入为同宽二补码位型。 */
final class CmpIO(dataWidth: Int) extends Bundle {
  val R1 = Input(UInt(dataWidth.W))
  val R2 = Input(UInt(dataWidth.W))
  val Eq = Output(Bool())
  val LT = Output(Bool())
  val LTU = Output(Bool())
}

/** 教材第 7 章 §7.1.3.6，Code Example 7.1，pp. 308–311：相等及大小比较。
  * 纯组合，无时钟、复位或状态；独立参数化字宽不表示整机支持 RV64。
  */
final class Cmp(val dataWidth: Int = 32) extends RawModule {
  require(dataWidth > 0, "Cmp dataWidth must be > 0")

  val io = IO(new CmpIO(dataWidth))

  // Code Example 7.1 的 sgnd=1：翻转最高位，把二补码顺序映射为无符号顺序。
  // 同宽掩码同时覆盖 dataWidth=1，避免构造空切片。
  val SignMask = (BigInt(1) << (dataWidth - 1)).U(dataWidth.W).suggestName("signMask")
  val FlippedA = Wire(UInt(dataWidth.W)).suggestName("af")
  val FlippedB = Wire(UInt(dataWidth.W)).suggestName("bf")
  FlippedA := io.R1 ^ SignMask
  FlippedB := io.R2 ^ SignMask

  val EqResult = Wire(Bool())
  val SignedLT = Wire(Bool())
  val UnsignedLT = Wire(Bool())
  EqResult := io.R1 === io.R2
  SignedLT := FlippedA < FlippedB
  UnsignedLT := io.R1 < io.R2
  io.Eq := EqResult
  io.LT := SignedLT
  io.LTU := UnsignedLT
}
