package riscvsingle

import chisel3.stage.ChiselStage
import riscvsingle.config.CpuConfig
import riscvsingle.ieu.IEU

/** Optional argument: output directory. CpuConfig currently requires RV32. */
object GenerateIEU extends App {
  require(args.length <= 1, "Usage: GenerateIEU [targetDir]")
  val targetDir = args.headOption.getOrElse("generated/ieu")
  (new ChiselStage).emitVerilog(new IEU(CpuConfig()),
    Array("--target-dir", targetDir))
}
