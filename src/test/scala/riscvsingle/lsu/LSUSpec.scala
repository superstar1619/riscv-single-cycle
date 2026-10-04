package riscvsingle.lsu

import chisel3._
import chisel3.stage.ChiselStage
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec
import riscvsingle.config.CpuConfig
import riscvsingle.ifu.IFU
import riscvsingle.ieu.IEU
import java.nio.file.Files
import scala.util.Random

private class LSUHarness(config: CpuConfig = CpuConfig()) extends Module {
  val io = IO(new LSUIO(config))
  val dut = Module(new LSU(config))
  dut.clk := clock
  dut.io <> io
}

private class ExecutionWithLSUTestIO extends Bundle {
  val ProbeEnable = Input(Bool())
  val ProbeAdr = Input(UInt(32.W))
  val PC = Output(UInt(32.W))
  val Instr = Output(UInt(32.W))
  val PCSrc = Output(Bool())
  val MemWrite = Output(Bool())
  val IEUAdr = Output(UInt(32.W))
  val WriteData = Output(UInt(32.W))
  val ReadData = Output(UInt(32.W))
}

// Test-only wiring. All program execution state resides in production modules.
// A read probe checks final RAM contents while the clock is stopped.
private class ExecutionWithLSUHarness(config: CpuConfig) extends Module {
  val io = IO(new ExecutionWithLSUTestIO)
  val ifu = Module(new IFU(config))
  val ieu = Module(new IEU(config))
  val lsu = Module(new LSU(config))
  ifu.clk := clock
  ifu.reset := reset.asBool
  ieu.clk := clock
  ieu.reset := reset.asBool
  lsu.clk := clock
  ifu.io.PCSrc := ieu.io.PCSrc
  ifu.io.IEUAdr := ieu.io.IEUAdr
  ieu.io.PC := ifu.io.PC
  ieu.io.PCPlus4 := ifu.io.PCPlus4
  ieu.io.Instr := ifu.io.Instr
  ieu.io.ReadData := lsu.io.ReadData
  lsu.io.MemWrite := ieu.io.MemWrite && !io.ProbeEnable
  lsu.io.MemRW := Mux(io.ProbeEnable, 2.U, ieu.io.MemRW)
  lsu.io.Funct3 := Mux(io.ProbeEnable, 2.U, ieu.io.Funct3)
  lsu.io.IEUAdr := Mux(io.ProbeEnable, io.ProbeAdr, ieu.io.IEUAdr)
  lsu.io.WriteData := ieu.io.WriteData
  io.PC := ifu.io.PC
  io.Instr := ifu.io.Instr
  io.PCSrc := ieu.io.PCSrc
  io.MemWrite := ieu.io.MemWrite
  io.IEUAdr := ieu.io.IEUAdr
  io.WriteData := ieu.io.WriteData
  io.ReadData := lsu.io.ReadData
}

class LSUSpec extends AnyFlatSpec with ChiselScalatestTester {
  private val mask = (BigInt(1) << 32) - 1

  private def write(dut: LSUHarness, address: BigInt, value: BigInt): Unit = {
    dut.io.IEUAdr.poke(address.U)
    dut.io.WriteData.poke(value.U)
    dut.io.Funct3.poke(2.U)
    dut.io.MemRW.poke(1.U)
    dut.io.MemWrite.poke(true.B)
    dut.clock.step()
    dut.io.MemWrite.poke(false.B)
    dut.io.MemRW.poke(2.U)
  }

  behavior of "LSU"

  for (depth <- Seq(2, 64, 128)) {
    it should s"store all $depth words and read aligned word and byte aliases" in {
      test(new LSUHarness(CpuConfig(dmemDepth = depth))) { dut =>
        val random = new Random(0x215 + depth)
        val words = Seq.tabulate(depth) { index =>
          if (index == 0) BigInt(0) else if (index == depth - 1) mask else BigInt(32, random)
        }
        // Initialize through the production write port before reading any location.
        for (index <- words.indices) write(dut, BigInt(index) * 4, words(index))
        val bytes = BigInt(depth) * 4
        val aliases = Seq(BigInt(0), bytes, BigInt(1) << 31, mask ^ (bytes - 1))
        // No clock steps: all read address changes must select data immediately.
        for (index <- random.shuffle(words.indices.toVector);
            offset <- 0 until 4; alias <- aliases) {
          dut.io.IEUAdr.poke(((BigInt(index) * 4 + offset) | alias).U)
          dut.io.Funct3.poke(4.U)
          dut.io.ReadData.expect(((words(index) >> (8 * offset)) & 0xff).U)
          dut.io.IEUAdr.poke(((BigInt(index) * 4) | alias).U)
          dut.io.Funct3.poke(2.U)
          dut.io.ReadData.expect(words(index).U)
        }
      }
    }
  }

  it should "sample word writes at rising edges without forwarding pending writes" in {
    test(new LSUHarness) { dut =>
      write(dut, 0, BigInt("11111111", 16))
      write(dut, 4, BigInt("22222222", 16))
      dut.io.IEUAdr.poke(0.U)
      dut.io.WriteData.poke("haaaaaaaa".U)
      dut.io.MemWrite.poke(true.B)
      dut.io.MemRW.poke(3.U)
      dut.io.ReadData.expect("h11111111".U)
      dut.io.IEUAdr.poke(4.U)
      dut.io.WriteData.poke("hbbbbbbbb".U)
      dut.io.ReadData.expect("h22222222".U)
      dut.clock.step()
      dut.io.ReadData.expect("hbbbbbbbb".U)
      dut.io.MemWrite.poke(false.B)
      dut.io.WriteData.poke("hffffffff".U)
      dut.clock.step()
      dut.io.ReadData.expect("hbbbbbbbb".U)
      dut.io.IEUAdr.poke(0.U)
      dut.io.ReadData.expect("h11111111".U)
      dut.io.IEUAdr.poke("h80000104".U) // Aligned upper-address alias of word 1.
      dut.io.MemWrite.poke(true.B)
      dut.io.WriteData.poke("hcccccccc".U)
      dut.io.ReadData.expect("hbbbbbbbb".U)
      dut.clock.step()
      dut.io.MemWrite.poke(false.B)
      dut.io.IEUAdr.poke(4.U)
      dut.io.ReadData.expect("hcccccccc".U)
      dut.io.IEUAdr.poke(0.U)
      dut.io.ReadData.expect("h11111111".U)
    }
  }

  it should "match an independent RAM model through 300 randomized read and write cycles" in {
    test(new LSUHarness) { dut =>
      val random = new Random(0x65)
      val model = Array.fill[BigInt](64)(BigInt(32, random))
      for (index <- model.indices) write(dut, BigInt(index) * 4, model(index))
      for (_ <- 0 until 300) {
        val address = BigInt(32, random) & (mask ^ 3)
        val index = ((address / 4) % model.length).toInt
        val value = BigInt(32, random)
        val enable = random.nextBoolean()
        dut.io.IEUAdr.poke(address.U)
        dut.io.WriteData.poke(value.U)
        dut.io.MemWrite.poke(enable.B)
        dut.io.MemRW.poke(3.U)
        dut.io.ReadData.expect(model(index).U)
        dut.clock.step()
        if (enable) model(index) = value
        dut.io.ReadData.expect(model(index).U)
      }
      dut.io.MemWrite.poke(false.B)
      for (index <- model.indices) {
        dut.io.IEUAdr.poke((index * 4).U)
        dut.io.ReadData.expect(model(index).U)
      }
    }
  }

  it should "execute Code Example 2.16 with hardware fetch execution and data memory" in {
    val config = CpuConfig(instructionInitFile = Some("programs/riscvtest.memfile"))
    test(new ExecutionWithLSUHarness(config)) { dut =>
      val expectedPCs = Seq(0x00, 0x04, 0x08, 0x0c, 0x10, 0x14, 0x18, 0x1c,
        0x20, 0x28, 0x2c, 0x30, 0x34, 0x38, 0x3c, 0x40, 0x48, 0x4c, 0x50)
      dut.io.ProbeEnable.poke(false.B)
      dut.io.ProbeAdr.poke(0.U)
      dut.reset.poke(false.B)
      for (pc <- expectedPCs) {
        dut.io.PC.expect(pc.U)
        dut.io.PCSrc.expect(Set(0x20, 0x40, 0x50).contains(pc).B)
        dut.io.MemWrite.expect((pc == 0x34 || pc == 0x4c).B)
        if (pc == 0x34) {
          dut.io.IEUAdr.expect(96.U)
          dut.io.WriteData.expect(7.U)
        } else if (pc == 0x38) {
          dut.io.IEUAdr.expect(96.U)
          dut.io.ReadData.expect(7.U) // Load receives actual LSU data.
        } else if (pc == 0x4c) {
          dut.io.IEUAdr.expect(100.U)
          dut.io.WriteData.expect(25.U)
        }
        dut.clock.step()
      }
      dut.io.PC.expect(0x50.U)
      dut.clock.step(3)
      dut.io.PC.expect(0x50.U)
      dut.io.MemWrite.expect(false.B)
      // Probe only after execution, with no further clock steps or memory writes.
      dut.io.ProbeEnable.poke(true.B)
      dut.io.ProbeAdr.poke(96.U)
      dut.io.ReadData.expect(7.U)
      dut.io.ProbeAdr.poke(100.U)
      dut.io.ReadData.expect(25.U)
    }
  }

  it should "emit seven RV32 ports and parameterized byte RAM with clock-edge writes" in {
    for (depth <- Seq(64, 128)) {
      val targetDir = Files.createTempDirectory("lsu-interface-")
      val verilog = (new ChiselStage).emitVerilog(new LSU(CpuConfig(dmemDepth = depth)),
        Array("--target-dir", targetDir.toString))
      val top = verilog.split("module LSU\\(")(1).split("\\);", 2)(0)
      val portPattern = "(?m)^\\s*(input|output)\\s+(?:\\[(\\d+):0\\]\\s+)?(\\w+)".r
      val ports = portPattern.findAllMatchIn(top).map { port =>
        val width = Option(port.group(2)).map(_.toInt + 1).getOrElse(1)
        port.group(3) -> (port.group(1), width)
      }.toMap
      assert(ports == Map("clk" -> ("input", 1), "io_MemWrite" -> ("input", 1),
        "io_MemRW" -> ("input", 2), "io_Funct3" -> ("input", 3),
        "io_IEUAdr" -> ("input", 32), "io_WriteData" -> ("input", 32),
        "io_ReadData" -> ("output", 32)))
      val memories = s"reg \\[7:0\\] RAM_\\d+ \\[0:${depth - 1}\\];".r
      assert(memories.findAllIn(verilog).size == 4, "Four byte lanes must follow depth")
      val modules = "(?m)^module (\\w+)\\(".r.findAllMatchIn(verilog).map(_.group(1)).toSet
      assert(modules == Set("LSU", "DTIM", "SwByteMask", "SubwordWrite", "SubwordRead"))
      val events = "always\\s*@\\s*\\(([^)]+)\\)".r
        .findAllMatchIn(verilog).map(_.group(1)).toSeq
      assert(events == Seq("posedge clk"), "RAM writes must be synchronous")
      assert(!verilog.contains("$readmemh"), "The book's data RAM has no initialization file")
    }
  }
}
