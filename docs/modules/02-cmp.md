# 模块 02：Cmp 相等比较器核验报告

## 1. 依据与交付状态

对应《RISC-V System-on-Chip Design, Edition 1》Code Example 2.15 中的 `cmp`，书中第 64 页：`assign Eq = (R1 == R2);`。它在 Datapath 中比较两个寄存器读出值，将相等标志提供给 Controller，供书中的 `beq` 跳转判断使用。

上一模块 Extend 已按用户指示提交，提交号为 `d6cf8cc`。本轮仅新增 **Cmp 一个硬件模块**、生成入口、测试及报告，没有实施 ALU 或后续模块。

状态：**Cmp 测试通过，尚未提交，等待用户核验；核验通过并允许继续后才实施 ALU。**

| 交付内容 | 路径 |
| --- | --- |
| 模块及端口定义 | [Cmp.scala](../../src/main/scala/riscvsingle/ieu/Cmp.scala) |
| Verilog 生成入口 | [GenerateCmp.scala](../../src/main/scala/riscvsingle/GenerateCmp.scala) |
| 模块测试 | [CmpSpec.scala](../../src/test/scala/riscvsingle/ieu/CmpSpec.scala) |
| 默认 32 位硬件 | [Cmp.v](../../generated/cmp/Cmp.v) |
| 64 位操作数版本 | [Cmp.v](../../generated/cmp64/Cmp.v) |

## 2. 参数与端口

模块类为 `riscvsingle.ieu.Cmp`，继承 `RawModule`，端口 Bundle 为 `CmpIO`。

| 参数 | 类型 | 默认值 | 约束及作用 |
| --- | --- | --- | --- |
| `dataWidth` | Scala `Int` | 32 | 必须大于 0；同时决定两个操作数的位宽，在 elaboration 时检查。 |

生成入口默认使用 `CpuConfig().xlen`，即 32。独立比较器支持其他正位宽，本轮验证了 1、4、32、64 位；这不改变 CPU 仅支持 RV32 的约束，也没有新增共享配置字段。

| Chisel 端口 | Verilog 端口 | 方向 | 类型 / 位宽 | 功能 |
| --- | --- | --- | --- | --- |
| `io.R1` | `io_R1` | 输入 | `UInt(dataWidth.W)` | 第一个寄存器操作数。 |
| `io.R2` | `io_R2` | 输入 | `UInt(dataWidth.W)` | 第二个寄存器操作数。 |
| `io.Eq` | `io_Eq` | 输出 | `Bool` / 1 位 | 两个操作数所有位均相同时为 1，否则为 0。 |

无时钟、复位、使能或握手端口。输入变化后，输出经组合逻辑传播更新，不等待时钟沿，没有寄存器或锁存器。

未来 Datapath 的连接方式为：

```scala
val cmp = Module(new Cmp(config.xlen))
cmp.io.R1 := R1
cmp.io.R2 := R2
io.Eq := cmp.io.Eq
```

## 3. 功能与内部信号

| 输入关系 | `Eq` |
| --- | --- |
| `R1` 与 `R2` 完全相同 | 1 |
| 任意一位不同 | 0 |

相等判断只比较位模式，与有符号或无符号数值解释无关。例如两个输入均为 `0xFFFFFFFF` 时，32 位版本输出 1；`0x80000000` 与 `0x00000000` 比较时输出 0。模块仅提供相等标志，不生成大小比较、分支选择或算术结果。

| 内部信号名称 | 所在层级 | 类型 / 位宽 | 功能 |
| --- | --- | --- | --- |
| `EqResult` | Chisel 源码和生成的 FIRRTL | `Wire(Bool())` / 1 位 | 保存 `io.R1 === io.R2` 的组合结果，直接驱动 `io.Eq`。 |
| `_EqResult_T` | FIRRTL 自动生成节点 | 1 位 | 对应相等运算 `eq(io.R1, io.R2)`，连接至 `EqResult`。 |

当前生成的 Verilog 将上述节点合并，实际逻辑为：

```verilog
assign io_Eq = io_R1 == io_R2;
```

因此 Verilog 中没有独立的内部 `wire`，查看该层级波形时可直接观察 `io_R1`、`io_R2`、`io_Eq`。没有使用 `dontTouch` 强制保留内部信号；自动节点名称和后续综合结果可能变化。

与原书的差异仅为操作数位宽可参数化、端口具有 `io_` 前缀，以及源码显式命名中间结果。默认 32 位二态相等判断与书中一致；X/Z 的四态传播不作为本轮验证保证。

## 4. 构建与实际验证结果

验证日期：2026-10-03。沿用 JDK 17、sbt 1.10.7、Scala 2.13.14、Chisel 3.6.1、chiseltest 0.6.2；功能仿真使用默认 Treadle 后端。

在项目目录执行：

```bash
./scripts/sbt-local.sh test \
  'runMain riscvsingle.GenerateCmp' \
  'runMain riscvsingle.GenerateCmp 64 generated/cmp64'
```

**实际结果：Cmp 新增 8 项测试全部通过；全项目 3 个套件、16 项测试全部通过，Extend 和配置检查回归无失败。32/64 位 Cmp Verilog 均已生成。**

| 验证内容 | 实际覆盖 |
| --- | --- |
| 边界模式 | 对 32/64 位分别交叉比较零、全一、最低位置一、最高位置一、最高位为零的其余全一及交替位，共 72 次检查。 |
| 任意位差异 | 对 32/64 位的每一位，分别检查全零与该位置一、全一与该位清零的差异，共 192 次检查。 |
| 随机对照 | 每种位宽使用固定种子 `0x215C0 + dataWidth`，生成 1000 组样本；每组检查相等、独立随机操作数及随机翻转一位的操作数，共 6000 次检查，参考值由 Scala `BigInt` 完整值比较得到。 |
| 小位宽穷举 | 1 位穷举 4 对输入，4 位穷举 256 对输入，合计 260 次检查。 |
| 数据检查总量 | 合计 **6524 次 Cmp 输出检查**，在连续输入变化时直接检查输出，不推进时钟。 |
| 生成接口 | 检查 32/64 位版本均只有 `R1`、`R2`、`Eq` 三个端口，无时钟/复位端口或 `always @` 时序块。 |
| 非法参数 | `dataWidth=-1` 和 `dataWidth=0` 均在 elaboration 时被拒绝。 |

chiseltest 0.6.2 要求测试顶层继承 `Module`，因此测试中使用 `CmpHarness` 包装。时钟/复位端口仅存在于测试包装层，交付的 `Cmp.v` 不包含该包装层。

编译中的 `ChiselStage` 弃用提示与上一模块相同；当前工具链正常编译和生成 Verilog。本轮未进行 FPGA 实现、时序分析或完整 CPU 仿真。

也可使用 Makefile 单独生成：

```bash
make generate-cmp SBT=./scripts/sbt-local.sh
make generate-cmp SBT=./scripts/sbt-local.sh WIDTH=64 CMP_TARGET_DIR=generated/cmp64
```

## 5. 核验停点

本轮交付到此停止。核验对象为参数范围、端口命名、相等比较功能、内部信号对应关系及报告格式。收到修改意见时，仅修改 Cmp 及其配套资料并重新验证；明确核验通过并允许继续后，下一模块为 ALU。
