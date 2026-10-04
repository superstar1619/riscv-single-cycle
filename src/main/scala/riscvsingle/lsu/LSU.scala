package riscvsingle.lsu

import chisel3._
import chisel3.util.MuxLookup
import riscvsingle.config.CpuConfig

/** RV32 访存请求；MemRW = {MemRead, MemWrite}，保留兼容 MemWrite。 */
final class LSUIO(config: CpuConfig) extends Bundle {
  val MemWrite = Input(Bool())
  val MemRW = Input(UInt(2.W))
  val Funct3 = Input(UInt(3.W))
  val IEUAdr = Input(UInt(config.xlen.W))
  val WriteData = Input(UInt(config.xlen.W))
  val ReadData = Output(UInt(config.xlen.W))
  val ByteMask = Output(UInt(4.W))
}

/** 教材 §7.1.6/图 7.9 的单周期 LSU，参考 CVW 的四个子字访存模块连接。
  * RV32 支持 LB/LH/LW/LBU/LHU 与 SB/SH/SW；非法或未对齐读零、不写。
  * DTIM 保持组合读、上升沿写、容量回绕，无 RAM 复位或初始化。
  * 写入要求兼容 MemWrite 与 MemRW(0) 同时有效；两位请求可独立控制。
  */
final class LSU(val config: CpuConfig = CpuConfig()) extends RawModule {
  val clk = IO(Input(Clock()))
  val io = IO(new LSUIO(config))

  val ByteOffset = Wire(UInt(2.W))
  ByteOffset := io.IEUAdr(1, 0)
  val HalfwordAligned = !ByteOffset(0)
  val WordAligned = ByteOffset === 0.U
  val LoadAllowed = Wire(Bool())
  LoadAllowed := MuxLookup(io.Funct3, false.B)(Seq(
    0.U(3.W) -> true.B,          // LB
    1.U(3.W) -> HalfwordAligned, // LH
    2.U(3.W) -> WordAligned,     // LW
    4.U(3.W) -> true.B,          // LBU
    5.U(3.W) -> HalfwordAligned  // LHU
  ))

  val swByteMask = Module(new SwByteMask(config.xlen))
  val subwordWrite = Module(new SubwordWrite(config.xlen))
  val dtim = Module(new DTIM(config.xlen, config.dmemDepth))
  val subwordRead = Module(new SubwordRead(config.xlen))

  swByteMask.io.Funct3 := io.Funct3
  swByteMask.io.ByteOffset := ByteOffset
  subwordWrite.io.Funct3 := io.Funct3
  subwordWrite.io.WriteData := io.WriteData

  // 掩码自身校验完整存储编码和自然对齐；未选中的通道保持内容。
  val ByteMask = Wire(UInt(4.W))
  val WriteDataWord = Wire(UInt(config.xlen.W))
  val ReadDataWord = Wire(UInt(config.xlen.W))
  val MemRead = io.MemRW(1) && LoadAllowed
  val MemWrite = io.MemWrite && io.MemRW(0) && ByteMask.orR
  ByteMask := swByteMask.io.ByteMask
  WriteDataWord := subwordWrite.io.WriteDataWord
  dtim.clk := clk
  dtim.io.Adr := io.IEUAdr
  dtim.io.MemRead := MemRead
  dtim.io.MemWrite := MemWrite
  dtim.io.ByteMask := ByteMask
  dtim.io.WriteDataWord := WriteDataWord
  ReadDataWord := dtim.io.ReadDataWord

  subwordRead.io.ReadDataWord := ReadDataWord
  subwordRead.io.ByteOffset := ByteOffset
  subwordRead.io.Funct3 := io.Funct3
  io.ReadData := subwordRead.io.ReadData
  // 对外报告实际写入通道；内部 ByteMask 仍保留尺寸/对齐译码结果。
  io.ByteMask := Mux(MemWrite, ByteMask, 0.U(4.W))
}
