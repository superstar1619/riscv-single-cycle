package riscvsingle.ieu

import chisel3._
import chisel3.stage.ChiselStage
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec
import java.nio.file.Files
import scala.util.Random

// chiseltest 0.6.2 requires a Module test top. Only this test wrapper has
// implicit clock/reset ports; the production Extend is a RawModule.
private class ExtendHarness(outputWidth: Int) extends Module {
  val io = IO(new ExtendIO(outputWidth))
  val dut = Module(new Extend(outputWidth))
  dut.io.Instr := io.Instr
  dut.io.ImmSrc := io.ImmSrc
  io.ImmExt := dut.io.ImmExt
}

class ExtendSpec extends AnyFlatSpec with ChiselScalatestTester {
  private val mask32 = (BigInt(1) << 32) - 1

  // Maps immediate bit positions to architectural instruction positions.
  // This bit-by-bit software decoder is independent of the RTL concatenations.
  private val positions = Seq(
    (20 to 31).toVector,
    ((7 to 11) ++ (25 to 31)).toVector,
    (Seq(-1) ++ (8 to 11) ++ (25 to 30) ++ Seq(7, 31)).toVector,
    (Seq(-1) ++ (21 to 30) ++ Seq(20) ++ (12 to 19) ++ Seq(31)).toVector,
    (Seq.fill(12)(-1) ++ (12 to 31)).toVector
  )

  private def decodeSigned(instruction: BigInt, selector: Int): BigInt = {
    if (selector >= positions.length) return BigInt(0)
    val mapping = positions(selector)
    val raw = mapping.zipWithIndex.foldLeft(BigInt(0)) { case (value, (source, destination)) =>
      if (source >= 0 && instruction.testBit(source)) value.setBit(destination) else value
    }
    if (raw.testBit(mapping.length - 1)) raw - (BigInt(1) << mapping.length) else raw
  }

  private def encodeImmediate(value: BigInt, selector: Int): BigInt = {
    val mapping = positions(selector)
    val raw = value & ((BigInt(1) << mapping.length) - 1)
    mapping.zipWithIndex.foldLeft(BigInt(0)) { case (instruction, (destination, source)) =>
      if (destination >= 0 && raw.testBit(source)) instruction.setBit(destination) else instruction
    }
  }

  private def expectImmediate(dut: ExtendHarness, instruction: BigInt, selector: Int,
      expected: BigInt, outputWidth: Int): Unit = {
    dut.io.Instr.poke((instruction >> 7).U)
    dut.io.ImmSrc.poke(selector.U)
    dut.io.ImmExt.expect((expected & ((BigInt(1) << outputWidth) - 1)).U)
  }

  behavior of "Extend"

  for (outputWidth <- Seq(32, 64)) {
    it should s"extend I/S/B/J/U boundaries and every immediate bit to $outputWidth bits" in {
      test(new ExtendHarness(outputWidth)) { dut =>
        for (selector <- 0 until 5) {
          val mapping = positions(selector)
          val halfRange = BigInt(1) << (mapping.length - 1)
          val step = if (selector == 4) BigInt(4096)
            else if (selector >= 2) BigInt(2) else BigInt(1)
          val boundaries = Seq(BigInt(0), step, -step, halfRange - step, -halfRange)
          val walkingBits = mapping.indices.filter(mapping(_) >= 0).map { bit =>
            if (bit == mapping.length - 1) -halfRange else BigInt(1) << bit
          }
          for (value <- boundaries ++ walkingBits) {
            expectImmediate(dut, encodeImmediate(value, selector), selector, value, outputWidth)
          }

          // Bits outside this format must not affect the selected immediate.
          val usedMask = mapping.filter(_ >= 0).foldLeft(BigInt(0))(_.setBit(_))
          val unusedMask = mask32 ^ usedMask
          for (value <- boundaries) {
            expectImmediate(dut, encodeImmediate(value, selector) | unusedMask,
              selector, value, outputWidth)
          }
        }
      }
    }

    it should s"map or ignore every architectural input bit for I/S/B/J/U at $outputWidth bits" in {
      test(new ExtendHarness(outputWidth)) { dut =>
        for (selector <- 0 until 5; bit <- 0 until 32) {
          // One-hot and one-cold inputs isolate each source bit, including
          // ignored instruction fields and the absent opcode bits [6:0].
          for (instruction <- Seq(BigInt(1) << bit, mask32 ^ (BigInt(1) << bit))) {
            expectImmediate(dut, instruction, selector,
              decodeSigned(instruction, selector), outputWidth)
          }
        }
      }
    }

    it should s"preserve U-format signed boundaries and twelve low zero bits at $outputWidth bits" in {
      test(new ExtendHarness(outputWidth)) { dut =>
        val cases = Seq(
          BigInt("00000000", 16) -> BigInt(0),
          BigInt("00001000", 16) -> BigInt(4096),
          BigInt("7ffff000", 16) -> BigInt(2147479552L),
          BigInt("80000000", 16) -> BigInt(-2147483648L),
          BigInt("80001000", 16) -> BigInt(-2147479552L),
          BigInt("fffff000", 16) -> BigInt(-4096)
        )
        for ((instruction, expected) <- cases; lowBits <- Seq(BigInt(0), BigInt(0xfff))) {
          expectImmediate(dut, instruction | lowBits, 4, expected, outputWidth)
        }
      }
    }

    it should s"return zero for unsupported selectors 5/6/7 at $outputWidth bits" in {
      test(new ExtendHarness(outputWidth)) { dut =>
        for (selector <- 5 until 8;
            instruction <- Seq(BigInt(0), mask32, BigInt("80000000", 16),
              BigInt("12345678", 16))) {
          expectImmediate(dut, instruction, selector, BigInt(0), outputWidth)
        }
      }
    }

    it should s"match independent decoding of 1000 random instructions at $outputWidth bits" in {
      test(new ExtendHarness(outputWidth)) { dut =>
        val random = new Random(0x215L)
        for (_ <- 0 until 1000) {
          val instruction = BigInt(32, random)
          for (selector <- 0 until 8) {
            expectImmediate(dut, instruction, selector,
              decodeSigned(instruction, selector), outputWidth)
          }
        }
      }
    }
  }

  for (outputWidth <- Seq(32, 64)) {
    it should s"elaborate exactly the three combinational book ports at $outputWidth bits" in {
      val targetDir = Files.createTempDirectory("extend-interface-")
      val verilog = (new ChiselStage).emitVerilog(new Extend(outputWidth),
        Array("--target-dir", targetDir.toString))
      val portPattern = "(?m)^\\s*(input|output)\\s+(?:\\[(\\d+):0\\]\\s+)?(\\w+)".r
      val ports = portPattern.findAllMatchIn(verilog).map { port =>
        val width = Option(port.group(2)).map(_.toInt + 1).getOrElse(1)
        port.group(3) -> (port.group(1), width)
      }.toMap
      assert(ports == Map("io_Instr" -> ("input", 25), "io_ImmSrc" -> ("input", 3),
        "io_ImmExt" -> ("output", outputWidth)))
      assert(!verilog.contains("always @"))
    }
  }

  it should "reject output widths below 32 at elaboration" in {
    for (outputWidth <- Seq(-1, 0, 12, 31)) {
      val error = intercept[IllegalArgumentException] {
        (new ChiselStage).emitChirrtl(new Extend(outputWidth))
      }
      assert(error.getMessage.contains("Extend outputWidth must be >= 32"))
    }
  }
}
