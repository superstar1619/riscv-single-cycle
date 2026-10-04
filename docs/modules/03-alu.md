# 模块 03：ALU 算术逻辑单元核验报告（第七章第 5 轮）

## 1. 依据与本轮范围

依据本地《RISC-V System-on-Chip Design, Edition 1》第七章 §7.1、图 7.2 和表 7.1。具体结构参考 §7.1.3.1（第 304–305 页）、图 7.3、表 7.3，加减法参考 §7.1.3.2（第 305 页），SLT/SLTU 的进位与溢出修正参考 §7.1.3.4（第 308 页）。移位使用上一轮按 §7.1.3.3 实现的漏斗 Shifter。

CVW 参考文件：`/home/unlastingstar/cvw/src/ieu/alu.sv`、`/home/unlastingstar/cvw/src/ieu/shifter.sv`。沿用基础加减器、比较标志、移位器和 ALUSelect 结果选择；不引入位操作扩展、旋转、RV64 字运算、W64/UW64 或其他扩展控制。

上一轮 Shifter 已按用户指示提交为 `d9d7861`。本轮只完善 **ALU** 并接入该 Shifter，保留原六个端口；配套适配测试参考模型及层次 RTL 导出。Controller 的译码和其他硬件模块不在本轮增加新功能。

| 交付内容 | 路径 |
| --- | --- |
| 模块与端口 | [ALU.scala](../../src/main/scala/riscvsingle/ieu/ALU.scala) |
| 已有生成入口 | [GenerateALU.scala](../../src/main/scala/riscvsingle/GenerateALU.scala) |
| 模块测试 | [ALUSpec.scala](../../src/test/scala/riscvsingle/ieu/ALUSpec.scala) |
| 移位子模块 | [Shifter.scala](../../src/main/scala/riscvsingle/ieu/Shifter.scala) |
| Datapath 测试适配 | [DatapathSpec.scala](../../src/test/scala/riscvsingle/ieu/DatapathSpec.scala) |
| 层次测试适配 | [IEUSpec.scala](../../src/test/scala/riscvsingle/ieu/IEUSpec.scala)、[RiscvSingleSpec.scala](../../src/test/scala/riscvsingle/RiscvSingleSpec.scala) |
| 32 位 RTL | [ALU.v](../../generated/alu/ALU.v) |
| 64 位 RTL | [ALU.v](../../generated/alu64/ALU.v) |

## 2. 参数与全部端口

类 `riscvsingle.ieu.ALU` 继承 `RawModule`，端口定义为 `ALUIO`。令 `W=dataWidth`、`K=log2Ceil(W)`。没有时钟、复位、使能或握手端口，内部均为组合逻辑。

| 参数 | 类型 | 默认值 | 合法范围与限制 |
| --- | --- | --- | --- |
| `dataWidth` | Scala `Int` | 32 | 保留正整数位宽约定，决定两个操作数、结果和地址的位宽。非法非正值在 elaboration 时被拒绝。32/64 位用于本阶段标准配置，独立 64 位 ALU 不代表整机已支持 RV64I。 |

生成入口仍默认使用 `CpuConfig().xlen=32`，接受数据位宽与输出目录两个可选参数，不更改共享配置。其他字段 `imemDepth、dmemDepth、resetVector、instructionInitFile` 不参与本模块。

| Chisel 名称 | 实际 Verilog 端口 | 方向 | 类型 / 位宽 | 用途 |
| --- | --- | --- | --- | --- |
| `io.SrcA` | `io_SrcA` | 输入 | `UInt(W.W)` | 第一操作数。 |
| `io.SrcB` | `io_SrcB` | 输入 | `UInt(W.W)` | 第二操作数；移位只取低 K 位作为移位量。 |
| `io.ALUControl` | `io_ALUControl` | 输入 | `UInt(2.W)` | `{SubArith, ALUOp}`；位 1 控制加减和算术右移，位 0 使能 Funct3 选择。 |
| `io.Funct3` | `io_Funct3` | 输入 | `UInt(3.W)` | 指令功能码，ALUOp=0 时结果选择屏蔽为 000。 |
| `io.ALUResult` | `io_ALUResult` | 输出 | `UInt(W.W)` | 选择的整数运算结果；SLT/SLTU 为全字宽的零或一。 |
| `io.IEUAdr` | `io_IEUAdr` | 输出 | `UInt(W.W)` | 始终输出加减器 Sum，与 ALUResult 选择无关。 |

有符号含义按 W 位补码解释；所有数据端口仍传递 UInt 位模式。输入变化后输出经组合逻辑更新，无需时钟沿。X/Z 四态传播不作为接口保证。

## 3. 控制编码、功能与边界

`ALUOp=ALUControl[0]`，`SubArith=ALUControl[1]`，`ALUSelect=ALUOp ? Funct3 : 000`。`CondInvb=SubArith ? ~SrcB : SrcB`；加减器计算 W+1 位扩展结果，低 W 位为 Sum，最高位为 Carry，`IEUAdr=Sum`。

| 条件 | ALUResult | IEUAdr |
| --- | --- | --- |
| ALUOp=0，SubArith=0，任意 Funct3 | SrcA + SrcB 的低 W 位 | 同一加法结果 |
| ALUOp=0，SubArith=1，任意 Funct3 | SrcA − SrcB 的低 W 位 | 同一减法结果 |
| ALUOp=1，Funct3=000 | 按 SubArith 选择加法/减法 | 同一加减结果 |
| ALUOp=1，Funct3=001 | SLL：SrcA 左移 SrcB 的低 K 位，取低 W 位 | 按 SubArith 选择加减结果 |
| ALUOp=1，Funct3=010，SubArith=1 | SLT：有符号 SrcA < SrcB 则为 1，否则为 0 | SrcA − SrcB |
| ALUOp=1，Funct3=011，SubArith=1 | SLTU：无符号 SrcA < SrcB 则为 1，否则为 0 | SrcA − SrcB |
| ALUOp=1，Funct3=100 | 原始 SrcA XOR SrcB | 按 SubArith 选择加减结果 |
| ALUOp=1，Funct3=101，SubArith=0 | SRL：SrcA 逻辑右移 SrcB 的低 K 位 | SrcA + SrcB |
| ALUOp=1，Funct3=101，SubArith=1 | SRA：SrcA 算术右移 SrcB 的低 K 位 | SrcA − SrcB |
| ALUOp=1，Funct3=110 | 原始 SrcA OR SrcB | 按 SubArith 选择加减结果 |
| ALUOp=1，Funct3=111 | 原始 SrcA AND SrcB | 按 SubArith 选择加减结果 |

三位 ALUSelect 的全部八种二态值均有结果，原先返回零的 001/011/100/101 现在实现相应功能。非法指令判断仍属于 Controller，不在 ALU 内实现。保留 ALUOp=0 且 SubArith=1 的强制减法行为。

**SLT/SLTU 的有效比较控制前提为 SubArith=1。** 比较标志来自减法，ALU 不自动修改加减控制；SubArith=0 时仍按原始加法标志驱动对应位，不承诺该控制组合表示有效小于比较。位运算使用原始 SrcB，SubArith 只改变加减器和算术右移，未引入 XNOR/ORN/ANDN。

加减法按模 `2^W` 回绕。SLT 用减法符号位及溢出修正，SLTU 用无借位进位的反相：

```text
Overflow = (SrcA[W−1] XOR SrcB[W−1]) AND (SrcA[W−1] XOR Sum[W−1])
Neg      = Sum[W−1]
LT       = Neg XOR Overflow
LTU      = NOT Carry
```

例如最小有符号值减 1 的低 W 位变为最大正值，溢出修正后 SLT 仍为 1。无符号 A<B 时减法产生借位，Carry=0、LTU=1；A=B 时 Carry=1、LTU=0。

移位量只取低 K 位，但 IEUAdr 使用完整 SrcB。例如 W=32、SrcA=1、SrcB=32、SLL 控制时，ALUResult=1（移位量 0），IEUAdr=33。左移不受 SubArith 影响，右移按该位选择零/符号扩展。所有整数寄存器运算与对应立即数运算共用同一组合数据通路，立即数扩展和指令合法性译码仍在外围。

## 4. 移位连接与当前整机范围

32/64 位配置直接实例化同宽 Shifter，将 SrcA 接 A、SrcB 的低 5/6 位接 Amt、Funct3[2] 接 Right、SubArith 接算术控制，Y 接 ShiftResult。ALUSelect=001 或 101 选择该结果。

为保留既有正位宽配置，小位宽或非二次幂位宽通过适配到不小于 W 且至少为 2 的二次幂 Shifter：A 的高位复制 `SrcA[W−1] AND SubArith`，因此逻辑右移补零、算术右移按原符号扩展；左移只取低 W 位，高位填充值不影响结果。W=1 时移位量固定为零。该适配不改变 32/64 位教材路径，不增加旋转或字运算。

Datapath、IEU 与整机现有 ALU 连接无需改端口。Datapath 的随机测试参考模型更新以验证新增运算；硬件译码仍是上一阶段 Controller，尚未补齐 SLTU/SRA 等控制和完整合法性判断，不以本轮 ALU 测试声称整机已实现全部表 7.1 指令。

本轮重新生成 ALU 32/64 位，以及包含它的 Datapath、IEU、默认 CPU 和 128 项 CPU，保持各自容量、初始化文件和复位地址。

## 5. 内部信号及实际 RTL 名称

以下对应本轮实际导出的 32/64 位 ALU；W 分别为 32/64，K 为 5/6。信号均为组合 Wire，没有新增状态。

| Chisel 名称 | 位宽 | 定义 | 实际生成 RTL |
| --- | --- | --- | --- |
| `ALUOp` | 1 | ALUControl[0]，使能功能选择 | `ALUOp` |
| `SubArith` | 1 | ALUControl[1]，减法/算术右移 | `SubArith` |
| `CondInvb` | W | 条件反相 SrcB | `CondInvb` |
| `SumExt` | W+1 | 零扩展 SrcA + CondInvb + SubArith | `SumExt`，33/65 位 |
| `Carry` | 1 | SumExt[W]，减法时表示无借位 | `Carry` |
| `Sum` | W | SumExt 的低 W 位 | `Sum`；`io_IEUAdr` 直接连接 `SumExt[31:0]` / `[63:0]` |
| `Overflow` | 1 | 减法有符号溢出修正标志 | `Overflow` |
| `Neg` | 1 | Sum[W−1] | 别名被优化，使用 `Sum[31]` / `[63]` |
| `LT` | 1 | Neg XOR Overflow | `LT` |
| `LTU` | 1 | NOT Carry | `LTU` |
| `SLT` / `SLTU` | W | 比较结果零扩展为全字宽 | `SLT` / `SLTU` |
| `ShiftResult` | W | Shifter.Y 的低 W 位 | `ShiftResult` |
| `ALUSelect` | 3 | Funct3 AND 三份 ALUOp | `ALUSelect` |
| `shifter` | 子模块 | 漏斗移位器实例 | `Shifter shifter` |

子模块连接实际为 `shifter_io_A=io_SrcA`、`shifter_io_Amt=io_SrcB[4:0]` / `[5:0]`、`shifter_io_Right=io_Funct3[2]`、`shifter_io_SubArith=io_ALUControl[1]`、`ShiftResult=shifter_io_Y`。Shifter 内部的 `Sign、Z、Offset、ZShift` 保留，定义与上一轮报告一致。

生成器将位运算、扩展加法中间项和结果选择分解为 `_CondInvb_T、_SumExt_T、_GEN_0、_Overflow_T_5、_ALUSelect_T、_io_ALUResult_T*` 等临时线网。这些名称仅描述当前编译输出，不是稳定接口。结果选择在 RTL 中展开为组合选择链；IEUAdr 直接来自加减器，不经过该链。

## 6. 测试过程与结果

先扩展 ALUSpec 和 Datapath 参考模型，再运行：

```bash
./scripts/sbt-local.sh 'testOnly riscvsingle.ieu.ALUSpec riscvsingle.ieu.DatapathSpec riscvsingle.ieu.ControllerSpec'
```

红阶段：3 个套件、24 项测试，10 通过、14 失败，退出码 1；失败对应未实现的 XOR/SLTU/移位、缺少 Shifter 子模块，以及旧 Datapath 对新增结果的期望。实现后同一命令：**24 项全部通过，退出码 0**。日志分别为 `target/alu-round5-red.log` 和 `target/alu-round5-focused.log`。

ALUSpec 共 14 项测试，覆盖 30 种有效控制/功能组合：ALUOp=0 的 16 种，以及 ALUOp=1 的 14 种（SLT/SLTU 只验证 SubArith=1）。每个输入同时断言 ALUResult 与 IEUAdr，参考模型用 BigInt 加减、直接有符号/无符号比较和移位，独立于实现的进位/溢出表达式。

| 位宽 | 输入用例数 | 覆盖 |
| --- | ---: | --- |
| 32 | 18,537 | 七种边界模式两两组合；500 对随机操作数；十种寄存器和九种立即数操作；八种操作数模式下的移位、全部 32 个移位量、SrcB 高位干扰 |
| 64 | 20,585 | 同类边界和随机验证；全部 64 个移位量；独立 64 位结果 |
| 1 | 120 | 所有操作数对和 30 种控制组合；零移位适配 |
| 4 | 7,680 | 所有操作数对和 30 种控制组合 |
| 3 | 1,920 | 所有操作数对和 30 种控制组合；非二次幂适配 |
| 5 | 30,720 | 所有操作数对和 30 种控制组合；非二次幂适配 |
| 合计 | **79,562** | **159,124 次结果/地址断言** |

边界涵盖加减回绕、有符号最小/最大值、比较溢出、无符号高位、相等及借位；移位验证零与最大移位、符号填充、量截取，并确认完整 SrcB 仍影响 IEUAdr。位运算在两个 SubArith 值下验证原始 B。立即数测试在 ALU 边界提供已扩展操作数，不替代后续 Controller 指令译码验证。

接口测试核对 32/64 位全部六个端口的方向和位宽、无时序块，以及五个 Shifter 端口连接；非法位宽 0/−1 在 elaboration 时被拒绝。Datapath 随机参考模型覆盖新增八路结果，并给 SLT/SLTU 与 SRA 设置相应减法/算术控制。

全工程回归命令：

```bash
./scripts/sbt-local.sh test
```

首次运行 99 项中 97 通过，两项失败仅因 IEU/顶层测试的精确模块集合未包括新增 Shifter。更新这两处层次期望后重跑，**13 个套件、99 项测试全部通过，退出码 0**；原书程序、容量配置、同步复位与 RAM 保持回归均通过。最终日志为 `target/alu-round5-regression-final.log`，首次日志为 `target/alu-round5-regression.log`。

## 7. RTL 生成与整机验证

实际生成命令：

```bash
./scripts/sbt-local.sh \
  'runMain riscvsingle.GenerateALU 32 generated/alu' \
  'runMain riscvsingle.GenerateALU 64 generated/alu64' \
  'runMain riscvsingle.GenerateDatapath generated/datapath' \
  'runMain riscvsingle.GenerateIEU generated/ieu' \
  'runMain riscvsingle.GenerateRiscvSingle 64 64 generated/riscv-single programs/riscvtest.memfile 0' \
  'runMain riscvsingle.GenerateRiscvSingle 128 128 generated/riscv-single128 programs/rv32-configtest.memfile 0x100'
make test-rtl
```

六个生成入口全部成功，退出码 0，日志 `target/alu-round5-generate.log`。产物：

| 配置 | 生成 RTL |
| --- | --- |
| ALU 32 位 | [generated/alu/ALU.v](../../generated/alu/ALU.v) |
| ALU 64 位 | [generated/alu64/ALU.v](../../generated/alu64/ALU.v) |
| Datapath | [generated/datapath/Datapath.v](../../generated/datapath/Datapath.v) |
| IEU | [generated/ieu/IEU.v](../../generated/ieu/IEU.v) |
| 默认 64 项 CPU、复位地址 0 | [generated/riscv-single/RiscvSingle.v](../../generated/riscv-single/RiscvSingle.v) |
| 128 项 CPU、复位地址 0x100 | [generated/riscv-single128/RiscvSingle.v](../../generated/riscv-single128/RiscvSingle.v) |

核对六份 RTL 中 ALU 均保持六个原端口，包含 Shifter 子模块，无新增时序逻辑。两种完整 CPU 均使用重新生成的 RTL。Verilator 5.036 对 book/expanded 配置分别使用随机初值种子 1、17、2026，**六次运行全部 PASS，退出码 0**；book 每次执行 19 个周期和 2 次存储，expanded 每次执行 13 个周期和 5 次存储，均检查结束循环及同步复位。日志 `target/alu-round5-rtl.log`。

当前整机已接入新增 ALU 数据通路，完整 Controller 译码仍待第 6 轮；本轮整机 RTL 运行验证既有合法程序，新增运算由 ALU 定向/参考模型及 Datapath 测试覆盖。整机仍限定 RV32，当前访存仍为原字访问，子字访存不在本轮实现。

## 8. 核验停点

**本轮停止，等待用户核验。** 第 5 轮 ALU 源码、测试、生成 RTL 和报告已交付。本轮改动保留在工作树；明确核验通过并允许继续后，提交当前轮并进入第 6 轮 Controller，不自动连续实施后续模块。
