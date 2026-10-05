package riscvsingle

import chisel3.stage.ChiselStage
import riscvsingle.config.CpuConfig
import riscvsingle.ieu.Extend

/** 独立生成 Extend；可选参数依次为输出位宽、输出目录。 */
object GenerateExtend extends App {
  require(args.length <= 2, "Usage: GenerateExtend [outputWidth] [targetDir]")
  val config = CpuConfig()
  val outputWidth = args.headOption.map(_.toInt).getOrElse(config.xlen)
  val targetDir = args.lift(1).getOrElse("generated/extend")
  (new ChiselStage).emitVerilog(
    new Extend(outputWidth),
    Array("--target-dir", targetDir)
  )
}
