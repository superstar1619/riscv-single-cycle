package riscvsingle

import chisel3._
import chisel3.stage.ChiselStage
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec
import riscvsingle.config.CpuConfig
import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Paths}

// Thin test top: no PC/memory models, probes, or extra execution logic.
private class RiscvSingleHarness(config: CpuConfig) extends Module {
  val io = IO(new RiscvSingleIO(config))
  val dut = Module(new RiscvSingle(config))
  dut.clk := clock
  dut.reset := reset.asBool
  dut.io <> io
}

class RiscvSingleSpec extends AnyFlatSpec with ChiselScalatestTester {
  private val bookConfig = CpuConfig(instructionInitFile = Some("programs/riscvtest.memfile"))
  private val expandedConfig = CpuConfig(imemDepth = 128, dmemDepth = 128, resetVector = 0x100,
    instructionInitFile = Some("programs/rv32-configtest.memfile"))

  behavior of "RiscvSingle"

  it should "execute Code Example 2.16 and generate its waveform" in {
    // Code Example 2.16, p. 68: programs/riscvtest.s and its memfile.
    test(new RiscvSingleHarness(bookConfig)).withAnnotations(Seq(WriteVcdAnnotation)) { dut =>
      // Expected address ALU output for each actually executed instruction,
      // including the sum output when the selected ALUResult is logical/slt.
      val addresses = Seq(5L, 12L, 3L, 8L, 19L, 11L, 0x48L, 5L, 0x28L,
        0xfffffffeL, 12L, 7L, 96L, 96L, 18L, 0x48L, 25L, 100L, 0x50L)
      dut.reset.poke(true.B)
      dut.clock.step(2) // Hold synchronous reset over two rising clock edges.
      dut.reset.poke(false.B)
      for ((address, cycle) <- addresses.zipWithIndex) {
        dut.io.IEUAdr.expect(BigInt(address).U)
        dut.io.MemWrite.expect((cycle == 12 || cycle == 17).B)
        if (cycle == 12) dut.io.WriteData.expect(7.U)
        if (cycle == 17) dut.io.WriteData.expect(25.U)
        dut.clock.step()
      }
      for (_ <- 0 until 5) {
        dut.io.IEUAdr.expect(0x50.U)
        dut.io.MemWrite.expect(false.B)
        dut.io.WriteData.expect(25.U) // beq x2,x2 sees the final x2 value.
        dut.clock.step()
      }
    }
  }

  it should "execute from a nonzero reset vector with independent 128-word instruction and data memories" in {
    test(new RiscvSingleHarness(expandedConfig)) { dut =>
      // The program distinguishes word 127 (508) from word 63 (252),
      // loads both through real RAM, checks a negative load offset and jal link,
      // and skips an instruction that would replace the expected value by 999.
      val addresses = Seq(31, 511, 511, 0, 508, 7, 252, 508, 100, 0x12c, 104, 108, 0x134)
      val stores = Map(2 -> 31, 6 -> 7, 8 -> 31, 10 -> 0x128, 11 -> 31)
      dut.reset.poke(false.B)
      for ((address, cycle) <- addresses.zipWithIndex) {
        dut.io.IEUAdr.expect(address.U)
        dut.io.MemWrite.expect(stores.contains(cycle).B)
        stores.get(cycle).foreach(value => dut.io.WriteData.expect(value.U))
        dut.clock.step()
      }
      dut.io.IEUAdr.expect(0x134.U)
      dut.io.MemWrite.expect(false.B)
    }
  }

  it should "restart only at a reset clock edge and retain data RAM across CPU reset" in {
    // Initial load/store values are unspecified and are not checked; later
    // instructions establish RAM[96]=9. After reset the same load must read 9.
    val directory = Files.createDirectories(Paths.get("target/cpu-test-images"))
    val file = Files.createTempFile(directory, "cpu-reset-", ".hex")
    val words = Seq("06002183", "06302223", "00900093", "06102023", "00000063") ++
      Seq.fill(59)("00000013")
    Files.write(file, words.mkString("", "\n", "\n").getBytes(StandardCharsets.UTF_8))
    test(new RiscvSingleHarness(CpuConfig(instructionInitFile = Some(file.toString)))) { dut =>
      dut.reset.poke(false.B)
      for (address <- Seq(96, 100, 9, 96, 16)) {
        dut.io.IEUAdr.expect(address.U)
        if (address == 96 && dut.io.MemWrite.peek().litToBoolean) dut.io.WriteData.expect(9.U)
        dut.clock.step()
      }
      dut.io.IEUAdr.expect(16.U)
      dut.reset.poke(true.B)
      dut.io.IEUAdr.expect(16.U) // No edge: PC and instruction remain at the loop.
      dut.reset.poke(false.B)
      dut.io.IEUAdr.expect(16.U) // Pulse without an edge is ignored.
      dut.reset.poke(true.B)
      dut.clock.step()
      dut.io.IEUAdr.expect(96.U) // Reset edge restarts at the first load.
      dut.io.MemWrite.expect(false.B)
      dut.clock.step(2)
      dut.io.IEUAdr.expect(96.U)
      dut.reset.poke(false.B)
      dut.io.IEUAdr.expect(96.U)
      dut.clock.step() // Load the value preserved in actual LSU RAM.
      dut.io.IEUAdr.expect(100.U)
      dut.io.MemWrite.expect(true.B)
      dut.io.WriteData.expect(9.U)
      dut.clock.step()
    }
  }

  it should "emit exactly the five book ports and the complete hierarchy with synchronous state updates" in {
    for (config <- Seq(bookConfig, expandedConfig, CpuConfig())) {
      val targetDir = Files.createTempDirectory("cpu-interface-")
      val verilog = (new ChiselStage).emitVerilog(new RiscvSingle(config),
        Array("--target-dir", targetDir.toString))
      val top = verilog.split("module RiscvSingle\\(")(1).split("\\);", 2)(0)
      val portPattern = "(?m)^\\s*(input|output)\\s+(?:\\[(\\d+):0\\]\\s+)?(\\w+)".r
      val ports = portPattern.findAllMatchIn(top).map { port =>
        val width = Option(port.group(2)).map(_.toInt + 1).getOrElse(1)
        port.group(3) -> (port.group(1), width)
      }.toMap
      assert(ports == Map("clk" -> ("input", 1), "reset" -> ("input", 1),
        "io_WriteData" -> ("output", 32), "io_IEUAdr" -> ("output", 32),
        "io_MemWrite" -> ("output", 1)))
      val modules = "(?m)^module (\\w+)\\(".r.findAllMatchIn(verilog).map(_.group(1)).toSet
      assert(modules == Set("RiscvSingle", "IFU", "IEU", "LSU", "IROM",
        "Controller", "Datapath", "RegFile", "Extend", "Cmp", "ALU", "Shifter"))
      for ((module, instance) <- Seq("IFU" -> "ifu", "IEU" -> "ieu", "LSU" -> "lsu")) {
        assert(verilog.contains(s"$module $instance ("), s"Missing child $instance")
      }
      val events = "always\\s*@\\s*\\(([^)]+)\\)".r.findAllMatchIn(verilog).map(_.group(1)).toSeq
      assert(events == Seq.fill(3)("posedge clk"), "PC, registers, and RAM must update synchronously")
      assert(verilog.contains(s"ROM [0:${config.imemDepth - 1}];"), "Instruction depth must reach IROM")
      assert(verilog.contains(s"RAM [0:${config.dmemDepth - 1}];"), "Data depth must reach LSU")
      assert(verilog.contains(s"pcreg <= 32'h${config.resetVector.toString(16)};"),
        "Reset address must reach IFU")
      assert(verilog.contains("$readmemh") == config.instructionInitFile.isDefined,
        "Instruction initialization must follow configuration")
    }
  }
}
