#!/usr/bin/env python3
"""汇编验收程序，用独立 RV32 模型生成逐周期接口预期并校验固定签名。

生产硬件仍全部来自 Chisel；本脚本只生成测试输入/预期。
未初始化寄存器/RAM 不作为预期值，语义操作数必须由程序先建立。
"""
import json
import os
from pathlib import Path
import shutil
import subprocess

ROOT = Path(__file__).resolve().parents[1]
MASK = 0xffffffff
CLASSES = set("ADD SUB AND OR XOR SLL SRL SRA SLT SLTU ADDI ANDI ORI XORI "
              "SLLI SRLI SRAI SLTI SLTIU LUI AUIPC BEQ BNE BLT BGE BLTU BGEU "
              "JAL JALR LB LBU LH LHU LW SB SH SW".split())


def signed(value, bits=32):
    return (value & ((1 << (bits - 1)) - 1)) - (value & (1 << (bits - 1)))


def tool(name):
    prefix = os.environ.get("RISCV_TOOL_PREFIX", "riscv64-unknown-elf-")
    executable = shutil.which(prefix + name)
    if executable is None:
        raise SystemExit(f"Missing {prefix + name}; set RISCV_TOOL_PREFIX if needed")
    return executable


def run(*args):
    return subprocess.check_output(args, cwd=ROOT, text=True)


def reference(words, vector, depth, symbols):
    regs, known = [0] * 32, [False] * 32
    known[0] = True
    ram, initialized = bytearray(depth * 4), [False] * (depth * 4)
    coverage, branches, trace = set(), set(), []
    pc, loops = vector, 0

    def read(index):
        assert known[index], f"Uninitialized x{index} at PC {pc:08x}"
        return regs[index]

    for _ in range(1000):
        inst = words[(pc // 4) % len(words)]
        op, rd, f3 = inst & 127, (inst >> 7) & 31, (inst >> 12) & 7
        rs1, rs2, f7 = (inst >> 15) & 31, (inst >> 20) & 31, inst >> 25
        imm_i = signed(inst >> 20, 12)
        imm_s = signed(((inst >> 25) << 5) | ((inst >> 7) & 31), 12)
        imm_b = signed(((inst >> 31) << 12) | (((inst >> 7) & 1) << 11) |
                       (((inst >> 25) & 63) << 5) | (((inst >> 8) & 15) << 1), 13)
        imm_j = signed(((inst >> 31) << 20) | (((inst >> 12) & 255) << 12) |
                       (((inst >> 20) & 1) << 11) | (((inst >> 21) & 1023) << 1), 21)
        imm_u = inst & 0xfffff000
        name, result = "ILLEGAL", None
        next_pc, memrw, mask, data = (pc + 4) & MASK, 0, 0, 0
        # 默认控制为 ADD；LUI 和非法指令的地址输出可包含未初始化 RF 内容。
        address_known = known[rs1] and known[rs2]
        address = (regs[rs1] + regs[rs2]) & MASK
        if op == 0x33 and (f7 == 0 or (f7 == 32 and f3 in (0, 5))):
            a, b = read(rs1), read(rs2)
            name = {0: "SUB" if f7 else "ADD", 1: "SLL", 2: "SLT", 3: "SLTU",
                    4: "XOR", 5: "SRA" if f7 else "SRL", 6: "OR", 7: "AND"}[f3]
            result = {"ADD": lambda: a + b, "SUB": lambda: a - b,
                      "SLL": lambda: a << (b & 31), "SLT": lambda: int(signed(a) < signed(b)),
                      "SLTU": lambda: int(a < b), "XOR": lambda: a ^ b,
                      "SRA": lambda: signed(a) >> (b & 31), "SRL": lambda: a >> (b & 31),
                      "OR": lambda: a | b, "AND": lambda: a & b}[name]()
            address = (a - b if name in ("SUB", "SLT", "SLTU", "SRA") else a + b) & MASK
            address_known = True
        elif op == 0x13 and (f3 not in (1, 5) or
                            (f3 == 1 and f7 == 0) or (f3 == 5 and f7 in (0, 32))):
            a, b = read(rs1), imm_i & MASK
            name = {0: "ADDI", 1: "SLLI", 2: "SLTI", 3: "SLTIU", 4: "XORI",
                    5: "SRAI" if f7 else "SRLI", 6: "ORI", 7: "ANDI"}[f3]
            result = {"ADDI": lambda: a + b, "SLLI": lambda: a << (b & 31),
                      "SLTI": lambda: int(signed(a) < signed(b)), "SLTIU": lambda: int(a < b),
                      "XORI": lambda: a ^ b, "SRAI": lambda: signed(a) >> (b & 31),
                      "SRLI": lambda: a >> (b & 31), "ORI": lambda: a | b,
                      "ANDI": lambda: a & b}[name]()
            address = (a - b if name in ("SLTI", "SLTIU", "SRAI") else a + b) & MASK
            address_known = True
        elif op == 0x37:
            name, result = "LUI", imm_u
        elif op == 0x17:
            name, result = "AUIPC", (pc + imm_u) & MASK
            address, address_known = result, True
        elif op == 0x63 and f3 in (0, 1, 4, 5, 6, 7):
            a, b = read(rs1), read(rs2)
            name = {0: "BEQ", 1: "BNE", 4: "BLT", 5: "BGE", 6: "BLTU", 7: "BGEU"}[f3]
            taken = {0: a == b, 1: a != b, 4: signed(a) < signed(b),
                     5: signed(a) >= signed(b), 6: a < b, 7: a >= b}[f3]
            address, address_known = (pc + imm_b) & MASK, True
            if taken:
                next_pc = address
            branches.add((name, taken))
        elif op == 0x6f or (op == 0x67 and f3 == 0):
            name = "JAL" if op == 0x6f else "JALR"
            address = ((pc + imm_j) if op == 0x6f else (read(rs1) + imm_i) & ~1) & MASK
            address_known, result, next_pc = True, (pc + 4) & MASK, address
        elif (op == 3 and f3 in (0, 1, 2, 4, 5)) or (op == 0x23 and f3 in (0, 1, 2)):
            load = op == 3
            address = (read(rs1) + (imm_i if load else imm_s)) & MASK
            address_known, memrw = True, 2 if load else 1
            size = 1 << (f3 & 3)
            aligned = address % size == 0
            offset = address % len(ram)
            name = ({0: "LB", 1: "LH", 2: "LW", 4: "LBU", 5: "LHU"} if load
                    else {0: "SB", 1: "SH", 2: "SW"})[f3]
            if load:
                result = 0
                if aligned:
                    assert all(initialized[offset:offset + size]), f"Uninitialized RAM at {pc:x}"
                    result = int.from_bytes(ram[offset:offset + size], "little")
                    if f3 in (0, 1):
                        result = signed(result, size * 8)
            else:
                data = read(rs2)
                if aligned:
                    mask = ((1 << size) - 1) << (address & 3)
                    ram[offset:offset + size] = (data & ((1 << (size * 8)) - 1)).to_bytes(size, "little")
                    initialized[offset:offset + size] = [True] * size
        # Datapath 对 opcode=JALR 的地址 bit 0 总是清零，即使 funct3 非法。
        if op == 0x67:
            address &= ~1
        trace.append((pc, inst, address, int(address_known), memrw, f3, mask, data))
        coverage.add(name)
        if result is not None and rd != 0:
            regs[rd], known[rd] = result & MASK, True
        if pc == symbols["acceptance_done"]:
            loops += 1
            if loops == 5:
                break
        pc = next_pc
    else:
        raise AssertionError("Program did not reach done")

    # 固定常量独立校验模型，而不是将模型自身算出的结果当成签名。
    expected = [0, 0xfffffffe, 0x80000000, 0x80000001, 0x7fffffff, 6,
                0x40000000, 0xc0000000, 1, 0, 0xfffffff8, 5, 0x80000001,
                0x7fffffff, 0x30, 0x08000000, 0xf8000000, 0, 1, 0xfffff000,
                symbols["acceptance_auipc"] + 0x1000, 6,
                symbols["acceptance_jal_return"], symbols["acceptance_jalr_return"],
                0xffffffff, 0xff, 0xffff8001, 0x8001, 0x800100ff, 0, 0x01000000]
    signatures = {128 + 4 * i: value for i, value in enumerate(expected)}
    signatures.update({0: 0x800100ff, 252: 0x01000000})
    for address, value in signatures.items():
        actual = int.from_bytes(ram[address:address + 4], "little")
        assert actual == value, f"Signature {address}: {actual:08x} != {value:08x}"
    assert CLASSES <= coverage, f"Missing classes: {CLASSES - coverage}"
    assert all((name, taken) in branches for name in
               ("BEQ", "BNE", "BLT", "BGE", "BLTU", "BGEU") for taken in (False, True))
    assert "ILLEGAL" in coverage
    return trace, signatures, sorted(coverage)


def main():
    build = ROOT / "target/acceptance-images"
    build.mkdir(parents=True, exist_ok=True)
    for suffix, vector, imem, dmem in (("", 0, 128, 64), ("-expanded", 256, 256, 128)):
        name = "rv32-acceptance" + suffix
        obj, elf, binary = (build / (name + ext) for ext in (".o", ".elf", ".bin"))
        run(tool("as"), "-march=rv32i", "-mabi=ilp32", "--defsym", f"RESET_VECTOR={vector}",
            "-o", str(obj), "programs/rv32-acceptance.s")
        run(tool("ld"), "-m", "elf32lriscv", "-Ttext=0", "-e", "_start", "--no-relax",
            "-o", str(elf), str(obj))
        run(tool("objcopy"), "-O", "binary", str(elf), str(binary))
        raw = binary.read_bytes()
        assert len(raw) % 4 == 0 and len(raw) <= imem * 4
        words = [int.from_bytes(raw[i:i + 4], "little") for i in range(0, len(raw), 4)]
        # .org 的填充改为明确 NOP；每个 ROM 条目都有初始化值。
        words[:vector // 4] = [0x00000013] * (vector // 4)
        words += [0x00000013] * (imem - len(words))
        symbols = {fields[2]: int(fields[0], 16) for line in run(tool("nm"), str(elf)).splitlines()
                   if len(fields := line.split()) == 3}
        trace, signatures, coverage = reference(words, vector, dmem, symbols)
        (ROOT / "programs" / (name + ".memfile")).write_text(
            "".join(f"{word:08x}\n" for word in words))
        (ROOT / "programs" / (name + ".trace")).write_text(
            "# pc instr address address_known memrw funct3 byte_mask write_data (hex)\n" +
            "".join(" ".join(f"{v:08x}" for v in row) + "\n" for row in trace))
        metadata = dict(resetVector=vector, imemDepth=imem, dmemDepth=dmem,
                        instructionCount=len(raw) // 4 - vector // 4, traceCycles=len(trace),
                        coverage=coverage, branchOutcomes=12, symbols=symbols,
                        signatures={str(a): f"{v:08x}" for a, v in signatures.items()})
        (ROOT / "programs" / (name + ".json")).write_text(
            json.dumps(metadata, indent=2, sort_keys=True) + "\n")
        (build / (name + ".disassembly")).write_text(run(tool("objdump"), "-d", str(elf)))
        print(f"{name}: {metadata['instructionCount']} instructions, {len(trace)} cycles, "
              f"37 classes, 12 branch outcomes, {len(signatures)} fixed signatures")


if __name__ == "__main__":
    main()
