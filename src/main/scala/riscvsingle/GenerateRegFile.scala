package riscvsingle

import chisel3.stage.ChiselStage
import riscvsingle.config.CpuConfig
import riscvsingle.ieu.RegFile

/** 独立生成 RegFile；参数依次为数据位宽、输出目录、寄存器项数。 */
object GenerateRegFile extends App {
  require(args.length <= 3, "Usage: GenerateRegFile [dataWidth] [targetDir] [registerCount]")
  val config = CpuConfig()
  val dataWidth = args.headOption.map(_.toInt).getOrElse(config.xlen)
  val targetDir = args.lift(1).getOrElse("generated/regfile")
  val registerCount = args.lift(2).map(_.toInt).getOrElse(32)
  (new ChiselStage).emitVerilog(
    new RegFile(dataWidth, registerCount),
    Array("--target-dir", targetDir)
  )
}
