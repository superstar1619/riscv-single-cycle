package riscvsingle

import chisel3.stage.ChiselStage
import riscvsingle.config.CpuConfig
import riscvsingle.ifu.IROM

/** Optional arguments: depth in words, output directory, hex file (or - for none). */
object GenerateIROM extends App {
  require(args.length <= 3, "Usage: GenerateIROM [imemDepth] [targetDir] [instructionInitFile|-]")
  val depth = args.headOption.map(_.toInt).getOrElse(64)
  val targetDir = args.lift(1).getOrElse("generated/irom")
  val file = args.lift(2).getOrElse("programs/riscvtest.memfile")
  val config = CpuConfig(imemDepth = depth,
    instructionInitFile = if (file == "-") None else Some(file))
  (new ChiselStage).emitVerilog(new IROM(config), Array("--target-dir", targetDir))
}
