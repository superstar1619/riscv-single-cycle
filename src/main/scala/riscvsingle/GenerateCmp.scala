package riscvsingle

import chisel3.stage.ChiselStage
import riscvsingle.config.CpuConfig
import riscvsingle.ieu.Cmp

/** Optional arguments: operand width, then output directory. */
object GenerateCmp extends App {
  require(args.length <= 2, "Usage: GenerateCmp [dataWidth] [targetDir]")
  val config = CpuConfig()
  val dataWidth = args.headOption.map(_.toInt).getOrElse(config.xlen)
  val targetDir = args.lift(1).getOrElse("generated/cmp")
  (new ChiselStage).emitVerilog(new Cmp(dataWidth), Array("--target-dir", targetDir))
}
