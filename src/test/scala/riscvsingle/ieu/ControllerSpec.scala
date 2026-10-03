package riscvsingle.ieu

import chisel3._
import chisel3.stage.ChiselStage
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec
import java.nio.file.Files

private class ControllerHarness extends Module {
  val io = IO(new ControllerIO)
  val dut = Module(new Controller)
  dut.io.Op := io.Op
  dut.io.Eq := io.Eq
  dut.io.Funct3 := io.Funct3
  dut.io.Funct7b5 := io.Funct7b5
  io.ALUResultSrc := dut.io.ALUResultSrc
  io.ResultSrc := dut.io.ResultSrc
  io.MemWrite := dut.io.MemWrite
  io.PCSrc := dut.io.PCSrc
  io.RegWrite := dut.io.RegWrite
  io.ALUSrc := dut.io.ALUSrc
  io.ImmSrc := dut.io.ImmSrc
  io.ALUControl := dut.io.ALUControl
}

private class ControllerALUTestIO extends Bundle {
  val Op = Input(UInt(7.W))
  val Funct3 = Input(UInt(3.W))
  val Funct7b5 = Input(Bool())
  val SrcA = Input(UInt(32.W))
  val SrcB = Input(UInt(32.W))
  val ALUResult = Output(UInt(32.W))
  val IEUAdr = Output(UInt(32.W))
}

// Test-only connection to the previously reviewed ALU, not a Datapath module.
private class ControllerALUHarness extends Module {
  val io = IO(new ControllerALUTestIO)
  val controller = Module(new Controller)
  val alu = Module(new ALU)
  controller.io.Op := io.Op
  controller.io.Funct3 := io.Funct3
  controller.io.Funct7b5 := io.Funct7b5
  controller.io.Eq := false.B
  alu.io.SrcA := io.SrcA
  alu.io.SrcB := io.SrcB
  alu.io.ALUControl := controller.io.ALUControl
  alu.io.Funct3 := io.Funct3
  io.ALUResult := alu.io.ALUResult
  io.IEUAdr := alu.io.IEUAdr
}

class ControllerSpec extends AnyFlatSpec with ChiselScalatestTester {
  // Behavioral fields from book Table 2.4; this reference does not unpack
  // or reuse the production controller's packed control constants.
  private case class MainControls(regWrite: Boolean = false, immSrc: Int = 0,
      aluSrc: Int = 0, aluOp: Boolean = false, aluResultSrc: Boolean = false,
      memWrite: Boolean = false, resultSrc: Boolean = false)

  private val mainTable = Map(
    0x03 -> MainControls(regWrite = true, aluSrc = 1, resultSrc = true),
    0x23 -> MainControls(immSrc = 1, aluSrc = 1, memWrite = true),
    0x33 -> MainControls(regWrite = true, aluOp = true),
    0x13 -> MainControls(regWrite = true, aluSrc = 1, aluOp = true),
    0x63 -> MainControls(immSrc = 2, aluSrc = 3),
    0x6f -> MainControls(regWrite = true, immSrc = 3, aluSrc = 3, aluResultSrc = true)
  )

  behavior of "Controller"

  it should "match every binary opcode, function, function-bit, and equality combination" in {
    test(new ControllerHarness) { dut =>
      for (op <- 0 until 128; funct3 <- 0 until 8;
          funct7b5 <- Seq(false, true); eq <- Seq(false, true)) {
        val expected = mainTable.getOrElse(op, MainControls())
        val control = if (!expected.aluOp) 0 else funct3 match {
          case 2 => 3 // Signed slt/slti always subtract.
          case 0 if op == 0x33 && funct7b5 => 3 // Only R-type subtract uses bit 30.
          case _ => 1
        }
        val pcSrc = if (op == 0x63) eq else op == 0x6f
        dut.io.Op.poke(op.U)
        dut.io.Funct3.poke(funct3.U)
        dut.io.Funct7b5.poke(funct7b5.B)
        dut.io.Eq.poke(eq.B)
        dut.io.RegWrite.expect(expected.regWrite.B)
        dut.io.ImmSrc.expect(expected.immSrc.U)
        dut.io.ALUSrc.expect(expected.aluSrc.U)
        dut.io.ALUResultSrc.expect(expected.aluResultSrc.B)
        dut.io.MemWrite.expect(expected.memWrite.B)
        dut.io.ResultSrc.expect(expected.resultSrc.B)
        dut.io.ALUControl.expect(control.U)
        dut.io.PCSrc.expect(pcSrc.B)
      }
    }
  }

  it should "drive the reviewed ALU for arithmetic, logic, and address generation" in {
    test(new ControllerALUHarness) { dut =>
      // op, funct3, bit30, a, b, ALUResult, IEUAdr
      val vectors = Seq(
        (0x33, 0, false, 12L, 7L, 19L, 19L), // add
        (0x33, 0, true, 12L, 7L, 5L, 5L), // sub
        (0x13, 0, true, 12L, 0xfffffff7L, 3L, 3L), // addi -9: bit30 must not select sub
        (0x33, 2, false, 0x80000000L, 1L, 1L, 0x7fffffffL), // slt with overflow
        (0x13, 2, true, 7L, 0xffffffffL, 0L, 8L), // slti: 7 < -1 is false
        (0x33, 6, false, 12L, 7L, 15L, 19L), // or
        (0x33, 7, false, 12L, 7L, 4L, 19L), // and
        (0x03, 2, true, 20L, 12L, 32L, 32L), // lw
        (0x23, 2, true, 20L, 12L, 32L, 32L), // sw
        (0x63, 0, false, 32L, 8L, 40L, 40L), // beq target
        (0x6f, 6, true, 32L, 8L, 40L, 40L) // jal target, unrelated funct bits ignored
      )
      for ((op, funct3, bit30, a, b, result, address) <- vectors) {
        dut.io.Op.poke(op.U)
        dut.io.Funct3.poke(funct3.U)
        dut.io.Funct7b5.poke(bit30.B)
        dut.io.SrcA.poke(BigInt(a).U)
        dut.io.SrcB.poke(BigInt(b).U)
        dut.io.ALUResult.expect(BigInt(result).U)
        dut.io.IEUAdr.expect(BigInt(address).U)
      }
    }
  }

  it should "elaborate exactly the twelve combinational book ports" in {
    val targetDir = Files.createTempDirectory("controller-interface-")
    val verilog = (new ChiselStage).emitVerilog(new Controller,
      Array("--target-dir", targetDir.toString))
    val portPattern = "(?m)^\\s*(input|output)\\s+(?:\\[(\\d+):0\\]\\s+)?(\\w+)".r
    val ports = portPattern.findAllMatchIn(verilog).map { port =>
      val width = Option(port.group(2)).map(_.toInt + 1).getOrElse(1)
      port.group(3) -> (port.group(1), width)
    }.toMap
    assert(ports == Map("io_Op" -> ("input", 7), "io_Eq" -> ("input", 1),
      "io_Funct3" -> ("input", 3), "io_Funct7b5" -> ("input", 1),
      "io_ALUResultSrc" -> ("output", 1), "io_ResultSrc" -> ("output", 1),
      "io_MemWrite" -> ("output", 1), "io_PCSrc" -> ("output", 1),
      "io_RegWrite" -> ("output", 1), "io_ALUSrc" -> ("output", 2),
      "io_ImmSrc" -> ("output", 2), "io_ALUControl" -> ("output", 2)))
    assert(!verilog.contains("always @"))
  }
}
