package riscvsingle.lsu

import chisel3._
import chisel3.stage.ChiselStage
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec
import java.nio.file.Files
import scala.util.Random

// 测试包装层提供时钟；其 reset 不连接到生产 RAM。
private class DTIMHarness(dataWidth: Int, depth: Int = 64) extends Module {
  val io = IO(new DTIMIO(dataWidth))
  val dut = Module(new DTIM(dataWidth, depth))
  dut.clk := clock
  dut.io.Adr := io.Adr
  dut.io.MemRead := io.MemRead
  dut.io.MemWrite := io.MemWrite
  dut.io.WriteDataWord := io.WriteDataWord
  dut.io.ByteMask := io.ByteMask
  io.ReadDataWord := dut.io.ReadDataWord
}

class DTIMSpec extends AnyFlatSpec with ChiselScalatestTester {
  private def hex(value: String): BigInt = BigInt(value, 16)
  private def wordMask(width: Int): BigInt = (BigInt(1) << width) - 1
  private def fullByteMask(width: Int): BigInt = (BigInt(1) << (width / 8)) - 1

  private def initializeInputs(dut: DTIMHarness): Unit = {
    dut.reset.poke(false.B)
    dut.io.Adr.poke(0.U)
    dut.io.MemRead.poke(false.B)
    dut.io.MemWrite.poke(false.B)
    dut.io.WriteDataWord.poke(0.U)
    dut.io.ByteMask.poke(0.U)
  }

  // 初始化只走生产写口；写入前不读取或假定 RAM 上电值。
  private def write(dut: DTIMHarness, address: BigInt,
      data: BigInt, mask: BigInt): Unit = {
    dut.io.MemRead.poke(false.B)
    dut.io.Adr.poke(address.U)
    dut.io.WriteDataWord.poke(data.U)
    dut.io.ByteMask.poke(mask.U)
    dut.io.MemWrite.poke(true.B)
    dut.clock.step()
    dut.io.MemWrite.poke(false.B)
  }

  // 改地址后立即检查，无时钟推进，验证组合读取。
  private def read(dut: DTIMHarness, address: BigInt, expected: BigInt): Unit = {
    dut.io.MemWrite.poke(false.B)
    dut.io.MemRead.poke(true.B)
    dut.io.Adr.poke(address.U)
    dut.io.ReadDataWord.expect(expected.U)
  }

  // 独立软件模型按字节更新，再按小端顺序重组原生字。
  private def pack(bytes: Seq[Int]): BigInt =
    bytes.zipWithIndex.foldLeft(BigInt(0)) { case (word, (byte, lane)) =>
      word | (BigInt(byte) << (8 * lane))
    }

  private def maskedWord(width: Int, old: BigInt, data: BigInt, mask: BigInt): BigInt = {
    val bytes = (0 until width / 8).map { lane =>
      val source = if (mask.testBit(lane)) data else old
      ((source >> (8 * lane)) & 0xff).toInt
    }
    pack(bytes)
  }

  behavior of "DTIM"

  for (width <- Seq(32, 64)) {
    it should s"demonstrate masked writes and generate a waveform at $width bits" in {
      test(new DTIMHarness(width, 4)).withAnnotations(Seq(WriteVcdAnnotation)) { dut =>
        initializeInputs(dut)
        val bytes = width / 8
        val initial = hex(if (width == 32) "11223344" else "1122334455667788")
        val neighbor = hex(if (width == 32) "aabbccdd" else "99aabbccddeeff00")
        dut.io.ReadDataWord.expect(0.U) // 禁用读取时输出确定的零。
        dut.clock.step(2)
        write(dut, 0, initial, fullByteMask(width))
        write(dut, bytes, neighbor, fullByteMask(width))
        read(dut, 0, initial)
        dut.clock.step(2)

        // 列顺序：字节掩码、原生写数据、写入后的完整字。
        val cases = if (width == 32) Seq(
          (0x2, "a5a5a5a5", "1122a544"), // SB：只改 byte 1。
          (0xc, "f00df00d", "f00da544"), // SH：只改高半字。
          (0xf, "89abcdef", "89abcdef")  // SW：写整个字。
        ) else Seq(
          (0x02, "a5a5a5a5a5a5a5a5", "112233445566a588"),
          (0x0c, "f00df00df00df00d", "11223344f00da588"),
          (0xf0, "89abcdef89abcdef", "89abcdeff00da588"), // SW：只改高字。
          (0xff, "deadbeef01234567", "deadbeef01234567")  // SD：写整个双字。
        )
        var current = initial
        for ((mask, dataHex, expectedHex) <- cases) {
          dut.io.WriteDataWord.poke(hex(dataHex).U)
          dut.io.ByteMask.poke(mask.U)
          dut.io.MemWrite.poke(true.B)
          dut.io.ReadDataWord.expect(current.U) // 上升沿前仍是旧值。
          dut.clock.step()
          current = hex(expectedHex)
          dut.io.ReadDataWord.expect(current.U)
          dut.io.MemWrite.poke(false.B)
          dut.clock.step(2)
        }

        dut.io.WriteDataWord.poke(wordMask(width).U)
        dut.io.ByteMask.poke(fullByteMask(width).U)
        dut.clock.step(2) // MemWrite=0，数据不变。
        dut.io.ReadDataWord.expect(current.U)
        dut.io.ByteMask.poke(0.U)
        dut.io.MemWrite.poke(true.B)
        dut.clock.step(2) // 全零掩码，数据不变。
        dut.io.ReadDataWord.expect(current.U)

        dut.io.MemRead.poke(false.B)
        dut.io.ByteMask.poke(1.U)
        dut.io.WriteDataWord.poke("h5a".U)
        dut.io.ReadDataWord.expect(0.U)
        dut.clock.step(2) // 禁用读取仍允许 byte 0 写入。
        dut.io.ReadDataWord.expect(0.U)
        val finalWord = hex(if (width == 32) "89abcd5a" else "deadbeef0123455a")
        read(dut, 0, finalWord)
        dut.clock.step(2)
        read(dut, bytes, neighbor)
        dut.clock.step(2)
        read(dut, bytes * 4 + 3, finalWord) // 容量回绕且忽略低字节地址位。
        dut.clock.step(2)
        dut.io.MemRead.poke(false.B)
        dut.io.ReadDataWord.expect(0.U)
        dut.clock.step(2)
      }
    }

    for (depth <- Seq(2, 64, 128)) {
      it should s"map all $depth words and wrap byte addresses at $width bits" in {
        test(new DTIMHarness(width, depth)) { dut =>
          initializeInputs(dut)
          val bytes = width / 8
          val words = Seq.tabulate(depth)(index => (BigInt(1) << (width - 1)) | index)
          for (index <- words.indices) {
            write(dut, index * bytes, words(index), fullByteMask(width))
          }
          val capacity = BigInt(depth) * bytes
          val aliases = Seq(BigInt(0), capacity, BigInt(1) << (width - 1),
            wordMask(width) ^ (capacity - 1))
          // 已初始化全部地址；改变偏移/高位后无需时钟沿。
          for (index <- words.indices; offset <- 0 until bytes; alias <- aliases) {
            read(dut, (BigInt(index) * bytes + offset) | alias, words(index))
          }
        }
      }
    }

    it should s"apply every byte mask while preserving neighboring words at $width bits" in {
      test(new DTIMHarness(width, 2)) { dut =>
        initializeInputs(dut)
        val original = hex(if (width == 32) "10203040" else "1020304050607080")
        val data = wordMask(width) ^ original // 每个字节都与旧值不同。
        val neighbor = wordMask(width)
        write(dut, width / 8, neighbor, fullByteMask(width))
        for (mask <- 0 until (1 << (width / 8))) {
          write(dut, 0, original, fullByteMask(width))
          read(dut, 0, original)
          dut.io.WriteDataWord.poke(data.U)
          dut.io.ByteMask.poke(mask.U)
          dut.io.MemWrite.poke(true.B)
          dut.io.ReadDataWord.expect(original.U)
          dut.clock.step()
          dut.io.ReadDataWord.expect(maskedWord(width, original, data, mask).U)
          read(dut, width / 8, neighbor)
        }
      }
    }

    it should s"sample address data and mask only at rising edges at $width bits" in {
      test(new DTIMHarness(width, 2)) { dut =>
        initializeInputs(dut)
        write(dut, 0, hex("11223344"), fullByteMask(width))
        write(dut, width / 8, hex("55667788"), fullByteMask(width))
        read(dut, 0, hex("11223344"))
        dut.io.WriteDataWord.poke(hex("aaaaaaaa").U)
        dut.io.ByteMask.poke(1.U)
        dut.io.MemWrite.poke(true.B)
        dut.io.ReadDataWord.expect(hex("11223344").U)
        dut.io.Adr.poke((width / 8).U)
        dut.io.WriteDataWord.poke(hex("bbbbbbbb").U)
        dut.io.ByteMask.poke(2.U)
        dut.io.ReadDataWord.expect(hex("55667788").U)
        dut.clock.step()
        dut.io.ReadDataWord.expect(hex("5566bb88").U)
        read(dut, 0, hex("11223344")) // 瞬时写请求不能写到旧地址。
        dut.io.ByteMask.poke(fullByteMask(width).U)
        dut.io.WriteDataWord.poke(wordMask(width).U)
        dut.io.MemWrite.poke(true.B)
        dut.io.MemWrite.poke(false.B) // 没有时钟沿的脉冲不写入。
        dut.clock.step()
        dut.io.ReadDataWord.expect(hex("11223344").U)
      }
    }

    it should s"gate reads independently of writes and suppress zero masks at $width bits" in {
      test(new DTIMHarness(width, 2)) { dut =>
        initializeInputs(dut)
        write(dut, 0, hex("12345678"), fullByteMask(width))
        read(dut, 0, hex("12345678"))
        dut.io.MemRead.poke(false.B)
        dut.io.ReadDataWord.expect(0.U)
        dut.io.WriteDataWord.poke(hex("abcdef01").U)
        dut.io.ByteMask.poke(fullByteMask(width).U)
        dut.io.MemWrite.poke(true.B)
        dut.clock.step()
        dut.io.ReadDataWord.expect(0.U)
        read(dut, 0, hex("abcdef01"))
        dut.io.WriteDataWord.poke(0.U)
        dut.clock.step() // MemWrite=0，即使掩码非零也不写。
        dut.io.ReadDataWord.expect(hex("abcdef01").U)
        dut.io.ByteMask.poke(0.U)
        dut.io.MemWrite.poke(true.B)
        dut.clock.step()
        dut.io.ReadDataWord.expect(hex("abcdef01").U)
        dut.io.MemWrite.poke(false.B)
        dut.io.MemRead.poke(false.B)
        dut.io.Adr.poke((width / 8).U) // 未写位置：禁用读取仍保证输出零。
        dut.io.ReadDataWord.expect(0.U)
      }
    }

    it should s"preserve RAM through wrapper reset at $width bits" in {
      test(new DTIMHarness(width, 2)) { dut =>
        initializeInputs(dut)
        write(dut, 0, hex("12345678"), fullByteMask(width))
        dut.reset.poke(true.B)
        dut.clock.step(2)
        read(dut, 0, hex("12345678"))
        write(dut, 0, hex("87654321"), fullByteMask(width))
        read(dut, 0, hex("87654321")) // 包装层复位不能屏蔽 RAM 正常写入。
        dut.reset.poke(false.B)
        read(dut, 0, hex("87654321"))
      }
    }

    it should s"match a seeded byte array model across 300 transactions at $width bits" in {
      test(new DTIMHarness(width)) { dut =>
        initializeInputs(dut)
        val bytes = width / 8
        val random = new Random(0xd712L + width)
        val model = Array.fill(64, bytes)(random.nextInt(256))
        for (index <- model.indices) {
          write(dut, index * bytes, pack(model(index).toSeq), fullByteMask(width))
        }
        for (cycle <- 0 until 300) {
          val address = BigInt(width, random)
          val index = ((address / bytes) % model.length).toInt
          val data = Seq.fill(bytes)(random.nextInt(256))
          val mask = BigInt(bytes, random)
          val readEnable = (cycle & 1) != 0
          val writeEnable = (cycle & 2) != 0
          dut.io.Adr.poke(address.U)
          dut.io.MemRead.poke(readEnable.B)
          dut.io.MemWrite.poke(writeEnable.B)
          dut.io.WriteDataWord.poke(pack(data).U)
          dut.io.ByteMask.poke(mask.U)
          val beforeEdge = if (readEnable) pack(model(index).toSeq) else BigInt(0)
          dut.io.ReadDataWord.expect(beforeEdge.U)
          dut.clock.step()
          if (writeEnable) {
            for (lane <- 0 until bytes if mask.testBit(lane)) model(index)(lane) = data(lane)
          }
          val afterEdge = if (readEnable) pack(model(index).toSeq) else BigInt(0)
          dut.io.ReadDataWord.expect(afterEdge.U)
        }
        for (index <- model.indices) read(dut, index * bytes, pack(model(index).toSeq))
      }
    }
  }

  it should "emit seven ports with asynchronous reads and byte masked clocked writes" in {
    for (width <- Seq(32, 64); depth <- Seq(2, 64, 128)) {
      val targetDir = Files.createTempDirectory("dtim-interface-")
      val verilog = (new ChiselStage).emitVerilog(new DTIM(width, depth),
        Array("--target-dir", targetDir.toString))
      val top = verilog.split("module DTIM\\(")(1).split("\\);", 2)(0)
      val pattern = "(?m)^\\s*(input|output)\\s+(?:\\[(\\d+):0\\]\\s+)?(\\w+)".r
      val ports = pattern.findAllMatchIn(top).map { port =>
        val portWidth = Option(port.group(2)).map(_.toInt + 1).getOrElse(1)
        port.group(3) -> (port.group(1), portWidth)
      }.toMap
      assert(ports == Map(
        "clk" -> ("input", 1), "io_Adr" -> ("input", width),
        "io_MemRead" -> ("input", 1), "io_MemWrite" -> ("input", 1),
        "io_WriteDataWord" -> ("input", width), "io_ByteMask" -> ("input", width / 8),
        "io_ReadDataWord" -> ("output", width)
      ))
      val memories = s"reg \\[7:0\\] RAM_\\d+ \\[0:${depth - 1}\\];".r
      assert(memories.findAllIn(verilog).size == width / 8)
      val events = "always\\s*@\\s*\\(([^)]+)\\)".r
        .findAllMatchIn(verilog).map(_.group(1)).toSeq
      assert(events.nonEmpty && events.forall(_ == "posedge clk"))
      assert(!verilog.contains("$readmemh"))
    }
  }

  it should "reject unsupported widths depths and excessive byte capacity" in {
    for (width <- Seq(-1, 0, 1, 8, 16, 31, 33, 63, 65, 128)) {
      val error = intercept[IllegalArgumentException] {
        (new ChiselStage).emitChirrtl(new DTIM(width))
      }
      assert(error.getMessage.contains("DTIM dataWidth must be 32 or 64"))
    }
    for (depth <- Seq(-1, 0, 1, 3, 6, 63, 65, 127, 129, Int.MaxValue)) {
      val error = intercept[IllegalArgumentException] {
        (new ChiselStage).emitChirrtl(new DTIM(32, depth))
      }
      assert(error.getMessage.contains("DTIM depth must be a power of two >= 2"))
    }
    val error = intercept[IllegalArgumentException] {
      (new ChiselStage).emitChirrtl(new DTIM(64, 1 << 30))
    }
    assert(error.getMessage.contains("DTIM capacity must not exceed 2^32 bytes"))
  }
}
