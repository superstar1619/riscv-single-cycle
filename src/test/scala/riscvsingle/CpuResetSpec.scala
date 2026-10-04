package riscvsingle

import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec
import riscvsingle.config.CpuConfig
import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Paths}

private class CpuResetHarness(config: CpuConfig) extends Module {
  val io = IO(new RiscvSingleIO(config))
  val dut = Module(new RiscvSingle(config))
  dut.clk := clock
  dut.reset := reset.asBool
  dut.io <> io
}

class CpuResetSpec extends AnyFlatSpec with ChiselScalatestTester {
  private def image(words: Seq[String]): CpuConfig = {
    val directory = Files.createDirectories(Paths.get("target/cpu-test-images"))
    val file = Files.createTempFile(directory, "reset-write-", ".hex")
    val contents = (words ++ Seq.fill(64 - words.size)("00000013"))
      .mkString("", "\n", "\n")
    Files.write(file, contents.getBytes(StandardCharsets.UTF_8))
    CpuConfig(instructionInitFile = Some(file.toString))
  }

  behavior of "CPU reset"

  it should "suppress a pending store and preserve established RAM contents" in {
    // RAM[0] 先写 9；复位恰好覆盖随后写 99 的周期，重启后应仍读到 9。
    val config = image(Seq("00002103", "00202223", "00900093", "00102023",
      "06300093", "00102023", "00000063"))
    test(new CpuResetHarness(config)) { dut =>
      dut.reset.poke(false.B)
      dut.clock.step(5)
      dut.io.MemWrite.expect(true.B)
      dut.io.WriteData.expect(99.U)
      dut.reset.poke(true.B)
      dut.io.MemWrite.expect(false.B) // 无需等边沿，立即屏蔽请求。
      dut.io.MemRW.expect(0.U)
      dut.io.ByteMask.expect(0.U)
      dut.io.Funct3.expect(2.U)
      dut.clock.step(3)
      dut.reset.poke(false.B)
      dut.clock.step() // 重启的 LW 使用真实 DTIM 中保存的数据。
      dut.io.MemWrite.expect(true.B)
      dut.io.WriteData.expect(9.U)
      dut.io.ByteMask.expect(15.U)
    }
  }

  it should "suppress register writes and a store at the reset vector" in {
    val config = image(Seq("00102023", "00900093", "00108093", "00102223",
      "00000063"))
    test(new CpuResetHarness(config)) { dut =>
      dut.reset.poke(false.B)
      dut.clock.step(2) // x1=9，当前 ADDI 将尝试把 x1 改成 10。
      dut.io.IEUAdr.expect(10.U)
      dut.reset.poke(true.B)
      dut.io.IEUAdr.expect(10.U) // PC 仍需等同步复位边沿。
      dut.clock.step()
      dut.io.WriteData.expect(9.U)
      dut.io.MemWrite.expect(false.B) // 复位向量处的 SW 也不能写 RAM。
      dut.io.MemRW.expect(0.U)
      dut.io.ByteMask.expect(0.U)
      dut.clock.step(2)
      dut.reset.poke(false.B)
      dut.io.MemWrite.expect(true.B)
      dut.io.MemRW.expect(1.U)
      dut.io.ByteMask.expect(15.U)
      dut.io.WriteData.expect(9.U)
    }
  }
}
