package riscvsingle

import chisel3.stage.ChiselStage
import riscvsingle.config.CpuConfig
import riscvsingle.ifu.IFU

/** 独立生成 IFU；参数依次为字深度、输出目录、镜像文件（或 -）、复位字节地址。 */
object GenerateIFU extends App {
  require(args.length <= 4,
    "Usage: GenerateIFU [imemDepth] [targetDir] [instructionInitFile|-] [resetVector]")
  val depth = args.headOption.map(_.toInt).getOrElse(64)
  val targetDir = args.lift(1).getOrElse("generated/ifu")
  val file = args.lift(2).getOrElse("programs/riscvtest.memfile")
  val resetVector = args.lift(3).map { value =>
    if (value.startsWith("0x") || value.startsWith("0X")) BigInt(value.drop(2), 16)
    else BigInt(value)
  }.getOrElse(BigInt(0))
  val config = CpuConfig(imemDepth = depth, resetVector = resetVector,
    instructionInitFile = if (file == "-") None else Some(file))
  (new ChiselStage).emitVerilog(
    new IFU(config),
    Array("--target-dir", targetDir)
  )
}
