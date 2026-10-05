package riscvsingle

import chisel3.stage.ChiselStage
import riscvsingle.config.CpuConfig
import riscvsingle.lsu.LSU

/** 独立生成 LSU；可选参数依次为数据存储器字深度、输出目录。 */
object GenerateLSU extends App {
  require(args.length <= 2, "Usage: GenerateLSU [dmemDepth] [targetDir]")
  val depth = args.headOption.map(_.toInt).getOrElse(64)
  val targetDir = args.lift(1).getOrElse("generated/lsu")
  (new ChiselStage).emitVerilog(
    new LSU(CpuConfig(dmemDepth = depth)),
    Array("--target-dir", targetDir)
  )
}
