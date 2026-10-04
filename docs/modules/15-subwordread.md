# 模块 15：SubwordRead 子字读取核验报告（第七章第 11 轮）

SubwordRead 从原生读取字中选择字节、半字或字，再做符号或零扩展。小端偏移 0 对应最低字节：例如输入 `80FF7F01`，偏移 3 的 LB 输出 `FFFFFF80`，LBU 输出 `00000080`。

本轮独立完成 32/64 位实现、测试、RTL 与波形。**16 项模块测试、全工程 149 项及现有整机 RTL 六次回归通过；完成后停止等待检查，下一轮为 DTIM。**

## 1. 依据与交付范围

依据本地《RISC-V System-on-Chip Design, Edition 1》§7.1.6（书中第 314–315 页）、图 7.9，以及实现前阅读的 `/home/unlastingstar/cvw/src/lsu/subwordread.sv`。

采用 CVW 的逐级选择结构：先选 32 位字，再选 16 位半字，最后选 8 位字节。用 Chisel `Mux`、`SInt.pad`、`UInt.pad` 和 `MuxLookup` 实现，Verilog 全部由 Chisel 生成。

| 对比项 | CVW | 本工程 |
| --- | --- | --- |
| 小端整数子字选择 | Word → Halfword → Byte。 | 相同结构，仅取字内偏移。 |
| 32 位的 LD / LWU | 011 返回原字，110 等价于原字。 | 编码非法，输出零。 |
| 111 | 返回原字。 | 非法，输出零。 |
| 大小相关的低偏移位 | 半字/字选择器忽略无关低位。 | 保留；自然对齐检查归于后续 LSU。 |
| 扩展能力 | 可配置大端、浮点 NaN boxing、128 位等。 | 仅小端整数，独立 32/64 位。 |

第 9 轮 SwByteMask 与第 10 轮 SubwordWrite 已按用户指令提交为 `886e708`。SwByteMask 是教材图 7.9 的字节写使能模块，后续 LSU/DTIM 需要它，本次保留。本轮只新增 SubwordRead，不修改 CPU/LSU，不提前实现 DTIM；独立 64 位模块不改变整机 RV32 范围。

| 交付 | 路径 |
| --- | --- |
| 模块与端口 | [SubwordRead.scala](../../src/main/scala/riscvsingle/lsu/SubwordRead.scala) |
| 生成入口 | [GenerateSubwordRead.scala](../../src/main/scala/riscvsingle/GenerateSubwordRead.scala) |
| 中文说明与测试 | [SubwordReadSpec.scala](../../src/test/scala/riscvsingle/lsu/SubwordReadSpec.scala) |
| 32 / 64 位 RTL | [32 位](../../generated/subwordread/SubwordRead.v)、[64 位](../../generated/subwordread64/SubwordRead.v) |
| 32 / 64 位 VCD | [32 位](../../target/waveforms/subwordread32.vcd)、[64 位](../../target/waveforms/subwordread64.vcd) |
| GTKWave 预选信号 | [32 位](../../waves/subwordread32.gtkw)、[64 位](../../waves/subwordread64.gtkw) |

## 2. 参数与全部端口

`riscvsingle.lsu.SubwordRead(dataWidth: Int = 32)` 继承 `RawModule`；`dataWidth` 仅允许 32 或 64，其他值在硬件生成时抛出 `IllegalArgumentException`。令 `W=dataWidth`、`K=log2Ceil(W/8)`。

| Chisel 端口 | 实际 Verilog 端口 | 方向 / 类型 | 位宽（32 / 64） | 含义 |
| --- | --- | --- | --- | --- |
| `io.ReadDataWord` | `io_ReadDataWord` | 输入 / UInt | 32 / 64 | 存储器返回的完整原生字。 |
| `io.ByteOffset` | `io_ByteOffset` | 输入 / UInt | K：2 / 3 | 字内字节偏移，0–3 / 0–7。 |
| `io.Funct3` | `io_Funct3` | 输入 / UInt | 3 / 3 | 完整加载功能编码。 |
| `io.ReadData` | `io_ReadData` | 输出 / UInt | 32 / 64 | 选择并扩展后的读取结果。 |

模块只有四个组合端口，没有时钟、复位、使能、寄存器、存储器或子模块。输入变化经组合传播更新输出，不等待时钟沿。生成入口默认宽度取 `CpuConfig().xlen=32`；存储深度等其他共享配置不参与本模块。

## 3. 编码、偏移与扩展规则

| Funct3 | 加载 | 选择的位数 | 32 位结果 | 64 位结果 |
| --- | --- | --- | --- | --- |
| 000 | LB | 8 | 符号扩展。 | 符号扩展。 |
| 001 | LH | 16 | 符号扩展。 | 符号扩展。 |
| 010 | LW | 32 | 原字位模式。 | 符号扩展。 |
| 011 | LD | 64 | 非法，输出零。 | 原字位模式。 |
| 100 | LBU | 8 | 零扩展。 | 零扩展。 |
| 101 | LHU | 16 | 零扩展。 | 零扩展。 |
| 110 | LWU | 32 | 非法，输出零。 | 零扩展。 |
| 111 | 非法 | — | 输出零。 | 输出零。 |

符号扩展复制所选子字的最高位；零扩展在高位补零。比如 64 位 LW 读取 `80000001` 得到 `FFFFFFFF80000001`，LWU 得到 `0000000080000001`。

| 加载大小 | 32 位使用的偏移位 | 64 位使用的偏移位 |
| --- | --- | --- |
| 字节 | bits 1:0。 | bits 2:0。 |
| 半字 | bit 1，忽略 bit 0。 | bits 2:1，忽略 bit 0。 |
| 字 | 无，忽略 bits 1:0。 | bit 2，忽略 bits 1:0。 |
| 双字 | 不支持。 | 无，忽略 bits 2:0。 |

**这是子字选择行为，不是未对齐访问许可。** 例如 LH 的偏移 1 与偏移 0 选同一低半字，偏移 3 与偏移 2 选同一高半字。模块不会跨原生字拼接数据，也不检查自然对齐；计划要求的整机未对齐读零由第 13 轮 LSU 门控。本模块不生成异常或陷阱。

## 4. 实现步骤与实际 RTL

1. 64 位配置用 ByteOffset[2] 选择低/高字；32 位配置直接取输入字。
2. 用 ByteOffset[1] 从字中选半字，再用 ByteOffset[0] 从半字中选字节。
3. 按完整 Funct3 选择符号扩展、零扩展或完整原生字；未匹配时输出同宽零。

| Chisel 名称 | 类型 / 位宽 | 定义 | 实际 RTL |
| --- | --- | --- | --- |
| `SelectedWord` | Wire / UInt(32.W) | 64 位选低/高字，32 位取输入。 | 64 位保留同名 wire；32 位折叠到 `io_ReadDataWord`。 |
| `SelectedHalfword` | Wire / UInt(16.W) | 按偏移 bit 1 选低/高半字。 | 两种宽度均有同名 wire。 |
| `SelectedByte` | Wire / UInt(8.W) | 按偏移 bit 0 选低/高字节。 | 两种宽度均有同名 wire。 |

`loadData` 是硬件生成阶段的 Scala 选择表，64 位附加 LD/LWU。`SInt.pad(W).asUInt` 生成符号位复制，`UInt.pad(W)` 生成高位补零。

实际 RTL 中，符号扩展字节/半字为 `_T_2` / `_T_5`；64 位字符号扩展为 `_T_8`。零扩展临时值在 32 位为 `_T_8` / `_T_9`，64 位为 `_T_9` / `_T_10` / `_T_11`。选择链使用 `_io_ReadData_T_*`，输出为连续赋值。这些自动名称仅用于本次对照，不作为稳定接口；没有用 dontTouch 保留别名。测试 VCD 保留 `SelectedWord` 等观察名称，包括 32 位的别名。

## 5. 测试先行与验证结果

验证日期：2026-10-04（Asia/Shanghai）。工具链为 JDK 17、sbt 1.10.7、Scala 2.13.14、Chisel 3.6.1、chiseltest 0.6.2（Treadle）、Verilator 5.036。

| 阶段 | 实际结果 |
| --- | --- |
| 前置提交前复验 | 全工程 133/133，现有整机 RTL 六次 PASS；随后提交 `886e708`。 |
| 常零接口骨架 | 16 项中 14 项功能失败、2 项接口/参数通过，退出码 1。 |
| 完成选择与扩展 | 同一模块命令 16/16 通过，退出码 0。 |
| 全工程回归 | 16 个套件、149 项全部通过，退出码 0。 |
| 两种宽度 RTL | Makefile 生成成功；各有四个组合端口，Verilator lint 均退出 0。 |
| 现有整机 RTL | 默认/扩容各用种子 1、17、2026，六次全部 PASS，退出码 0。 |
| 波形命令 | 两项测试通过、VCD 成功复制；13/20 个输出阶段和八个 GTKWave 信号路径已核对。 |

常零骨架的失败是功能断言失败，例如期望 `1`、`7F`、`FFFFFFFFFFFFFF81`，实际为零；没有依赖编译错误证明测试有效。

| 测试内容 | 实际覆盖 / 功能断言数 |
| --- | --- |
| 固定值与波形 | 32 位 10 组、64 位 17 组加载；各加初始零、非法清零、恢复合法三组，共 33 次。 |
| 符号边界 | LB/LH/LW 的零、最大正数、最小负数、全一；同步验证对应无符号加载，共 44 次。 |
| 非法编码 | RV32 的 011/110/111、RV64 的 111；每个偏移先设置合法非零输出再检查非法零，共 40 次。 |
| 偏移选择 | 半字/字忽略低偏移位，64 位 LD 不使用偏移，遍历全部偏移，共 32 次。 |
| 邻接字节隔离 | 每个合法子字位置，未选中字节从全零变全一，选择结果保持，共 80 次。 |
| 组合更新 | 两种宽度各八组，分别变化输入字、偏移或编码，并检查非法后恢复；不调用 clock.step，共 16 次。 |
| 随机参考 | 每宽度种子 `0x5eed11L + width`；128 个随机原生字加零/全一/仅最高位置位三个边界。每个输入检查全部 8 个编码与全部偏移，共 12,576 次。 |
| 功能断言总量 | **12,821 次**，不含独立波形命令的重复执行。 |
| 参数检查 | −1、0、1、8、16、31、33、63、65、128 全部拒绝。 |

参考模型使用小端字节数组，按大小选取字节；负数用减去 2^位数计算，再截为原生字宽，不复用硬件多级选择器或扩展逻辑。接口测试核对全部方向/位宽及无时序块/寄存器。

### 编码规范核对范围

另对照工作区新增的 [Chisel 编码规范](../chisel-coding-standard.md) 与本地 `chisel-coding-standard` skill 检查本模块，保留现有接口：

| 条款 | 本轮证据 |
| --- | --- |
| §1–2 / 4.23 | 中文说明、硬件与 Scala 命名区分；新增模块、生成入口、测试均使用空格，行宽不超过 95。 |
| C3 | 选择节点明确为 32/16/8 位；SInt/UInt 的 pad 分别符号/零扩展到原生字宽，RTL 已对照。 |
| C4–C5 | Scala if 仅裁剪宽度配置；Mux/MuxLookup 使用运行时输入，非法编码有完整默认零驱动。 |
| C8–C10 | 测试包装层逐字段连接；require 仅允许 32/64，四个端口字段在两种配置中保持一致。 |
| C11 | 区分编译、RTL/端口检查与仿真；不把仿真/lint 等同于综合或时序证明。 |

本模块没有状态或存储器，C6/C7 的复位与存储时序不涉及本轮；没有扩展审计既有模块。仅缩短随机测试的说明文字以满足行宽，功能输入、期望值和覆盖保持一致。

## 6. 复现命令与波形阅读

在项目根目录运行模块与全工程测试：

```bash
./scripts/sbt-local.sh 'testOnly riscvsingle.lsu.SubwordReadSpec'
./scripts/sbt-local.sh test
make test-rtl
```

生成两种宽度的 RTL：

```bash
make generate-subwordread SBT=./scripts/sbt-local.sh
make generate-subwordread SBT=./scripts/sbt-local.sh \
  WIDTH=64 SUBWORDREAD_TARGET_DIR=generated/subwordread64
verilator --lint-only -Wall generated/subwordread/SubwordRead.v
verilator --lint-only -Wall generated/subwordread64/SubwordRead.v
```

生成入口为 `riscvsingle.GenerateSubwordRead [dataWidth] [targetDir]`，最多两个参数；无参数时默认 32、`generated/subwordread`。Makefile 对应 `WIDTH`、`SUBWORDREAD_TARGET_DIR`；已有 sbt 环境可省略 `SBT=./scripts/sbt-local.sh`。

一条命令生成两份 VCD，再打开预选信号：

```bash
make test-subwordread-wave SBT=./scripts/sbt-local.sh
gtkwave target/waveforms/subwordread32.vcd waves/subwordread32.gtkw
gtkwave target/waveforms/subwordread64.vcd waves/subwordread64.gtkw
```

固定输入为 32 位 `80FF7F01`、64 位 `8000000180FF7F01`。32 位波形顺序为 LB 正/负边界 → LBU → LH → LHU → LW → 非法 111 → 恢复 LB；64 位增加高字/最高字节选择、LWU 与 LD。每组保持两个观察周期，期望值直接列在测试表中。

八个预选信号为观察时钟、Funct3、ByteOffset、原始读取字、所选字/半字/字节、最终 ReadData。Funct3/ByteOffset 显示二进制，数据为十六进制；可对照输入最低字节 `01`、负字节 `80/FF`，检查符号位复制或补零。

VCD 的初始输出在 0 ns，之后从 26 ns 起每 20 ns 更新一次；32 位有 13 个阶段，最后更新在 246 ns，64 位有 20 个阶段，最后更新在 386 ns。输入/输出变化与包装层时钟下降沿同时间戳；纯组合性质由不推进时钟的独立测试验证。已解析输出值、时间戳、文件复制一致性及信号路径，未启动 GTKWave 图形界面。

最后一次规范核对后的完整回归仍为 149/149；独立只读审查未发现待修复问题。

日志位于 `target/subwordread-round11/{red,focused,regression,final-regression,standard-focused,generate32,generate64,wave,rtl,lint32,lint64}.log`。提交前证据为 `target/subwordwrite-precommit{,-rtl}.log`。target、test_run_dir、缓存及 .fir/.anno.json 沿用忽略规则，可用上述命令重建；源码、测试、两份 .v 和 GTKWave 配置为本轮交付。沿用现有 ChiselStage/FIRRTL 流程，其弃用提示与前置轮次一致。

## 7. 接通边界与停点

CPU、旧 LSU 和现有整机 RTL 本轮没有变化，整机回归仍验证原字访存程序。新子字读取的行为由独立模块测试验证；完整 LB/LH/LBU/LHU 与字节写入、未对齐门控仍待 LSU 接通，不据此宣称整机子字访存完成。未进行 FPGA/ASIC 实现或时序分析。

**本轮停止，等待用户核验。** SubwordRead 保留为未提交交付；收到继续指令后，下一轮为 DTIM。前置 SwByteMask、SubwordWrite 已提交为 `886e708`，未推送或合并。
