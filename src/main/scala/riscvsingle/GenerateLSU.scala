package riscvsingle

import chisel3.stage.ChiselStage
import riscvsingle.config.CpuConfig
import riscvsingle.lsu.LSU

/** Optional arguments: data memory depth in words and output directory. */
object GenerateLSU extends App {
  require(args.length <= 2, "Usage: GenerateLSU [dmemDepth] [targetDir]")
  val depth = args.headOption.map(_.toInt).getOrElse(64)
  val targetDir = args.lift(1).getOrElse("generated/lsu")
  (new ChiselStage).emitVerilog(new LSU(CpuConfig(dmemDepth = depth)),
    Array("--target-dir", targetDir))
}
