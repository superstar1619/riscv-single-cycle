package riscvsingle.lsu

import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec
import riscvsingle.config.CpuConfig
import riscvsingle.ieu.IEU
import scala.util.Random

// reset 只属于测试包装层，不连接生产 LSU。
private class SubwordLSUHarness(depth: Int = 64) extends Module {
  val io = IO(new LSUIO(CpuConfig(dmemDepth = depth)))
  val dut = Module(new LSU(CpuConfig(dmemDepth = depth)))
  dut.clk := clock
  dut.io <> io
}

private class IEUWithLSUTestIO extends Bundle {
  val Instr = Input(UInt(32.W))
  val ProbeEnable = Input(Bool())
  val ProbeAdr = Input(UInt(32.W))
  val ReadData = Output(UInt(32.W))
}

// 手动提供指令，仅验证 IEU 写回与 LSU 子字通路；状态来自生产模块。
private class IEUWithLSUHarness extends Module {
  val io = IO(new IEUWithLSUTestIO)
  val ieu = Module(new IEU())
  val lsu = Module(new LSU())
  ieu.clk := clock
  ieu.reset := reset.asBool
  ieu.io.Instr := io.Instr
  ieu.io.PC := 0.U
  ieu.io.PCPlus4 := 4.U
  ieu.io.ReadData := lsu.io.ReadData
  lsu.clk := clock
  lsu.io.MemWrite := ieu.io.MemWrite && !io.ProbeEnable
  lsu.io.MemRW := Mux(io.ProbeEnable, 2.U, ieu.io.MemRW)
  lsu.io.Funct3 := Mux(io.ProbeEnable, 2.U, ieu.io.Funct3)
  lsu.io.IEUAdr := Mux(io.ProbeEnable, io.ProbeAdr, ieu.io.IEUAdr)
  lsu.io.WriteData := ieu.io.WriteData
  io.ReadData := lsu.io.ReadData
}

class LSUSubwordSpec extends AnyFlatSpec with ChiselScalatestTester {
  private val wordMask = (BigInt(1) << 32) - 1
  private def hex(value: String): BigInt = BigInt(value, 16)

  private def request(dut: SubwordLSUHarness, address: BigInt, funct3: Int,
      memRW: Int, memWrite: Boolean, data: BigInt = 0): Unit = {
    dut.io.IEUAdr.poke(address.U)
    dut.io.Funct3.poke(funct3.U)
    dut.io.MemRW.poke(memRW.U)
    dut.io.MemWrite.poke(memWrite.B)
    dut.io.WriteData.poke(data.U)
  }

  // 初始化只走真实 SW 写口；写入前不读取 RAM。
  private def write(dut: SubwordLSUHarness, address: BigInt, data: BigInt): Unit = {
    request(dut, address, 2, 1, true, data)
    dut.clock.step()
    request(dut, address, 2, 0, false)
  }

  // 不推进时钟，检查地址和功能编码的组合更新。
  private def read(dut: SubwordLSUHarness, address: BigInt,
      funct3: Int, expected: BigInt): Unit = {
    request(dut, address, funct3, 2, false)
    dut.io.ReadData.expect(expected.U)
  }

  // 独立软件模型按字节寻址，不使用生产掩码或逐级选择结构。
  private def size(funct3: Int, store: Boolean): Int = funct3 match {
    case 0 => 1
    case 1 => 2
    case 2 => 4
    case 4 if !store => 1
    case 5 if !store => 2
    case _ => 0
  }

  private def load(model: Array[Int], address: BigInt, funct3: Int): BigInt = {
    val bytes = size(funct3, store = false)
    if (bytes == 0 || address % bytes != 0) return BigInt(0)
    val base = (address % model.length).toInt
    val value = (0 until bytes).foldLeft(BigInt(0)) { (word, offset) =>
      word | (BigInt(model(base + offset)) << (8 * offset))
    }
    val signed = funct3 < 4 && value.testBit(bytes * 8 - 1)
    (if (signed) value - (BigInt(1) << (bytes * 8)) else value) & wordMask
  }

  private def store(model: Array[Int], address: BigInt, funct3: Int,
      data: BigInt): Unit = {
    val bytes = size(funct3, store = true)
    if (bytes != 0 && address % bytes == 0) {
      val base = (address % model.length).toInt
      for (offset <- 0 until bytes) {
        model(base + offset) = ((data >> (8 * offset)) & 0xff).toInt
      }
    }
  }

  behavior of "LSU subword access"

  it should "demonstrate fixed accesses and generate a waveform" in {
    test(new SubwordLSUHarness).withAnnotations(Seq(WriteVcdAnnotation)) { dut =>
      dut.reset.poke(false.B)
      request(dut, 0, 2, 0, false)
      dut.io.ReadData.expect(0.U)
      dut.clock.step(2)
      write(dut, 0, hex("11223344"))
      write(dut, 4, hex("aabbccdd"))
      request(dut, 1, 0, 1, true, hex("deadbeef"))
      dut.io.ReadData.expect(0.U)
      dut.clock.step()
      read(dut, 0, 2, hex("1122ef44")) // SB 只更新 byte 1，忽略数据高位。
      dut.clock.step(2)
      request(dut, 2, 1, 1, true, hex("face8001"))
      dut.clock.step()
      read(dut, 0, 2, hex("8001ef44")) // SH 只更新高半字。
      dut.clock.step(2)

      // 列顺序：字节地址、Funct3、固定期望值。
      val loads = Seq(
        (1, 0, "ffffffef"), // LB
        (1, 4, "000000ef"), // LBU
        (2, 1, "ffff8001"), // LH
        (2, 5, "00008001"), // LHU
        (0, 2, "8001ef44"), // LW
        (1, 1, "00000000"), // 未对齐 LH
        (2, 2, "00000000"), // 未对齐 LW
        (0, 3, "00000000")  // RV32 不支持 LD
      )
      for ((address, funct3, expected) <- loads) {
        read(dut, address, funct3, hex(expected))
        dut.clock.step(2)
      }
      for ((address, funct3) <- Seq(1 -> 1, 2 -> 2, 0 -> 4)) {
        request(dut, address, funct3, 1, true, wordMask)
        dut.clock.step(2) // 未对齐或非法存储不能破坏原字。
      }
      read(dut, 256, 2, hex("8001ef44"))
      dut.clock.step(2)
      read(dut, 4, 2, hex("aabbccdd"))
      dut.clock.step(2)
      request(dut, 0, 0, 0, true, 0)
      dut.io.ReadData.expect(0.U)
      dut.clock.step(2) // 单独 MemWrite=1 不写入。
      read(dut, 0, 2, hex("8001ef44"))
      dut.clock.step(2)
      request(dut, 0, 2, 0, false)
      dut.io.ReadData.expect(0.U)
      dut.clock.step(2)
    }
  }

  it should "check every function offset request and compatibility combination" in {
    test(new SubwordLSUHarness(2)) { dut =>
      for (funct3 <- 0 until 8; offset <- 0 until 4;
          memRW <- 0 until 4; compatible <- Seq(false, true)) {
        write(dut, 0, hex("80ff7f01"))
        write(dut, 4, hex("aabbccdd"))
        val model = Array(0x01, 0x7f, 0xff, 0x80, 0xdd, 0xcc, 0xbb, 0xaa)
        request(dut, offset, funct3, memRW, compatible, hex("dead8123"))
        val before = if ((memRW & 2) != 0) load(model, offset, funct3) else BigInt(0)
        dut.io.ReadData.expect(before.U)
        val bytes = size(funct3, store = true)
        val mask = if ((memRW & 1) != 0 && compatible && bytes != 0 &&
          offset % bytes == 0) ((1 << bytes) - 1) << offset else 0
        dut.io.ByteMask.expect(mask.U) // 有效写掩码须包含请求、兼容使能与对齐条件。
        dut.clock.step()
        if ((memRW & 1) != 0 && compatible) store(model, offset, funct3, hex("dead8123"))
        val after = if ((memRW & 2) != 0) load(model, offset, funct3) else BigInt(0)
        dut.io.ReadData.expect(after.U)
        read(dut, 0, 2, load(model, 0, 2))
        read(dut, 4, 2, hex("aabbccdd"))
      }
    }
  }

  it should "extend signed boundaries for every legal load without a clock edge" in {
    test(new SubwordLSUHarness) { dut =>
      write(dut, 0, hex("807fff00"))
      write(dut, 4, hex("80007fff"))
      val cases = Seq((0, 0, "0"), (1, 0, "ffffffff"), (2, 0, "7f"),
        (3, 0, "ffffff80"), (3, 4, "80"), (4, 1, "7fff"),
        (6, 1, "ffff8000"), (6, 5, "8000"), (4, 2, "80007fff"))
      for ((address, funct3, expected) <- cases) read(dut, address, funct3, hex(expected))
    }
  }

  it should "sample final address data and size only at rising edges" in {
    test(new SubwordLSUHarness) { dut =>
      write(dut, 0, hex("11111111"))
      write(dut, 4, hex("22222222"))
      request(dut, 0, 2, 3, true, hex("aaaaaaaa"))
      dut.io.ReadData.expect(hex("11111111").U)
      request(dut, 5, 0, 3, true, hex("deadbeef"))
      dut.io.ReadData.expect("h22".U)
      dut.clock.step()
      dut.io.ReadData.expect("hffffffef".U)
      read(dut, 0, 2, hex("11111111"))
      read(dut, 4, 2, hex("2222ef22"))
    }
  }

  it should "require both write controls and gate reads independently" in {
    test(new SubwordLSUHarness) { dut =>
      write(dut, 0, hex("12345678"))
      for ((memRW, compatible) <- Seq(0 -> true, 1 -> false, 2 -> true, 3 -> false)) {
        request(dut, 0, 2, memRW, compatible, 0)
        val expected = if ((memRW & 2) != 0) hex("12345678") else BigInt(0)
        dut.io.ReadData.expect(expected.U)
        dut.clock.step()
        read(dut, 0, 2, hex("12345678"))
      }
      request(dut, 0, 2, 1, true, hex("89abcdef"))
      dut.io.ReadData.expect(0.U)
      dut.clock.step()
      read(dut, 0, 2, hex("89abcdef"))
    }
  }

  it should "retain data and permit writes during wrapper reset" in {
    test(new SubwordLSUHarness) { dut =>
      write(dut, 0, hex("12345678"))
      dut.reset.poke(true.B)
      dut.clock.step(2)
      read(dut, 0, 2, hex("12345678"))
      write(dut, 0, hex("87654321"))
      dut.reset.poke(false.B)
      read(dut, 0, 2, hex("87654321"))
    }
  }

  it should "ignore write pulses without a rising edge" in {
    test(new SubwordLSUHarness) { dut =>
      write(dut, 0, hex("12345678"))
      request(dut, 0, 0, 3, true, wordMask)
      request(dut, 0, 0, 2, false, wordMask)
      dut.clock.step()
      read(dut, 0, 2, hex("12345678"))
    }
  }

  it should "match a seeded byte model across 600 mixed transactions" in {
    test(new SubwordLSUHarness) { dut =>
      val random = new Random(0x713)
      val model = Array.fill(256)(random.nextInt(256))
      for (address <- 0 until 256 by 4) write(dut, address, load(model, address, 2))
      for (cycle <- 0 until 600) {
        val address = (BigInt(32, random) & (wordMask ^ 3)) | ((cycle >> 3) & 3)
        val funct3 = (cycle >> 5) & 7
        val memRW = (cycle >> 1) & 3
        val compatible = (cycle & 1) != 0
        val data = BigInt(32, random)
        request(dut, address, funct3, memRW, compatible, data)
        val before = if ((memRW & 2) != 0) load(model, address, funct3) else BigInt(0)
        dut.io.ReadData.expect(before.U)
        dut.clock.step()
        if ((memRW & 1) != 0 && compatible) store(model, address, funct3, data)
        val after = if ((memRW & 2) != 0) load(model, address, funct3) else BigInt(0)
        dut.io.ReadData.expect(after.U)
      }
      for (address <- model.indices) read(dut, address, 4, model(address))
    }
  }

  it should "write back subword loads through the production IEU and LSU" in {
    test(new IEUWithLSUHarness) { dut =>
      dut.io.ProbeEnable.poke(false.B)
      dut.io.ProbeAdr.poke(0.U)
      dut.io.Instr.poke(0.U)
      dut.reset.poke(true.B)
      dut.clock.step()
      dut.reset.poke(false.B)
      val instructions = Seq(
        "80ff80b7", // lui  x1, 0x80ff8
        "08008093", // addi x1, x1, 128 -> 80ff8080
        "00102023", // sw   x1, 0(x0)
        "00000103", // lb   x2, 0(x0)
        "00004183", // lbu  x3, 0(x0)
        "00201203", // lh   x4, 2(x0)
        "00205283", // lhu  x5, 2(x0)
        "00202223", // sw   x2, 4(x0)
        "00302423", // sw   x3, 8(x0)
        "00402623", // sw   x4, 12(x0)
        "00502823", // sw   x5, 16(x0)
        "00301123", // sh   x3, 2(x0)
        "005000a3", // sb   x5, 1(x0)
        "00002303", // lw   x6, 0(x0)
        "00602a23"  // sw   x6, 20(x0)
      )
      for (instruction <- instructions) {
        dut.io.Instr.poke(hex(instruction).U)
        dut.clock.step()
      }
      dut.io.ProbeEnable.poke(true.B)
      val signatures = Seq(0 -> "0080ff80", 4 -> "ffffff80", 8 -> "00000080",
        12 -> "ffff80ff", 16 -> "000080ff", 20 -> "0080ff80")
      for ((address, expected) <- signatures) {
        dut.io.ProbeAdr.poke(address.U)
        dut.io.ReadData.expect(hex(expected).U)
      }
    }
  }

  it should "preserve boundary bytes and reject accesses crossing native words" in {
    test(new SubwordLSUHarness(2)) { dut =>
      write(dut, 0, hex("11223344"))
      write(dut, 4, hex("55667788"))
      request(dut, wordMask, 0, 1, true, hex("deadbeef"))
      dut.clock.step() // 最大字节地址回绕到最后字的 byte 3。
      read(dut, 4, 2, hex("ef667788"))
      request(dut, 7, 1, 3, true, wordMask)
      dut.io.ReadData.expect(0.U)
      dut.clock.step()
      read(dut, 4, 2, hex("ef667788"))
      read(dut, 0, 2, hex("11223344"))
      request(dut, 8, 1, 1, true, hex("face8001"))
      dut.clock.step()
      read(dut, 0, 2, hex("11228001"))
      read(dut, 8, 1, hex("ffff8001"))
    }
  }
}
