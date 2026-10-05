package riscvsingle.config

/** 教材 RV32 单周期 CPU 的不可变生成阶段配置，由顶层传递给各单元。
  * imemDepth/dmemDepth 以 32 位字计，resetVector 是字节地址；仅支持 XLEN=32。
  * 配置只决定生成结构、初始镜像与复位值，不是硬件运行时信号。
  */
final case class CpuConfig(
  xlen: Int = 32,
  imemDepth: Int = 64,
  dmemDepth: Int = 64,
  resetVector: BigInt = 0,
  instructionInitFile: Option[String] = None
) {
  require(xlen == 32, "The first CPU implementation supports only XLEN=32")

  private def validDepth(depth: Int): Boolean =
    depth >= 2 && (depth & (depth - 1)) == 0 && BigInt(depth) * 4 <= (BigInt(1) << xlen)

  require(
    validDepth(imemDepth),
    "imemDepth must be a power of two >= 2 fitting the address space"
  )
  require(
    validDepth(dmemDepth),
    "dmemDepth must be a power of two >= 2 fitting the address space"
  )
  require(resetVector >= 0 && resetVector < (BigInt(1) << xlen), "resetVector must fit XLEN")
  require(resetVector % 4 == 0, "resetVector must be aligned to a 4-byte instruction")
  require(instructionInitFile.forall(_.trim.nonEmpty), "instructionInitFile must not be blank")
}
