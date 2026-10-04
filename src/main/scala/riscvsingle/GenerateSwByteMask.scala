package riscvsingle

import chisel3.stage.ChiselStage
import riscvsingle.config.CpuConfig
import riscvsingle.lsu.SwByteMask

/** Optional arguments: native data width, then output directory. */
object GenerateSwByteMask extends App {
  require(args.length <= 2, "Usage: GenerateSwByteMask [dataWidth] [targetDir]")
  val dataWidth = args.headOption.map(_.toInt).getOrElse(CpuConfig().xlen)
  val targetDir = args.lift(1).getOrElse("generated/swbytemask")
  (new ChiselStage).emitVerilog(new SwByteMask(dataWidth), Array("--target-dir", targetDir))
}
