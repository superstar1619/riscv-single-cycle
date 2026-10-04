# 模块 12：Shifter 漏斗移位器核验报告（第七章第 4 轮）

## 1. 依据与本轮范围

依据本地《RISC-V System-on-Chip Design, Edition 1》第七章 §7.1、图 7.2 和表 7.1。结构依据为 §7.1.3.3（书中第 306–307 页）、图 7.4，以及表 7.4 中的全宽 SRL/SRA/SLL 和表 7.5 的 RV32 移位源字规则。

CVW 参考文件：`/home/unlastingstar/cvw/src/ieu/shifter.sv`。沿用其漏斗源字、方向相关偏移和统一右移后取低位的结构。本轮仅支持全宽左移、逻辑右移和算术右移，省去旋转、`W64` 控制与 RV64 的 32 位字运算路径。

上一轮 RegFile 已按用户指示提交为 `96576f0`。本轮只新增 **Shifter**、生成入口、测试和独立 RTL，并更新 Makefile 与 README 索引；不提前完善或接入 ALU。独立 64 位模块不代表整机已支持 RV64I。

| 交付内容 | 路径 |
| --- | --- |
| 模块与端口 | [Shifter.scala](../../src/main/scala/riscvsingle/ieu/Shifter.scala) |
| 生成入口 | [GenerateShifter.scala](../../src/main/scala/riscvsingle/GenerateShifter.scala) |
| 模块测试 | [ShifterSpec.scala](../../src/test/scala/riscvsingle/ieu/ShifterSpec.scala) |
| 32 位 RTL | [Shifter.v](../../generated/shifter/Shifter.v) |
| 64 位 RTL | [Shifter.v](../../generated/shifter64/Shifter.v) |

## 2. 参数与全部端口

类 `riscvsingle.ieu.Shifter` 继承 `RawModule`，端口 Bundle 为 `ShifterIO`。令 `W=dataWidth`、`K=log2Ceil(W)`。所有移位路径为组合逻辑，无时钟、复位、使能或握手端口，无子模块、寄存器或锁存器。

| 参数 | 类型 | 默认值 | 合法范围与限制 |
| --- | --- | --- | --- |
| `dataWidth` | Scala `Int` | 32 | 不小于 2 的二次幂，在 elaboration 时确定；非法值抛出 `IllegalArgumentException`。约束保证 K 位 `~Amt` 等于 `W−1−Amt`，且移位量端口至少为 1 位。 |

生成入口默认取 `CpuConfig().xlen=32`；`imemDepth、dmemDepth、resetVector、instructionInitFile` 不参与此独立模块。位宽由生成参数决定，不在运行时切换。整机仍限定 RV32。

| Chisel 名称 | 实际 Verilog 端口 | 方向 | 类型 / 位宽 | 用途 |
| --- | --- | --- | --- | --- |
| `io.A` | `io_A` | 输入 | `UInt(W.W)` | 待移位的原始位模式。 |
| `io.Amt` | `io_Amt` | 输入 | `UInt(K.W)`，32/64 位配置分别为 5/6 位 | 移位量，范围 0 至 W−1。 |
| `io.Right` | `io_Right` | 输入 | `Bool` / 1 位 | 0 选择左移，1 选择右移。 |
| `io.SubArith` | `io_SubArith` | 输入 | `Bool` / 1 位 | 右移时 0 为逻辑移位、1 为算术移位；左移时不影响结果。 |
| `io.Y` | `io_Y` | 输出 | `UInt(W.W)` | 低 W 位移位结果，负值以补码位模式表示。 |

输入变化后输出经组合逻辑传播更新，不等待时钟沿。输入端口仅能表示本模块范围内的移位量；从完整寄存器提取低 K 位属于后续 ALU 连接轮次。X/Z 四态传播不作为接口保证。

## 3. 功能规则与内部信号

| `Right` | `SubArith` | 操作 | 输出规则 |
| --- | --- | --- | --- |
| 0 | 0 或 1 | SLL | `(A << Amt)` 的低 W 位，低位补零。 |
| 1 | 0 | SRL | 无符号 `A >> Amt`，高位补零。 |
| 1 | 1 | SRA | 按 W 位补码解释 A 后算术右移，高位复制 A 的最高位。 |

移位量为零时所有操作返回 A；最大移位量为 W−1。左移丢弃越过高位的位，逻辑右移丢弃低位并补零，算术右移复制原符号位。没有旋转、饱和、异常或 RV64 字移位行为。

| Chisel 内部名称 | 类型 / 位宽 | 定义与连接 |
| --- | --- | --- |
| `Sign` | `Wire(Bool())` / 1 位 | `A[W−1] AND SubArith`；右移源字的高位填充值。 |
| `Z` | `Wire(UInt((2W−1).W))`，32/64 位配置为 63/127 位 | 右移时为 `{Sign 重复 W−1 次, A}`；左移时为 `{A, W−1 位零}`。 |
| `Offset` | `Wire(UInt(K.W))`，5/6 位 | 右移时为 Amt，左移时为 K 位 `~Amt = W−1−Amt`。 |
| `ZShift` | `Wire(UInt((2W−1).W))`，63/127 位 | 漏斗源字执行无符号右移 `Z >> Offset` 的结果。 |
| `io.Y` | `UInt(W.W)` | `ZShift[W−1:0]`。 |

左移源字先将 A 放在高 W 位，原先相当于左移 W−1；再向右移动 W−1−Amt，低 W 位即得到左移 Amt 的结果。右移源字则把 A 放在低 W 位，直接右移 Amt。三种运算由同一右移通路完成。

以上信号均为源码功能名，实际 RTL 名称及优化在验证节记录；编译器临时名称不作为稳定接口，不使用 `dontTouch` 强制保留。图 7.4 展示的逐级多路选择由动态右移运算的 RTL 和后续综合实现，本模块不手工指定门级单元。

## 4. 生成入口与当前整机接通情况

生成入口 `riscvsingle.GenerateShifter` 接受两个可选参数：数据位宽、输出目录。默认输出到 `generated/shifter`；Makefile 新增 `generate-shifter`，使用 `WIDTH` 和 `SHIFTER_TARGET_DIR`，可通过 `SBT=./scripts/sbt-local.sh` 使用本地工具链。

本轮仅生成 `generated/shifter` 和 `generated/shifter64` 中的 `.v、.fir、.anno.json`。Shifter 尚未接入 ALU、Datapath 或 IEU，整机仍执行已有简化 RV32 指令子集；不会因独立模块已支持移位而声称整机已支持 SLL/SRL/SRA。接入 ALU 属于下一轮。

## 5. 实际 RTL 名称与验证结果

验证日期：2026-10-04。工具链：JDK 17、sbt 1.10.7、Scala 2.13.14、Chisel 3.6.1、chiseltest 0.6.2（Treadle）、Verilator 5.036。

### 5.1 实际生成名称

两份独立 RTL 均保留功能名称 `Sign、Z、Offset、ZShift`：

| 实际 RTL 名称 | 32 位配置 | 64 位配置 |
| --- | --- | --- |
| `Sign` | 1 位 `wire`，取 `io_A[31] & io_SubArith` | 1 位 `wire`，取 `io_A[63] & io_SubArith` |
| `Z` | `wire [62:0]` | `wire [126:0]` |
| `Offset` | `wire [4:0]` | `wire [5:0]` |
| `ZShift` | `wire [62:0]` | `wire [126:0]` |
| `io_Y` | `ZShift[31:0]` | `ZShift[63:0]` |

编译器生成 `_Z_T` 保存 W−1 次 Sign 填充（31/63 位），`_Z_T_1` 为右移源字、`_Z_T_2` 为左移源字（63/127 位），`_Offset_T` 为 K 位 Amt 取反（5/6 位）。这些是本次导出名称，不保证后续版本或综合保持。两份 RTL 都只有五个端口，没有时钟、复位或 `always` 块。

### 5.2 先测试、再实现

先加入 `ShifterSpec`，执行：

```bash
cd riscv-single-cycle
./scripts/sbt-local.sh 'testOnly riscvsingle.ieu.ShifterSpec'
```

初次运行因模块尚未存在而出现 4 个缺少 `Shifter/ShifterIO` 的编译错误，尚未执行测试。随后增加仅包含参数检查、端口与 Y=0 的骨架，再运行同一命令：**10 项测试中 8 项功能测试按预期失败，接口与非法参数两项通过**，失败明确显示 Y=0 而预期为非零。

实现教材漏斗逻辑后重复同一命令：**Shifter 10 项测试全部通过**。

| 验证内容 | 实际覆盖 |
| --- | --- |
| 全移位量与边界模式 | 32/64 位分别对零、全一、1、最高位、最大有符号正值、最高位加 1、两种交替位遍历 0 至 W−1 的全部移位量；每次覆盖四种 Right/SubArith 组合，合计 3072 次检查。 |
| 手工参考边界 | 每种位宽 20 次：四种控制的零移位、最大移位量、正/负数的零扩展与符号扩展、左移截断及算术控制不影响左移，合计 40 次检查。 |
| 随机软件对照 | 每种位宽固定种子 `0x7133 + dataWidth`，1000 个随机操作数和移位量各检查四种控制组合，合计 8000 次检查。BigInt 模型直接使用左移、逻辑右移和补码算术右移，再取低 W 位，独立于漏斗源字与偏移表达式。 |
| 小位宽穷举 | 2 位配置的所有输入、移位量和控制组合共 32 次；4 位共 256 次，合计 288 次。 |
| 总输出检查 | 32 位 5044 次、64 位 6068 次、小位宽 288 次，合计 **11,400 次 Y 输出检查**。 |
| 生成接口 | 32/64 位均恰好五个组合端口，Amt 分别为 5/6 位，无时钟/复位和时序块。 |
| 非法参数 | −1、0、1、3、31、33、63 均在 elaboration 时被拒绝。 |

测试包装 `ShifterHarness` 为 chiseltest 提供 `Module` 顶层；时钟与复位仅在包装层出现，功能检查不推进时钟，生产导出不包含包装层。

### 5.3 全工程回归与 RTL 生成

实际执行：

```bash
./scripts/sbt-local.sh test
make generate-shifter SBT=./scripts/sbt-local.sh
make generate-shifter SBT=./scripts/sbt-local.sh WIDTH=64 SHIFTER_TARGET_DIR=generated/shifter64
```

**全工程结果：13 个套件、95 项测试全部通过，无失败或中止。** 基线为 85 项，本轮新增 Shifter 10 项，已有合法程序和全部模块回归保持通过。两次实际 Makefile 生成目标均成功，分别调用：

```text
runMain riscvsingle.GenerateShifter 32 generated/shifter
runMain riscvsingle.GenerateShifter 64 generated/shifter64
```

重新生成后，额外运行：

```bash
verilator --lint-only -Wall -Wno-UNUSEDSIGNAL --top-module Shifter generated/shifter/Shifter.v
verilator --lint-only -Wall -Wno-UNUSEDSIGNAL --top-module Shifter generated/shifter64/Shifter.v
```

两份独立 RTL lint 均退出成功。未使用的漏斗高位由输出截断规则产生，关闭相应未使用信号提示；没有将 lint 当作生成 RTL 的功能仿真。新增能力的功能结果由上述 Chisel 测试验证，本轮没有重新生成或仿真未接入 Shifter 的整机 RTL，也没有 FPGA 实现或时序分析。

日志为 `target/shifter-round4-{red,red-logic,focused,generate,regression}.log`，属于本地验证产物。沿用现有 `ChiselStage` 和 FIRRTL 流程，已有弃用提示不影响编译和生成。

## 6. 核验停点

**本轮停止，等待用户核验。** 核验对象为五个端口、位宽参数限制、教材漏斗源字与偏移规则、内部命名、32/64 位三种移位结果及左移对 SubArith 的忽略规则。

用户意见优先用于修订本模块。明确核验通过并允许继续后，才进入第 5 轮 ALU。本轮不自动提交、合并或进入下一模块。
