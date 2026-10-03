package riscvsingle.ieu

import chisel3._
import chisel3.stage.ChiselStage
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec
import java.nio.file.Files
import scala.util.Random

// Connect the production RegFile's explicit clock and synchronous reset ports
// to chiseltest's Module clock/reset.
private class RegFileHarness(dataWidth: Int, registerCount: Int) extends Module {
  val io = IO(new RegFileIO(dataWidth, registerCount))
  val dut = Module(new RegFile(dataWidth, registerCount))
  dut.clk := clock
  dut.reset := reset.asBool
  dut.io.WE3 := io.WE3
  dut.io.A1 := io.A1
  dut.io.A2 := io.A2
  dut.io.A3 := io.A3
  dut.io.WD3 := io.WD3
  io.RD1 := dut.io.RD1
  io.RD2 := dut.io.RD2
}

class RegFileSpec extends AnyFlatSpec with ChiselScalatestTester {
  private def idle(dut: RegFileHarness): Unit = {
    dut.io.WE3.poke(false.B)
    dut.io.A1.poke(0.U)
    dut.io.A2.poke(0.U)
    dut.io.A3.poke(0.U)
    dut.io.WD3.poke(0.U)
  }

  // Explicitly write every nonzero register before expecting its contents;
  // no test relies on the simulator's value for an uninitialized register.
  private def initialize(dut: RegFileHarness, model: Array[BigInt]): Unit = {
    idle(dut)
    dut.reset.poke(true.B)
    dut.clock.step()
    dut.reset.poke(false.B)
    for (address <- 1 until model.length) {
      dut.io.A3.poke(address.U)
      dut.io.WD3.poke(model(address).U)
      dut.io.WE3.poke(true.B)
      dut.clock.step()
    }
    dut.io.WE3.poke(false.B)
  }

  private def read(dut: RegFileHarness, model: Array[BigInt], a1: Int, a2: Int): Unit = {
    dut.io.A1.poke(a1.U)
    dut.io.A2.poke(a2.U)
    dut.io.RD1.expect(model(a1).U)
    dut.io.RD2.expect(model(a2).U)
  }

  private def randomized(dut: RegFileHarness, dataWidth: Int, registerCount: Int,
      cycles: Int): Unit = {
    val model = Array.fill(registerCount)(BigInt(0))
    initialize(dut, model)
    val random = new Random(0x215F1L + dataWidth * 100 + registerCount)
    for (cycle <- 0 until cycles) {
      val writeAddress = if (cycle % 7 == 0) 0 else random.nextInt(registerCount)
      val value = BigInt(dataWidth, random)
      val enabled = random.nextBoolean()
      val resetting = cycle % 29 == 0
      val a1 = if (cycle % 4 == 0) writeAddress else random.nextInt(registerCount)
      val a2 = if (cycle % 5 == 0) writeAddress else random.nextInt(registerCount)
      dut.io.WE3.poke(enabled.B)
      dut.io.A3.poke(writeAddress.U)
      dut.io.WD3.poke(value.U)
      dut.reset.poke(resetting.B)
      read(dut, model, a1, a2) // No write-through before the clock edge.
      dut.clock.step()
      if (!resetting && enabled && writeAddress != 0) model(writeAddress) = value
      read(dut, model, a1, a2)
      read(dut, model, random.nextInt(registerCount), random.nextInt(registerCount))
    }
    dut.io.WE3.poke(false.B)
    dut.reset.poke(false.B)
    for (address <- 0 until registerCount) {
      read(dut, model, address, registerCount - 1 - address)
    }
  }

  behavior of "RegFile"

  for (dataWidth <- Seq(32, 64)) {
    it should s"keep x0 zero after reset and read every register pair combinationally at $dataWidth bits" in {
      test(new RegFileHarness(dataWidth, 32)) { dut =>
        idle(dut)
        dut.reset.poke(true.B)
        dut.clock.step()
        dut.reset.poke(false.B)
        dut.io.RD1.expect(0.U)
        dut.io.RD2.expect(0.U)
        val mask = (BigInt(1) << dataWidth) - 1
        val model = Array.tabulate(32) { address =>
          if (address == 0) BigInt(0)
          else ((BigInt(1) << ((address * 7) % dataWidth)) | BigInt(address)) & mask
        }
        initialize(dut, model)
        for (a1 <- 0 until 32; a2 <- 0 until 32) read(dut, model, a1, a2)

        for (value <- Seq(BigInt(1), mask, BigInt(1) << (dataWidth - 1))) {
          dut.io.WE3.poke(true.B)
          dut.io.A3.poke(0.U)
          dut.io.WD3.poke(value.U)
          read(dut, model, 0, 0)
          dut.clock.step()
          read(dut, model, 0, 0)
        }
        dut.io.WE3.poke(false.B)
        for (address <- 0 until 32) read(dut, model, address, 31 - address)
      }
    }

    it should s"sample writes at rising edges and reset only x0 with priority over writes at $dataWidth bits" in {
      test(new RegFileHarness(dataWidth, 32)) { dut =>
        val model = Array.fill(32)(BigInt(0))
        initialize(dut, model)
        val mask = (BigInt(1) << dataWidth) - 1
        val highValue = (BigInt(1) << (dataWidth - 1)) | BigInt(1)

        dut.io.WE3.poke(true.B)
        dut.io.A3.poke(1.U)
        dut.io.WD3.poke(1.U)
        read(dut, model, 1, 1)
        dut.io.WD3.poke(mask.U)
        read(dut, model, 1, 1)
        dut.clock.step()
        model(1) = mask
        read(dut, model, 1, 1)

        dut.io.A3.poke(31.U)
        dut.io.WD3.poke(highValue.U)
        read(dut, model, 1, 31)
        dut.clock.step()
        model(31) = highValue
        read(dut, model, 1, 31)

        dut.io.WE3.poke(false.B)
        dut.io.A3.poke(1.U)
        dut.io.WD3.poke(0.U)
        dut.clock.step(3)
        read(dut, model, 1, 31)
        dut.io.WE3.poke(true.B)
        dut.reset.poke(true.B)
        read(dut, model, 1, 31)
        dut.clock.step(2)
        read(dut, model, 1, 31)
        read(dut, model, 0, 0)
        dut.reset.poke(false.B)
        dut.io.WE3.poke(false.B)
        for (address <- 0 until 32) read(dut, model, address, 31 - address)

        dut.io.WE3.poke(true.B)
        dut.io.WD3.poke(1.U)
        dut.clock.step()
        model(1) = 1
        read(dut, model, 1, 31)
      }
    }

    it should s"match a sequential reference model across 300 cycles at $dataWidth bits" in {
      test(new RegFileHarness(dataWidth, 32)) { dut => randomized(dut, dataWidth, 32, 300) }
    }
  }

  for ((dataWidth, registerCount) <- Seq((8, 16), (1, 2))) {
    it should s"support $registerCount registers with $dataWidth-bit data" in {
      test(new RegFileHarness(dataWidth, registerCount)) { dut =>
        randomized(dut, dataWidth, registerCount, 100)
      }
    }
  }

  it should "elaborate an N-entry register bank with direct indices and an explicit synchronous reset" in {
    for ((dataWidth, registerCount) <- Seq((32, 32), (64, 32), (8, 16), (1, 2))) {
      val targetDir = Files.createTempDirectory("regfile-interface-")
      val verilog = (new ChiselStage).emitVerilog(new RegFile(dataWidth, registerCount),
        Array("--target-dir", targetDir.toString))
      val portPattern = "(?m)^\\s*(input|output)\\s+(?:\\[(\\d+):0\\]\\s+)?(\\w+)".r
      val ports = portPattern.findAllMatchIn(verilog).map { port =>
        val width = Option(port.group(2)).map(_.toInt + 1).getOrElse(1)
        port.group(3) -> (port.group(1), width)
      }.toMap
      val addressWidth = chisel3.util.log2Ceil(registerCount)
      assert(ports == Map("clk" -> ("input", 1), "reset" -> ("input", 1), "io_WE3" -> ("input", 1),
        "io_A1" -> ("input", addressWidth), "io_A2" -> ("input", addressWidth),
        "io_A3" -> ("input", addressWidth), "io_WD3" -> ("input", dataWidth),
        "io_RD1" -> ("output", dataWidth), "io_RD2" -> ("output", dataWidth)))
      assert(verilog.contains("always @(posedge clk)"))
      val firrtl = Files.readString(targetDir.resolve("RegFile.fir"))
      assert(firrtl.contains(s"reg rf : UInt<$dataWidth>[$registerCount], clk"))
      assert(firrtl.contains("when reset :"))
      assert(firrtl.contains(s"rf[0] <= UInt<$dataWidth>(\"h0\")"))
      assert(firrtl.contains("rf[io.A3] <= io.WD3"))
      assert(firrtl.contains("io.RD1 <= rf[io.A1]"))
      assert(firrtl.contains("io.RD2 <= rf[io.A2]"))
      assert(!firrtl.contains("sub("))
      assert(!firrtl.contains("ReadIndex") && !firrtl.contains("WriteIndex"))
    }
  }

  it should "reject invalid data widths and register counts at elaboration" in {
    for ((dataWidth, registerCount, message) <-
      Seq((-1, 32, "dataWidth"), (0, 32, "dataWidth"), (32, -2, "registerCount"),
        (32, 0, "registerCount"), (32, 1, "registerCount"), (32, 3, "registerCount"),
        (32, 17, "registerCount"))) {
      val error = intercept[IllegalArgumentException] {
        (new ChiselStage).emitChirrtl(new RegFile(dataWidth, registerCount))
      }
      assert(error.getMessage.contains(message))
    }
  }
}
