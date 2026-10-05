package riscvsingle

import chisel3.stage.ChiselStage
import riscvsingle.ieu.Controller

/** 独立生成组合 Controller；可选参数为输出目录，控制位宽遵循指令编码。 */
object GenerateController extends App {
  require(args.length <= 1, "Usage: GenerateController [targetDir]")
  val targetDir = args.headOption.getOrElse("generated/controller")
  (new ChiselStage).emitVerilog(
    new Controller,
    Array("--target-dir", targetDir)
  )
}
