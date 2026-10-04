package riscvsingle

import chisel3.stage.ChiselStage
import riscvsingle.config.CpuConfig
import riscvsingle.lsu.SubwordRead

/** 独立生成 SubwordRead；可选参数依次为数据位宽、输出目录。 */
object GenerateSubwordRead extends App {
  require(args.length <= 2, "Usage: GenerateSubwordRead [dataWidth] [targetDir]")
  val dataWidth = args.headOption.map(_.toInt).getOrElse(CpuConfig().xlen)
  val targetDir = args.lift(1).getOrElse("generated/subwordread")
  (new ChiselStage).emitVerilog(
    new SubwordRead(dataWidth),
    Array("--target-dir", targetDir)
  )
}
