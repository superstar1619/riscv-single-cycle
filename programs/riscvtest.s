# RISC-V System-on-Chip Design, Edition 1, Code Example 2.16, p. 68.
# Entry address: 0. Instruction words are listed in riscvtest.memfile.
# Success: store 7 at byte address 96, then 25 at byte address 100.
# Requires the simplified CPU's add/sub/and/or/slt/addi/lw/sw/beq/jal subset.

    .option norvc
    .option norelax
    .section .text
    .globl main
main:
    addi x2, x0, 5           # 0x00  00500113  x2 = 5
    addi x3, x0, 12          # 0x04  00c00193  x3 = 12
    addi x7, x3, -9          # 0x08  ff718393  x7 = 3
    or   x4, x7, x2          # 0x0c  0023e233  x4 = 7
    and  x5, x3, x4          # 0x10  0041f2b3  x5 = 4
    add  x5, x5, x4          # 0x14  004282b3  x5 = 11
    beq  x5, x7, end         # 0x18  02728863  not taken
    slt  x4, x3, x4          # 0x1c  0041a233  x4 = 0
    beq  x4, x0, around      # 0x20  00020463  taken
    addi x5, x0, 0           # 0x24  00000293  skipped
around:
    slt  x4, x7, x2          # 0x28  0023a233  x4 = 1
    add  x7, x4, x5          # 0x2c  005203b3  x7 = 12
    sub  x7, x7, x2          # 0x30  402383b3  x7 = 7
    sw   x7, 84(x3)          # 0x34  0471aa23  memory[96] = 7
    lw   x2, 96(x0)          # 0x38  06002103  x2 = 7
    add  x9, x2, x5          # 0x3c  005104b3  x9 = 18
    jal  x3, end             # 0x40  008001ef  x3 = 0x44; jump to 0x48
    addi x2, x0, 1           # 0x44  00100113  skipped
end:
    add  x2, x2, x9          # 0x48  00910133  x2 = 25
    sw   x2, 0x20(x3)        # 0x4c  0221a023  memory[100] = 25
done:
    beq  x2, x2, done        # 0x50  00210063  final loop
