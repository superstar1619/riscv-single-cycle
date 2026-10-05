package riscvsingle

import chisel3.stage.ChiselStage
import riscvsingle.config.CpuConfig
import riscvsingle.lsu.SwByteMask

/** 独立生成 SwByteMask；可选参数依次为原生数据位宽、输出目录。 */
object GenerateSwByteMask extends App {
  require(args.length <= 2, "Usage: GenerateSwByteMask [dataWidth] [targetDir]")
  val dataWidth = args.headOption.map(_.toInt).getOrElse(CpuConfig().xlen)
  val targetDir = args.lift(1).getOrElse("generated/swbytemask")
  (new ChiselStage).emitVerilog(
    new SwByteMask(dataWidth),
    Array("--target-dir", targetDir)
  )
}
