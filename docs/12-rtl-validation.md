# 生成 RTL 整机验证报告

## 1. 交付状态与验证范围

11 个预定硬件模块已全部实现。按用户本轮“git commit后继续”指示，RiscvSingle 及配套文件已提交，提交号为 `d218f06`。

本轮补充 Verilator 测试入口，直接编译并运行该提交中的两份完整 CPU Verilog。硬件端口、模块功能、内部信号及同步复位实现沿用已核验版本，详情见 [模块 11 报告](modules/11-riscv-single.md)。本报告是整机验证阶段的报告，不新增硬件模块。

状态：**六次 RTL 仿真全部通过；测试脚本、测试源码与报告已按用户“提交后写个总结报告再结束”指示纳入交付提交。**

| 文件 | 功能 |
| --- | --- |
| [test-rtl.sh](../scripts/test-rtl.sh) | 编译默认与扩容 RTL，在项目根目录运行各三个随机种子。 |
| [RiscvSingleRtlTest.cpp](../src/test/cpp/RiscvSingleRtlTest.cpp) | 通过生产顶层端口驱动时钟、同步复位并检查执行结果。 |
| [Makefile](../Makefile) | 增加 `test-rtl` 目标及 `VERILATOR` 参数。 |
| [默认 CPU](../generated/riscv-single/RiscvSingle.v) | 64 项 IROM/RAM，复位地址 0，加载书中程序。 |
| [扩容 CPU](../generated/riscv-single128/RiscvSingle.v) | 128 项 IROM/RAM，复位地址 0x100，加载配置测试程序。 |

## 2. 使用的生产端口与功能

| 端口 | 方向 / 位宽 | 本轮测试用途 |
| --- | --- | --- |
| `clk` | 输入 / 1 | C++ 驱动高低电平，每次上升沿执行一条指令或同步复位。 |
| `reset` | 输入 / 1 | 启动时跨越上升沿复位；结束时检查无边沿脉冲、复位上升沿、持续复位和释放。 |
| `io_IEUAdr` | 输出 / 32 | 每条实际执行指令的地址计算结果；存储时为写入字节地址。 |
| `io_WriteData` | 输出 / 32 | MemWrite=1 时检查写数据；书中最终循环检查 x2 读数为 25。 |
| `io_MemWrite` | 输出 / 1 | 每周期检查写使能，确认正常运算与最终循环无额外存储。 |

测试在上升沿之前检查组合输出，再产生时钟边沿。没有通过模拟器层次访问寄存器、PC 或内存，加载数据来自生产 LSU；分支与跳转由实际 CPU 执行。

## 3. 内部信号与测试变量

生产顶层内部信号沿用 `PC`、`PCPlus4`、`Instr`、`ReadData`、`PCSrc`，直接子模块为 `ifu`、`ieu`、`lsu`。其中 PC 状态位于 IFU 的 `pcreg`，寄存器状态位于 RegFile 的 `rf`，数据存储位于 LSU 的 `RAM`。本轮没有修改这些名称或增加调试端口。

| 测试变量 / 函数 | 功能 |
| --- | --- |
| `context` | Verilator 运行上下文，设置随机初值种子和仿真时间。 |
| `dut` | 从完整 Verilog 编译得到的 `VRiscvSingle` 实例。 |
| `ExpectedCycle.address/write/data` | 当前周期的预期地址、写使能及有效写数据。 |
| `book` / `expanded` / `expected` | 两套程序的逐周期期望值及当前选用的检查表。 |
| `tick` | 产生上升沿后回到低电平，更新仿真时间并求值。 |
| `loopAddress` / `checkLoop` | 最终循环的期望地址及无存储检查。 |
| `writes` | 统计已核对的数据存储次数。 |
| `require` | 断言失败时抛异常，进程返回 1，脚本和 Make 立即失败。 |

## 4. 运行方式与实际结果

验证日期：2026-10-04。工具为 **Verilator 5.036**，配合系统 C++ 编译器，采用 C++17。

在项目根目录执行：

```bash
make test-rtl
# 指定工具路径：
make test-rtl VERILATOR=/home/unlastingstar/riscv/bin/verilator
```

脚本对两份已经生成的 RTL 分别执行 `--cc --exe --build`，每份只编译一个完整 CPU 文件，避免与各模块独立导出文件重复定义。开启 `-Wall`，关闭生成文件名称及未使用信号两类提示（`DECLFILENAME`、`UNUSEDSIGNAL`）；其他警告仍会使构建失败。本次两个构建日志均无其余警告或错误。

采用 `--x-initial unique --x-assign unique`、`randReset(2)`，在创建 DUT 前设置种子 1、17、2026。未初始化寄存器与 RAM 使用随机初值，之后通过真正的同步复位建立 PC/x0，并由程序写入其需要的非零寄存器及数据。生成的 Verilator 模型已确认这些状态采用 `VL_RAND_RESET_I` 初始化。测试不依赖未写入状态默认为 0。

实际输出：

```text
PASS book seed=1 cycles=19 stores=2 loop=5 synchronous-reset=PASS
PASS book seed=17 cycles=19 stores=2 loop=5 synchronous-reset=PASS
PASS book seed=2026 cycles=19 stores=2 loop=5 synchronous-reset=PASS
PASS expanded seed=1 cycles=13 stores=5 loop=5 synchronous-reset=PASS
PASS expanded seed=17 cycles=13 stores=5 loop=5 synchronous-reset=PASS
PASS expanded seed=2026 cycles=13 stores=5 loop=5 synchronous-reset=PASS
```

| 配置 | 检查结果 |
| --- | --- |
| 默认 64 项 | 19 条实际执行指令逐周期符合期望；地址 96 写入 7，地址 100 写入 25；随后五周期保持循环地址 0x50、MemWrite=0、WriteData=25。 |
| 128 项、复位地址 0x100 | 13 条实际执行指令逐周期符合期望；写入地址/数据依次为 511/31、252/7、100/31、104/0x128、108/31；随后五周期保持循环地址 0x134、MemWrite=0。覆盖独立 RAM 字 127/63、负偏移加载、jal 链接与跳过指令。 |
| 两种配置的同步复位 | 循环中 reset 拉高及无上升沿脉冲不会重启；跨越上升沿后回到各自配置的第一条指令；持续复位时不推进，释放复位但不产生边沿时仍停在第一条指令。 |

编译日志位于 `target/rtl-test/book/build.log`、`target/rtl-test/expanded/build.log`；二进制及编译产物同在对应目录。ccache 使用 `.cache/ccache`，避免写入工作区之外的默认缓存目录。这些产物已由现有 `.gitignore` 排除。

## 5. 验证边界与交付结论

上一轮 Chisel/Treadle 全项目已有 **12 个套件、74 项测试通过**。本轮补充六次实际生成 RTL 的仿真执行，生产 Chisel 和 Verilog 均未改动，因此没有重跑既有套件。

Verilator 的随机初值测试采用二态仿真，不能替代四态 X 传播检查或形式验证。本轮复位检查通过公开端口确认执行位置，不单独读取 RAM 验证复位保留；实际 RAM 保留已有 RiscvSingleSpec 的专门程序覆盖。

IROM 的 `$readmemh` 仍位于 `ifndef SYNTHESIS`，本轮确认的是仿真加载与执行；FPGA/ASIC 程序固化、综合和时序尚未验证。ISA 仍为书中的简化 RV32 子集。

本阶段验证已完成，按用户指示提交并结束项目。后续新增硬件功能需要先确定具体扩展范围。
