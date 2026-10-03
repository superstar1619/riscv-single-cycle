package riscvsingle.ieu

import chisel3._
import chisel3.stage.ChiselStage
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec
import java.nio.file.Files
import scala.util.Random

// Only this test wrapper has clock/reset; the production ALU is combinational.
private class ALUHarness(dataWidth: Int) extends Module {
  val io = IO(new ALUIO(dataWidth))
  val dut = Module(new ALU(dataWidth))
  dut.io.SrcA := io.SrcA
  dut.io.SrcB := io.SrcB
  dut.io.ALUControl := io.ALUControl
  dut.io.Funct3 := io.Funct3
  io.ALUResult := dut.io.ALUResult
  io.IEUAdr := dut.io.IEUAdr
}

class ALUSpec extends AnyFlatSpec with ChiselScalatestTester {
  // 16 forced-arithmetic settings, plus 7 supported ALU-mode settings.
  // Sub=1/ALUOp=0 and Sub=1 for OR/AND also check that the two outputs
  // retain their independent roles, even for settings unused by the controller.
  private val cases = (for (control <- Seq(0, 2); funct3 <- 0 until 8)
    yield (control, funct3)) ++ Seq((1, 0), (3, 0), (3, 2), (1, 6), (1, 7), (3, 6), (3, 7))

  private def signed(value: BigInt, dataWidth: Int): BigInt =
    if (value.testBit(dataWidth - 1)) value - (BigInt(1) << dataWidth) else value

  private def check(dut: ALUHarness, dataWidth: Int, a: BigInt, b: BigInt,
      control: Int, funct3: Int): Unit = {
    val mask = (BigInt(1) << dataWidth) - 1
    val subtract = (control & 2) != 0
    val address = (if (subtract) a - b else a + b) & mask
    val function = if ((control & 1) != 0) funct3 else 0
    // Independent arithmetic model: subtraction uses BigInt subtraction,
    // and signed slt directly compares signed numbers, without RTL overflow logic.
    val result = function match {
      case 0 => address
      case 2 =>
        require(subtract, "Signed slt requires Sub=1, matching the book's controller")
        if (signed(a, dataWidth) < signed(b, dataWidth)) BigInt(1) else BigInt(0)
      case 6 => a | b
      case 7 => a & b
      case _ => BigInt(0)
    }
    dut.io.SrcA.poke(a.U)
    dut.io.SrcB.poke(b.U)
    dut.io.ALUControl.poke(control.U)
    dut.io.Funct3.poke(funct3.U)
    dut.io.ALUResult.expect(result.U)
    dut.io.IEUAdr.expect(address.U)
  }

  behavior of "ALU"

  for (dataWidth <- Seq(32, 64)) {
    it should s"handle wraparound and signed overflow boundaries at $dataWidth bits" in {
      test(new ALUHarness(dataWidth)) { dut =>
        val mask = (BigInt(1) << dataWidth) - 1
        val signBit = BigInt(1) << (dataWidth - 1)
        val alternating = (0 until dataWidth by 2).foldLeft(BigInt(0))(_.setBit(_))
        val patterns = Seq(BigInt(0), BigInt(1), mask, signBit,
          signBit - 1, signBit + 1, alternating)
        for (a <- patterns; b <- patterns; (control, funct3) <- cases) {
          check(dut, dataWidth, a, b, control, funct3)
        }
      }
    }

    it should s"match independent arithmetic and both outputs on random $dataWidth-bit operands" in {
      test(new ALUHarness(dataWidth)) { dut =>
        val random = new Random(0x215A1L + dataWidth)
        for (_ <- 0 until 500) {
          val a = BigInt(dataWidth, random)
          val b = BigInt(dataWidth, random)
          for ((control, funct3) <- cases) {
            check(dut, dataWidth, a, b, control, funct3)
          }
        }
      }
    }

    it should s"return zero for unsupported ALU functions while preserving IEUAdr at $dataWidth bits" in {
      test(new ALUHarness(dataWidth)) { dut =>
        val a = (BigInt(1) << (dataWidth - 1)) + 1
        val b = (BigInt(1) << dataWidth) - 1
        for (control <- Seq(1, 3); funct3 <- Seq(1, 3, 4, 5)) {
          check(dut, dataWidth, a, b, control, funct3)
        }
      }
    }
  }

  for (dataWidth <- Seq(1, 4)) {
    it should s"match all operand pairs for the supported control cases at $dataWidth bits" in {
      test(new ALUHarness(dataWidth)) { dut =>
        for (a <- 0 until (1 << dataWidth); b <- 0 until (1 << dataWidth);
            (control, funct3) <- cases) {
          check(dut, dataWidth, BigInt(a), BigInt(b), control, funct3)
        }
      }
    }
  }

  it should "elaborate exactly the six combinational book ports at 32 and 64 bits" in {
    for (dataWidth <- Seq(32, 64)) {
      val targetDir = Files.createTempDirectory("alu-interface-")
      val verilog = (new ChiselStage).emitVerilog(new ALU(dataWidth),
        Array("--target-dir", targetDir.toString))
      val portPattern = "(?m)^\\s*(input|output)\\s+(?:\\[(\\d+):0\\]\\s+)?(\\w+)".r
      val ports = portPattern.findAllMatchIn(verilog).map { port =>
        val width = Option(port.group(2)).map(_.toInt + 1).getOrElse(1)
        port.group(3) -> (port.group(1), width)
      }.toMap
      assert(ports == Map("io_SrcA" -> ("input", dataWidth),
        "io_SrcB" -> ("input", dataWidth), "io_ALUControl" -> ("input", 2),
        "io_Funct3" -> ("input", 3), "io_ALUResult" -> ("output", dataWidth),
        "io_IEUAdr" -> ("output", dataWidth)))
      assert(!verilog.contains("always @"))
    }
  }

  it should "reject nonpositive operand widths at elaboration" in {
    for (dataWidth <- Seq(-1, 0)) {
      val error = intercept[IllegalArgumentException] {
        (new ChiselStage).emitChirrtl(new ALU(dataWidth))
      }
      assert(error.getMessage.contains("ALU dataWidth must be > 0"))
    }
  }
}
