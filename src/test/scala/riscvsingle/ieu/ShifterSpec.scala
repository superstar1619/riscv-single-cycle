package riscvsingle.ieu

import chisel3._
import chisel3.stage.ChiselStage
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec
import java.nio.file.Files
import scala.util.Random

// Only the test wrapper has the Module clock/reset required by chiseltest.
private class ShifterHarness(dataWidth: Int) extends Module {
  val io = IO(new ShifterIO(dataWidth))
  val dut = Module(new Shifter(dataWidth))
  dut.io.A := io.A
  dut.io.Amt := io.Amt
  dut.io.Right := io.Right
  dut.io.SubArith := io.SubArith
  io.Y := dut.io.Y
}

class ShifterSpec extends AnyFlatSpec with ChiselScalatestTester {
  // Ordinary software shifts form an oracle independent of the funnel's
  // source concatenation and complemented right-shift offset.
  private def reference(
    value: BigInt,
    amount: Int,
    right: Boolean,
    arithmetic: Boolean,
    dataWidth: Int
  ): BigInt = {
    val signed = if (value.testBit(dataWidth - 1)) value - (BigInt(1) << dataWidth) else value
    val shifted = if (!right) value << amount
      else if (arithmetic) signed >> amount else value >> amount
    shifted & ((BigInt(1) << dataWidth) - 1)
  }

  private def expectShift(
    dut: ShifterHarness,
    value: BigInt,
    amount: Int,
    right: Boolean,
    arithmetic: Boolean,
    expected: BigInt
  ): Unit = {
    dut.io.A.poke(value.U)
    dut.io.Amt.poke(amount.U)
    dut.io.Right.poke(right.B)
    dut.io.SubArith.poke(arithmetic.B)
    dut.io.Y.expect(expected.U)
  }

  behavior of "Shifter"

  for (dataWidth <- Seq(32, 64)) {
    it should (s"shift boundary patterns by every amount in all four control modes at " +
      s"$dataWidth bits") in {
      test(new ShifterHarness(dataWidth)) { dut =>
        val mask = (BigInt(1) << dataWidth) - 1
        val signBit = BigInt(1) << (dataWidth - 1)
        val alternating = (0 until dataWidth by 2).foldLeft(BigInt(0))(_.setBit(_))
        val patterns = Seq(BigInt(0), mask, BigInt(1), signBit, signBit - 1,
          signBit | 1, alternating, mask ^ alternating)
        for (value <- patterns; amount <- 0 until dataWidth;
            right <- Seq(false, true); arithmetic <- Seq(false, true)) {
          expectShift(dut, value, amount, right, arithmetic,
            reference(value, amount, right, arithmetic, dataWidth))
        }
      }
    }

    it should (s"preserve zero shifts and handle maximum amounts and sign extension at " +
      s"$dataWidth bits") in {
      test(new ShifterHarness(dataWidth)) { dut =>
        val mask = (BigInt(1) << dataWidth) - 1
        val signBit = BigInt(1) << (dataWidth - 1)
        for (right <- Seq(false, true); arithmetic <- Seq(false, true)) {
          expectShift(dut, signBit | 1, 0, right, arithmetic, signBit | 1)
        }
        // Hand-checked results for truncation and sign extension, including
        // the signed minimum, signed maximum, and negative odd operands.
        val maximumCases = Seq(
          (BigInt(1), false, false, signBit),
          (BigInt(1), false, true, signBit),
          (mask, false, false, signBit),
          (mask, false, true, signBit),
          (signBit, false, false, BigInt(0)),
          (signBit, false, true, BigInt(0)),
          (signBit, true, false, BigInt(1)),
          (signBit, true, true, mask),
          (mask, true, false, BigInt(1)),
          (mask, true, true, mask),
          (signBit - 1, true, false, BigInt(0)),
          (signBit - 1, true, true, BigInt(0))
        )
        for ((value, right, arithmetic, expected) <- maximumCases) {
          expectShift(dut, value, dataWidth - 1, right, arithmetic, expected)
        }
        expectShift(dut, signBit | 1, 1, true, false, signBit >> 1)
        expectShift(dut, signBit | 1, 1, true, true, signBit | (signBit >> 1))
        expectShift(dut, signBit | 1, 1, false, false, BigInt(2))
        expectShift(dut, signBit | 1, 1, false, true, BigInt(2))
      }
    }

    it should (s"match an independent model for 1000 seeded random inputs at " +
      s"$dataWidth bits") in {
      test(new ShifterHarness(dataWidth)) { dut =>
        val random = new Random(0x7133L + dataWidth)
        for (_ <- 0 until 1000) {
          val value = BigInt(dataWidth, random)
          val amount = random.nextInt(dataWidth)
          for (right <- Seq(false, true); arithmetic <- Seq(false, true)) {
            expectShift(dut, value, amount, right, arithmetic,
              reference(value, amount, right, arithmetic, dataWidth))
          }
        }
      }
    }
  }

  for (dataWidth <- Seq(2, 4)) {
    it should s"shift every operand and amount in all modes at $dataWidth bits" in {
      test(new ShifterHarness(dataWidth)) { dut =>
        for (value <- 0 until (1 << dataWidth); amount <- 0 until dataWidth;
            right <- Seq(false, true); arithmetic <- Seq(false, true)) {
          expectShift(dut, BigInt(value), amount, right, arithmetic,
            reference(BigInt(value), amount, right, arithmetic, dataWidth))
        }
      }
    }
  }

  it should "elaborate exactly five combinational ports at 32 and 64 bits" in {
    for (dataWidth <- Seq(32, 64)) {
      val targetDir = Files.createTempDirectory("shifter-interface-")
      val verilog = (new ChiselStage).emitVerilog(new Shifter(dataWidth),
        Array("--target-dir", targetDir.toString))
      val portPattern = "(?m)^\\s*(input|output)\\s+(?:\\[(\\d+):0\\]\\s+)?(\\w+)".r
      val ports = portPattern.findAllMatchIn(verilog).map { port =>
        val width = Option(port.group(2)).map(_.toInt + 1).getOrElse(1)
        port.group(3) -> (port.group(1), width)
      }.toMap
      assert(ports == Map("io_A" -> ("input", dataWidth),
        "io_Amt" -> ("input", chisel3.util.log2Ceil(dataWidth)),
        "io_Right" -> ("input", 1), "io_SubArith" -> ("input", 1),
        "io_Y" -> ("output", dataWidth)))
      assert(!verilog.contains("always @"))
    }
  }

  it should "reject widths that are not powers of two greater than or equal to two" in {
    for (dataWidth <- Seq(-1, 0, 1, 3, 31, 33, 63)) {
      val error = intercept[IllegalArgumentException] {
        (new ChiselStage).emitChirrtl(new Shifter(dataWidth))
      }
      assert(error.getMessage.contains("Shifter dataWidth must be a power of two >= 2"))
    }
  }
}
