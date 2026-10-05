package riscvsingle

import chisel3.stage.ChiselStage
import riscvsingle.config.CpuConfig

/** 生成整机；参数依次为指令/数据字深度、输出目录、镜像文件（或 -）、复位字节地址。 */
object GenerateRiscvSingle extends App {
  require(
    args.length <= 5,
    "Usage: GenerateRiscvSingle [imemDepth] [dmemDepth] [targetDir] " +
      "[instructionInitFile|-] [resetVector]"
  )
  val imemDepth = args.headOption.map(_.toInt).getOrElse(64)
  val dmemDepth = args.lift(1).map(_.toInt).getOrElse(64)
  val targetDir = args.lift(2).getOrElse("generated/riscv-single")
  val file = args.lift(3).getOrElse("programs/riscvtest.memfile")
  val resetVector = args.lift(4).map { value =>
    if (value.startsWith("0x") || value.startsWith("0X")) BigInt(value.drop(2), 16)
    else BigInt(value)
  }.getOrElse(BigInt(0))
  val config = CpuConfig(imemDepth = imemDepth, dmemDepth = dmemDepth,
    resetVector = resetVector, instructionInitFile = if (file == "-") None else Some(file))
  (new ChiselStage).emitVerilog(
    new RiscvSingle(config),
    Array("--target-dir", targetDir)
  )
}
