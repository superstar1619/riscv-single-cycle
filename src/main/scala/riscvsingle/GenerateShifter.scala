package riscvsingle

import chisel3.stage.ChiselStage
import riscvsingle.config.CpuConfig
import riscvsingle.ieu.Shifter

/** 独立生成 Shifter；可选参数依次为操作数字宽、输出目录。 */
object GenerateShifter extends App {
  require(args.length <= 2, "Usage: GenerateShifter [dataWidth] [targetDir]")
  val config = CpuConfig()
  val dataWidth = args.headOption.map(_.toInt).getOrElse(config.xlen)
  val targetDir = args.lift(1).getOrElse("generated/shifter")
  (new ChiselStage).emitVerilog(
    new Shifter(dataWidth),
    Array("--target-dir", targetDir)
  )
}
