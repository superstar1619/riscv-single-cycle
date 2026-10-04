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

private class IROMHarness(config: CpuConfig) extends Module {
  val io = IO(new IROMIO(config))
  val dut = Module(new IROM(config))
  dut.io <> io
}

private class IROMIEUTestIO extends Bundle {
  val PC = Input(UInt(32.W))
  val ReadData = Input(UInt(32.W))
  val Instr = Output(UInt(32.W))
  val PCSrc = Output(Bool())
  val MemWrite = Output(Bool())
  val IEUAdr = Output(UInt(32.W))
  val WriteData = Output(UInt(32.W))
}

// Test-only wiring. The Scala test owns PC and data memory; no IFU is implemented.
private class IROMIEUHarness(config: CpuConfig) extends Module {
  val io = IO(new IROMIEUTestIO)
  val rom = Module(new IROM(config))
  val ieu = Module(new IEU(config))
  rom.io.a := io.PC
  ieu.clk := clock
  ieu.reset := reset.asBool
  ieu.io.Instr := rom.io.rd
  ieu.io.PC := io.PC
  ieu.io.PCPlus4 := io.PC + 4.U
  ieu.io.ReadData := io.ReadData
  io.Instr := rom.io.rd
  io.PCSrc := ieu.io.PCSrc
  io.MemWrite := ieu.io.MemWrite
  io.IEUAdr := ieu.io.IEUAdr
  io.WriteData := ieu.io.WriteData
}

class IROMSpec extends AnyFlatSpec with ChiselScalatestTester {
  private val mask = (BigInt(1) << 32) - 1
  private val bookFile = "programs/riscvtest.memfile"
  private val bookWords = Seq("00500113", "00c00193", "ff718393", "0023e233", "0041f2b3",
    "004282b3", "02728863", "0041a233", "00020463", "00000293", "0023a233",
    "005203b3", "402383b3", "0471aa23", "06002103", "005104b3", "008001ef",
    "00100113", "00910133", "0221a023", "00210063").map(BigInt(_, 16))

  behavior of "IROM"

  for (depth <- Seq(2, 64, 128)) {
    it should s"read all $depth words and address aliases without a clock edge" in {
      val random = new Random(0x215 + depth)
      val words = Seq.tabulate(depth) { index =>
        if (index == 0) BigInt(0) else if (index == depth - 1) mask else BigInt(32, random)
      }
      // Treadle's memory-file parser requires a relative path.
      val imageDir = Files.createDirectories(Paths.get("target/irom-test-images"))
      val file = Files.createTempFile(imageDir, s"irom-$depth-", ".hex")
      val contents = words.map(word => f"${word.toLong}%08x").mkString("", "\n", "\n")
      Files.write(file, contents.getBytes(StandardCharsets.UTF_8))
      val config = CpuConfig(imemDepth = depth, instructionInitFile = Some(file.toString))
      test(new IROMHarness(config)) { dut =>
        val bytes = BigInt(depth) * 4
        val aliases = Seq(BigInt(0), bytes, BigInt(1) << 31, mask ^ (bytes - 1))
        // No clock steps: successive address changes must immediately select data.
        for (index <- random.shuffle((0 until depth).toVector);
            offset <- 0 until 4; alias <- aliases) {
          val address = (BigInt(index) * 4 + offset) | alias
          dut.io.a.poke(address.U)
          dut.io.rd.expect(words(index).U)
        }
      }
    }
  }

  it should "load the bundled book program and its NOP padding" in {
    test(new IROMHarness(CpuConfig(instructionInitFile = Some(bookFile)))) { dut =>
      for (index <- 0 until 64) {
        dut.io.a.poke((index * 4).U)
        dut.io.rd.expect(bookWords.lift(index).getOrElse(BigInt(0x13)).U)
      }
    }
  }

  it should "supply instructions to the reviewed IEU throughout Code Example 2.16" in {
    test(new IROMIEUHarness(CpuConfig(instructionInitFile = Some(bookFile)))) { dut =>
      val expectedPCs = Seq(0x00, 0x04, 0x08, 0x0c, 0x10, 0x14, 0x18, 0x1c,
        0x20, 0x28, 0x2c, 0x30, 0x34, 0x38, 0x3c, 0x40, 0x48, 0x4c, 0x50)
      val memory = scala.collection.mutable.Map.empty[BigInt, BigInt]
      var pc = 0
      dut.reset.poke(false.B)
      for (expectedPC <- expectedPCs) {
        assert(pc == expectedPC)
        dut.io.PC.poke(pc.U)
        dut.io.ReadData.poke(0.U)
        dut.io.Instr.expect(bookWords(pc / 4).U)
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
        val nextPC = if (dut.io.PCSrc.peek().litToBoolean) address.toInt else pc + 4
        dut.clock.step()
        pc = nextPC
      }
      assert(pc == 0x50)
      assert(memory.toMap == Map(BigInt(96) -> BigInt(7), BigInt(100) -> BigInt(25)))
    }
  }

  it should "emit two ports and an asynchronous ROM with optional initialization" in {
    for (file <- Seq(None, Some(bookFile))) {
      val targetDir = Files.createTempDirectory("irom-interface-")
      val verilog = (new ChiselStage).emitVerilog(new IROM(CpuConfig(instructionInitFile = file)),
        Array("--target-dir", targetDir.toString))
      val top = verilog.split("module IROM\\(")(1).split("\\);", 2)(0)
      val portPattern = "(?m)^\\s*(input|output)\\s+(?:\\[(\\d+):0\\]\\s+)?(\\w+)".r
      val ports = portPattern.findAllMatchIn(top).map { port =>
        val width = Option(port.group(2)).map(_.toInt + 1).getOrElse(1)
        port.group(3) -> (port.group(1), width)
      }.toMap
      assert(ports == Map("io_a" -> ("input", 32), "io_rd" -> ("output", 32)))
      assert(verilog.contains("reg [31:0] ROM [0:63];"), "Default ROM must have 64 32-bit entries")
      assert(!"always\\s*@\\s*\\(posedge".r.findFirstIn(verilog).isDefined,
        "Asynchronous read must not depend on a rising clock edge")
      assert(verilog.contains("$readmemh") == file.isDefined,
        "Only a configured initialization file should emit readmemh")
      file.foreach(path => assert(verilog.contains(s"$$readmemh(\"$path\", ROM);"),
        "The configured path must appear in the memory initialization"))
    }
  }

  it should "map the full expanded image across the nonzero reset-vector boundary" in {
    val config = CpuConfig(imemDepth = 128,
      instructionInitFile = Some("programs/rv32-configtest.memfile"))
    test(new IROMHarness(config)) { dut =>
      // 地址、机器码是独立固定常量；同时确认边界两侧与最大地址别名。
      val cases = Seq(0xfcL -> "00000013", 0x100L -> "01f00093",
        0x108L -> "fe112ea3", 0x134L -> "00000063", 0x1fcL -> "00000013",
        0x300L -> "01f00093", 0x80000108L -> "fe112ea3",
        0xffffffffL -> "00000013")
      for ((address, instruction) <- cases) {
        dut.io.a.poke(BigInt(address).U)
        dut.io.rd.expect(BigInt(instruction, 16).U) // 不推进时钟。
      }
    }
  }

  it should "retain loaded instructions through wrapper reset clock edges" in {
    val waves = if (sys.env.get("GENERATE_WAVES").contains("1")) {
      Seq(WriteVcdAnnotation)
    } else Seq.empty
    test(new IROMHarness(CpuConfig(instructionInitFile = Some(bookFile))))
      .withAnnotations(waves) { dut =>
      dut.io.a.poke(0.U)
      dut.io.rd.expect("h00500113".U)
      dut.reset.poke(true.B)
      dut.clock.step(3)
      dut.io.rd.expect("h00500113".U)
      dut.io.a.poke(0x50.U)
      dut.io.rd.expect("h00210063".U)
      dut.reset.poke(false.B)
      dut.clock.step()
      dut.io.rd.expect("h00210063".U) // 生产 ROM 无 reset 或重新加载路径。
    }
  }
}
