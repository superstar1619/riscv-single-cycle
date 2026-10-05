package riscvsingle.lsu

import chisel3._
import chisel3.stage.ChiselStage
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec
import java.nio.file.Files

// The production module is combinational; only chiseltest needs a Module wrapper.
private class SwByteMaskHarness(dataWidth: Int) extends Module {
  val io = IO(new SwByteMaskIO(dataWidth))
  val dut = Module(new SwByteMask(dataWidth))
  dut.io.Funct3 := io.Funct3
  dut.io.ByteOffset := io.ByteOffset
  io.ByteMask := dut.io.ByteMask
}

class SwByteMaskSpec extends AnyFlatSpec with ChiselScalatestTester {
  // Use byte membership rather than the hardware's shifted base mask.
  private def reference(dataWidth: Int, funct3: Int, offset: Int): BigInt = {
    val size = Map(0 -> 1, 1 -> 2, 2 -> 4, 3 -> 8).getOrElse(funct3, 0)
    if (size == 0 || size > dataWidth / 8 || offset % size != 0) BigInt(0)
    else (0 until dataWidth / 8).foldLeft(BigInt(0)) { (mask, lane) =>
      if (lane >= offset && lane < offset + size) mask.setBit(lane) else mask
    }
  }

  private def expectMask(
    dut: SwByteMaskHarness,
    funct3: Int,
    offset: Int,
    expected: BigInt
  ): Unit = {
    dut.io.Funct3.poke(funct3.U)
    dut.io.ByteOffset.poke(offset.U)
    dut.io.ByteMask.expect(expected.U)
  }

  behavior of "SwByteMask"

  for (dataWidth <- Seq(32, 64)) {
    it should s"select every naturally aligned store lane at $dataWidth bits" in {
      test(new SwByteMaskHarness(dataWidth)) { dut =>
        val cases = if (dataWidth == 32) Seq(
          (0, 0, 0x1), (0, 1, 0x2), (0, 2, 0x4), (0, 3, 0x8),
          (1, 0, 0x3), (1, 2, 0xc), (2, 0, 0xf)
        ) else Seq(
          (0, 0, 0x01), (0, 1, 0x02), (0, 2, 0x04), (0, 3, 0x08),
          (0, 4, 0x10), (0, 5, 0x20), (0, 6, 0x40), (0, 7, 0x80),
          (1, 0, 0x03), (1, 2, 0x0c), (1, 4, 0x30), (1, 6, 0xc0),
          (2, 0, 0x0f), (2, 4, 0xf0), (3, 0, 0xff)
        )
        for ((funct3, offset, expected) <- cases) {
          expectMask(dut, funct3, offset, BigInt(expected))
        }
      }
    }

    it should (s"match all function and offset combinations in both orders at " +
      s"$dataWidth bits") in {
      test(new SwByteMaskHarness(dataWidth)) { dut =>
        val cases = for (funct3 <- 0 until 8; offset <- 0 until dataWidth / 8)
          yield (funct3, offset)
        // Includes high Funct3 bits, overlarge accesses, and all misalignments.
        for ((funct3, offset) <- cases ++ cases.reverse) {
          expectMask(dut, funct3, offset, reference(dataWidth, funct3, offset))
        }
      }
    }

    it should s"clear stale masks and recover without a clock edge at $dataWidth bits" in {
      test(new SwByteMaskHarness(dataWidth)) { dut =>
        expectMask(dut, 2, 0, BigInt(0x0f))
        expectMask(dut, 6, 0, BigInt(0)) // Full Funct3, not just its low two bits.
        expectMask(dut, 1, 0, BigInt(3))
        expectMask(dut, 1, 1, BigInt(0)) // Same size, invalid alignment.
        expectMask(dut, 0, 1, BigInt(2))
        expectMask(dut, 2, dataWidth / 8 - 1, BigInt(0)) // Would cross a word.
      }
    }
  }

  it should "emit exactly three combinational ports at 32 and 64 bits" in {
    for (dataWidth <- Seq(32, 64)) {
      val targetDir = Files.createTempDirectory("swbytemask-interface-")
      val verilog = (new ChiselStage).emitVerilog(new SwByteMask(dataWidth),
        Array("--target-dir", targetDir.toString))
      val portPattern = "(?m)^\\s*(input|output)\\s+(?:\\[(\\d+):0\\]\\s+)?(\\w+)".r
      val ports = portPattern.findAllMatchIn(verilog).map { port =>
        val width = Option(port.group(2)).map(_.toInt + 1).getOrElse(1)
        port.group(3) -> (port.group(1), width)
      }.toMap
      assert(ports == Map("io_Funct3" -> ("input", 3),
        "io_ByteOffset" -> ("input", chisel3.util.log2Ceil(dataWidth / 8)),
        "io_ByteMask" -> ("output", dataWidth / 8)))
      assert(!verilog.contains("always @"))
      assert(!verilog.contains("reg "))
    }
  }

  it should "reject unsupported native widths during elaboration" in {
    for (dataWidth <- Seq(-1, 0, 1, 8, 16, 31, 33, 63, 65, 128)) {
      val error = intercept[IllegalArgumentException] {
        (new ChiselStage).emitChirrtl(new SwByteMask(dataWidth))
      }
      assert(error.getMessage.contains("SwByteMask dataWidth must be 32 or 64"))
    }
  }
}
