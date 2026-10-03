# 模块 03：ALU 算术逻辑单元核验报告

## 1. 依据与交付状态

对应《RISC-V System-on-Chip Design, Edition 1》Code Example 2.15 中的 `alu`，书中第 64–65 页。模块完成加法、减法、有符号小于比较、按位或、按位与，并同时向 IFU/LSU 提供加减法结果 `IEUAdr`。

上一模块 Cmp 已按用户指示提交，提交号为 `4b96fd7`。本轮仅新增 **ALU 一个硬件模块**、生成入口、测试及报告，没有实施 RegFile 或后续模块。

状态：**ALU 测试通过，尚未提交，等待用户核验；核验通过并允许继续后才实施 RegFile。**

| 交付内容 | 路径 |
| --- | --- |
| 模块及端口定义 | [ALU.scala](../../src/main/scala/riscvsingle/ieu/ALU.scala) |
| Verilog 生成入口 | [GenerateALU.scala](../../src/main/scala/riscvsingle/GenerateALU.scala) |
| 模块测试 | [ALUSpec.scala](../../src/test/scala/riscvsingle/ieu/ALUSpec.scala) |
| 默认 32 位硬件 | [ALU.v](../../generated/alu/ALU.v) |
| 64 位操作数版本 | [ALU.v](../../generated/alu64/ALU.v) |

## 2. 参数与端口

模块类为 `riscvsingle.ieu.ALU`，继承 `RawModule`，端口 Bundle 为 `ALUIO`。

| 参数 | 类型 | 默认值 | 约束及作用 |
| --- | --- | --- | --- |
| `dataWidth` | Scala `Int` | 32 | 必须大于 0；决定操作数、结果和地址的位宽，在 elaboration 时检查。 |

生成入口默认使用 `CpuConfig().xlen`，即 32；没有新增共享配置字段。独立模块验证了 1、4、32、64 位，64 位版本表示同一组运算可复用，不代表 CPU 已支持 RV64。

| Chisel 端口 | Verilog 端口 | 方向 | 类型 / 位宽 | 功能 |
| --- | --- | --- | --- | --- |
| `io.SrcA` | `io_SrcA` | 输入 | `UInt(dataWidth.W)` | 第一个操作数。 |
| `io.SrcB` | `io_SrcB` | 输入 | `UInt(dataWidth.W)` | 第二个操作数。 |
| `io.ALUControl` | `io_ALUControl` | 输入 | `UInt(2.W)` | `{Sub, ALUOp}`；位 1 选择加减法，位 0 使能 `Funct3` 功能选择。 |
| `io.Funct3` | `io_Funct3` | 输入 | `UInt(3.W)` | 指令功能码，`ALUOp=0` 时屏蔽为 `000`。 |
| `io.ALUResult` | `io_ALUResult` | 输出 | `UInt(dataWidth.W)` | 选中的算术或逻辑运算结果；SLT 的结果为整字宽的 0 或 1。 |
| `io.IEUAdr` | `io_IEUAdr` | 输出 | `UInt(dataWidth.W)` | 始终输出加减法结果 `Sum`，不随逻辑运算结果选择改变。 |

所有数据均为二进制位模式；有符号比较按 `dataWidth` 位补码解释。无时钟、复位、使能或握手端口，全部为组合逻辑，没有寄存器或锁存器。

未来 Datapath 应将原有 `ALUControl`、指令 `Funct3` 和两个操作数接入 `Module(new ALU(config.xlen))`，分别使用 `ALUResult` 做结果选择、`IEUAdr` 做地址计算。

## 3. 功能与控制编码

令 `W=dataWidth`，所有加减法结果按模 `2^W` 回绕，不输出额外进位位。组合关系为：

```text
ALUOp    = ALUControl[0]
Sub      = ALUControl[1]
CondInvb = Sub ? ~SrcB : SrcB
Sum      = SrcA + CondInvb + Sub   （保留低 W 位）
IEUAdr   = Sum
ALUFunct = ALUOp ? Funct3 : 000
```

| `ALUControl` | `Funct3` | `ALUResult` | `IEUAdr` |
| --- | --- | --- | --- |
| `00` | 任意 | `SrcA + SrcB` | `SrcA + SrcB` |
| `01` | `000` | `SrcA + SrcB` | `SrcA + SrcB` |
| `11` | `000` | `SrcA - SrcB` | `SrcA - SrcB` |
| `11` | `010` | 有符号 `SrcA < SrcB` 时为 1，否则为 0 | `SrcA - SrcB` |
| `01` 或 `11` | `110` | `SrcA OR SrcB` | 由 `Sub` 决定加法或减法 |
| `01` 或 `11` | `111` | `SrcA AND SrcB` | 由 `Sub` 决定加法或减法 |
| `10` | 任意 | `SrcA - SrcB` | `SrcA - SrcB` |
| `01` 或 `11` | `001/011/100/101` | 确定的 0 | 由 `Sub` 决定加法或减法 |

书中的 Controller 不生成 `10`，但本模块按原有控制位定义保留其行为。正常 OR/AND 使用 `01`，设置 `11` 时仍按位计算 `ALUResult`，同时让 `IEUAdr` 输出减法结果。

**SLT 的控制器前提是 `ALUControl=11`。** `01` 配合 `Funct3=010` 不是书中 Controller 会生成的组合；本模块保留书中内部公式，不将该组合承诺为有效的有符号小于比较，也不自动纠正控制信号。

书中的 SLT 使用减法符号位及溢出修正：

```text
Overflow = (SrcA[W-1] XOR SrcB[W-1]) AND (SrcA[W-1] XOR Sum[W-1])
Neg      = Sum[W-1]
LT       = Neg XOR Overflow
SLT      = LT ? W位的1 : W位的0
```

例如 32 位 `SrcA=0x80000000`、`SrcB=1`，执行 SLT 时减法回绕为 `0x7FFFFFFF`，符号位本身不能表示正确的大小关系；`Overflow=1` 修正后输出 `ALUResult=1`。`Overflow` 沿用书中的减法比较公式，不能将其当成通用加法溢出标志。

对于逻辑运算，两个输出可以不同：`SrcA=12`、`SrcB=7`、控制码 `01`、功能码 `110` 时，`ALUResult=15`，而 `IEUAdr=19`。

## 4. 内部信号名称

以下均为源码显式命名的组合 `Wire`，名称与书中一致。

| 信号名称 | 类型 / 位宽 | 功能 |
| --- | --- | --- |
| `ALUOp` | `Bool` / 1 | `ALUControl` 位 0，允许使用指令功能码。 |
| `Sub` | `Bool` / 1 | `ALUControl` 位 1，控制第二操作数取反及加一。 |
| `CondInvb` | `UInt(W.W)` | 加法时为 `SrcB`，减法时为 `~SrcB`。 |
| `Sum` | `UInt(W.W)` | 加减法结果，驱动 `IEUAdr`，并参与算术结果及 SLT 计算。 |
| `Overflow` | `Bool` / 1 | 减法有符号溢出修正值。 |
| `Neg` | `Bool` / 1 | `Sum` 的最高位。 |
| `LT` | `Bool` / 1 | `Neg XOR Overflow`；减法模式下为有符号小于标志。 |
| `SLT` | `UInt(W.W)` | 将 `LT` 表示为整字宽的 0 或 1。 |
| `ALUFunct` | `UInt(3.W)` | `Funct3 & Fill(3, ALUOp)`，屏蔽后的功能码。 |

当前 Verilog 保留了上述信号中的八个；`Neg` 在 FIRRTL 中有独立名称，在 Verilog 中被直接合并为 `Sum[W-1]`。没有使用 `dontTouch` 强制保留信号。

当前生成器还产生以下辅助信号：

| Verilog 辅助信号 | 位宽 | 作用 |
| --- | --- | --- |
| `_CondInvb_T` | W | `~SrcB`。 |
| `_Sum_T_1` | W | `SrcA + CondInvb` 的低 W 位。 |
| `_GEN_0` | W | 将 `Sub` 零扩展为 W 位，用作加一操作数。 |
| `_Overflow_T_5` | 1 | `SrcA[W-1] XOR Sum[W-1]`。 |
| `_ALUFunct_T` | 3 | `ALUOp` 为 1 时为 `111`，否则为 `000`。 |
| `_io_ALUResult_T` | W | 按位或结果。 |
| `_io_ALUResult_T_1` | W | 按位与结果。 |
| `_io_ALUResult_T_3` | W | 选择 `Sum` 或默认值 0。 |
| `_io_ALUResult_T_5` | W | 选择 `SLT` 或上一层结果。 |
| `_io_ALUResult_T_7` | W | 选择按位或或上一层结果；最终再选择按位与。 |

自动生成名称可能随源码、编译器或综合优化变化，核验功能应以命名逻辑及端口关系为准。

## 5. 与原书的差异及验证结果

- 原书固定 32 位；本模块允许正整数数据位宽，默认 32。
- Chisel Bundle 使 Verilog 端口具有 `io_` 前缀，逻辑信号名称沿用原书。
- 原书对未实现的有效功能码返回 `'x`；本实现返回确定的 0，便于重复仿真和扩展。此默认值不代表非法指令异常处理，Controller 尚未实现。
- SLT 采用显式 W 位 0/1 选择，与原书高位补零的结果一致。X/Z 四态传播不作为本轮接口保证。

验证日期：2026-10-03。工具链为 JDK 17、sbt 1.10.7、Scala 2.13.14、Chisel 3.6.1、chiseltest 0.6.2；功能仿真使用默认 Treadle 后端。

```bash
./scripts/sbt-local.sh test \
  'runMain riscvsingle.GenerateALU' \
  'runMain riscvsingle.GenerateALU 64 generated/alu64'
```

**实际结果：ALU 新增 10 项测试全部通过；全项目 4 个套件、26 项测试全部通过，原有模块及配置检查回归无失败。32/64 位 Verilog 均已生成。**

| 验证内容 | 实际覆盖 |
| --- | --- |
| 控制与双输出 | 每对操作数覆盖 23 种设置：`00/10` 各遍历全部 8 种功能码，加法、减法、SLT，以及两种 Sub 设置下的 OR/AND；每次同时检查 `ALUResult` 和 `IEUAdr`。 |
| 边界测试 | 32/64 位分别交叉使用零、1、全一、最小负数、最大正数、最小负数加一及交替位，共 `2×7×7×23=2254` 组检查，覆盖回绕及 SLT 的符号/溢出边界。 |
| 随机对照 | 固定种子 `0x215A1 + dataWidth`，32/64 位各生成 500 对随机操作数，共 `2×500×23=23000` 组检查。加减法参考使用 BigInt 加减，有符号 SLT 参考直接比较补码解释后的数值，不复用 RTL 溢出公式。 |
| 小位宽穷举 | 穷举 1 位及 4 位的全部操作数对和 23 种设置，共 `(4+256)×23=5980` 组检查。 |
| 默认功能码 | 32/64 位对两种 Sub 设置和 4 个未实现功能码检查默认 0，并确认 `IEUAdr` 仍输出正确加减法结果，共 16 组。 |
| 数据检查总量 | 合计 **31250 组输入、62500 次输出检查**，直接验证组合输出，不推进时钟。 |
| 生成接口 | 32/64 位均只有书中的 6 个端口，控制码 2 位、功能码 3 位，操作数及输出位宽匹配，无时钟/复位端口或 `always @` 时序块。 |
| 非法参数 | `dataWidth=-1/0` 在 elaboration 时被拒绝。 |

测试使用 `ALUHarness` 满足 chiseltest 0.6.2 对 `Module` 测试顶层的要求，生产 `ALU` 仍为 `RawModule`。编译中的 `ChiselStage` 弃用提示与前两轮相同，本轮未进行 FPGA 实现、时序分析或完整 CPU 仿真。

单独生成可执行：

```bash
make generate-alu SBT=./scripts/sbt-local.sh
make generate-alu SBT=./scripts/sbt-local.sh WIDTH=64 ALU_TARGET_DIR=generated/alu64
```

## 6. 核验停点

本轮到此停止，ALU 变更保留在工作区等待核验。收到修改意见时，仅修改当前模块及配套资料并重新验证；明确核验通过并允许继续后，下一模块为 RegFile。
