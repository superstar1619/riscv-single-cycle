package riscvsingle.ieu

import chisel3._
import chisel3.stage.ChiselStage
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec
import riscvsingle.config.CpuConfig
import java.nio.file.Files
import scala.util.Random

private class DatapathHarness(config: CpuConfig = CpuConfig()) extends Module {
  val io = IO(new DatapathIO(config))
  val dut = Module(new Datapath(config))
  dut.clk := clock
  dut.reset := reset.asBool
  dut.io <> io
}

private class ControlledDatapathTestIO extends Bundle {
  val Instr = Input(UInt(32.W))
  val PC = Input(UInt(32.W))
  val ReadData = Input(UInt(32.W))
  val IEUAdr = Output(UInt(32.W))
  val WriteData = Output(UInt(32.W))
  val MemWrite = Output(Bool())
  val PCSrc = Output(Bool())
}

// Test-only wiring; PC state and memory are modeled in Scala, outside the DUT.
private class ControlledDatapathHarness extends Module {
  val io = IO(new ControlledDatapathTestIO)
  val datapath = Module(new Datapath)
  val controller = Module(new Controller)
  datapath.clk := clock
  datapath.reset := reset.asBool
  controller.io.Op := io.Instr(6, 0)
  controller.io.Funct3 := io.Instr(14, 12)
  controller.io.Funct7b5 := io.Instr(30)
  controller.io.Eq := datapath.io.Eq
  datapath.io.Funct3 := io.Instr(14, 12)
  datapath.io.ALUResultSrc := controller.io.ALUResultSrc
  datapath.io.ResultSrc := controller.io.ResultSrc
  datapath.io.ALUSrc := controller.io.ALUSrc
  datapath.io.RegWrite := controller.io.RegWrite
  datapath.io.ImmSrc := controller.io.ImmSrc
  datapath.io.ALUControl := controller.io.ALUControl
  datapath.io.PC := io.PC
  datapath.io.PCPlus4 := io.PC + 4.U
  datapath.io.Instr := io.Instr
  datapath.io.ReadData := io.ReadData
  io.IEUAdr := datapath.io.IEUAdr
  io.WriteData := datapath.io.WriteData
  io.MemWrite := controller.io.MemWrite
  io.PCSrc := controller.io.PCSrc
}

class DatapathSpec extends AnyFlatSpec with ChiselScalatestTester {
  private val mask = (BigInt(1) << 32) - 1
  private def u32(value: BigInt): BigInt = value & mask
  private def fields(rs1: Int = 0, rs2: Int = 0, rd: Int = 0): BigInt =
    (BigInt(rs2) << 20) | (BigInt(rs1) << 15) | (BigInt(rd) << 7)

  private def defaults(dut: DatapathHarness): Unit = {
    dut.reset.poke(false.B)
    dut.io.Funct3.poke(0.U)
    dut.io.ALUResultSrc.poke(false.B)
    dut.io.ResultSrc.poke(false.B)
    dut.io.ALUSrc.poke(0.U)
    dut.io.RegWrite.poke(false.B)
    dut.io.ImmSrc.poke(0.U)
    dut.io.ALUControl.poke(0.U)
    dut.io.PC.poke(0.U)
    dut.io.PCPlus4.poke(4.U)
    dut.io.Instr.poke(0.U)
    dut.io.ReadData.poke(0.U)
  }

  private def write(dut: DatapathHarness, index: Int, value: BigInt): Unit = {
    dut.io.Instr.poke(fields(rd = index).U)
    dut.io.ResultSrc.poke(true.B)
    dut.io.ReadData.poke(u32(value).U)
    dut.io.RegWrite.poke(true.B)
    dut.clock.step()
    dut.io.RegWrite.poke(false.B)
    dut.io.ResultSrc.poke(false.B)
  }

  private def expectRegister(dut: DatapathHarness, index: Int, value: BigInt): Unit = {
    defaults(dut)
    dut.io.ALUSrc.poke(1.U) // I immediate is zero; IEUAdr reads R1 + 0.
    dut.io.Instr.poke(fields(rs1 = index).U)
    dut.io.IEUAdr.expect(u32(value).U)
    dut.io.Instr.poke(fields(rs2 = index).U)
    dut.io.WriteData.expect(u32(value).U)
  }

  behavior of "Datapath"

  it should "select all four operand pairs while comparing and storing original registers" in {
    test(new DatapathHarness) { dut =>
      defaults(dut)
      write(dut, 1, 12)
      write(dut, 2, 7)
      // I immediate=2 also selects rs2=x2. PC=2 equals this immediate,
      // but Eq must still compare R1=12 and R2=7.
      dut.io.Instr.poke(fields(rs1 = 1, rs2 = 2).U)
      dut.io.PC.poke(2.U)
      for ((selection, address) <- Seq(0 -> 19, 1 -> 14, 2 -> 9, 3 -> 4)) {
        dut.io.ALUSrc.poke(selection.U)
        dut.io.IEUAdr.expect(address.U)
        dut.io.WriteData.expect(7.U)
        dut.io.Eq.expect(false.B)
      }
      dut.io.Instr.poke(fields(rs1 = 2, rs2 = 2).U)
      dut.io.PC.poke(100.U)
      dut.io.Eq.expect(true.B) // Selected PC/ImmExt differ, original registers equal.
      dut.io.WriteData.expect(7.U)
    }
  }

  it should "write back ALU results, link addresses, and memory data with memory taking priority" in {
    test(new DatapathHarness) { dut =>
      defaults(dut)
      write(dut, 1, 12)
      write(dut, 2, 7)
      for (link <- Seq(false, true); memory <- Seq(false, true)) {
        defaults(dut)
        // Instruction funct3 bits are zero; the explicit port requests OR.
        dut.io.Instr.poke(fields(rs1 = 1, rs2 = 2, rd = 3).U)
        dut.io.Funct3.poke(6.U)
        dut.io.ALUControl.poke(1.U)
        dut.io.ALUResultSrc.poke(link.B)
        dut.io.ResultSrc.poke(memory.B)
        dut.io.PCPlus4.poke(0x104.U)
        dut.io.ReadData.poke("hdeadbeef".U)
        dut.io.RegWrite.poke(true.B)
        dut.io.IEUAdr.expect(19.U) // Address uses sum even when ALUResult is OR.
        dut.clock.step()
        val expected = if (memory) BigInt("deadbeef", 16) else if (link) BigInt(0x104) else BigInt(15)
        expectRegister(dut, 3, expected)
      }
    }
  }

  it should "extend negative I S B and J immediates and wrap 32-bit addresses" in {
    test(new DatapathHarness) { dut =>
      defaults(dut)
      // addi -9; sw -4; beq -8; jal -16. PC is the selected A operand.
      val vectors = Seq(
        (0, BigInt("ff700013", 16), -9),
        (1, BigInt("fe002e23", 16), -4),
        (2, BigInt("fe000ce3", 16), -8),
        (3, BigInt("ff1ff06f", 16), -16)
      )
      dut.io.ALUSrc.poke(3.U)
      for ((format, instruction, immediate) <- vectors; pc <- Seq(4, 0x100)) {
        dut.io.Instr.poke(instruction.U)
        dut.io.ImmSrc.poke(format.U)
        dut.io.PC.poke(pc.U)
        dut.io.IEUAdr.expect(u32(BigInt(pc) + immediate).U)
      }
    }
  }

  it should "update only at clock edges and preserve x0 and nonzero state across reset" in {
    test(new DatapathHarness) { dut =>
      defaults(dut)
      write(dut, 1, 9)
      dut.io.Instr.poke(fields(rs1 = 1, rd = 1).U)
      dut.io.ALUSrc.poke(1.U)
      dut.io.ResultSrc.poke(true.B)
      dut.io.ReadData.poke(42.U)
      dut.io.RegWrite.poke(true.B)
      dut.io.IEUAdr.expect(9.U)
      dut.clock.step()
      dut.io.IEUAdr.expect(42.U)
      dut.io.RegWrite.poke(false.B)
      dut.io.ReadData.poke(99.U)
      dut.clock.step()
      dut.io.IEUAdr.expect(42.U)
      dut.reset.poke(true.B)
      dut.io.RegWrite.poke(true.B)
      dut.clock.step() // Reset wins over a requested x1 write.
      dut.io.IEUAdr.expect(42.U)
      dut.reset.poke(false.B)
      dut.io.RegWrite.poke(false.B)
      write(dut, 0, mask)
      expectRegister(dut, 0, 0)
      expectRegister(dut, 1, 42)
    }
  }

  it should "match an independent register scoreboard through randomized control sequences" in {
    test(new DatapathHarness(CpuConfig(imemDepth = 128, dmemDepth = 256, resetVector = 0x100))) { dut =>
      defaults(dut)
      val random = new Random(0x215)
      val registers = Array.fill[BigInt](32)(BigInt(0))
      for (index <- 1 until 32) {
        registers(index) = BigInt(32, random)
        write(dut, index, registers(index))
      }
      def signed(value: BigInt, width: Int): BigInt =
        if (value.testBit(width - 1)) value - (BigInt(1) << width) else value
      def immediate(instruction: BigInt, format: Int): BigInt = {
        def bits(hi: Int, lo: Int): BigInt = (instruction >> lo) & ((BigInt(1) << (hi - lo + 1)) - 1)
        format match {
          case 0 => signed(bits(31, 20), 12)
          case 1 => signed((bits(31, 25) << 5) | bits(11, 7), 12)
          case 2 => signed((bits(31, 31) << 12) | (bits(7, 7) << 11) |
            (bits(30, 25) << 5) | (bits(11, 8) << 1), 13)
          case 3 => signed((bits(31, 31) << 20) | (bits(19, 12) << 12) |
            (bits(20, 20) << 11) | (bits(30, 21) << 1), 21)
        }
      }
      for (_ <- 0 until 200) {
        val instruction = BigInt(32, random)
        val rs1 = ((instruction >> 15) & 31).toInt
        val rs2 = ((instruction >> 20) & 31).toInt
        val rd = ((instruction >> 7) & 31).toInt
        val format = random.nextInt(4)
        val selection = random.nextInt(4)
        val funct3 = random.nextInt(8)
        val aluOp = random.nextBoolean()
        val sub = aluOp && (funct3 == 2 || funct3 == 3 ||
          ((funct3 == 0 || funct3 == 5) && random.nextBoolean()))
        val pc = BigInt(32, random)
        val linkAddress = BigInt(32, random)
        val readData = BigInt(32, random)
        val link = random.nextBoolean()
        val memory = random.nextBoolean()
        val enable = random.nextBoolean()
        val a = if ((selection & 2) != 0) pc else registers(rs1)
        val b = if ((selection & 1) != 0) u32(immediate(instruction, format)) else registers(rs2)
        val address = u32(if (sub) a - b else a + b)
        val result = (if (aluOp) funct3 else 0) match {
          case 0 => address
          case 1 => u32(a << (b & 31).toInt)
          case 2 => if (signed(a, 32) < signed(b, 32)) BigInt(1) else BigInt(0)
          case 3 => if (a < b) BigInt(1) else BigInt(0)
          case 4 => a ^ b
          case 5 => u32(if (sub) signed(a, 32) >> (b & 31).toInt else a >> (b & 31).toInt)
          case 6 => a | b
          case 7 => a & b
        }
        dut.io.Instr.poke(instruction.U)
        dut.io.ImmSrc.poke(format.U)
        dut.io.ALUSrc.poke(selection.U)
        dut.io.Funct3.poke(funct3.U)
        dut.io.ALUControl.poke(((if (sub) 2 else 0) | (if (aluOp) 1 else 0)).U)
        dut.io.PC.poke(pc.U)
        dut.io.PCPlus4.poke(linkAddress.U)
        dut.io.ReadData.poke(readData.U)
        dut.io.ALUResultSrc.poke(link.B)
        dut.io.ResultSrc.poke(memory.B)
        dut.io.RegWrite.poke(enable.B)
        dut.io.Eq.expect((registers(rs1) == registers(rs2)).B)
        dut.io.WriteData.expect(registers(rs2).U)
        dut.io.IEUAdr.expect(address.U)
        dut.clock.step()
        if (enable && rd != 0) registers(rd) = if (memory) readData else if (link) linkAddress else result
      }
      for (index <- 0 until 32) expectRegister(dut, index, registers(index))
    }
  }

  it should "execute the book program with the reviewed Controller and test-side PC and memory" in {
    test(new ControlledDatapathHarness) { dut =>
      val words = Seq("00500113", "00c00193", "ff718393", "0023e233", "0041f2b3",
        "004282b3", "02728863", "0041a233", "00020463", "00000293", "0023a233",
        "005203b3", "402383b3", "0471aa23", "06002103", "005104b3", "008001ef",
        "00100113", "00910133", "0221a023", "00210063").map(BigInt(_, 16))
      val expectedPCs = Seq(0x00, 0x04, 0x08, 0x0c, 0x10, 0x14, 0x18, 0x1c,
        0x20, 0x28, 0x2c, 0x30, 0x34, 0x38, 0x3c, 0x40, 0x48, 0x4c, 0x50)
      val memory = scala.collection.mutable.Map.empty[BigInt, BigInt]
      var pc = 0
      dut.reset.poke(false.B)
      for (expectedPC <- expectedPCs) {
        assert(pc == expectedPC)
        dut.io.PC.poke(pc.U)
        dut.io.Instr.poke(words(pc / 4).U)
        dut.io.ReadData.poke(0.U)
        val address = dut.io.IEUAdr.peek().litValue
        dut.io.ReadData.poke(memory.getOrElse(address, BigInt(0)).U)
        if (dut.io.MemWrite.peek().litToBoolean) {
          val data = dut.io.WriteData.peek().litValue
          if (pc == 0x34) { assert(address == 96); assert(data == 7) }
          else { assert(pc == 0x4c); assert(address == 100); assert(data == 25) }
          memory(address) = data
        }
        val nextPC = if (dut.io.PCSrc.peek().litToBoolean) address.toInt else pc + 4
        dut.clock.step()
        pc = nextPC
      }
      assert(pc == 0x50)
      assert(memory.toMap == Map(BigInt(96) -> BigInt(7), BigInt(100) -> BigInt(25)))
    }
  }

  it should "elaborate the book ports and four child modules without implicit clock or reset" in {
    val targetDir = Files.createTempDirectory("datapath-interface-")
    val verilog = (new ChiselStage).emitVerilog(new Datapath,
      Array("--target-dir", targetDir.toString))
    val top = verilog.split("module Datapath\\(")(1).split("\\);", 2)(0)
    val portPattern = "(?m)^\\s*(input|output)\\s+(?:\\[(\\d+):0\\]\\s+)?(\\w+)".r
    val ports = portPattern.findAllMatchIn(top).map { port =>
      val width = Option(port.group(2)).map(_.toInt + 1).getOrElse(1)
      port.group(3) -> (port.group(1), width)
    }.toMap
    assert(ports == Map("clk" -> ("input", 1), "reset" -> ("input", 1),
      "io_Funct3" -> ("input", 3), "io_ALUResultSrc" -> ("input", 1),
      "io_ResultSrc" -> ("input", 1), "io_ALUSrc" -> ("input", 2),
      "io_RegWrite" -> ("input", 1), "io_ImmSrc" -> ("input", 2),
      "io_ALUControl" -> ("input", 2), "io_Eq" -> ("output", 1),
      "io_PC" -> ("input", 32), "io_PCPlus4" -> ("input", 32),
      "io_Instr" -> ("input", 32), "io_IEUAdr" -> ("output", 32),
      "io_WriteData" -> ("output", 32), "io_ReadData" -> ("input", 32)))
    for ((module, instance) <- Seq("RegFile" -> "rf", "Extend" -> "ext", "Cmp" -> "cmp", "ALU" -> "alu")) {
      assert(verilog.contains(s"$module $instance ("))
    }
    assert(verilog.contains(".reset(rf_reset)"), "RegFile must receive the reset connection")
    assert(verilog.contains("assign rf_reset = reset;"), "RegFile reset must follow the external reset")
  }
}
