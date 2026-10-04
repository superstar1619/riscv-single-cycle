package riscvsingle.ifu

import chisel3._
import riscvsingle.config.CpuConfig

/** 下一 PC 选择与当前指令/地址；目标地址由 IEU 处理。 */
final class IFUIO(config: CpuConfig) extends Bundle {
  val PCSrc = Input(Bool())
  val IEUAdr = Input(UInt(config.xlen.W))
  val Instr = Output(UInt(32.W))
  val PC = Output(UInt(config.xlen.W))
  val PCPlus4 = Output(UInt(config.xlen.W))
}

/** 教材 §7.1.5 的单周期取指单元，参考 CVW ifu.sv 的 PC 选择和递增结构。
  * 高有效同步 reset 在上升沿加载 resetVector；正常时选 PC+4 或 IEUAdr。
  * IROM 组合输出当前指令；不移植停顿、预测或流水线寄存器。
  * JALR 清位属于 IEU，IFU 原样接收目标地址，不重复清除低位。
  */
final class IFU(val config: CpuConfig = CpuConfig()) extends RawModule {
  val clk = IO(Input(Clock()))
  val reset = IO(Input(Bool()))
  val io = IO(new IFUIO(config))

  val PCNext = Wire(UInt(config.xlen.W))
  val pcreg = withClockAndReset(clk, reset) {
    RegInit(config.resetVector.U(config.xlen.W))
  }
  val irom = Module(new IROM(config))

  io.PC := pcreg
  io.PCPlus4 := pcreg + 4.U(config.xlen.W)
  PCNext := Mux(io.PCSrc, io.IEUAdr, io.PCPlus4)
  pcreg := PCNext
  irom.io.a := pcreg
  io.Instr := irom.io.rd
}
