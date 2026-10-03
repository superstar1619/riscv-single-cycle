package riscvsingle

import chisel3.stage.ChiselStage
import riscvsingle.ieu.Controller

/** Optional argument: output directory. Control widths follow RISC-V encoding. */
object GenerateController extends App {
  require(args.length <= 1, "Usage: GenerateController [targetDir]")
  val targetDir = args.headOption.getOrElse("generated/controller")
  (new ChiselStage).emitVerilog(new Controller, Array("--target-dir", targetDir))
}
