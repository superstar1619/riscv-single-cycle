package riscvsingle.ieu

import chisel3._
import chisel3.util.{Cat, Fill, log2Ceil}

/** 操作数、以位计的移位量、方向、算术控制与结果；移位量位宽由数据字宽决定。 */
final class ShifterIO(dataWidth: Int) extends Bundle {
  val A = Input(UInt(dataWidth.W))
  val Amt = Input(UInt(log2Ceil(dataWidth).W))
  val Right = Input(Bool())
  val SubArith = Input(Bool())
  val Y = Output(UInt(dataWidth.W))
}

/** 教材第 7 章 §7.1.3.3，pp. 306–307：漏斗移位器。
  * 纯组合，无时钟、复位或状态；独立支持逻辑左/右移与算术右移，整机仍采用 RV32。
  */
final class Shifter(val dataWidth: Int = 32) extends RawModule {
  require(dataWidth >= 2 && (dataWidth & (dataWidth - 1)) == 0,
    "Shifter dataWidth must be a power of two >= 2")

  val io = IO(new ShifterIO(dataWidth))

  val Sign = Wire(Bool())
  val Z = Wire(UInt((2 * dataWidth - 1).W))
  val Offset = Wire(UInt(log2Ceil(dataWidth).W))
  val ZShift = Wire(UInt((2 * dataWidth - 1).W))

  Sign := io.A(dataWidth - 1) && io.SubArith
  Z := Mux(io.Right, Cat(Fill(dataWidth - 1, Sign), io.A),
    Cat(io.A, 0.U((dataWidth - 1).W)))
  // 二次幂字宽下，Amt 取反等于 dataWidth - 1 - Amt，使左右移共用漏斗路径。
  Offset := Mux(io.Right, io.Amt, ~io.Amt)
  ZShift := Z >> Offset
  io.Y := ZShift(dataWidth - 1, 0)
}
