package riscvsingle

import chisel3.stage.ChiselStage
import riscvsingle.config.CpuConfig
import riscvsingle.ieu.Shifter

/** Optional arguments: operand width, then output directory. */
object GenerateShifter extends App {
  require(args.length <= 2, "Usage: GenerateShifter [dataWidth] [targetDir]")
  val config = CpuConfig()
  val dataWidth = args.headOption.map(_.toInt).getOrElse(config.xlen)
  val targetDir = args.lift(1).getOrElse("generated/shifter")
  (new ChiselStage).emitVerilog(new Shifter(dataWidth), Array("--target-dir", targetDir))
}
