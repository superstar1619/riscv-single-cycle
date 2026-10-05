package riscvsingle

import chisel3.stage.ChiselStage
import riscvsingle.config.CpuConfig
import riscvsingle.ieu.Datapath

/** 独立生成 Datapath；可选参数为输出目录，CpuConfig 仅支持 RV32。 */
object GenerateDatapath extends App {
  require(args.length <= 1, "Usage: GenerateDatapath [targetDir]")
  val targetDir = args.headOption.getOrElse("generated/datapath")
  (new ChiselStage).emitVerilog(
    new Datapath(CpuConfig()),
    Array("--target-dir", targetDir)
  )
}
