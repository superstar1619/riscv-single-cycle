# 模块 14：SubwordWrite 写数据复制核验报告（第七章第 10 轮）

SubwordWrite 将写数据的**最低字节、半字或字重复铺满整个输出**。例如，32 位输入 `89ABCDEF` 在 SB 模式下输出 `EFEFEFEF`，在 SH 模式下输出 `CDEFCDEF`。64 位的 SD 直接输出原数据。

生产模块是纯组合电路，只有三个端口。当前已独立完成测试与波形，等待用户核验；后续 LSU 负责把它接入实际存储。

## 1. 依据与交付范围

依据本地《RISC-V System-on-Chip Design, Edition 1》§7.1.6（书中第 314–315 页）与图 7.9：DTIM 以原生字宽访问，SB/SH 将低字节/半字复制到写数据的所有对应位置；RV64 的 SW 也需要复制。实际写哪些字节由 ByteMask 决定。教材本节采用组合读、上升沿写，本模块只负责写数据组合处理。

参考源码：`/home/unlastingstar/cvw/src/lsu/subwordwrite.sv`。本工程沿用低位数据复制和大小选择结构，用 Chisel `Fill`、`MuxLookup` 实现，再生成 Verilog。

与 CVW 的编码处理区别如下：

| 对比项 | CVW 的 32/64 位分支 | 本工程 |
| --- | --- | --- |
| 译码字段 | 只看 Funct3 低两位。 | 检查完整三位。 |
| 32 位的 011 | 输出原数据。 | 非法，输出零。 |
| 100–111 | 按低两位选择，等效于其他大小。 | 全部非法，输出零。 |

合法操作的复制结果一致。本工程延续 SwByteMask 的完整编码校验，仅支持 32/64 位；不纳入 CVW 的 LLEN=128、浮点和扩展接口。

本轮仅新增 SubwordWrite；第 9 轮 SwByteMask 的未提交交付保留，LSU 与 CPU 尚未接通子字访存。独立 64 位模块不表示整机支持 RV64I。不自动提交、合并或进入下一轮。

| 交付 | 路径 |
| --- | --- |
| 模块与端口 | [SubwordWrite.scala](../../src/main/scala/riscvsingle/lsu/SubwordWrite.scala) |
| 生成入口 | [GenerateSubwordWrite.scala](../../src/main/scala/riscvsingle/GenerateSubwordWrite.scala) |
| 中文说明与测试 | [SubwordWriteSpec.scala](../../src/test/scala/riscvsingle/lsu/SubwordWriteSpec.scala) |
| 32 / 64 位 RTL | [32 位](../../generated/subwordwrite/SubwordWrite.v)、[64 位](../../generated/subwordwrite64/SubwordWrite.v) |
| 32 / 64 位 VCD | [32 位](../../target/waveforms/subwordwrite32.vcd)、[64 位](../../target/waveforms/subwordwrite64.vcd) |
| GTKWave 预选信号 | [32 位](../../waves/subwordwrite32.gtkw)、[64 位](../../waves/subwordwrite64.gtkw) |

## 2. 参数与全部端口

`riscvsingle.lsu.SubwordWrite(dataWidth: Int = 32)` 继承 `RawModule`，端口 Bundle 为 `SubwordWriteIO`。`dataWidth` 仅允许 32 或 64，其他值在 elaboration 时抛出 `IllegalArgumentException`。生成入口默认宽度取 `CpuConfig().xlen=32`，其他共享配置不参与本模块。

| Chisel 端口 | 实际 Verilog 端口 | 方向 / 类型 | 位宽（32 / 64） | 含义 |
| --- | --- | --- | --- | --- |
| `io.WriteData` | `io_WriteData` | 输入 / UInt | 32 / 64 | 寄存器原始写数据。 |
| `io.Funct3` | `io_Funct3` | 输入 / UInt | 3 / 3 | 完整存储功能编码。 |
| `io.WriteDataWord` | `io_WriteDataWord` | 输出 / UInt | 32 / 64 | 复制后的原生字写数据。 |

输入变化后，输出通过组合逻辑立即更新，无需时钟沿。生产模块没有时钟、复位、使能或存储状态。

SubwordWrite 负责**准备写数据**，SwByteMask 负责**选择写入字节**，后续 LSU/DTIM 负责**执行存储**。本模块不接收地址或 ByteMask，不检查对齐；非法编码输出零也不能代替存储使能门控。

## 3. 功能与固定常量示例

下表所有数据均为十六进制。32 位输入为 `89ABCDEF`，64 位输入为 `0123456789ABCDEF`。

| Funct3 | 操作 | 32 位 WriteDataWord | 64 位 WriteDataWord |
| --- | --- | --- | --- |
| 000 | SB，复制低 8 位 | `EFEFEFEF` | `EFEFEFEFEFEFEFEF` |
| 001 | SH，复制低 16 位 | `CDEFCDEF` | `CDEFCDEFCDEFCDEF` |
| 010 | SW，复制低 32 位 | `89ABCDEF` | `89ABCDEF89ABCDEF` |
| 011 | SD | 非法，`00000000` | `0123456789ABCDEF`，原样输出 |
| 100–111 | 非法 | `00000000` | `0000000000000000` |

SB/SH 仅使用输入最低子字，高位输入不影响结果；64 位 SW 同样隔离输入高 32 位。这是位模式复制，不进行符号或零扩展，也不按地址移位。小端字节位置选择属于 ByteMask 与后续存储器。

## 4. 实现步骤与实际 RTL

源码按三个步骤排列：

1. `Fill` 生成低字节、低半字、低字的复制结果。
2. `storeData` 列出 SB/SH/SW 的对应关系，64 位配置增加 SD。
3. `MuxLookup` 按完整 Funct3 选择结果，未匹配时输出零。

| Chisel 名称 | 类型 / 位宽 | 定义 | 本次实际 RTL |
| --- | --- | --- | --- |
| `ByteCopies` | Wire / UInt(dataWidth.W) | `Fill(dataWidth/8, WriteData(7,0))` | 同名 32/64 位 wire，低字节串接。 |
| `HalfwordCopies` | Wire / UInt(dataWidth.W) | `Fill(dataWidth/16, WriteData(15,0))` | 同名 32/64 位 wire，低半字串接。 |
| `WordCopies` | Wire / UInt(dataWidth.W) | `Fill(dataWidth/32, WriteData(31,0))` | 64 位保留同名 wire；32 位的一次复制折叠到 `io_WriteData`。 |

`storeData` 是硬件生成阶段的 Scala 选择表；输出在 RTL 中表现为连续赋值，每份文件只有一个模块、三个端口，没有时序块，也没有子模块连接。

自动生成的中间 wire：32 位包含 `_io_WriteDataWord_T_1`、`_io_WriteDataWord_T_3`；64 位另有 `_io_WriteDataWord_T_5`。这些名字用于本次 RTL 对照，不是稳定接口；没有使用 dontTouch 强制保留别名。

## 5. 测试先行与验证证据

验证日期：2026-10-04（Asia/Shanghai）。沿用 JDK 17、sbt 1.10.7、Scala 2.13.14、Chisel 3.6.1、chiseltest 0.6.2（Treadle）与 Verilator 5.036。

首轮实现遵循测试先行，过程与结果如下：

| 阶段 | 实际结果 |
| --- | --- |
| 原工程基线 | 14 个套件，121 项通过。 |
| 常零接口骨架 | 12 项中 10 项功能失败、2 项接口/参数通过，退出码 1。 |
| 完成复制与选择逻辑 | 同一模块命令 12/12 通过，退出码 0。 |
| 使用 Chisel 3.6 的非弃用 MuxLookup 写法后 | 全工程 15 个套件、133 项通过。 |

骨架阶段的失败原因明确：例如期望 `EFEFEFEF`、`A5A5A5A5`，实际输出为零。

| 验证 | 实际覆盖 / 结果 |
| --- | --- |
| 定向常量与波形 | 上表全部合法大小；零初值、非法 111 清零、恢复 SB；32 位 6 次、64 位 7 次断言。 |
| 完整 Funct3 | 32 位 011–111、64 位 100–111，每个编码前先设置合法非零输出，共 18 次断言。 |
| 高位数据隔离 | SB/SH、64 位 SW；低子字保持不变，所有未使用高位从全零变全一，共 10 次断言。 |
| 组合更新 | 每个宽度 7 次检查，数据变化、功能变化、合法→非法→合法及清零；不推进时钟。 |
| 固定种子参考 | 每个宽度使用种子 `0x5eedL + width`，256 个随机输入及零/全一/仅最高位置位三个边界；每个输入测试全部 8 个编码，共 4,144 次断言。参考模型按输出字节位置逐字节取输入，不调用硬件逻辑。 |
| 功能断言总量 | 13 + 18 + 10 + 14 + 4,144 = **4,199 次**，不含后续波形命令的重复执行。 |
| 接口与状态 | 两种宽度恰好三个端口，方向/位宽正确，无时序块或寄存器。 |
| 参数检查 | −1、0、1、8、16、31、33、63、65、128 全部拒绝。 |
| 全工程 | **15 个套件、133 项全部通过**，退出码 0。 |
| 新 RTL | 两个 Makefile 生成命令成功，端口已核对；两份 `verilator --lint-only -Wall` 均退出 0。 |
| 现有整机 RTL | 默认/扩容各使用种子 1、17、2026，**六次全部 PASS**，退出码 0。 |
| 波形与配置 | Makefile 波形目标的两个测试通过，两份 VCD 成功复制；已解析并核对全部输出阶段及 GTKWave 四个信号路径。 |

模块测试与完整回归，在项目根目录运行：

```bash
./scripts/sbt-local.sh 'testOnly riscvsingle.lsu.SubwordWriteSpec'
./scripts/sbt-local.sh test
make test-rtl
```

生成并检查两种宽度的 RTL：

```bash
make generate-subwordwrite SBT=./scripts/sbt-local.sh
make generate-subwordwrite SBT=./scripts/sbt-local.sh \
  WIDTH=64 SUBWORDWRITE_TARGET_DIR=generated/subwordwrite64
verilator --lint-only -Wall generated/subwordwrite/SubwordWrite.v
verilator --lint-only -Wall generated/subwordwrite64/SubwordWrite.v
```

生成入口为 `riscvsingle.GenerateSubwordWrite [dataWidth] [targetDir]`，无参数时默认 32、`generated/subwordwrite`；最多接受两个参数。Makefile 对应 `WIDTH` 和 `SUBWORDWRITE_TARGET_DIR`。已有 sbt 环境可省略 `SBT=./scripts/sbt-local.sh`。

本轮日志为 `target/subwordwrite-round10-{baseline,red,focused,regression,generate32,generate64,wave,rtl}.log`。target、test_run_dir、缓存与 .fir/.anno.json 沿用项目忽略规则；可用上述命令重建。沿用现有 ChiselStage/FIRRTL 生成流程，其弃用提示与前置轮次一致。

## 6. 波形生成与阅读

一条命令生成 32/64 位波形：

```bash
make test-subwordwrite-wave SBT=./scripts/sbt-local.sh
```

打开预选信号：

```bash
gtkwave target/waveforms/subwordwrite32.vcd waves/subwordwrite32.gtkw
gtkwave target/waveforms/subwordwrite64.vcd waves/subwordwrite64.gtkw
```

`SubwordWriteHarness` 仅存在于测试目录，是有观察时钟的 Module 包装层；内部 SubwordWrite 仍是 RawModule。每个例子保留两个观察周期，断言在推进时钟前检查组合结果。GTKWave 选择 `clock`、`io_Funct3`（二进制）、`io_WriteData` 与 `io_WriteDataWord`（十六进制）。

波形中的各阶段如下；每一阶段保持两个观察周期，便于比较输入和输出。

| 阶段 | 32 位输出 | 64 位输出 |
| --- | --- | --- |
| 初始 | 零。 | 零。 |
| SB | `EFEFEFEF` | `EFEFEFEFEFEFEFEF` |
| SH | `CDEFCDEF` | `CDEFCDEFCDEFCDEF` |
| SW | `89ABCDEF` | `89ABCDEF89ABCDEF` |
| SD | 无此阶段。 | `0123456789ABCDEF` |
| 非法 111 | 零。 | 零。 |
| 恢复 SB | `EFEFEFEF` | `EFEFEFEFEFEFEFEF` |

本次 VCD 的输出变化时间（ns）：32 位为 0/26/46/66/86/106，64 位为 0/26/46/66/86/106/126。非初始阶段的变化与包装层时钟下降沿同时间戳，位于相邻上升沿之间；**纯组合更新由不调用 `clock.step` 的独立测试验证**。

已解析 VCD 核对输出阶段和预选信号路径，未启动 GTKWave 图形界面。

## 7. 本次可读性整理

源码补充中文接口说明，并按“复制低位 → 选择大小 → 非法清零”分步排列。测试用显式 Funct3/期望值表替代按下标推导编码，给参考模型和高位隔离输入使用更明确的名称；全部测试输入、期望结果、随机种子与 4,199 次功能断言保持一致。

整理前模块基线 12/12 通过；整理后全工程重新运行，15 个套件、133 项通过，现有整机 RTL 六次 PASS。两份 RTL 重新生成后，去除源文件位置注释，与整理前逐字一致；Verilator lint 均退出 0。波形目标的两项测试通过，重新核对 VCD 输出值、时间戳及 GTKWave 信号路径。

本次日志位于 `target/subwordwrite-readability/{baseline,regression,generate32,generate64,wave,rtl,lint32,lint64}.log`。

## 8. 接通边界与停点

本轮未修改旧 LSU、CPU 或既有整机 RTL。整机 RTL 回归验证原程序兼容性，新复制逻辑由独立模块测试验证；SB/SH 整机写入、未对齐禁止写入仍待后续 LSU 接通。没有加入 SubwordRead、DTIM、流水线、缓存、总线或扩展接口。

首轮交付时已按约定停止等待核验。用户现已授权提交第 9/10 轮交付及可读性更新，并继续第 11 轮 SubwordRead；下一轮完成后再次停止等待检查。
