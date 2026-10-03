package riscvsingle.ieu

import chisel3._
import chisel3.stage.ChiselStage
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec
import java.nio.file.Files
import scala.util.Random

// chiseltest 0.6.2 requires a Module top. Only the test wrapper has clock/reset.
private class CmpHarness(dataWidth: Int) extends Module {
  val io = IO(new CmpIO(dataWidth))
  val dut = Module(new Cmp(dataWidth))
  dut.io.R1 := io.R1
  dut.io.R2 := io.R2
  io.Eq := dut.io.Eq
}

class CmpSpec extends AnyFlatSpec with ChiselScalatestTester {
  private def expectEqual(dut: CmpHarness, r1: BigInt, r2: BigInt): Unit = {
    dut.io.R1.poke(r1.U)
    dut.io.R2.poke(r2.U)
    dut.io.Eq.expect((r1 == r2).B)
  }

  behavior of "Cmp"

  for (dataWidth <- Seq(32, 64)) {
    it should s"compare boundary patterns and detect a difference in every bit at $dataWidth bits" in {
      test(new CmpHarness(dataWidth)) { dut =>
        val mask = (BigInt(1) << dataWidth) - 1
        val signBit = BigInt(1) << (dataWidth - 1)
        val alternating = (0 until dataWidth by 2).foldLeft(BigInt(0))(_.setBit(_))
        val patterns = Seq(BigInt(0), mask, BigInt(1), signBit, mask ^ signBit, alternating)
        for (r1 <- patterns; r2 <- patterns) {
          expectEqual(dut, r1, r2)
        }
        for (bit <- 0 until dataWidth) {
          // Check differences against both all-zero and all-one operands.
          expectEqual(dut, BigInt(0), BigInt(1) << bit)
          expectEqual(dut, mask ^ (BigInt(1) << bit), mask)
        }
      }
    }

    it should s"compare equal, random, and one-bit-different operands at $dataWidth bits" in {
      test(new CmpHarness(dataWidth)) { dut =>
        val random = new Random(0x215C0L + dataWidth)
        for (_ <- 0 until 1000) {
          val r1 = BigInt(dataWidth, random)
          val r2 = BigInt(dataWidth, random)
          expectEqual(dut, r1, r1)
          expectEqual(dut, r1, r2)
          expectEqual(dut, r1, r1 ^ (BigInt(1) << random.nextInt(dataWidth)))
        }
      }
    }
  }

  for (dataWidth <- Seq(1, 4)) {
    it should s"compare all operand pairs at $dataWidth bits" in {
      test(new CmpHarness(dataWidth)) { dut =>
        for (r1 <- 0 until (1 << dataWidth); r2 <- 0 until (1 << dataWidth)) {
          expectEqual(dut, BigInt(r1), BigInt(r2))
        }
      }
    }
  }

  it should "elaborate only the three combinational book ports at 32 and 64 bits" in {
    for (dataWidth <- Seq(32, 64)) {
      val targetDir = Files.createTempDirectory("cmp-interface-")
      val verilog = (new ChiselStage).emitVerilog(new Cmp(dataWidth),
        Array("--target-dir", targetDir.toString))
      val portPattern = "(?m)^\\s*(input|output)\\s+(?:\\[(\\d+):0\\]\\s+)?(\\w+)".r
      val ports = portPattern.findAllMatchIn(verilog).map { port =>
        val width = Option(port.group(2)).map(_.toInt + 1).getOrElse(1)
        port.group(3) -> (port.group(1), width)
      }.toMap
      assert(ports == Map("io_R1" -> ("input", dataWidth),
        "io_R2" -> ("input", dataWidth), "io_Eq" -> ("output", 1)))
      assert(!verilog.contains("always @"))
    }
  }

  it should "reject nonpositive operand widths at elaboration" in {
    for (dataWidth <- Seq(-1, 0)) {
      val error = intercept[IllegalArgumentException] {
        (new ChiselStage).emitChirrtl(new Cmp(dataWidth))
      }
      assert(error.getMessage.contains("Cmp dataWidth must be > 0"))
    }
  }
}
