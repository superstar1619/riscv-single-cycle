package riscvsingle

import chisel3.stage.ChiselStage
import riscvsingle.config.CpuConfig
import riscvsingle.ieu.ALU

/** Optional arguments: operand width, then output directory. */
object GenerateALU extends App {
  require(args.length <= 2, "Usage: GenerateALU [dataWidth] [targetDir]")
  val config = CpuConfig()
  val dataWidth = args.headOption.map(_.toInt).getOrElse(config.xlen)
  val targetDir = args.lift(1).getOrElse("generated/alu")
  (new ChiselStage).emitVerilog(new ALU(dataWidth), Array("--target-dir", targetDir))
}
