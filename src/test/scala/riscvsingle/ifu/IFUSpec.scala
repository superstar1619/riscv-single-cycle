package riscvsingle.ifu

import chisel3._
import chisel3.stage.ChiselStage
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec
import riscvsingle.config.CpuConfig
import riscvsingle.ieu.IEU
import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Paths}
import scala.util.Random

private class IFUHarness(config: CpuConfig) extends Module {
  val io = IO(new IFUIO(config))
  val dut = Module(new IFU(config))
  dut.clk := clock
  dut.reset := reset.asBool
  dut.io <> io
}

private class IFUIEUTestIO extends Bundle {
  val ReadData = Input(UInt(32.W))
  val PC = Output(UInt(32.W))
  val PCPlus4 = Output(UInt(32.W))
  val Instr = Output(UInt(32.W))
  val PCSrc = Output(Bool())
  val MemWrite = Output(Bool())
  val IEUAdr = Output(UInt(32.W))
  val WriteData = Output(UInt(32.W))
}

// Test-only connection of reviewed modules. Data memory remains a Scala model.
private class IFUIEUHarness(config: CpuConfig) extends Module {
  val io = IO(new IFUIEUTestIO)
  val ifu = Module(new IFU(config))
  val ieu = Module(new IEU(config))
  ifu.clk := clock
  ifu.reset := reset.asBool
  ieu.clk := clock
  ieu.reset := reset.asBool
  ifu.io.PCSrc := ieu.io.PCSrc
  ifu.io.IEUAdr := ieu.io.IEUAdr
  ieu.io.Instr := ifu.io.Instr
  ieu.io.PC := ifu.io.PC
  ieu.io.PCPlus4 := ifu.io.PCPlus4
  ieu.io.ReadData := io.ReadData
  io.PC := ifu.io.PC
  io.PCPlus4 := ifu.io.PCPlus4
  io.Instr := ifu.io.Instr
  io.PCSrc := ieu.io.PCSrc
  io.MemWrite := ieu.io.MemWrite
  io.IEUAdr := ieu.io.IEUAdr
  io.WriteData := ieu.io.WriteData
}

class IFUSpec extends AnyFlatSpec with ChiselScalatestTester {
  private val bookFile = "programs/riscvtest.memfile"
  private val bookConfig = CpuConfig(instructionInitFile = Some(bookFile))
  private val bookWords = Seq("00500113", "00c00193", "ff718393", "0023e233", "0041f2b3",
    "004282b3", "02728863", "0041a233", "00020463", "00000293", "0023a233",
    "005203b3", "402383b3", "0471aa23", "06002103", "005104b3", "008001ef",
    "00100113", "00910133", "0221a023", "00210063").map(BigInt(_, 16))

  behavior of "IFU"

  it should "fetch the current instruction and advance by four only at rising edges" in {
    test(new IFUHarness(bookConfig)) { dut =>
      dut.reset.poke(false.B)
      dut.io.PCSrc.poke(false.B)
      for (index <- bookWords.indices) {
        dut.io.IEUAdr.poke((0x80 + index * 4).U) // Sequential path ignores this input.
        dut.io.PC.expect((index * 4).U)
        dut.io.PCPlus4.expect((index * 4 + 4).U)
        dut.io.Instr.expect(bookWords(index).U)
        dut.clock.step()
      }
      dut.io.PC.expect(0x54.U)
      dut.io.Instr.expect(0x13.U)
    }
  }

  it should "sample selected targets without masking address bits and wrap PCPlus4 to 32 bits" in {
    val waves = if (sys.env.get("GENERATE_WAVES").contains("1")) {
      Seq(WriteVcdAnnotation)
    } else Seq.empty
    test(new IFUHarness(bookConfig)).withAnnotations(waves) { dut =>
      dut.reset.poke(false.B)
      dut.io.PCSrc.poke(false.B)
      dut.io.IEUAdr.poke(0x18.U)
      dut.clock.step()
      dut.io.PC.expect(4.U)
      dut.io.PCSrc.poke(true.B)
      dut.io.IEUAdr.poke(0x10.U)
      dut.io.PC.expect(4.U) // Control/input changes do not immediately alter PC.
      dut.io.IEUAdr.poke(0x18.U) // The value present at the edge must be used.
      dut.clock.step()
      dut.io.PC.expect(0x18.U)
      dut.io.Instr.expect(bookWords(6).U)
      dut.io.IEUAdr.poke(3.U)
      dut.clock.step()
      dut.io.PC.expect(3.U)
      dut.io.PCPlus4.expect(7.U)
      dut.io.Instr.expect(bookWords(0).U) // IROM ignores the low two bits.
      dut.io.IEUAdr.poke("h80000100".U)
      dut.clock.step()
      dut.io.PC.expect("h80000100".U)
      dut.io.Instr.expect(bookWords(0).U) // IROM also aliases upper address bits.
      dut.io.IEUAdr.poke("hfffffffc".U)
      dut.clock.step()
      dut.io.PC.expect("hfffffffc".U)
      dut.io.PCPlus4.expect(0.U)
      dut.io.Instr.expect(0x13.U)
      dut.io.PCSrc.poke(false.B)
      dut.clock.step()
      dut.io.PC.expect(0.U)
      dut.io.Instr.expect(bookWords(0).U)
    }
  }

  for (vector <- Seq(0, 0x100)) {
    it should s"synchronously reset to $vector only at rising edges with priority over target selection" in {
      // A complete 128-word image verifies resetVector and depth reach the child IROM.
      val directory = Files.createDirectories(Paths.get("target/ifu-test-images"))
      val file = Files.createTempFile(directory, "ifu-reset-", ".hex")
      val words = (0 until 128).map(index => f"${0x10000000L + index}%08x").mkString("", "\n", "\n")
      Files.write(file, words.getBytes(StandardCharsets.UTF_8))
      val config = CpuConfig(imemDepth = 128, resetVector = vector,
        instructionInitFile = Some(file.toString))
      test(new IFUHarness(config)) { dut =>
        dut.reset.poke(false.B)
        dut.io.PCSrc.poke(true.B)
        dut.io.IEUAdr.poke(0x20.U)
        dut.clock.step()
        dut.io.PC.expect(0x20.U)
        dut.reset.poke(true.B) // Assertion alone must preserve PC and the current instruction.
        dut.io.PC.expect(0x20.U)
        dut.io.PCPlus4.expect(0x24.U)
        dut.io.Instr.expect(0x10000008.U)
        dut.reset.poke(false.B) // A reset pulse with no clock edge has no effect.
        dut.io.PC.expect(0x20.U)
        dut.reset.poke(true.B)
        dut.io.IEUAdr.poke(0x40.U)
        dut.clock.step() // Reset wins over the selected jump target at this edge.
        dut.io.PC.expect(vector.U)
        dut.io.PCPlus4.expect((vector + 4).U)
        dut.io.Instr.expect((0x10000000L + vector / 4).U)
        dut.clock.step(2)
        dut.io.PC.expect(vector.U)
        dut.reset.poke(false.B)
        dut.io.PC.expect(vector.U) // Release alone does not advance PC.
        dut.io.PCSrc.poke(false.B)
        dut.clock.step()
        dut.io.PC.expect((vector + 4).U)
        dut.io.Instr.expect((0x10000000L + vector / 4 + 1).U)
      }
    }
  }

  it should "execute the book program with hardware PC and instruction memory driving the reviewed IEU" in {
    test(new IFUIEUHarness(bookConfig)) { dut =>
      val expectedPCs = Seq(0x00, 0x04, 0x08, 0x0c, 0x10, 0x14, 0x18, 0x1c,
        0x20, 0x28, 0x2c, 0x30, 0x34, 0x38, 0x3c, 0x40, 0x48, 0x4c, 0x50)
      val memory = scala.collection.mutable.Map.empty[BigInt, BigInt]
      dut.reset.poke(false.B)
      for (pc <- expectedPCs) {
        dut.io.PC.expect(pc.U)
        dut.io.PCPlus4.expect((pc + 4).U)
        dut.io.Instr.expect(bookWords(pc / 4).U)
        dut.io.ReadData.poke(0.U)
        val address = dut.io.IEUAdr.peek().litValue
        dut.io.ReadData.poke(memory.getOrElse(address, BigInt(0)).U)
        dut.io.PCSrc.expect(Set(0x20, 0x40, 0x50).contains(pc).B)
        dut.io.MemWrite.expect((pc == 0x34 || pc == 0x4c).B)
        if (dut.io.MemWrite.peek().litToBoolean) {
          val data = dut.io.WriteData.peek().litValue
          if (pc == 0x34) { assert(address == 96); assert(data == 7) }
          else { assert(address == 100); assert(data == 25) }
          memory(address) = data
        }
        dut.clock.step()
      }
      dut.io.PC.expect(0x50.U)
      dut.clock.step(3)
      dut.io.PC.expect(0x50.U)
      assert(memory.toMap == Map(BigInt(96) -> BigInt(7), BigInt(100) -> BigInt(25)))
    }
  }

  it should "emit the seven book ports, a synchronous PC reset, and the IROM child" in {
    val targetDir = Files.createTempDirectory("ifu-interface-")
    val verilog = (new ChiselStage).emitVerilog(new IFU(bookConfig.copy(resetVector = 0x100)),
      Array("--target-dir", targetDir.toString))
    val top = verilog.split("module IFU\\(")(1).split("\\);", 2)(0)
    val portPattern = "(?m)^\\s*(input|output)\\s+(?:\\[(\\d+):0\\]\\s+)?(\\w+)".r
    val ports = portPattern.findAllMatchIn(top).map { port =>
      val width = Option(port.group(2)).map(_.toInt + 1).getOrElse(1)
      port.group(3) -> (port.group(1), width)
    }.toMap
    assert(ports == Map("clk" -> ("input", 1), "reset" -> ("input", 1),
      "io_PCSrc" -> ("input", 1), "io_IEUAdr" -> ("input", 32),
      "io_Instr" -> ("output", 32), "io_PC" -> ("output", 32),
      "io_PCPlus4" -> ("output", 32)))
    val events = "always\\s*@\\s*\\(([^)]+)\\)".r.findAllMatchIn(verilog).map(_.group(1)).toSeq
    assert(events == Seq("posedge clk"), "PC state must update only on rising clock edges")
    assert(verilog.contains("pcreg <= 32'h100;"), "PC reset must use the configured vector")
    assert(verilog.contains("IROM irom ("), "IFU must contain the reviewed IROM")
  }

  for ((depth, vector) <- Seq(2 -> 0, 64 -> 0x100, 128 -> 0x100)) {
    it should s"match a seeded PC model at depth $depth and reset vector $vector" in {
      val directory = Files.createDirectories(Paths.get("target/ifu-test-images"))
      val file = Files.createTempFile(directory, s"ifu-model-$depth-", ".hex")
      val words = Seq.tabulate(depth)(index => BigInt(0x12000000L + index))
      Files.write(file, words.map(_.toString(16)).mkString("", "\n", "\n")
        .getBytes(StandardCharsets.UTF_8))
      val config = CpuConfig(imemDepth = depth, resetVector = vector,
        instructionInitFile = Some(file.toString))
      test(new IFUHarness(config)) { dut =>
        val random = new Random(0x715 + depth)
        val mask = (BigInt(1) << 32) - 1
        var pc = BigInt(vector)
        dut.io.PCSrc.poke(false.B)
        dut.io.IEUAdr.poke(0.U)
        dut.reset.poke(true.B)
        dut.clock.step() // 通过实际同步复位建立初始 PC。
        for (cycle <- 0 until 300) {
          val target = BigInt(32, random)
          val jump = (cycle & 1) != 0
          val reset = cycle % 17 == 0
          dut.io.PCSrc.poke(jump.B)
          dut.io.IEUAdr.poke(target.U)
          dut.reset.poke(reset.B)
          dut.io.PC.expect(pc.U) // 控制和 reset 改变本身不能更新 PC。
          dut.io.PCPlus4.expect(((pc + 4) & mask).U)
          dut.io.Instr.expect(words(((pc / 4) % depth).toInt).U)
          dut.clock.step()
          pc = if (reset) BigInt(vector) else if (jump) target else (pc + 4) & mask
          dut.io.PC.expect(pc.U)
          dut.io.PCPlus4.expect(((pc + 4) & mask).U)
          dut.io.Instr.expect(words(((pc / 4) % depth).toInt).U)
          dut.reset.poke(false.B)
          dut.io.PC.expect(pc.U) // 释放复位同样不产生状态更新。
        }
      }
    }
  }
}
