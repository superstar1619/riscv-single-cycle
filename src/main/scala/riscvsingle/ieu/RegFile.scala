package riscvsingle.ieu

import chisel3._
import chisel3.util.log2Ceil

/** 两个组合读口、一个同步写口；A1/A2/A3 是架构寄存器索引，不是字节地址。 */
final class RegFileIO(dataWidth: Int, registerCount: Int) extends Bundle {
  private val addressWidth = log2Ceil(registerCount)
  val WE3 = Input(Bool())
  val A1 = Input(UInt(addressWidth.W))
  val A2 = Input(UInt(addressWidth.W))
  val A3 = Input(UInt(addressWidth.W))
  val WD3 = Input(UInt(dataWidth.W))
  val RD1 = Output(UInt(dataWidth.W))
  val RD2 = Output(UInt(dataWidth.W))
}

/** 教材 Code Example 2.15，pp. 63–64：全部 N 项均为寄存器状态。
  * 高有效同步 reset 仅清零 x0，并优先于写入；x1..xN-1 无复位值，复位期间保值。
  * 正常写入排除 x0，上升沿后两个组合读口可见新值，无边沿前写穿透。
  */
final class RegFile(val dataWidth: Int = 32, val registerCount: Int = 32) extends RawModule {
  require(dataWidth > 0, "RegFile dataWidth must be > 0")
  require(registerCount >= 2 && (registerCount & (registerCount - 1)) == 0,
    "RegFile registerCount must be a power of two >= 2")

  val clk = IO(Input(Clock()))
  val reset = IO(Input(Bool()))
  val io = IO(new RegFileIO(dataWidth, registerCount))

  // 普通 Reg 不附加全阵列复位；只在下面显式更新 x0 或有效写入的目的寄存器。
  val Registers = withClock(clk) {
    Reg(Vec(registerCount, UInt(dataWidth.W))).suggestName("rf")
  }
  val WriteEnable = Wire(Bool())
  WriteEnable := io.WE3 && (io.A3 =/= 0.U)

  withClock(clk) {
    when(reset) {
      Registers(0) := 0.U(dataWidth.W)
    }.elsewhen(WriteEnable) {
      Registers(io.A3) := io.WD3
    }
  }

  io.RD1 := Registers(io.A1)
  io.RD2 := Registers(io.A2)
}
