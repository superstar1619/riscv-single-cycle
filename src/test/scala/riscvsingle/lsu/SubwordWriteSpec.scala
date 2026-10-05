package riscvsingle.lsu

import chisel3._
import chisel3.stage.ChiselStage
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec
import java.nio.file.Files
import scala.util.Random

// 只有测试包装层有观察时钟；生产 SubwordWrite 是纯组合 RawModule。
private class SubwordWriteHarness(dataWidth: Int) extends Module {
  val io = IO(new SubwordWriteIO(dataWidth))
  val dut = Module(new SubwordWrite(dataWidth))
  dut.io.WriteData := io.WriteData
  dut.io.Funct3 := io.Funct3
  io.WriteDataWord := dut.io.WriteDataWord
}

class SubwordWriteSpec extends AnyFlatSpec with ChiselScalatestTester {
  // 设置输入后立即检查，不推进时钟；调用者按需保留波形观察周期。
  private def check(
    dut: SubwordWriteHarness,
    data: BigInt,
    funct3: Int,
    expected: BigInt
  ): Unit = {
    dut.io.WriteData.poke(data.U)
    dut.io.Funct3.poke(funct3.U)
    dut.io.WriteDataWord.expect(expected.U)
  }

  // 软件逐字节取值，独立于硬件的 Fill 与选择器结构。
  private def reference(width: Int, data: BigInt, funct3: Int): BigInt = {
    val accessBytes = Map(0 -> 1, 1 -> 2, 2 -> 4, 3 -> 8).getOrElse(funct3, 0)
    val wordBytes = width / 8

    if (accessBytes == 0 || accessBytes > wordBytes) BigInt(0)
    else (0 until wordBytes).foldLeft(BigInt(0)) { (word, lane) =>
      val sourceByteIndex = lane % accessBytes
      val sourceByte = (data >> (8 * sourceByteIndex)) & 0xff
      word | (sourceByte << (8 * lane))
    }
  }

  behavior of "SubwordWrite"

  for (width <- Seq(32, 64)) {
    // 固定常量：每行明确列出 Funct3 与手工计算的期望值。
    it should s"demonstrate fixed store copies and generate a waveform at $width bits" in {
      test(new SubwordWriteHarness(width)).withAnnotations(Seq(WriteVcdAnnotation)) { dut =>
        // 低位是 EF / CDEF / 89ABCDEF；SB/SH/SW 复制它们，SD 原样输出。
        val data = BigInt(if (width == 32) "89abcdef" else "0123456789abcdef", 16)
        val cases = if (width == 32) Seq(
          (0, "efefefef"), // SB
          (1, "cdefcdef"), // SH
          (2, "89abcdef")  // SW
        ) else Seq(
          (0, "efefefefefefefef"), // SB
          (1, "cdefcdefcdefcdef"), // SH
          (2, "89abcdef89abcdef"), // SW
          (3, "0123456789abcdef")  // SD
        )

        dut.reset.poke(false.B)
        check(dut, BigInt(0), 0, BigInt(0))
        dut.clock.step(2)
        for ((funct3, expectedHex) <- cases) {
          check(dut, data, funct3, BigInt(expectedHex, 16))
          dut.clock.step(2) // 每个例子留两周期，便于观察波形。
        }
        check(dut, data, 7, BigInt(0)) // 非法编码清零。
        dut.clock.step(2)
        check(dut, data, 0, BigInt(cases.head._2, 16))
        dut.clock.step(2)
      }
    }

    it should s"reject every illegal full Funct3 at $width bits" in {
      test(new SubwordWriteHarness(width)) { dut =>
        val ones = (BigInt(1) << width) - 1
        // 32 位的 011 也非法；100/101/110 不能别名到 SB/SH/SW。
        val firstIllegalCode = if (width == 32) 3 else 4
        for (funct3 <- firstIllegalCode until 8) {
          check(dut, ones, 0, ones)
          check(dut, ones, funct3, BigInt(0))
        }
      }
    }

    it should s"ignore all bits above the selected subword at $width bits" in {
      test(new SubwordWriteHarness(width)) { dut =>
        // 列顺序：Funct3、高位全零的输入、高位全一的输入、相同期望输出。
        val cases = if (width == 32) Seq(
          (0, "000000a5", "ffffffa5", "a5a5a5a5"),
          (1, "0000f00d", "fffff00d", "f00df00d")
        ) else Seq(
          (0, "00000000000000a5", "ffffffffffffffa5", "a5a5a5a5a5a5a5a5"),
          (1, "000000000000f00d", "fffffffffffff00d", "f00df00df00df00d"),
          (2, "0000000089abcdef", "ffffffff89abcdef", "89abcdef89abcdef")
        )
        for ((funct3, highBitsZero, highBitsOne, expectedHex) <- cases) {
          val expected = BigInt(expectedHex, 16)
          check(dut, BigInt(highBitsZero, 16), funct3, expected)
          check(dut, BigInt(highBitsOne, 16), funct3, expected)
        }
      }
    }

    it should s"update data and function without a clock edge at $width bits" in {
      test(new SubwordWriteHarness(width)) { dut =>
        val byteWord = BigInt(if (width == 32) "80808080" else "8080808080808080", 16)
        val halfWord = BigInt(if (width == 32) "ff80ff80" else "ff80ff80ff80ff80", 16)
        val oneByteWord = BigInt(if (width == 32) "01010101" else "0101010101010101", 16)
        val data = BigInt("ff80", 16)

        check(dut, data, 0, byteWord)
        // 仅数据变化，随后仅 Funct3 变化；全程不调用 clock.step。
        check(dut, BigInt(1), 0, oneByteWord)
        check(dut, data, 0, byteWord)
        check(dut, data, 1, halfWord)
        check(dut, data, 5, BigInt(0))
        check(dut, data, 0, byteWord)
        check(dut, BigInt(0), 0, BigInt(0))
      }
    }

    it should s"match a fixed-seed byte reference for every Funct3 at $width bits" in {
      test(new SubwordWriteHarness(width)) { dut =>
        val random = new Random(0x5eedL + width)
        val edges = Seq(
          BigInt(0),                   // 全零。
          (BigInt(1) << width) - 1,     // 全一。
          BigInt(1) << (width - 1)      // 仅最高位置位。
        )
        val randomInputs = Seq.fill(256)(BigInt(width, random))
        for (data <- edges ++ randomInputs; funct3 <- 0 until 8) {
          check(dut, data, funct3, reference(width, data, funct3))
        }
      }
    }
  }

  it should "emit only three combinational ports at 32 and 64 bits" in {
    for (width <- Seq(32, 64)) {
      val targetDir = Files.createTempDirectory("subwordwrite-interface-")
      val verilog = (new ChiselStage).emitVerilog(new SubwordWrite(width),
        Array("--target-dir", targetDir.toString))
      val pattern = "(?m)^\\s*(input|output)\\s+(?:\\[(\\d+):0\\]\\s+)?(\\w+)".r
      // 正则捕获组：1=方向，2=最高位编号（可省略），3=端口名。
      val ports = pattern.findAllMatchIn(verilog).map { port =>
        val direction = port.group(1)
        val portWidth = Option(port.group(2)).map(_.toInt + 1).getOrElse(1)
        val name = port.group(3)
        name -> (direction, portWidth)
      }.toMap
      assert(ports == Map(
        "io_WriteData" -> ("input", width),
        "io_Funct3" -> ("input", 3),
        "io_WriteDataWord" -> ("output", width)
      ))
      assert(!verilog.contains("always @"))
      assert(!verilog.contains("reg "))
    }
  }

  it should "reject unsupported data widths during elaboration" in {
    for (width <- Seq(-1, 0, 1, 8, 16, 31, 33, 63, 65, 128)) {
      val error = intercept[IllegalArgumentException] {
        (new ChiselStage).emitChirrtl(new SubwordWrite(width))
      }
      assert(error.getMessage.contains("SubwordWrite dataWidth must be 32 or 64"))
    }
  }
}
