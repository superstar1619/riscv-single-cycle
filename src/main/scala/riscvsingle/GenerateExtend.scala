package riscvsingle

import chisel3.stage.ChiselStage
import riscvsingle.config.CpuConfig
import riscvsingle.ieu.Extend

/** Optional arguments: output width, then output directory. */
object GenerateExtend extends App {
  require(args.length <= 2, "Usage: GenerateExtend [outputWidth] [targetDir]")
  val config = CpuConfig()
  val outputWidth = args.headOption.map(_.toInt).getOrElse(config.xlen)
  val targetDir = args.lift(1).getOrElse("generated/extend")
  (new ChiselStage).emitVerilog(new Extend(outputWidth), Array("--target-dir", targetDir))
}
