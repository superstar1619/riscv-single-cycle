package riscvsingle.ifu

import chisel3._
import chisel3.util.log2Ceil
import chisel3.util.experimental.loadMemoryFromFileInline
import riscvsingle.config.CpuConfig

/** 字节地址输入与固定 32 位指令输出；保留原公开 a/rd 名称。 */
final class IROMIO(config: CpuConfig) extends Bundle {
  val a = Input(UInt(config.xlen.W))
  val rd = Output(UInt(32.W))
}

/** 教材 §7.1.5 的组合取指 ROM，参考 CVW ifu/irom.sv 的容量内字索引。
  * 每项固定 32 位；忽略低两位和容量外高位，保持地址回绕。
  * 按教材单周期要求不保存读地址，区别于 CVW 的同步 ROM 接口。
  * 镜像只在仿真初始化时加载，不随复位重载；未覆盖的内容未指定。
  */
final class IROM(val config: CpuConfig = CpuConfig()) extends RawModule {
  val io = IO(new IROMIO(config))
  private val indexWidth = log2Ceil(config.imemDepth)

  val WordIndex = Wire(UInt(indexWidth.W))
  WordIndex := io.a(indexWidth + 1, 2)

  val ROM = Mem(config.imemDepth, UInt(32.W))
  config.instructionInitFile.foreach(path => loadMemoryFromFileInline(ROM, path))

  // Mem 的零延迟读口不使用时钟；常低占位不会引入生产 clk/reset 端口。
  io.rd := ROM.read(WordIndex, 0.B.asClock)
}
