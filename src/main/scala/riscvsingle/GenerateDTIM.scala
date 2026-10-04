package riscvsingle

import chisel3.stage.ChiselStage
import riscvsingle.config.CpuConfig
import riscvsingle.lsu.DTIM

/** 独立生成 DTIM；参数依次为数据位宽、原生字深度、输出目录。 */
object GenerateDTIM extends App {
  require(args.length <= 3, "Usage: GenerateDTIM [dataWidth] [depth] [targetDir]")
  val config = CpuConfig()
  val dataWidth = args.headOption.map(_.toInt).getOrElse(config.xlen)
  val depth = args.lift(1).map(_.toInt).getOrElse(config.dmemDepth)
  val targetDir = args.lift(2).getOrElse("generated/dtim")
  (new ChiselStage).emitVerilog(
    new DTIM(dataWidth, depth),
    Array("--target-dir", targetDir)
  )
}
