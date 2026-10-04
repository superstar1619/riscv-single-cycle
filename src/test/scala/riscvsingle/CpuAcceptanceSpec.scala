package riscvsingle

import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec
import riscvsingle.config.CpuConfig
import scala.io.Source

// 仅连接时钟/复位，预期来自独立软件模型，不添加硬件功能或公开探针。
private class CpuAcceptanceHarness(config: CpuConfig) extends Module {
  val io = IO(new RiscvSingleIO(config))
  val dut = Module(new RiscvSingle(config))
  dut.clk := clock
  dut.reset := reset.asBool
  dut.io <> io
}

class CpuAcceptanceSpec extends AnyFlatSpec with ChiselScalatestTester {
  behavior of "RiscvSingle acceptance"

  for ((suffix, vector, imem, dmem) <- Seq(("", 0, 128, 64),
    ("-expanded", 256, 256, 128))) {
    it should s"execute complete RV32 acceptance at reset vector $vector" in {
      val name = s"programs/rv32-acceptance$suffix"
      val config = CpuConfig(imemDepth = imem, dmemDepth = dmem, resetVector = vector,
        instructionInitFile = Some(s"$name.memfile"))
      val source = Source.fromFile(s"$name.trace")
      val rows = try {
        source.getLines().filterNot(_.startsWith("#"))
          .map(_.split("\\s+").map(BigInt(_, 16))).toVector
      } finally source.close()
      val waves = if (sys.env.get("GENERATE_WAVES").contains("1")) {
        Seq(WriteVcdAnnotation)
      } else Seq.empty
      test(new CpuAcceptanceHarness(config)).withAnnotations(waves) { dut =>
        dut.reset.poke(true.B)
        dut.io.MemRW.expect(0.U)
        dut.io.MemWrite.expect(false.B)
        dut.io.ByteMask.expect(0.U)
        dut.clock.step(2)
        dut.reset.poke(false.B)
        for ((row, cycle) <- rows.zipWithIndex) {
          withClue(s"cycle=$cycle pc=${row(0).toString(16)} " +
            s"instruction=${row(1).toString(16)}: ") {
            if (row(3) != 0) dut.io.IEUAdr.expect(row(2).U)
            dut.io.MemRW.expect(row(4).U)
            dut.io.Funct3.expect(row(5).U)
            dut.io.ByteMask.expect(row(6).U)
            dut.io.MemWrite.expect((row(6) != 0).B)
            if (row(6) != 0) dut.io.WriteData.expect(row(7).U)
            dut.clock.step()
          }
        }
      }
    }
  }
}
