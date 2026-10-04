package riscvsingle

import chisel3.stage.ChiselStage
import riscvsingle.config.CpuConfig
import riscvsingle.lsu.SubwordWrite

/** 独立生成 SubwordWrite；可选参数依次为数据位宽、输出目录。 */
object GenerateSubwordWrite extends App {
  require(args.length <= 2, "Usage: GenerateSubwordWrite [dataWidth] [targetDir]")
  val dataWidth = args.headOption.map(_.toInt).getOrElse(CpuConfig().xlen)
  val targetDir = args.lift(1).getOrElse("generated/subwordwrite")
  (new ChiselStage).emitVerilog(
    new SubwordWrite(dataWidth),
    Array("--target-dir", targetDir)
  )
}
