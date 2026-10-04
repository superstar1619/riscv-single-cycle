# Code Example 2.16 测试程序与波形报告

验证日期：2026-10-04。依据《RISC-V System-on-Chip Design, Edition 1》第 68 页 Code Example 2.16。

## 1. 本轮交付

| 文件 | 功能 |
| --- | --- |
| [riscvtest.s](../programs/riscvtest.s) | 书中 21 条汇编指令，保留 main/around/end/done 标签，逐条标注地址、机器码与期望行为。 |
| [riscvtest.memfile](../programs/riscvtest.memfile) | 已有指令镜像，其前 21 项与 Code Example 2.16 完全一致，后 43 项为 NOP。 |
| [RiscvSingleSpec.scala](../src/test/scala/riscvsingle/RiscvSingleSpec.scala) | 整机测试明确命名为 Code Example 2.16，启用 WriteVcdAnnotation；显式保持同步复位两个上升沿后运行程序。 |
| [Makefile](../Makefile) | 增加 test-wave 入口，运行该测试并复制波形至固定目录。 |
| [code-example-2.16.gtkw](../waves/code-example-2.16.gtkw) | GTKWave 信号布局，预选时钟、复位、指令、数据通路、存储端口及关键寄存器。 |

CPU 生产硬件、寄存器堆规则和同步复位实现沿用现有版本。本轮修改测试与文档，没有新增硬件模块。

## 2. 程序功能

程序覆盖 add、sub、and、or、slt、addi、lw、sw、beq、jal。分支包含跳转与不跳转两种结果；jal 同时检查目标跳转与链接地址，lw 加载真实 LSU RAM 中先前 sw 写入的数据。

实际 PC 序列（十六进制）：

```text
00 04 08 0c 10 14 18 1c 20 28 2c 30 34 38 3c 40 48 4c 50
```

`0x24` 的 addi 与 `0x44` 的 addi 被跳过。首次执行最终循环前共执行 19 条指令，程序含 21 条指令。

| 存储指令 PC | 字节地址 | 写数据 | 说明 |
| --- | --- | --- | --- |
| 0x34 | 96 / 0x60 | 7 / 0x07 | x7 的计算结果。 |
| 0x4c | 100 / 0x64 | 25 / 0x19 | 经真实 RAM 加载及后续运算得到的结果。 |

在 PC=0x50 时，`beq x2,x2,done` 保持循环，MemWrite=0，WriteData=25。测试逐周期检查 IEUAdr/MemWrite，在存储周期核对 WriteData，并检查五个额外循环周期。

## 3. 端口与波形内部信号

测试包装 `RiscvSingleHarness` 连接生产 RiscvSingle 的五个端口。波形同时记录包装层的 `clock`、`reset` 和 `io_IEUAdr/io_WriteData/io_MemWrite`，以及实际生产层次内的信号。

| 波形信号（根层次为 RiscvSingleHarness） | 功能 |
| --- | --- |
| `clock` / `reset` | 测试时钟与高有效同步复位。 |
| `dut.PC` / `dut.Instr` | 当前指令字节地址及机器码。 |
| `dut.PCPlus4` / `dut.PCSrc` | 顺序地址及下一 PC 选择。 |
| `dut.ieu.dp.SrcA` / `SrcB` | ALU 操作数，可查看负立即数扩展。 |
| `dut.ieu.dp.Result` / `ALUResult` | 写回结果与 ALU 结果。 |
| `io_IEUAdr` / `io_WriteData` / `io_MemWrite` | 顶层存储地址、写数据与写使能。 |
| `dut.ReadData` | 真实 LSU RAM 的读数据。 |
| `dut.ieu.dp.rf.rf_0/2/3/7/9` | x0、x2、x3、x7、x9 的状态。 |

GTKWave 布局共预选 19 个信号，均已在实际生成的 VCD 中确认存在。VCD 还包含其余内部信号，可在查看器中按需添加。

## 4. 生成与查看

在项目根目录执行：

```bash
make test-wave SBT=./scripts/sbt-local.sh
gtkwave target/waveforms/code-example-2.16.vcd waves/code-example-2.16.gtkw
```

已有 sbt 在 PATH 时可省略 SBT 参数。可设置 `WAVE_DIR` 更改输出目录；默认波形为 `target/waveforms/code-example-2.16.vcd`。原始仿真输出也保留在 `test_run_dir/RiscvSingle_should_execute_Code_Example_216_and_generate_its_waveform/RiscvSingleHarness.vcd`。

该波形由 Chisel/Treadle 对生产 CPU 的实际仿真生成，时间单位为 1 ns。本次文件大小 56,755 字节，最后时间戳为 266 ns。chiseltest 自身的启动复位加上测试显式复位会影响起始时间，因此应按 PC 和指令定位事件，时间戳与书中 SystemVerilog 测试平台不必相同。

波形位于现有忽略的 target/test_run_dir 目录，可随时重新生成；汇编源文件与 GTKWave 布局作为项目文件保留。

## 5. 实际验证结果

- 使用本地 RISC-V GNU 汇编器，以 rv32i/ilp32 汇编并从地址 0 链接；所得 .text 为 84 字节、21 条指令，与现有镜像前 21 项逐项一致，后 43 项 NOP 也已核对。
- `testOnly riscvsingle.RiscvSingleSpec` 的四项整机测试全部通过。
- `make test-wave SBT=./scripts/sbt-local.sh` 的程序测试通过，并成功生成固定路径 VCD。
- 解析生成的 VCD，确认完整 PC 序列、两次存储地址/数据和全部 19 个 GTKWave 预选信号。

本轮文件尚未提交，完成程序与波形交付后停下等待核验。
