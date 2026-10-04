package riscvsingle.ieu

import chisel3._
import chisel3.stage.ChiselStage
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec
import riscvsingle.config.CpuConfig
import java.nio.file.Files

private class IEUHarness(config: CpuConfig = CpuConfig()) extends Module {
  val io = IO(new IEUIO(config))
  val dut = Module(new IEU(config))
  dut.clk := clock
  dut.reset := reset.asBool
  dut.io <> io
}

class IEUSpec extends AnyFlatSpec with ChiselScalatestTester {
  private val mask = (BigInt(1) << 32) - 1
  private def u32(value: BigInt): BigInt = value & mask

  // Test encoders use architectural instruction fields, not control words.
  private def iType(rd: Int, rs1: Int, immediate: Int, funct3: Int = 0, opcode: Int = 0x13): BigInt =
    (BigInt(immediate & 0xfff) << 20) | (BigInt(rs1) << 15) |
      (BigInt(funct3) << 12) | (BigInt(rd) << 7) | opcode
  private def rType(rd: Int, rs1: Int, rs2: Int, funct3: Int = 0, subtract: Boolean = false): BigInt =
    (if (subtract) BigInt(1) << 30 else BigInt(0)) | (BigInt(rs2) << 20) |
      (BigInt(rs1) << 15) | (BigInt(funct3) << 12) | (BigInt(rd) << 7) | 0x33
  private def sType(rs1: Int, rs2: Int, immediate: Int = 0, funct3: Int = 2): BigInt = {
    val imm = immediate & 0xfff
    (BigInt(imm >> 5) << 25) | (BigInt(rs2) << 20) | (BigInt(rs1) << 15) |
      (BigInt(funct3) << 12) | (BigInt(imm & 31) << 7) | 0x23
  }
  private def bType(rs1: Int, rs2: Int, immediate: Int, funct3: Int = 0): BigInt = {
    val imm = immediate & 0x1fff
    (BigInt((imm >> 12) & 1) << 31) | (BigInt((imm >> 5) & 63) << 25) |
      (BigInt(rs2) << 20) | (BigInt(rs1) << 15) |
      (BigInt(funct3) << 12) |
      (BigInt((imm >> 1) & 15) << 8) | (BigInt((imm >> 11) & 1) << 7) | 0x63
  }
  private def jType(rd: Int, immediate: Int): BigInt = {
    val imm = immediate & 0x1fffff
    (BigInt((imm >> 20) & 1) << 31) | (BigInt((imm >> 1) & 1023) << 21) |
      (BigInt((imm >> 11) & 1) << 20) | (BigInt((imm >> 12) & 255) << 12) |
      (BigInt(rd) << 7) | 0x6f
  }

  private def drive(dut: IEUHarness, instruction: BigInt, pc: Int = 0, readData: BigInt = 0): Unit = {
    dut.io.Instr.poke(instruction.U)
    dut.io.Funct3.expect(((instruction >> 12) & 7).U)
    dut.io.PC.poke(u32(BigInt(pc)).U)
    dut.io.PCPlus4.poke(u32(BigInt(pc) + 4).U)
    dut.io.ReadData.poke(u32(readData).U)
  }

  private def retire(dut: IEUHarness, instruction: BigInt, readData: BigInt = 0): Unit = {
    drive(dut, instruction, readData = readData)
    dut.clock.step()
  }

  // Observe architectural registers using a store's public WriteData output.
  // No edge is advanced and no memory is connected during this observation.
  private def expectRegister(dut: IEUHarness, index: Int, value: BigInt): Unit = {
    drive(dut, sType(rs1 = 0, rs2 = index))
    dut.io.MemWrite.expect(true.B)
    dut.io.MemRW.expect(1.U)
    dut.io.Funct3.expect(2.U)
    dut.io.PCSrc.expect(false.B)
    dut.io.IEUAdr.expect(0.U)
    dut.io.WriteData.expect(u32(value).U)
  }


  private def uType(rd: Int, immediate20: Int, opcode: Int): BigInt =
    (BigInt(immediate20 & 0xfffff) << 12) | (BigInt(rd) << 7) | opcode
  private def signed(value: BigInt, width: Int): BigInt = {
    val bits = value & ((BigInt(1) << width) - 1)
    if (bits.testBit(width - 1)) bits - (BigInt(1) << width) else bits
  }

  private case class Observation(name: String, address: BigInt, request: Int,
    taken: Boolean, data: BigInt, write: Option[(Int, BigInt)])

  // Architectural operations and immediates, independent of packed controller controls.
  // Address is the public adder contract, which differs from logical/shift results.
  private def reference(word: BigInt, pc: BigInt, regs: Array[BigInt], readData: BigInt,
    link: BigInt): Observation = {
    def field(high: Int, low: Int): Int = ((word >> low) & ((1 << (high-low+1))-1)).toInt
    val op = field(6,0); val rd = field(11,7); val f3 = field(14,12)
    val a = regs(field(19,15)); val b = regs(field(24,20)); val f7 = field(31,25)
    val immI = signed(word >> 20, 12)
    val immS = signed(((word >> 25) << 5) | ((word >> 7) & 31), 12)
    val immB = signed(((word >> 31) << 12) | (((word >> 7) & 1) << 11) |
      (((word >> 25) & 63) << 5) | (((word >> 8) & 15) << 1), 13)
    val immJ = signed(((word >> 31) << 20) | (((word >> 12) & 255) << 12) |
      (((word >> 20) & 1) << 11) | (((word >> 21) & 1023) << 1), 21)
    val immU = word & BigInt("fffff000",16)
    var name = "illegal"; var address = u32(a+b); var request = 0
    var taken = false; var write = Option.empty[(Int,BigInt)]
    def result(n: String, value: BigInt): Unit = { name = n; write = Some(rd -> u32(value)) }
    if (op == 0x33 && (f7 == 0 || (f7 == 32 && Set(0,5)(f3))) ||
        op == 0x13 && (!Set(1,5)(f3) || f7 == 0 || (f3 == 5 && f7 == 32))) {
      val rhs = if (op == 0x33) b else u32(immI)
      val shift = (rhs & 31).toInt
      val subtract = Set(2,3)(f3) || (f3 == 5 && f7 == 32) || (op == 0x33 && f3 == 0 && f7 == 32)
      address = u32(if (subtract) a-rhs else a+rhs)
      val (mnemonic,value) = f3 match {
        case 0 => if (op == 0x33 && f7 == 32) ("SUB",a-rhs) else ("ADD",a+rhs)
        case 1 => ("SLL",a << shift)
        case 2 => ("SLT",if (signed(a,32) < signed(rhs,32)) BigInt(1) else BigInt(0))
        case 3 => ("SLTU",if (a < rhs) BigInt(1) else BigInt(0))
        case 4 => ("XOR",a ^ rhs)
        case 5 => if (f7 == 32) ("SRA",signed(a,32) >> shift) else ("SRL",a >> shift)
        case 6 => ("OR",a | rhs)
        case 7 => ("AND",a & rhs)
      }
      result(if (op == 0x13) { if (mnemonic == "SLTU") "SLTIU" else mnemonic+"I" } else mnemonic,value)
    } else op match {
      case 0x03 if Set(0,1,2,4,5)(f3) =>
        address=u32(a+immI); request=2
        result(Map(0->"LB",1->"LH",2->"LW",4->"LBU",5->"LHU")(f3),readData)
      case 0x23 if Set(0,1,2)(f3) =>
        address=u32(a+immS); request=1; name=Map(0->"SB",1->"SH",2->"SW")(f3)
      case 0x63 if Set(0,1,4,5,6,7)(f3) =>
        address=u32(pc+immB)
        val branches = Map(0->("BEQ",a==b),1->("BNE",a!=b),
          4->("BLT",signed(a,32)<signed(b,32)),5->("BGE",signed(a,32)>=signed(b,32)),
          6->("BLTU",a<b),7->("BGEU",a>=b))
        name=branches(f3)._1; taken=branches(f3)._2
      case 0x6f => address=u32(pc+immJ); taken=true; result("JAL",link)
      case 0x67 if f3 == 0 => address=u32(a+immI); taken=true; result("JALR",link)
      case 0x37 => result("LUI",immU)
      case 0x17 => address=u32(pc+immU); result("AUIPC",address)
      case _ =>
    }
    // JALR bit-zero masking is an opcode-level Datapath contract, even if illegal.
    if (op == 0x67) address = (address / 2) * 2
    Observation(name,address,request,taken,b,write)
  }

  private def initialize(dut: IEUHarness): Array[BigInt] = {
    dut.reset.poke(false.B)
    val regs = Array.tabulate[BigInt](32)(i => BigInt(i))
    for (i <- 1 until 32) retire(dut,iType(i,0,i))
    regs
  }

  private def checkedStep(dut: IEUHarness, word: BigInt, pc: Int, regs: Array[BigInt],
    readData: BigInt = 0, resetting: Boolean = false, link: Option[BigInt] = None): (Observation, Int) = {
    val expected = reference(word,BigInt(pc),regs,u32(readData),link.getOrElse(u32(BigInt(pc)+4)))
    val observedRd=((word >> 7) & 31).toInt
    expectRegister(dut,observedRd,regs(observedRd))
    drive(dut,word,pc,readData)
    link.foreach(value => dut.io.PCPlus4.poke(u32(value).U))
    dut.reset.poke(resetting.B)
    dut.io.MemRW.expect(expected.request.U)
    dut.io.MemWrite.expect((expected.request == 1).B)
    dut.io.Funct3.expect(((word >> 12) & 7).U)
    dut.io.PCSrc.expect(expected.taken.B)
    dut.io.IEUAdr.expect(expected.address.U)
    dut.io.WriteData.expect(expected.data.U)
    val next = if (dut.io.PCSrc.peek().litToBoolean) dut.io.IEUAdr.peek().litValue.toInt else pc+4
    dut.clock.step()
    if (!resetting) expected.write.foreach { case (rd,value) => if (rd != 0) regs(rd)=value }
    regs(0)=0
    expectRegister(dut,observedRd,regs(observedRd))
    expectRegister(dut,0,0)
    (expected,next)
  }

  private val legalNames = Set("ADD","SUB","AND","OR","XOR","SLL","SRL","SRA","SLT","SLTU",
    "ADDI","ANDI","ORI","XORI","SLLI","SRLI","SRAI","SLTI","SLTIU",
    "LB","LH","LW","LBU","LHU","SB","SH","SW","BEQ","BNE","BLT","BGE","BLTU","BGEU",
    "JAL","JALR","LUI","AUIPC")

  behavior of "IEU"

  it should "decode arithmetic and memory instructions and write the selected results" in {
    test(new IEUHarness) { dut =>
      dut.reset.poke(false.B)
      retire(dut, iType(1, 0, 12))
      retire(dut, iType(2, 0, 7))
      retire(dut, iType(4, 0, 0, funct3 = 2, opcode = 0x03), BigInt("80000000", 16))
      val vectors = Seq(
        (iType(3, 1, -9), 3, BigInt(3), BigInt(3)),
        (rType(5, 1, 2), 5, BigInt(19), BigInt(19)),
        (rType(6, 1, 2, subtract = true), 6, BigInt(5), BigInt(5)),
        (rType(7, 1, 2, funct3 = 6), 7, BigInt(15), BigInt(19)),
        (rType(8, 1, 2, funct3 = 7), 8, BigInt(4), BigInt(19)),
        (rType(9, 4, 2, funct3 = 2), 9, BigInt(1), BigInt("7ffffff9", 16)),
        (iType(10, 2, -1, funct3 = 2), 10, BigInt(0), BigInt(8)),
        (iType(11, 1, -1, funct3 = 6), 11, mask, BigInt(11)),
        (iType(12, 1, 5, funct3 = 7), 12, BigInt(4), BigInt(17))
      )
      for ((instruction, rd, result, address) <- vectors) {
        drive(dut, instruction)
        dut.io.PCSrc.expect(false.B)
        dut.io.MemWrite.expect(false.B)
        dut.io.MemRW.expect(0.U)
        dut.io.IEUAdr.expect(address.U)
        dut.clock.step()
        expectRegister(dut, rd, result)
      }
      expectRegister(dut, 4, BigInt("80000000", 16))
      drive(dut, sType(rs1 = 1, rs2 = 8, immediate = -4))
      dut.io.IEUAdr.expect(8.U)
      dut.io.WriteData.expect(4.U)
      dut.io.MemWrite.expect(true.B)
      dut.io.MemRW.expect(1.U)
      dut.io.PCSrc.expect(false.B)
      dut.clock.step()
      expectRegister(dut, 8, 4)
    }
  }

  it should "feed register equality into beq and use external PCPlus4 for jal writeback" in {
    test(new IEUHarness) { dut =>
      dut.reset.poke(false.B)
      retire(dut, iType(1, 0, 9))
      retire(dut, iType(2, 0, 9))
      retire(dut, iType(3, 0, 13))
      drive(dut, bType(1, 2, 16), pc = 0x100)
      dut.io.PCSrc.expect(true.B)
      dut.io.MemWrite.expect(false.B)
      dut.io.MemRW.expect(0.U)
      dut.io.IEUAdr.expect(0x110.U)
      dut.clock.step()
      expectRegister(dut, 1, 9)
      expectRegister(dut, 2, 9)
      retire(dut, iType(2, 0, 7))
      drive(dut, bType(1, 2, -8), pc = 0x100)
      dut.io.PCSrc.expect(false.B)
      dut.io.IEUAdr.expect(0xf8.U)
      drive(dut, bType(1, 1, -8), pc = 0x100)
      dut.io.PCSrc.expect(true.B)
      dut.io.IEUAdr.expect(0xf8.U)
      drive(dut, jType(3, -16), pc = 0x100)
      dut.io.PCPlus4.poke(0xabc.U) // Distinct from PC+4 to verify the input wiring.
      dut.io.PCSrc.expect(true.B)
      dut.io.MemWrite.expect(false.B)
      dut.io.MemRW.expect(0.U)
      dut.io.IEUAdr.expect(0xf0.U)
      dut.clock.step()
      expectRegister(dut, 3, 0xabc)
      drive(dut, jType(0, 12), pc = 0x80)
      dut.io.PCSrc.expect(true.B)
      dut.io.IEUAdr.expect(0x8c.U)
      dut.clock.step()
      expectRegister(dut, 0, 0)
    }
  }

  it should "preserve reviewed reset and x0 behavior and disable writes for undefined opcodes" in {
    test(new IEUHarness(CpuConfig(imemDepth = 128, dmemDepth = 256, resetVector = 0x100))) { dut =>
      dut.reset.poke(false.B)
      retire(dut, iType(5, 0, 19))
      drive(dut, iType(5, 0, 42))
      // Poking a new instruction does not write a register before an edge.
      expectRegister(dut, 5, 19)
      drive(dut, iType(5, 0, 42))
      dut.reset.poke(true.B)
      dut.clock.step()
      expectRegister(dut, 5, 19)
      // Reset clears x0 and blocks register writes; decode remains combinational.
      drive(dut, sType(rs1 = 5, rs2 = 5))
      dut.io.MemWrite.expect(true.B)
      dut.io.MemRW.expect(1.U)
      dut.io.IEUAdr.expect(19.U)
      dut.io.WriteData.expect(19.U)
      dut.reset.poke(false.B)
      for (instruction <- Seq(iType(0, 0, 99), iType(0, 0, 0, funct3 = 2, opcode = 0x03))) {
        retire(dut, instruction, readData = mask)
        expectRegister(dut, 0, 0)
      }
      // These opcode values are undefined; rd=x5 and rs1=rs2=x5 stress suppression.
      for (opcode <- Seq(0x00, 0x0f, 0x73, 0x7f)) {
        val instruction = (BigInt(5) << 20) | (BigInt(5) << 15) | (BigInt(5) << 7) | opcode
        drive(dut, instruction)
        dut.io.PCSrc.expect(false.B)
        dut.io.MemWrite.expect(false.B)
        dut.io.MemRW.expect(0.U)
        dut.io.IEUAdr.expect(38.U)
        dut.clock.step()
        expectRegister(dut, 5, 19)
      }
    }
  }

  it should "feed equality signed and unsigned comparisons into every branch function" in {
    test(new IEUHarness) { dut =>
      dut.reset.poke(false.B)
      retire(dut, iType(1, 0, -1))
      retire(dut, iType(2, 0, 1))
      val values = Map(0 -> BigInt(0), 1 -> mask, 2 -> BigInt(1))
      def signed(value: BigInt): BigInt = if (value.testBit(31)) value - (BigInt(1) << 32) else value
      for ((rs1, rs2) <- Seq((1, 2), (2, 1), (1, 1), (0, 2), (2, 0));
          funct3 <- 0 until 8) {
        val a = values(rs1)
        val b = values(rs2)
        val taken = funct3 match {
          case 0 => a == b
          case 1 => a != b
          case 4 => signed(a) < signed(b)
          case 5 => signed(a) >= signed(b)
          case 6 => a < b
          case 7 => a >= b
          case _ => false
        }
        drive(dut, bType(rs1, rs2, 16, funct3), pc = 0x100)
        dut.io.PCSrc.expect(taken.B)
        dut.io.MemWrite.expect(false.B)
        dut.io.MemRW.expect(0.U)
        dut.io.WriteData.expect(b.U)
        // Invalid branches have all controls zero, selecting register addition.
        dut.io.IEUAdr.expect((if (funct3 == 2 || funct3 == 3) u32(a + b) else BigInt(0x110)).U)
        dut.clock.step()
      }
      for ((address, value) <- values) expectRegister(dut, address, value)
    }
  }

  it should "wire upper immediates and JALR links through the upgraded Datapath" in {
    test(new IEUHarness) { dut =>
      dut.reset.poke(false.B)
      drive(dut, BigInt("800000b7", 16), pc = 0x100) // lui x1, 0x80000
      dut.io.PCPlus4.poke("hfeedbeef".U)
      dut.io.PCSrc.expect(false.B)
      dut.clock.step()
      expectRegister(dut, 1, BigInt("80000000", 16))
      drive(dut, BigInt("fffff117", 16), pc = 0x100) // auipc x2, 0xfffff
      dut.io.PCPlus4.poke("hfeedbeef".U)
      dut.io.IEUAdr.expect("hfffff100".U)
      dut.clock.step()
      expectRegister(dut, 2, BigInt("fffff100", 16))
      retire(dut, iType(3, 0, 3))
      drive(dut, iType(3, 3, 0, opcode = 0x67), pc = 0x200)
      dut.io.PCPlus4.poke(0xabc.U)
      dut.io.PCSrc.expect(true.B)
      dut.io.IEUAdr.expect(2.U)
      dut.clock.step()
      expectRegister(dut, 3, 0xabc)
      drive(dut, iType(0, 3, -1, opcode = 0x67), pc = 0x200)
      dut.io.PCSrc.expect(true.B)
      dut.io.IEUAdr.expect(0xaba.U)
      dut.clock.step()
      expectRegister(dut, 0, 0)
      expectRegister(dut, 3, 0xabc)
    }
  }

  it should "execute Code Example 2.16 using only the IEU production interface" in {
    test(new IEUHarness) { dut =>
      val words = Seq("00500113", "00c00193", "ff718393", "0023e233", "0041f2b3",
        "004282b3", "02728863", "0041a233", "00020463", "00000293", "0023a233",
        "005203b3", "402383b3", "0471aa23", "06002103", "005104b3", "008001ef",
        "00100113", "00910133", "0221a023", "00210063").map(BigInt(_, 16))
      val expectedPCs = Seq(0x00, 0x04, 0x08, 0x0c, 0x10, 0x14, 0x18, 0x1c,
        0x20, 0x28, 0x2c, 0x30, 0x34, 0x38, 0x3c, 0x40, 0x48, 0x4c, 0x50)
      val takenPCs = Set(0x20, 0x40, 0x50)
      val memory = scala.collection.mutable.Map.empty[BigInt, BigInt]
      var pc = 0
      dut.reset.poke(false.B)
      for (expectedPC <- expectedPCs) {
        assert(pc == expectedPC)
        drive(dut, words(pc / 4), pc)
        val address = dut.io.IEUAdr.peek().litValue
        dut.io.ReadData.poke(memory.getOrElse(address, BigInt(0)).U)
        dut.io.PCSrc.expect(takenPCs.contains(pc).B)
        dut.io.MemWrite.expect((pc == 0x34 || pc == 0x4c).B)
        dut.io.MemRW.expect((if (pc == 0x38) 2 else if (pc == 0x34 || pc == 0x4c) 1 else 0).U)
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
      expectRegister(dut, 2, 25)
      expectRegister(dut, 3, 0x44)
      expectRegister(dut, 9, 18)
    }
  }

  it should "forward every memory request and externally completed load without reformatting" in {
    test(new IEUHarness) { dut =>
      val regs=initialize(dut)
      checkedStep(dut,uType(1,0x12345,0x37),0,regs)
      checkedStep(dut,iType(2,0,-1),0,regs)
      val fixtures=Seq(BigInt("a581807f",16),BigInt("7e80ff01",16),BigInt("ffffffff",16))
      var count=0
      for (f3 <- Seq(0,1,2,4,5); offset <- Seq(17,-19); rd <- Seq(0,9)) {
        val value=fixtures(count % fixtures.size); count+=1
        val (observed,_)=checkedStep(dut,iType(rd,1,offset,f3,0x03),0x100,regs,value)
        assert(observed.request == 2 && observed.address == u32(regs(1)+offset))
        if (rd != 0) assert(regs(rd) == value) // ReadData is already processed by the external LSU.
      }
      assert(count == 20)
      for (f3 <- Seq(0,1,2); offset <- Seq(23,-29)) {
        val before=regs.toVector
        val (observed,_)=checkedStep(dut,sType(1,2,offset,f3),0x100,regs)
        assert(observed.request == 1 && observed.data == mask)
        assert(regs.toVector == before)
        for (rd <- 0 until 32) expectRegister(dut,rd,before(rd))
      }
      for (rd <- 0 until 32) expectRegister(dut,rd,regs(rd))
    }
  }

  it should "forward raw functions while suppressing illegal controls and retain registers under reset" in {
    test(new IEUHarness) { dut =>
      val regs=initialize(dut)
      val invalid =
        Seq(3,6,7).map(f => iType(9,1,-3,f,0x03)) ++
        (3 until 8).map(f => sType(1,2,-3,f)) ++
        Seq(2,3).map(f => bType(1,2,-8,f)) ++
        Seq(1,2,31,33,127).map(f => rType(9,1,2) | (BigInt(f)<<25)) ++
        Seq(1,5).flatMap(f => Seq(0x23,0x423).map(imm => iType(9,1,imm,f))) ++
        Seq(0x1b,0x3b,0x0f,0x73,0x00,0x7f).map(op => iType(9,1,2,7,op)) ++
        (1 until 8).map(f => iType(9,1,-3,f,0x67))
      val before=regs.toVector
      for ((word,index) <- invalid.zipWithIndex) {
        val (observed,_)=checkedStep(dut,word,0x100,regs,BigInt("deadbeef",16))
        assert(observed.name == "illegal",s"invalid case $index")
        assert(observed.request == 0 && !observed.taken)
        assert(regs.toVector == before)
        expectRegister(dut,9,before(9))
      }
      assert(invalid.size == 32)
      // A legal request remains asserted throughout synchronous reset; only register writes stop.
      val resetWords=Seq(iType(9,1,-9,0,0x03),sType(1,2,7,1),iType(9,1,31,7),
        iType(9,1,0,0,0x67),uType(9,0xabcde,0x37))
      for (word <- resetWords) {
        checkedStep(dut,word,0x100,regs,mask,resetting=true)
        assert(regs.toVector == before)
        for (rd <- 0 until 32) expectRegister(dut,rd,before(rd))
      }
      dut.reset.poke(false.B)
      checkedStep(dut,iType(9,1,-9,5,0x03),0x100,regs,BigInt("81fffe80",16))
      assert(regs(9) == BigInt("81fffe80",16))
    }
  }

  it should "fetch a complete RV32I program by observed PC selection and produce independent word signatures" in {
    test(new IEUHarness) { dut =>
      val regs=initialize(dut)
      val program=scala.collection.mutable.ArrayBuffer.empty[BigInt]
      def emit(word: BigInt): Unit = program += word
      for (rd <- 1 until 32) emit(iType(rd,0,rd))
      Seq(iType(1,0,-1),iType(2,0,1),uType(3,0x80000,0x37),uType(4,0x80000,0x37),
        iType(4,4,-1),iType(5,0,33),iType(6,0,3),iType(7,0,1024),iType(29,0,0)).foreach(emit)
      Seq(rType(10,1,2),rType(11,2,6,subtract=true),rType(12,3,1,7),rType(13,3,2,6),
        rType(14,1,3,4),rType(15,6,5,1),rType(16,3,5,5),rType(17,3,5,5,subtract=true),
        rType(18,3,4,2),rType(19,1,2,3),iType(20,2,-9),iType(21,1,5,7),iType(22,3,1,6),
        iType(23,3,-1,4),iType(24,6,4,1),iType(25,3,4,5),iType(26,3,0x404,5),
        iType(27,2,-1,2),iType(28,2,-1,3)).foreach(emit)
      for (rd <- 10 to 28) emit(sType(7,rd,(rd-10)*4))
      for ((f3,a,b,c,d) <- Seq((0,2,2,1,2),(1,1,2,2,2),(4,3,4,4,3),
        (5,4,3,3,4),(6,2,1,1,2),(7,1,2,2,1))) {
        emit(bType(a,b,8,f3)); emit(iType(29,29,1000)); emit(iType(29,29,1))
        emit(bType(c,d,8,f3)); emit(iType(29,29,1)); emit(iType(29,29,1))
      }
      assert(program.size*4 == 456)
      Seq(jType(30,12),jType(0,16),iType(29,29,1000),iType(29,29,2),jType(0,-12)).foreach(emit)
      assert(program.size*4 == 476)
      Seq(iType(9,0,489),iType(9,9,0,0,0x67),jType(0,12),iType(29,29,4),
        iType(0,9,0,0,0x67)).foreach(emit)
      assert(program.size*4 == 496)
      Seq(iType(8,0,509),iType(0,8,-4,0,0x67),iType(29,29,8),
        sType(7,29,76),sType(7,30,80),sType(7,9,84),uType(8,0xfffff,0x37),sType(7,8,88),
        uType(9,1,0x17),sType(7,9,92),iType(31,7,0,2,0x03),sType(7,31,96)).foreach(emit)
      Seq(0,1,4,5).foreach(f => emit(iType(31,7,0,f,0x03)))
      emit(sType(7,31,100,0)); emit(sType(7,31,102,1))
      assert(program.size == 142)
      val signatureValues=Seq("0","fffffffe","80000000","80000001","7fffffff","6",
        "40000000","c0000000","1","0","fffffff8","5","80000001","7fffffff","30",
        "08000000","f8000000","0","1","20","1cc","1e4","fffff000","1210","0")
      val signatures=signatureValues.zipWithIndex.map { case (v,i) => BigInt(1024+i*4)->BigInt(v,16) }.toMap
      val memory=scala.collection.mutable.Map.empty[BigInt,BigInt]
      val coverage=scala.collection.mutable.Set.empty[String]
      val branchDirections=scala.collection.mutable.Set.empty[(String,Boolean)]
      val partialRequests=scala.collection.mutable.ArrayBuffer.empty[(String,BigInt,BigInt)]
      var pc=0; var steps=0
      while (pc != program.size*4 && steps < 200) {
        assert(pc >= 0 && pc % 4 == 0 && pc/4 < program.size)
        val word=program(pc/4)
        val pre=reference(word,BigInt(pc),regs,0,BigInt(pc)+4)
        val read = if (pre.request == 2) {
          assert(memory.contains(pre.address),"every external load return must be initialized")
          memory(pre.address)
        } else BigInt(0)
        val (observed,next)=checkedStep(dut,word,pc,regs,read)
        coverage += observed.name
        if (observed.name.startsWith("B")) branchDirections += ((observed.name,observed.taken))
        if (observed.name == "SW") memory(observed.address)=observed.data
        if (Set("SB","SH")(observed.name)) partialRequests += ((observed.name,observed.address,observed.data))
        pc=next; steps+=1
      }
      assert(pc == 568 && steps == 135,s"pc=$pc steps=$steps")
      assert(coverage.toSet == legalNames)
      assert(branchDirections.toSet == (for (n <- Set("BEQ","BNE","BLT","BGE","BLTU","BGEU");
        direction <- Set(false,true)) yield (n,direction)))
      assert(memory.toMap == signatures)
      assert(partialRequests.toVector == Vector(("SB",BigInt(1124),BigInt(0)),("SH",BigInt(1126),BigInt(0))))
      for (rd <- 0 until 32) expectRegister(dut,rd,regs(rd))
      println(s"IEU fixed program: ${program.size} words, $steps retired, ${coverage.size} classes, ${memory.size} word signatures")
    }
  }

  it should "match independent architectural semantics for 564 scheduled mixed instructions" in {
    test(new IEUHarness) { dut =>
      val regs=initialize(dut)
      val rng=new scala.util.Random(0x1e008L)
      val coverage=scala.collection.mutable.Map.empty[String,Int].withDefaultValue(0)
      val externalReturns=scala.collection.mutable.Map.empty[BigInt,BigInt]
      val rFunctions=Seq(("ADD",0,false),("SUB",0,true),("AND",7,false),("OR",6,false),
        ("XOR",4,false),("SLL",1,false),("SRL",5,false),("SRA",5,true),("SLT",2,false),("SLTU",3,false))
      val iFunctions=Seq(("ADDI",0,0),("ANDI",7,0),("ORI",6,0),("XORI",4,0),("SLLI",1,0),
        ("SRLI",5,0),("SRAI",5,0x400),("SLTI",2,0),("SLTIU",3,0))
      val loadFunctions=Seq(("LB",0),("LH",1),("LW",2),("LBU",4),("LHU",5))
      val storeFunctions=Seq(("SB",0),("SH",1),("SW",2))
      val branchFunctions=Seq(("BEQ",0),("BNE",1),("BLT",4),("BGE",5),("BLTU",6),("BGEU",7))
      for (cycle <- 0 until 564) {
        val slot=cycle % 47
        val a=rng.nextInt(32); val b=if (cycle % 5 == 0) a else rng.nextInt(32)
        val rd=cycle % 4 match { case 0 => 0; case 1 => a; case 2 => b; case _ => rng.nextInt(32) }
        val imm=rng.nextInt(4096)-2048; val shift=rng.nextInt(32)
        val (scheduled,word): (String,BigInt) = if (slot < 10) {
          val (n,f,sub)=rFunctions(slot); (n,rType(rd,a,b,f,sub))
        } else if (slot < 19) {
          val (n,f,prefix)=iFunctions(slot-10)
          (n,iType(rd,a,if (Set(1,5)(f)) prefix+shift else imm,f))
        } else if (slot < 24) {
          val (n,f)=loadFunctions(slot-19); (n,iType(rd,a,imm,f,0x03))
        } else if (slot < 27) {
          val (n,f)=storeFunctions(slot-24); (n,sType(a,b,imm,f))
        } else if (slot < 33) {
          val (n,f)=branchFunctions(slot-27); (n,bType(a,b,imm & ~1,f))
        } else slot match {
          case 33 => ("JAL",jType(rd,(rng.nextInt(8192)-4096) & ~1))
          case 34 => ("JALR",iType(rd,a,imm,0,0x67))
          case 35 => ("LUI",uType(rd,rng.nextInt(1<<20),0x37))
          case 36 => ("AUIPC",uType(rd,rng.nextInt(1<<20),0x17))
          case 37 => ("bad-load",iType(rd,a,imm,3,0x03))
          case 38 => ("bad-store",sType(a,b,imm,7))
          case 39 => ("bad-branch",bType(a,b,imm & ~1,2))
          case 40 => ("bad-r",rType(rd,a,b,7) | (BigInt(1)<<25))
          case 41 => ("bad-slli",iType(rd,a,0x20+shift,1))
          case 42 => ("bad-srli",iType(rd,a,0x420+shift,5))
          case 43 => ("RV64I",iType(rd,a,imm,0,0x1b))
          case 44 => ("RV64R",(rType(rd,a,b) & ~BigInt(0x7f)) | 0x3b)
          case 45 => ("FENCE",iType(rd,a,imm,0,0x0f))
          case 46 => ("SYSTEM",iType(rd,a,imm,0,0x73))
        }
        val pc=0x100+(rng.nextInt(16384)*4)
        val returnValue=BigInt(32,rng)
        val pre=reference(word,BigInt(pc),regs,returnValue,BigInt(pc)+4)
        if (pre.request == 2) externalReturns(pre.address)=returnValue
        val supplied=if (pre.request == 2) externalReturns(pre.address) else returnValue
        val link=if (Set("JAL","JALR")(scheduled)) Some(BigInt(32,rng)) else None
        val (observed,_)=checkedStep(dut,word,pc,regs,supplied,resetting=cycle % 71 == 0,link=link)
        assert(observed.name == (if (slot < 37) scheduled else "illegal"),s"cycle $cycle $scheduled")
        coverage(scheduled)+=1
      }
      assert(coverage.size == 47 && coverage.values.forall(_ == 12))
      assert(legalNames.subsetOf(coverage.keySet.toSet))
      for (rd <- 0 until 32) expectRegister(dut,rd,regs(rd))
      println("IEU mixed sequence: 564 instructions, 37 legal classes and 10 illegal categories, each scheduled 12 times")
    }
  }

  it should "elaborate the twelve book ports and the Controller Datapath hierarchy" in {
    val targetDir = Files.createTempDirectory("ieu-interface-")
    val verilog = (new ChiselStage).emitVerilog(new IEU,
      Array("--target-dir", targetDir.toString))
    val top = verilog.split("module IEU\\(")(1).split("\\);", 2)(0)
    val portPattern = "(?m)^\\s*(input|output)\\s+(?:\\[(\\d+):0\\]\\s+)?(\\w+)".r
    val ports = portPattern.findAllMatchIn(top).map { port =>
      val width = Option(port.group(2)).map(_.toInt + 1).getOrElse(1)
      port.group(3) -> (port.group(1), width)
    }.toMap
    assert(ports == Map("clk" -> ("input", 1), "reset" -> ("input", 1),
      "io_Instr" -> ("input", 32), "io_PC" -> ("input", 32),
      "io_PCPlus4" -> ("input", 32), "io_PCSrc" -> ("output", 1),
      "io_MemWrite" -> ("output", 1), "io_IEUAdr" -> ("output", 32),
      "io_MemRW" -> ("output", 2), "io_Funct3" -> ("output", 3),
      "io_WriteData" -> ("output", 32), "io_ReadData" -> ("input", 32)))
    val modules = "(?m)^module (\\w+)\\(".r.findAllMatchIn(verilog).map(_.group(1)).toSet
    assert(modules == Set("IEU", "Controller", "Datapath", "RegFile", "Extend", "Cmp", "ALU", "Shifter"))
    assert(verilog.contains("Controller c ("), "IEU must contain Controller instance c")
    assert(verilog.contains("Datapath dp ("), "IEU must contain Datapath instance dp")
  }
}
