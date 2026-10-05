package riscvsingle.ieu

import chisel3._
import chisel3.stage.ChiselStage
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec
import java.nio.file.Files

private class ControllerHarness extends Module {
  val io = IO(new ControllerIO)
  val dut = Module(new Controller)
  dut.io <> io
}

private class ControllerALUTestIO extends Bundle {
  val Op = Input(UInt(7.W))
  val Funct3 = Input(UInt(3.W))
  val Funct7 = Input(UInt(7.W))
  val SrcA = Input(UInt(32.W))
  val SrcB = Input(UInt(32.W))
  val ALUResult = Output(UInt(32.W))
  val IEUAdr = Output(UInt(32.W))
}

private class ControllerALUHarness extends Module {
  val io = IO(new ControllerALUTestIO)
  val controller = Module(new Controller)
  val alu = Module(new ALU)
  controller.io.Op := io.Op
  controller.io.Funct3 := io.Funct3
  controller.io.Funct7 := io.Funct7
  controller.io.Eq := false.B
  controller.io.LT := false.B
  controller.io.LTU := false.B
  alu.io.SrcA := io.SrcA
  alu.io.SrcB := io.SrcB
  alu.io.ALUControl := controller.io.ALUControl
  alu.io.Funct3 := io.Funct3
  io.ALUResult := alu.io.ALUResult
  io.IEUAdr := alu.io.IEUAdr
}

class ControllerSpec extends AnyFlatSpec with ChiselScalatestTester {
  // Independent behavioral table; this reference never packs control bits.
  private case class MainControls(
    regWrite: Boolean = false,
    immSrc: Int = 0,
    aluSrc: Int = 0,
    aluOp: Boolean = false,
    aluResultSrc: Boolean = false,
    memRW: Int = 0,
    resultSrc: Boolean = false,
    branch: Boolean = false,
    jump: Boolean = false
  )
  private val mainTable = Map(
    0x33 -> MainControls(regWrite = true, aluOp = true),
    0x13 -> MainControls(regWrite = true, aluSrc = 1, aluOp = true),
    0x03 -> MainControls(regWrite = true, aluSrc = 1, memRW = 2, resultSrc = true),
    0x23 -> MainControls(immSrc = 1, aluSrc = 1, memRW = 1),
    0x63 -> MainControls(immSrc = 2, aluSrc = 3, branch = true),
    0x6f -> MainControls(regWrite = true, immSrc = 3, aluSrc = 3,
      aluResultSrc = true, jump = true),
    0x67 -> MainControls(regWrite = true, aluSrc = 1, aluResultSrc = true, jump = true),
    0x37 -> MainControls(regWrite = true, immSrc = 4, aluResultSrc = true),
    0x17 -> MainControls(regWrite = true, immSrc = 4, aluSrc = 3)
  )

  private def legal(op: Int, f3: Int, f7: Int): Boolean = op match {
    case 0x33 => f7 == 0 || (f7 == 0x20 && Set(0, 5).contains(f3))
    case 0x13 => f3 match {
      case 1 => f7 == 0
      case 5 => Set(0, 0x20).contains(f7)
      case _ => true
    }
    case 0x03 => Set(0, 1, 2, 4, 5).contains(f3)
    case 0x23 => Set(0, 1, 2).contains(f3)
    case 0x63 => Set(0, 1, 4, 5, 6, 7).contains(f3)
    case 0x67 => f3 == 0
    case 0x6f | 0x37 | 0x17 => true
    case _ => false
  }

  private def check(
    dut: ControllerHarness,
    op: Int,
    f3: Int,
    f7: Int,
    flags: Int,
    expected: MainControls
  ): Unit = {
    val eq = (flags & 1) != 0
    val lt = (flags & 2) != 0
    val ltu = (flags & 4) != 0
    val subtract = expected.aluOp && (f3 == 2 || f3 == 3 ||
      (f3 == 5 && (f7 & 32) != 0) || (op == 0x33 && f3 == 0 && (f7 & 32) != 0))
    val taken = f3 match {
      case 0 => eq
      case 1 => !eq
      case 4 => lt
      case 5 => !lt
      case 6 => ltu
      case 7 => !ltu
      case _ => false
    }
    dut.io.Op.poke(op.U)
    dut.io.Funct3.poke(f3.U)
    dut.io.Funct7.poke(f7.U)
    dut.io.Eq.poke(eq.B)
    dut.io.LT.poke(lt.B)
    dut.io.LTU.poke(ltu.B)
    dut.io.RegWrite.expect(expected.regWrite.B)
    dut.io.ImmSrc.expect(expected.immSrc.U)
    dut.io.ALUSrc.expect(expected.aluSrc.U)
    dut.io.ALUResultSrc.expect(expected.aluResultSrc.B)
    dut.io.MemRW.expect(expected.memRW.U)
    dut.io.MemWrite.expect(((expected.memRW & 1) != 0).B)
    dut.io.ResultSrc.expect(expected.resultSrc.B)
    dut.io.ALUControl.expect(((if (subtract) 2 else 0) | (if (expected.aluOp) 1 else 0)).U)
    dut.io.Jump.expect(expected.jump.B)
    dut.io.PCSrc.expect((expected.jump || (expected.branch && taken)).B)
  }

  behavior of "Controller"

  it should ("match independent legality and controls for all opcode funct3 funct7 " +
    "combinations") in {
    test(new ControllerHarness) { dut =>
      for (op <- 0 until 128; f3 <- 0 until 8; f7 <- 0 until 128) {
        val expected = if (legal(op, f3, f7)) mainTable(op) else MainControls()
        check(dut, op, f3, f7, (op ^ f3 ^ f7) & 7, expected)
      }
    }
  }

  it should ("select all six branch conditions for every function field and all eight " +
    "flag combinations") in {
    test(new ControllerHarness) { dut =>
      for (f3 <- 0 until 8; f7 <- 0 until 128; flags <- 0 until 8) {
        val expected = if (legal(0x63, f3, f7)) mainTable(0x63) else MainControls()
        check(dut, 0x63, f3, f7, flags, expected)
      }
    }
  }

  it should ("distinguish immediate data from reserved shift and unsupported extension " +
    "encodings") in {
    test(new ControllerHarness) { dut =>
      // Each fixture states legality explicitly, independent of the sweep decoder.
      val vectors = Seq(
        (0x33, 0, 0, true), (0x33, 0, 32, true), (0x33, 5, 32, true),
        (0x33, 1, 32, false), (0x33, 7, 32, false), (0x33, 0, 1, false),
        (0x33, 4, 4, false), (0x33, 0, 64, false),
        (0x13, 0, 127, true), (0x13, 2, 127, true), (0x13, 3, 127, true),
        (0x13, 4, 127, true), (0x13, 6, 127, true), (0x13, 7, 127, true),
        (0x13, 1, 0, true), (0x13, 1, 1, false), (0x13, 1, 32, false),
        (0x13, 5, 0, true), (0x13, 5, 32, true), (0x13, 5, 1, false),
        (0x13, 5, 33, false), (0x03, 3, 127, false), (0x23, 3, 127, false),
        (0x63, 3, 127, false), (0x67, 0, 127, true), (0x67, 1, 127, false),
        (0x6f, 7, 127, true), (0x37, 7, 127, true), (0x17, 7, 127, true),
        (0x1b, 0, 0, false), (0x3b, 0, 0, false), (0x0f, 0, 0, false),
        (0x73, 1, 0, false)
      )
      for ((op, f3, f7, isLegal) <- vectors) {
        check(dut, op, f3, f7, 7, if (isLegal) mainTable(op) else MainControls())
      }
    }
  }

  it should "drive all nineteen ALU operations and six address-generation categories" in {
    test(new ControllerALUHarness) { dut =>
      // op, funct3, full funct7, a, b, result, address; literal hand-checked outputs.
      val vectors = Seq(
        (0x33, 0, 0, 5L, 7L, 12L, 12L),
        (0x33, 0, 32, 5L, 7L, 0xfffffffeL, 0xfffffffeL),
        (0x33, 1, 0, 3L, 4L, 48L, 7L),
        (0x33, 2, 0, 0x80000000L, 0x7fffffffL, 1L, 1L),
        (0x33, 3, 0, 0L, 0xffffffffL, 1L, 1L),
        (0x33, 4, 0, 0x55L, 0xfL, 0x5aL, 0x64L),
        (0x33, 5, 0, 0x80000000L, 1L, 0x40000000L, 0x80000001L),
        (0x33, 5, 32, 0x80000000L, 1L, 0xc0000000L, 0x7fffffffL),
        (0x33, 6, 0, 0x55L, 0xfL, 0x5fL, 0x64L),
        (0x33, 7, 0, 0x55L, 0xfL, 5L, 0x64L),
        (0x13, 0, 127, 5L, 0xffffffffL, 4L, 4L),
        (0x13, 1, 0, 3L, 4L, 48L, 7L),
        (0x13, 2, 127, 0L, 0xffffffffL, 0L, 1L),
        (0x13, 3, 127, 0L, 0xffffffffL, 1L, 1L),
        (0x13, 4, 127, 0x55L, 0xffffffffL, 0xffffffaaL, 0x54L),
        (0x13, 5, 0, 0x80000000L, 1L, 0x40000000L, 0x80000001L),
        (0x13, 5, 32, 0x80000000L, 1L, 0xc0000000L, 0x7fffffffL),
        (0x13, 6, 127, 0x55L, 0xffffffffL, 0xffffffffL, 0x54L),
        (0x13, 7, 127, 0x55L, 0xffffffffL, 0x55L, 0x54L),
        (0x03, 0, 127, 20L, 12L, 32L, 32L),
        (0x23, 2, 127, 20L, 12L, 32L, 32L),
        (0x63, 6, 127, 32L, 8L, 40L, 40L),
        (0x6f, 7, 127, 32L, 8L, 40L, 40L),
        (0x67, 0, 127, 32L, 8L, 40L, 40L),
        (0x17, 3, 127, 32L, 4096L, 4128L, 4128L)
      )
      for ((op, f3, f7, a, b, result, address) <- vectors) {
        dut.io.Op.poke(op.U)
        dut.io.Funct3.poke(f3.U)
        dut.io.Funct7.poke(f7.U)
        dut.io.SrcA.poke(BigInt(a).U)
        dut.io.SrcB.poke(BigInt(b).U)
        dut.io.ALUResult.expect(BigInt(result).U)
        dut.io.IEUAdr.expect(BigInt(address).U)
      }
    }
  }

  it should ("elaborate exactly sixteen combinational ports with full function and " +
    "comparison inputs") in {
    val targetDir = Files.createTempDirectory("controller-interface-")
    val verilog = (new ChiselStage).emitVerilog(new Controller,
      Array("--target-dir", targetDir.toString))
    val portPattern = "(?m)^\\s*(input|output)\\s+(?:\\[(\\d+):0\\]\\s+)?(\\w+)".r
    val ports = portPattern.findAllMatchIn(verilog).map { port =>
      val width = Option(port.group(2)).map(_.toInt + 1).getOrElse(1)
      port.group(3) -> (port.group(1), width)
    }.toMap
    assert(ports == Map("io_Op" -> ("input", 7), "io_Eq" -> ("input", 1),
      "io_LT" -> ("input", 1), "io_LTU" -> ("input", 1),
      "io_Funct3" -> ("input", 3), "io_Funct7" -> ("input", 7),
      "io_ALUResultSrc" -> ("output", 1), "io_ResultSrc" -> ("output", 1),
      "io_MemWrite" -> ("output", 1), "io_MemRW" -> ("output", 2),
      "io_Jump" -> ("output", 1), "io_PCSrc" -> ("output", 1),
      "io_RegWrite" -> ("output", 1), "io_ALUSrc" -> ("output", 2),
      "io_ImmSrc" -> ("output", 3), "io_ALUControl" -> ("output", 2)))
    assert(!verilog.contains("always @"))
  }
}
