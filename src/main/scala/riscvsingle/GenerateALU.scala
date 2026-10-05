package riscvsingle

import chisel3.stage.ChiselStage
import riscvsingle.config.CpuConfig
import riscvsingle.ieu.ALU

/** 独立生成 ALU；可选参数依次为操作数字宽、输出目录。 */
object GenerateALU extends App {
  require(args.length <= 2, "Usage: GenerateALU [dataWidth] [targetDir]")
  val config = CpuConfig()
  val dataWidth = args.headOption.map(_.toInt).getOrElse(config.xlen)
  val targetDir = args.lift(1).getOrElse("generated/alu")
  (new ChiselStage).emitVerilog(
    new ALU(dataWidth),
    Array("--target-dir", targetDir)
  )
}
