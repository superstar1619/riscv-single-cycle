package riscvsingle.lsu

import chisel3._
import chisel3.stage.ChiselStage
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec
import java.nio.file.Files
import scala.util.Random

// 观察时钟仅用于测试与波形；生产模块没有时钟或复位。
private class SubwordReadHarness(dataWidth: Int) extends Module {
  val io = IO(new SubwordReadIO(dataWidth))
  val dut = Module(new SubwordRead(dataWidth))
  dut.io.ReadDataWord := io.ReadDataWord
  dut.io.ByteOffset := io.ByteOffset
  dut.io.Funct3 := io.Funct3
  io.ReadData := dut.io.ReadData
}

class SubwordReadSpec extends AnyFlatSpec with ChiselScalatestTester {
  // 设置输入后立即检查组合输出；只有波形测试按需推进观察时钟。
  private def check(dut: SubwordReadHarness, data: BigInt,
      offset: Int, funct3: Int, expected: BigInt): Unit = {
    dut.io.ReadDataWord.poke(data.U)
    dut.io.ByteOffset.poke(offset.U)
    dut.io.Funct3.poke(funct3.U)
    dut.io.ReadData.expect(expected.U)
  }

  private def hex(value: String): BigInt = BigInt(value, 16)
  private def wordMask(width: Int): BigInt = (BigInt(1) << width) - 1

  // 软件按小端字节数组取值，负数用减去 2^位数计算，不复用硬件选择器。
  private def reference(width: Int, data: BigInt, offset: Int, funct3: Int): BigInt = {
    val loads = Map(0 -> (1, true), 1 -> (2, true), 2 -> (4, true),
      4 -> (1, false), 5 -> (2, false)) ++
      (if (width == 64) Map(3 -> (8, true), 6 -> (4, false)) else Map.empty)

    loads.get(funct3).map { case (size, signed) =>
      val bytes = (0 until width / 8).map(lane => (data >> (8 * lane)) & 0xff)
      val firstByte = offset / size * size // 对应忽略大小相关的低偏移位。
      val unsigned = bytes.slice(firstByte, firstByte + size).reverse
        .foldLeft(BigInt(0))((value, byte) => (value << 8) | byte)
      val modulus = BigInt(1) << (size * 8)
      val value = if (signed && unsigned >= modulus / 2) unsigned - modulus else unsigned
      value & wordMask(width)
    }.getOrElse(BigInt(0))
  }

  behavior of "SubwordRead"

  for (width <- Seq(32, 64)) {
    it should s"demonstrate fixed loads and generate a waveform at $width bits" in {
      test(new SubwordReadHarness(width)).withAnnotations(Seq(WriteVcdAnnotation)) { dut =>
        // 小端字节依次为 01、7F、FF、80；64 位高字为 80000001。
        val data = hex(if (width == 32) "80ff7f01" else "8000000180ff7f01")
        // 列顺序：Funct3、字节偏移、手工计算的期望值。
        val cases = if (width == 32) Seq(
          (0, 0, "00000001"), (0, 1, "0000007f"), // LB，正数。
          (0, 2, "ffffffff"), (0, 3, "ffffff80"), // LB，负数。
          (4, 2, "000000ff"), (4, 3, "00000080"), // LBU。
          (1, 0, "00007f01"), (1, 2, "ffff80ff"), // LH。
          (5, 2, "000080ff"),                     // LHU。
          (2, 0, "80ff7f01")                      // LW。
        ) else Seq(
          (0, 0, "0000000000000001"), (0, 1, "000000000000007f"),
          (0, 2, "ffffffffffffffff"), (0, 3, "ffffffffffffff80"),
          (4, 2, "00000000000000ff"), (4, 3, "0000000000000080"),
          (1, 0, "0000000000007f01"), (1, 2, "ffffffffffff80ff"),
          (5, 2, "00000000000080ff"),
          (2, 0, "ffffffff80ff7f01"), // LW，选低字并符号扩展。
          (0, 7, "ffffffffffffff80"), // LB，选最高字节。
          (1, 4, "0000000000000001"), (1, 6, "ffffffffffff8000"),
          (2, 4, "ffffffff80000001"), // LW，选高字并符号扩展。
          (6, 0, "0000000080ff7f01"), (6, 4, "0000000080000001"), // LWU。
          (3, 0, "8000000180ff7f01")  // LD，原样输出。
        )

        dut.reset.poke(false.B)
        check(dut, BigInt(0), 0, 0, BigInt(0))
        dut.clock.step(2)
        for ((funct3, offset, expectedHex) <- cases) {
          check(dut, data, offset, funct3, hex(expectedHex))
          dut.clock.step(2)
        }
        check(dut, data, 0, 7, BigInt(0)) // 非法编码清零。
        dut.clock.step(2)
        check(dut, data, 0, 0, BigInt(1)) // 恢复合法读取。
        dut.clock.step(2)
      }
    }

    it should s"extend zero and both sides of every sign boundary at $width bits" in {
      test(new SubwordReadHarness(width)) { dut =>
        // LB/LH/LW：零、最大正数、最小负数、全一。
        val cases = if (width == 32) Seq(
          (0, "00", "00000000"), (0, "7f", "0000007f"),
          (0, "80", "ffffff80"), (0, "ff", "ffffffff"),
          (1, "0000", "00000000"), (1, "7fff", "00007fff"),
          (1, "8000", "ffff8000"), (1, "ffff", "ffffffff"),
          (2, "00000000", "00000000"), (2, "7fffffff", "7fffffff"),
          (2, "80000000", "80000000"), (2, "ffffffff", "ffffffff")
        ) else Seq(
          (0, "00", "0000000000000000"), (0, "7f", "000000000000007f"),
          (0, "80", "ffffffffffffff80"), (0, "ff", "ffffffffffffffff"),
          (1, "0000", "0000000000000000"), (1, "7fff", "0000000000007fff"),
          (1, "8000", "ffffffffffff8000"), (1, "ffff", "ffffffffffffffff"),
          (2, "00000000", "0000000000000000"), (2, "7fffffff", "000000007fffffff"),
          (2, "80000000", "ffffffff80000000"), (2, "ffffffff", "ffffffffffffffff")
        )
        for ((funct3, dataHex, signedHex) <- cases) {
          val data = hex(dataHex)
          check(dut, data, 0, funct3, hex(signedHex))
          // 对应 LBU/LHU/LWU 应保留正值；RV32 不支持 LWU。
          if (funct3 < 2 || width == 64) check(dut, data, 0, funct3 + 4, data)
        }
      }
    }

    it should s"reject every illegal load code at every offset at $width bits" in {
      test(new SubwordReadHarness(width)) { dut =>
        val illegalCodes = if (width == 32) Seq(3, 6, 7) else Seq(7)
        for (funct3 <- illegalCodes; offset <- 0 until width / 8) {
          check(dut, wordMask(width), offset, 0, wordMask(width))
          check(dut, wordMask(width), offset, funct3, BigInt(0))
        }
      }
    }

    it should s"use only the relevant offset bits for wider loads at $width bits" in {
      test(new SubwordReadHarness(width)) { dut =>
        val data = hex(if (width == 32) "80ff7f01" else "8000000180ff7f01")
        for (offset <- 0 until width / 8) {
          check(dut, data, offset, 1, reference(width, data, offset, 1))
          check(dut, data, offset, 2, reference(width, data, offset, 2))
          if (width == 64) check(dut, data, offset, 3, data)
        }
        // 本模块只选子字；未自然对齐的读零门控由后续 LSU 实施。
      }
    }

    it should s"isolate every selected subword from neighboring bytes at $width bits" in {
      test(new SubwordReadHarness(width)) { dut =>
        val loadSizes = Seq(0 -> 1, 4 -> 1, 1 -> 2, 5 -> 2) ++
          (if (width == 64) Seq(2 -> 4, 6 -> 4) else Seq.empty)
        for ((funct3, size) <- loadSizes; offset <- 0 until width / 8 by size) {
          val selectedMask = ((BigInt(1) << (8 * size)) - 1) << (8 * offset)
          val selectedValue = ((BigInt(1) << (8 * size - 1)) | 1) << (8 * offset)
          val neighborsOne = selectedValue | (wordMask(width) ^ selectedMask)
          val expected = reference(width, selectedValue, offset, funct3)
          check(dut, selectedValue, offset, funct3, expected)
          check(dut, neighborsOne, offset, funct3, expected)
        }
      }
    }

    it should s"update word offset and function without a clock edge at $width bits" in {
      test(new SubwordReadHarness(width)) { dut =>
        val data = hex("80ff7f01")
        val negativeByte = hex(if (width == 32) "ffffff80" else "ffffffffffffff80")
        check(dut, data, 0, 0, BigInt(1))
        check(dut, data, 3, 0, negativeByte) // 仅偏移变化。
        check(dut, data, 3, 4, BigInt(128)) // 仅功能变化：符号扩展 → 零扩展。
        check(dut, data, 3, 7, BigInt(0))
        check(dut, data, 3, 0, negativeByte) // 恢复合法读取。
        check(dut, BigInt(1), 3, 0, BigInt(0)) // 仅数据变化。
        check(dut, BigInt(1), 0, 0, BigInt(1))
        check(dut, BigInt(1), 0, 2, BigInt(1))
      }
    }

    it should s"match a seeded byte reference for all codes and offsets at $width bits" in {
      test(new SubwordReadHarness(width)) { dut =>
        val random = new Random(0x5eed11L + width)
        val edges = Seq(BigInt(0), wordMask(width), BigInt(1) << (width - 1))
        val inputs = edges ++ Seq.fill(128)(BigInt(width, random))
        for (data <- inputs; funct3 <- 0 until 8; offset <- 0 until width / 8) {
          check(dut, data, offset, funct3, reference(width, data, offset, funct3))
        }
      }
    }
  }

  it should "emit only four combinational ports at 32 and 64 bits" in {
    for (width <- Seq(32, 64)) {
      val targetDir = Files.createTempDirectory("subwordread-interface-")
      val verilog = (new ChiselStage).emitVerilog(new SubwordRead(width),
        Array("--target-dir", targetDir.toString))
      val pattern = "(?m)^\\s*(input|output)\\s+(?:\\[(\\d+):0\\]\\s+)?(\\w+)".r
      val ports = pattern.findAllMatchIn(verilog).map { port =>
        val portWidth = Option(port.group(2)).map(_.toInt + 1).getOrElse(1)
        port.group(3) -> (port.group(1), portWidth)
      }.toMap
      assert(ports == Map(
        "io_ReadDataWord" -> ("input", width),
        "io_ByteOffset" -> ("input", chisel3.util.log2Ceil(width / 8)),
        "io_Funct3" -> ("input", 3),
        "io_ReadData" -> ("output", width)
      ))
      assert(!verilog.contains("always @"))
      assert(!verilog.contains("reg "))
    }
  }

  it should "reject unsupported data widths during elaboration" in {
    for (width <- Seq(-1, 0, 1, 8, 16, 31, 33, 63, 65, 128)) {
      val error = intercept[IllegalArgumentException] {
        (new ChiselStage).emitChirrtl(new SubwordRead(width))
      }
      assert(error.getMessage.contains("SubwordRead dataWidth must be 32 or 64"))
    }
  }
}
