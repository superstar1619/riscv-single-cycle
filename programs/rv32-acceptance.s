# 教材表 7.1：全部 37 类 RV32 指令，结果写入固定签名区 128..248。
# RESET_VECTOR 由生成脚本定义；仅使用 RV32I，无压缩指令、链接松弛或栈。
.option norvc
.option norelax
.text
.org RESET_VECTOR
.globl _start, acceptance_auipc, acceptance_jal_return
.globl acceptance_jalr_return, acceptance_done
_start:
  li x1, -1
  li x2, 1
  lui x3, 0x80000
  lui x4, 0x80000
  addi x4, x4, -1
  li x5, 33                 # 寄存器移位只使用低 5 位
  li x6, 3
  li x7, 128
  li x29, 0

  add x10, x1, x2
  sw x10, 0(x7)
  sub x10, x2, x6
  sw x10, 4(x7)
  and x10, x3, x1
  sw x10, 8(x7)
  or x10, x3, x2
  sw x10, 12(x7)
  xor x10, x1, x3
  sw x10, 16(x7)
  sll x10, x6, x5
  sw x10, 20(x7)
  srl x10, x3, x5
  sw x10, 24(x7)
  sra x10, x3, x5
  sw x10, 28(x7)
  slt x10, x3, x4
  sw x10, 32(x7)
  sltu x10, x1, x2
  sw x10, 36(x7)

  addi x10, x2, -9
  sw x10, 40(x7)
  andi x10, x1, 5
  sw x10, 44(x7)
  ori x10, x3, 1
  sw x10, 48(x7)
  xori x10, x3, -1
  sw x10, 52(x7)
  slli x10, x6, 4
  sw x10, 56(x7)
  srli x10, x3, 4
  sw x10, 60(x7)
  srai x10, x3, 4
  sw x10, 64(x7)
  slti x10, x2, -1
  sw x10, 68(x7)
  sltiu x10, x2, -1
  sw x10, 72(x7)
  lui x10, 0xfffff
  sw x10, 76(x7)
acceptance_auipc:
  auipc x10, 1
  sw x10, 80(x7)

  # 每种条件分支各执行一次 taken 和一次 untaken，最终 x29=6。
  beq x2, x2, 1f
  addi x29, x29, 1000
1:beq x1, x2, 2f
  addi x29, x29, 1
2:bne x1, x2, 3f
  addi x29, x29, 1000
3:bne x2, x2, 4f
  addi x29, x29, 1
4:blt x3, x4, 5f
  addi x29, x29, 1000
5:blt x4, x3, 6f
  addi x29, x29, 1
6:bge x4, x3, 7f
  addi x29, x29, 1000
7:bge x3, x4, 8f
  addi x29, x29, 1
8:bltu x2, x1, 9f
  addi x29, x29, 1000
9:bltu x1, x2, 10f
  addi x29, x29, 1
10:bgeu x1, x2, 11f
  addi x29, x29, 1000
11:bgeu x2, x1, 12f
  addi x29, x29, 1
12:sw x29, 84(x7)

  jal x30, .Ljal_target
acceptance_jal_return:
  addi x29, x29, 1000
.Ljal_target:
  sw x30, 88(x7)
  la x9, .Ljalr_target
  ori x9, x9, 1             # 故意构造奇地址，JALR 必须清除 bit 0。
  jalr x30, 0(x9)
acceptance_jalr_return:
  addi x29, x29, 1000
.Ljalr_target:
  sw x30, 92(x7)

  sw x3, 0(x0)
  sb x1, 0(x0)
  li x11, 0x8001
  sh x11, 2(x0)            # RAM[0]=0x800100ff，小端字节保持
  lb x10, 0(x0)
  sw x10, 96(x7)
  lbu x10, 0(x0)
  sw x10, 100(x7)
  lh x10, 2(x0)
  sw x10, 104(x7)
  lhu x10, 2(x0)
  sw x10, 108(x7)
  lw x10, 0(x0)
  sw x10, 112(x7)

  sh x1, 1(x0)             # 工程约定：未对齐存储不写，未对齐读取返回零。
  lw x10, 1(x0)
  sw x10, 116(x7)
  .word 0x02000033         # 不支持的 MUL
  .word 0x00003003         # RV32 中非法的 LD
  .word 0x00004023         # 非法存储 Funct3
  .word 0x00001067         # 非法 JALR Funct3

  sw x0, 252(x0)
  sb x11, 255(x0)          # 最后一个字节：RAM[252]=0x01000000
  lw x10, 252(x0)
  sw x10, 120(x7)
  lw x10, 0(x0)
  sw x10, 112(x7)          # 再次证明未对齐 SH 未改变 RAM[0]
acceptance_done:
  beq x0, x0, acceptance_done
