package riscvsingle.config

/** Shared elaboration-time configuration for the book's RV32 single-cycle CPU.
  * Memory depths are counts of 32-bit words; resetVector is a byte address.
  * These settings are consumed as the corresponding modules are introduced.
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

  require(validDepth(imemDepth), "imemDepth must be a power of two >= 2 fitting the address space")
  require(validDepth(dmemDepth), "dmemDepth must be a power of two >= 2 fitting the address space")
  require(resetVector >= 0 && resetVector < (BigInt(1) << xlen), "resetVector must fit XLEN")
  require(resetVector % 4 == 0, "resetVector must be aligned to a 4-byte instruction")
  require(instructionInitFile.forall(_.trim.nonEmpty), "instructionInitFile must not be blank")
}
