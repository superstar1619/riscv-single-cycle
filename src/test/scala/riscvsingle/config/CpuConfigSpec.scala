package riscvsingle.config

import org.scalatest.flatspec.AnyFlatSpec

class CpuConfigSpec extends AnyFlatSpec {
  behavior of "CpuConfig"

  it should "use the book defaults and accept supported structural settings" in {
    val defaults = CpuConfig()
    assert(defaults.xlen == 32)
    assert(defaults.imemDepth == 64 && defaults.dmemDepth == 64)
    assert(defaults.resetVector == 0 && defaults.instructionInitFile.isEmpty)
    val configured = CpuConfig(imemDepth = 128, dmemDepth = 256,
      resetVector = BigInt("80000000", 16), instructionInitFile = Some("program.mem"))
    assert(configured.imemDepth == 128 && configured.dmemDepth == 256)
    assert(configured.resetVector == BigInt("80000000", 16))
    assert(configured.instructionInitFile.contains("program.mem"))
  }

  it should "reject unsupported widths and invalid memory, address, or file settings" in {
    for (xlen <- Seq(16, 64)) {
      intercept[IllegalArgumentException] { CpuConfig(xlen = xlen) }
    }
    for (depth <- Seq(-1, 0, 1, 3, Int.MaxValue)) {
      intercept[IllegalArgumentException] { CpuConfig(imemDepth = depth) }
      intercept[IllegalArgumentException] { CpuConfig(dmemDepth = depth) }
    }
    for (address <- Seq(BigInt(-4), BigInt(2), BigInt(1) << 32)) {
      intercept[IllegalArgumentException] { CpuConfig(resetVector = address) }
    }
    intercept[IllegalArgumentException] { CpuConfig(instructionInitFile = Some("  ")) }
  }
}
