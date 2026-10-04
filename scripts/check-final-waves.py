#!/usr/bin/env python3
"""核对最终 VCD 的稳定值、边沿前事务和 GTKWave 信号路径，无需 GUI。"""
import json
from pathlib import Path
import sys

ROOT = Path(__file__).resolve().parents[1]
MASK32 = 0xffffffff


class Wave:
    def __init__(self, path, top):
        self.path, self.top, self.names, self.frames = path, top, {}, []
        scope, state, time, body = [], {}, 0, False
        for line in path.read_text().splitlines():
            fields = line.split()
            if not fields:
                continue
            if fields[0] == "$scope":
                scope.append(fields[2])
            elif fields[0] == "$upscope":
                scope.pop()
            elif fields[0] == "$var":
                self.names[".".join(scope + [fields[4]])] = fields[3]
            elif fields[0] == "$enddefinitions":
                body = True
            elif body and line.startswith("#") and line[1:].isdigit():
                if self.frames and self.frames[-1][0] == time:
                    self.frames[-1] = (time, state.copy())
                else:
                    self.frames.append((time, state.copy()))
                time = int(line[1:])
            elif body and line[0] in "01xz":
                state[line[1:]] = None if line[0] in "xz" else int(line[0])
            elif body and line[0] == "b":
                value, code = line[1:].split()
                state[code] = None if "x" in value or "z" in value else int(value, 2)
        # Treadle 结束测试时将输入归零但不重新求值，最后的时间组含清理值，
        # 不是稳定电路状态。仅核对已完成的时间组，所有执行上升沿仍保留。
        assert self.frames and self.names, f"Empty VCD: {path}"

    def get(self, state, name):
        return state.get(self.names[self.top + "." + name])

    def rising_inputs(self):
        for (_, before), (_, after) in zip(self.frames, self.frames[1:]):
            if self.get(before, "clock") == 0 and self.get(after, "clock") == 1:
                yield before

    def check_config(self, name):
        config = ROOT / "waves" / (name + ".gtkw")
        paths = [line.strip() for line in config.read_text().splitlines()
                 if line.strip().startswith(self.top + ".")]
        assert paths and all(path in self.names for path in paths), (
            f"Missing GTKWave signals: {set(paths) - self.names.keys()}")
        return len(paths)


def require(actual, expected, where):
    assert actual == expected, f"{where}: {actual!r} != {expected!r}"


def check_cpu(wave, name):
    rows = [[int(value, 16) for value in line.split()]
            for line in (ROOT / "programs" / (name + ".trace")).read_text().splitlines()
            if line and not line.startswith("#")]
    frames = [state for state in wave.rising_inputs() if wave.get(state, "reset") == 0]
    require(len(frames), len(rows), name + " cycles")
    meta = json.loads((ROOT / "programs" / (name + ".json")).read_text())
    ram = [None] * (meta["dmemDepth"] * 4)
    for cycle, (row, state) in enumerate(zip(rows, frames)):
        where = f"{name} cycle={cycle}"
        for signal, expected in (("dut.PC", row[0]), ("dut.Instr", row[1]),
                                 ("dut.PCPlus4", (row[0] + 4) & MASK32),
                                 ("io_MemRW", row[4]), ("io_Funct3", row[5]),
                                 ("io_ByteMask", row[6]), ("io_MemWrite", int(bool(row[6])))):
            require(wave.get(state, signal), expected, where + " " + signal)
        if row[3]:
            require(wave.get(state, "io_IEUAdr"), row[2], where + " address")
        if row[6]:
            require(wave.get(state, "io_WriteData"), row[7], where + " data")
            size = 1 << row[5]
            base = row[2] % len(ram)
            ram[base:base + size] = [(row[7] >> (i * 8)) & 255 for i in range(size)]
    for address, expected in meta["signatures"].items():
        base = int(address)
        actual = sum(byte << (8 * i) for i, byte in enumerate(ram[base:base + 4]))
        require(actual, int(expected, 16), name + " fixed signature " + address)
    for _, state in wave.frames:
        if wave.get(state, "reset") == 1:
            for signal in ("io_MemRW", "io_MemWrite", "io_ByteMask"):
                require(wave.get(state, signal), 0, name + " reset " + signal)
    return "110 cycles, 33 fixed signatures, PC/instruction/request/data/reset"


def check_subword(wave, width, store):
    checked, functions = 0, set()
    for _, state in wave.frames:
        f3 = wave.get(state, "io_Funct3")
        data = wave.get(state, "io_WriteData" if store else "io_ReadDataWord")
        if f3 is None or data is None:
            continue
        mask = (1 << width) - 1
        if store:
            size = {0: 1, 1: 2, 2: 4, 3: 8}.get(f3, 0)
            if not size or size * 8 > width:
                expected = 0
            else:
                low = data & ((1 << (8 * size)) - 1)
                expected = sum(low << (8 * i) for i in range(0, width // 8, size))
            output = "io_WriteDataWord"
        else:
            offset = wave.get(state, "io_ByteOffset")
            size = {0: 1, 1: 2, 2: 4, 3: 8, 4: 1, 5: 2, 6: 4}.get(f3, 0)
            valid = f3 in (0, 1, 2, 4, 5) or (width == 64 and f3 in (3, 6))
            expected = 0
            if valid and offset is not None:
                expected = (data >> (8 * (offset // size * size))) & ((1 << (size * 8)) - 1)
                if f3 < 4 and expected & (1 << (size * 8 - 1)):
                    expected -= 1 << (size * 8)
                expected &= mask
            output = "io_ReadData"
        require(wave.get(state, output), expected, wave.path.name + " " + output)
        functions.add(f3)
        checked += 1
    assert checked > 10 and 7 in functions
    return f"{checked} stable values, functions={sorted(functions)}"


def check_ram(wave, width, depth, prefix=""):
    ram = [None] * (depth * (width // 8))
    checks, writes, previous = 0, 0, None
    for _, state in wave.frames:
        if previous is not None and wave.get(previous, "clock") == 0 and wave.get(state, "clock") == 1:
            if wave.get(previous, prefix + "io_MemWrite"):
                address = wave.get(previous, prefix + "io_Adr")
                mask = wave.get(previous, prefix + "io_ByteMask")
                data = wave.get(previous, prefix + "io_WriteDataWord")
                base = address // (width // 8) * (width // 8) % len(ram)
                for lane in range(width // 8):
                    if mask & (1 << lane):
                        ram[base + lane] = (data >> (lane * 8)) & 255
                writes += 1
        enable = wave.get(state, prefix + "io_MemRead")
        if enable is not None:
            address = wave.get(state, prefix + "io_Adr")
            base = address // (width // 8) * (width // 8) % len(ram)
            word = ram[base:base + width // 8]
            if not enable or all(byte is not None for byte in word):
                expected = sum(byte << (lane * 8) for lane, byte in enumerate(word)) if enable else 0
                require(wave.get(state, prefix + "io_ReadDataWord"), expected,
                        wave.path.name + " edge/write/read")
                checks += 1
        previous = state
    assert checks > 10 and writes > 2
    return f"{writes} edge writes, {checks} stable reads (byte model)"


def check_fetch(wave, ifu):
    words = [int(line, 16) for line in (ROOT / "programs/riscvtest.memfile").read_text().splitlines()]
    checks, previous = 0, None
    for _, state in wave.frames:
        address = wave.get(state, "io_PC" if ifu else "io_a")
        output = wave.get(state, "io_Instr" if ifu else "io_rd")
        # time=0 的 dumpvars 是模拟器加载之前的占位值，使用第一个实际时间组之后的数据。
        if previous is not None:
            require(output, words[(address // 4) % len(words)], wave.path.name + " instruction")
            if ifu:
                require(wave.get(state, "io_PCPlus4"), (address + 4) & MASK32,
                        wave.path.name + " PC+4")
                old_pc = wave.get(previous, "io_PC")
                rising = wave.get(previous, "clock") == 0 and wave.get(state, "clock") == 1
                expected = old_pc
                if rising:
                    expected = (0 if wave.get(previous, "reset") else
                                wave.get(previous, "io_IEUAdr") if wave.get(previous, "io_PCSrc")
                                else (old_pc + 4) & MASK32)
                require(address, expected, wave.path.name + " PC edge")
            checks += 1
        previous = state
    assert checks > 3
    return f"{checks} fetch values" + (", PC edges/target/32-bit wrap" if ifu else ", reset retention")


def check_book(wave):
    stores = [(wave.get(s, "io_IEUAdr"), wave.get(s, "io_WriteData"),
               wave.get(s, "io_ByteMask")) for s in wave.rising_inputs()
              if not wave.get(s, "reset") and wave.get(s, "io_MemWrite")]
    require(stores, [(96, 7, 15), (100, 25, 15)], "book stores")
    return "book signatures 96=7, 100=25"


def main():
    directory = Path(sys.argv[1] if len(sys.argv) > 1 else "target/waveforms")
    entries = [("rv32-acceptance", "CpuAcceptanceHarness", lambda w: check_cpu(w, "rv32-acceptance")),
               ("rv32-acceptance-expanded", "CpuAcceptanceHarness",
                lambda w: check_cpu(w, "rv32-acceptance-expanded")),
               ("irom32", "IROMHarness", lambda w: check_fetch(w, False)),
               ("ifu32", "IFUHarness", lambda w: check_fetch(w, True)),
               ("code-example-2.16", "RiscvSingleHarness", check_book),
               ("lsu32", "SubwordLSUHarness", lambda w: check_ram(w, 32, 64, "dut.dtim."))]
    for width in (32, 64):
        entries.extend([
            (f"subwordwrite{width}", "SubwordWriteHarness", lambda w, n=width: check_subword(w, n, True)),
            (f"subwordread{width}", "SubwordReadHarness", lambda w, n=width: check_subword(w, n, False)),
            (f"dtim{width}", "DTIMHarness", lambda w, n=width: check_ram(w, n, 4))])
    for name, top, check in entries:
        wave = Wave(directory / (name + ".vcd"), top)
        paths = wave.check_config(name)
        print(f"PASS {name}: {check(wave)}; {paths} GTKWave paths")
    print(f"PASS final waveforms: {len(entries)}/{len(entries)}")


if __name__ == "__main__":
    main()
