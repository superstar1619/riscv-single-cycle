package riscvsingle.ieu

import chisel3._
import chisel3.stage.ChiselStage
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec
import riscvsingle.config.CpuConfig
import java.nio.file.Files

private class IEUHarness(config: CpuConfig = CpuConfig()) extends Module {
  val io = IO(new IEUIO(config))
  val dut = Module(new IEU(config))
  dut.clk := clock
  dut.reset := reset.asBool
  dut.io <> io
}

class IEUSpec extends AnyFlatSpec with ChiselScalatestTester {
  private val mask = (BigInt(1) << 32) - 1
  private def u32(value: BigInt): BigInt = value & mask

  // Test encoders use architectural instruction fields, not control words.
  private def iType(rd: Int, rs1: Int, immediate: Int, funct3: Int = 0, opcode: Int = 0x13): BigInt =
    (BigInt(immediate & 0xfff) << 20) | (BigInt(rs1) << 15) |
      (BigInt(funct3) << 12) | (BigInt(rd) << 7) | opcode
  private def rType(rd: Int, rs1: Int, rs2: Int, funct3: Int = 0, subtract: Boolean = false): BigInt =
    (if (subtract) BigInt(1) << 30 else BigInt(0)) | (BigInt(rs2) << 20) |
      (BigInt(rs1) << 15) | (BigInt(funct3) << 12) | (BigInt(rd) << 7) | 0x33
  private def sType(rs1: Int, rs2: Int, immediate: Int = 0): BigInt = {
    val imm = immediate & 0xfff
    (BigInt(imm >> 5) << 25) | (BigInt(rs2) << 20) | (BigInt(rs1) << 15) |
      (BigInt(2) << 12) | (BigInt(imm & 31) << 7) | 0x23
  }
  private def bType(rs1: Int, rs2: Int, immediate: Int): BigInt = {
    val imm = immediate & 0x1fff
    (BigInt((imm >> 12) & 1) << 31) | (BigInt((imm >> 5) & 63) << 25) |
      (BigInt(rs2) << 20) | (BigInt(rs1) << 15) |
      (BigInt((imm >> 1) & 15) << 8) | (BigInt((imm >> 11) & 1) << 7) | 0x63
  }
  private def jType(rd: Int, immediate: Int): BigInt = {
    val imm = immediate & 0x1fffff
    (BigInt((imm >> 20) & 1) << 31) | (BigInt((imm >> 1) & 1023) << 21) |
      (BigInt((imm >> 11) & 1) << 20) | (BigInt((imm >> 12) & 255) << 12) |
      (BigInt(rd) << 7) | 0x6f
  }

  private def drive(dut: IEUHarness, instruction: BigInt, pc: Int = 0, readData: BigInt = 0): Unit = {
    dut.io.Instr.poke(instruction.U)
    dut.io.PC.poke(pc.U)
    dut.io.PCPlus4.poke(u32(BigInt(pc) + 4).U)
    dut.io.ReadData.poke(u32(readData).U)
  }

  private def retire(dut: IEUHarness, instruction: BigInt, readData: BigInt = 0): Unit = {
    drive(dut, instruction, readData = readData)
    dut.clock.step()
  }

  // Observe architectural registers using a store's public WriteData output.
  // No edge is advanced and no memory is connected during this observation.
  private def expectRegister(dut: IEUHarness, index: Int, value: BigInt): Unit = {
    drive(dut, sType(rs1 = 0, rs2 = index))
    dut.io.MemWrite.expect(true.B)
    dut.io.PCSrc.expect(false.B)
    dut.io.IEUAdr.expect(0.U)
    dut.io.WriteData.expect(u32(value).U)
  }

  behavior of "IEU"

  it should "decode arithmetic and memory instructions and write the selected results" in {
    test(new IEUHarness) { dut =>
      dut.reset.poke(false.B)
      retire(dut, iType(1, 0, 12))
      retire(dut, iType(2, 0, 7))
      retire(dut, iType(4, 0, 0, funct3 = 2, opcode = 0x03), BigInt("80000000", 16))
      val vectors = Seq(
        (iType(3, 1, -9), 3, BigInt(3), BigInt(3)),
        (rType(5, 1, 2), 5, BigInt(19), BigInt(19)),
        (rType(6, 1, 2, subtract = true), 6, BigInt(5), BigInt(5)),
        (rType(7, 1, 2, funct3 = 6), 7, BigInt(15), BigInt(19)),
        (rType(8, 1, 2, funct3 = 7), 8, BigInt(4), BigInt(19)),
        (rType(9, 4, 2, funct3 = 2), 9, BigInt(1), BigInt("7ffffff9", 16)),
        (iType(10, 2, -1, funct3 = 2), 10, BigInt(0), BigInt(8)),
        (iType(11, 1, -1, funct3 = 6), 11, mask, BigInt(11)),
        (iType(12, 1, 5, funct3 = 7), 12, BigInt(4), BigInt(17))
      )
      for ((instruction, rd, result, address) <- vectors) {
        drive(dut, instruction)
        dut.io.PCSrc.expect(false.B)
        dut.io.MemWrite.expect(false.B)
        dut.io.IEUAdr.expect(address.U)
        dut.clock.step()
        expectRegister(dut, rd, result)
      }
      expectRegister(dut, 4, BigInt("80000000", 16))
      drive(dut, sType(rs1 = 1, rs2 = 8, immediate = -4))
      dut.io.IEUAdr.expect(8.U)
      dut.io.WriteData.expect(4.U)
      dut.io.MemWrite.expect(true.B)
      dut.io.PCSrc.expect(false.B)
      dut.clock.step()
      expectRegister(dut, 8, 4)
    }
  }

  it should "feed register equality into beq and use external PCPlus4 for jal writeback" in {
    test(new IEUHarness) { dut =>
      dut.reset.poke(false.B)
      retire(dut, iType(1, 0, 9))
      retire(dut, iType(2, 0, 9))
      retire(dut, iType(3, 0, 13))
      drive(dut, bType(1, 2, 16), pc = 0x100)
      dut.io.PCSrc.expect(true.B)
      dut.io.MemWrite.expect(false.B)
      dut.io.IEUAdr.expect(0x110.U)
      dut.clock.step()
      expectRegister(dut, 1, 9)
      expectRegister(dut, 2, 9)
      retire(dut, iType(2, 0, 7))
      drive(dut, bType(1, 2, -8), pc = 0x100)
      dut.io.PCSrc.expect(false.B)
      dut.io.IEUAdr.expect(0xf8.U)
      drive(dut, bType(1, 1, -8), pc = 0x100)
      dut.io.PCSrc.expect(true.B)
      dut.io.IEUAdr.expect(0xf8.U)
      drive(dut, jType(3, -16), pc = 0x100)
      dut.io.PCPlus4.poke(0xabc.U) // Distinct from PC+4 to verify the input wiring.
      dut.io.PCSrc.expect(true.B)
      dut.io.MemWrite.expect(false.B)
      dut.io.IEUAdr.expect(0xf0.U)
      dut.clock.step()
      expectRegister(dut, 3, 0xabc)
      drive(dut, jType(0, 12), pc = 0x80)
      dut.io.PCSrc.expect(true.B)
      dut.io.IEUAdr.expect(0x8c.U)
      dut.clock.step()
      expectRegister(dut, 0, 0)
    }
  }

  it should "preserve reviewed reset and x0 behavior and disable writes for undefined opcodes" in {
    test(new IEUHarness(CpuConfig(imemDepth = 128, dmemDepth = 256, resetVector = 0x100))) { dut =>
      dut.reset.poke(false.B)
      retire(dut, iType(5, 0, 19))
      drive(dut, iType(5, 0, 42))
      // Poking a new instruction does not write a register before an edge.
      expectRegister(dut, 5, 19)
      drive(dut, iType(5, 0, 42))
      dut.reset.poke(true.B)
      dut.clock.step()
      expectRegister(dut, 5, 19)
      // Reset clears x0 and blocks register writes; decode remains combinational.
      drive(dut, sType(rs1 = 5, rs2 = 5))
      dut.io.MemWrite.expect(true.B)
      dut.io.IEUAdr.expect(19.U)
      dut.io.WriteData.expect(19.U)
      dut.reset.poke(false.B)
      for (instruction <- Seq(iType(0, 0, 99), iType(0, 0, 0, funct3 = 2, opcode = 0x03))) {
        retire(dut, instruction, readData = mask)
        expectRegister(dut, 0, 0)
      }
      // These opcode values are undefined; rd=x5 and rs1=rs2=x5 stress suppression.
      for (opcode <- Seq(0x00, 0x37, 0x67, 0x7f)) {
        val instruction = (BigInt(5) << 20) | (BigInt(5) << 15) | (BigInt(5) << 7) | opcode
        drive(dut, instruction)
        dut.io.PCSrc.expect(false.B)
        dut.io.MemWrite.expect(false.B)
        dut.io.IEUAdr.expect(38.U)
        dut.clock.step()
        expectRegister(dut, 5, 19)
      }
    }
  }

  it should "execute Code Example 2.16 using only the IEU production interface" in {
    test(new IEUHarness) { dut =>
      val words = Seq("00500113", "00c00193", "ff718393", "0023e233", "0041f2b3",
        "004282b3", "02728863", "0041a233", "00020463", "00000293", "0023a233",
        "005203b3", "402383b3", "0471aa23", "06002103", "005104b3", "008001ef",
        "00100113", "00910133", "0221a023", "00210063").map(BigInt(_, 16))
      val expectedPCs = Seq(0x00, 0x04, 0x08, 0x0c, 0x10, 0x14, 0x18, 0x1c,
        0x20, 0x28, 0x2c, 0x30, 0x34, 0x38, 0x3c, 0x40, 0x48, 0x4c, 0x50)
      val takenPCs = Set(0x20, 0x40, 0x50)
      val memory = scala.collection.mutable.Map.empty[BigInt, BigInt]
      var pc = 0
      dut.reset.poke(false.B)
      for (expectedPC <- expectedPCs) {
        assert(pc == expectedPC)
        drive(dut, words(pc / 4), pc)
        val address = dut.io.IEUAdr.peek().litValue
        dut.io.ReadData.poke(memory.getOrElse(address, BigInt(0)).U)
        dut.io.PCSrc.expect(takenPCs.contains(pc).B)
        dut.io.MemWrite.expect((pc == 0x34 || pc == 0x4c).B)
        if (dut.io.MemWrite.peek().litToBoolean) {
          val data = dut.io.WriteData.peek().litValue
          if (pc == 0x34) { assert(address == 96); assert(data == 7) }
          else { assert(address == 100); assert(data == 25) }
          memory(address) = data
        }
        val nextPC = if (dut.io.PCSrc.peek().litToBoolean) address.toInt else pc + 4
        dut.clock.step()
        pc = nextPC
      }
      assert(pc == 0x50)
      assert(memory.toMap == Map(BigInt(96) -> BigInt(7), BigInt(100) -> BigInt(25)))
      expectRegister(dut, 2, 25)
      expectRegister(dut, 3, 0x44)
      expectRegister(dut, 9, 18)
    }
  }

  it should "elaborate the ten book ports and the Controller Datapath hierarchy" in {
    val targetDir = Files.createTempDirectory("ieu-interface-")
    val verilog = (new ChiselStage).emitVerilog(new IEU,
      Array("--target-dir", targetDir.toString))
    val top = verilog.split("module IEU\\(")(1).split("\\);", 2)(0)
    val portPattern = "(?m)^\\s*(input|output)\\s+(?:\\[(\\d+):0\\]\\s+)?(\\w+)".r
    val ports = portPattern.findAllMatchIn(top).map { port =>
      val width = Option(port.group(2)).map(_.toInt + 1).getOrElse(1)
      port.group(3) -> (port.group(1), width)
    }.toMap
    assert(ports == Map("clk" -> ("input", 1), "reset" -> ("input", 1),
      "io_Instr" -> ("input", 32), "io_PC" -> ("input", 32),
      "io_PCPlus4" -> ("input", 32), "io_PCSrc" -> ("output", 1),
      "io_MemWrite" -> ("output", 1), "io_IEUAdr" -> ("output", 32),
      "io_WriteData" -> ("output", 32), "io_ReadData" -> ("input", 32)))
    val modules = "(?m)^module (\\w+)\\(".r.findAllMatchIn(verilog).map(_.group(1)).toSet
    assert(modules == Set("IEU", "Controller", "Datapath", "RegFile", "Extend", "Cmp", "ALU"))
    assert(verilog.contains("Controller c ("), "IEU must contain Controller instance c")
    assert(verilog.contains("Datapath dp ("), "IEU must contain Datapath instance dp")
  }
}
