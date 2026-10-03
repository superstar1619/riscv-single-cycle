# 模块 01：Extend 立即数扩展核验报告

## 1. 依据与交付状态

对应《RISC-V System-on-Chip Design, Edition 1》Code Example 2.15 中的 `extend`，书中第 64 页。它将指令内的立即数按 I/S/B/J 格式重排并符号扩展，供后续 Datapath 使用。

本轮已完成 **Extend 一个硬件模块**、共享配置定义、生成入口及测试；尚未实现 Cmp 或后续 CPU 模块。项目文件夹已由 `chisel-empty` 改名为 `riscv-single-cycle`，原加法器源码和 Quartus 加法器模板已移除。

状态：**测试通过，等待用户核验；核验通过并允许继续后才实施 Cmp。**

| 交付内容 | 路径 |
| --- | --- |
| 模块及端口定义 | [Extend.scala](../../src/main/scala/riscvsingle/ieu/Extend.scala) |
| 共享 CPU 配置 | [CpuConfig.scala](../../src/main/scala/riscvsingle/config/CpuConfig.scala) |
| Verilog 生成入口 | [GenerateExtend.scala](../../src/main/scala/riscvsingle/GenerateExtend.scala) |
| 模块测试 | [ExtendSpec.scala](../../src/test/scala/riscvsingle/ieu/ExtendSpec.scala) |
| 配置检查测试 | [CpuConfigSpec.scala](../../src/test/scala/riscvsingle/config/CpuConfigSpec.scala) |
| 默认 32 位硬件 | [Extend.v](../../generated/extend/Extend.v) |
| 可复用 64 位输出版本 | [Extend.v](../../generated/extend64/Extend.v) |

## 2. 参数与端口

模块类为 `riscvsingle.ieu.Extend`，继承 `RawModule`，端口 Bundle 为 `ExtendIO`。所有端口为 `UInt`；负立即数通过二进制补码表示。

| 参数 | 类型 | 默认值 | 约束及作用 |
| --- | --- | --- | --- |
| `outputWidth` | Scala `Int` | 32 | 必须不小于 32；决定立即数输出和四路扩展结果的位宽。非法值在 elaboration 时抛出异常。 |

该参数在生成硬件时确定，不能在运行期间改变。`new Extend(64)` 仅说明此模块能输出 64 位符号扩展结果，第一版 CPU 仍是 RV32。

| Chisel 端口 | 生成的 Verilog 端口 | 方向 | 位宽 | 功能 |
| --- | --- | --- | --- | --- |
| `io.Instr` | `io_Instr` | 输入 | 25 | 对应书中的 `Instr[31:7]`。Chisel 输入下标 24 对应指令位 31，下标 0 对应指令位 7。 |
| `io.ImmSrc` | `io_ImmSrc` | 输入 | 2 | 选择 I/S/B/J 格式，编码与书中一致。 |
| `io.ImmExt` | `io_ImmExt` | 输出 | `outputWidth` | 选中的符号扩展立即数。B/J 型结果最低位恒为 0。 |

**无 `clock`、`reset`、使能或握手端口。** 输入变化后，输出经组合逻辑传播更新，不等待时钟沿，没有寄存器或锁存器。

未来 Datapath 的连接方式为：

```scala
val ext = Module(new Extend(config.xlen))
ext.io.Instr := io.Instr(31, 7) // 此处 Datapath 的 Instr 是完整的 32 位指令
ext.io.ImmSrc := io.ImmSrc
val ImmExt = ext.io.ImmExt
```

## 3. 功能与内部信号

下表中的 `instr[n]` 指完整指令的架构位编号，`SExt(value, W)` 表示扩展为 W 位补码；`W = outputWidth`。

| `ImmSrc` | 格式 | 符号扩展前的位拼接（从高位到低位） | 有符号数值范围 |
| --- | --- | --- | --- |
| `00` | I 型 | `{instr[31:20]}` | −2048 至 2047 |
| `01` | S 型 | `{instr[31:25], instr[11:7]}` | −2048 至 2047 |
| `10` | B 型 | `{instr[31], instr[7], instr[30:25], instr[11:8], 0}` | −4096 至 4094，步长 2 |
| `11` | J 型 | `{instr[31], instr[19:12], instr[20], instr[30:21], 0}` | −1048576 至 1048574，步长 2 |

所有格式的符号来源均为指令位 31。B/J 型结果是字节偏移量，低位补零已包含在拼接中，后续使用时不应再次左移一位。

| 内部信号名称 | 类型 / 位宽 | 功能及定义 |
| --- | --- | --- |
| `InstrFull` | `UInt(32.W)` | `{io.Instr, 7'b0}`；补齐低 7 位，使源码中的位索引与书中架构位号一致。补零位不参与立即数提取。 |
| `ImmI` | `UInt(W.W)` | `SExt(InstrFull[31:20], W)`，I 型扩展结果。 |
| `ImmS` | `UInt(W.W)` | `SExt({InstrFull[31:25], InstrFull[11:7]}, W)`，S 型扩展结果。 |
| `ImmB` | `UInt(W.W)` | 按上表重组 13 位分支偏移并符号扩展，最低位为 0。 |
| `ImmJ` | `UInt(W.W)` | 按上表重组 21 位跳转偏移并符号扩展，最低位为 0。 |

这些信号全部为组合 `Wire`。`MuxLookup` 按 `ImmSrc` 选择四路结果，直接驱动 `io.ImmExt`。

当前生成的 Verilog 保留了上述五个信号，还生成以下临时信号，方便核验波形或阅读 RTL：

| 生成器临时信号 | 32 位版本的位宽 | 作用 |
| --- | --- | --- |
| `_ImmI_T_1` | 20 | I/S/B 型共用的符号位复制结果；一般位宽为 `W-12`。 |
| `_ImmJ_T_1` | 12 | J 型符号位复制结果；一般位宽为 `W-20`。 |
| `_io_ImmExt_T_1` | 32 | `ImmSrc==01` 时选择 `ImmS`，否则选择 `ImmI`。 |
| `_io_ImmExt_T_3` | 32 | `ImmSrc==10` 时选择 `ImmB`，否则沿用上一层结果。 |

最后一层选择 J 型或上一层结果，驱动输出。临时信号名由编译器生成，修改源码或编译器版本后可能变化；综合也可能优化、合并上述信号，本轮没有使用 `dontTouch` 强制保留。

例如书中 Code Example 2.16 的 `addi x7, x3, -9`，完整指令为 `0xFF718393`：将其 `[31:7]` 接入 `Instr`，选择 `ImmSrc=00`，32 位结果为 `0xFFFFFFF7`，64 位结果为 `0xFFFFFFFFFFFFFFF7`。

## 4. 共享配置及与原书的差异

`CpuConfig` 是 Scala 配置类，不是硬件模块，不增加 RTL 端口或内部信号。本轮生成入口使用其默认 `xlen` 选择 Extend 输出宽度；其他参数将在对应模块实施时接入。

| 配置项 | 默认值 | 当前检查规则 |
| --- | --- | --- |
| `xlen` | 32 | 第一版只接受 32。 |
| `imemDepth` | 64 个字 | 不小于 2 的 2 次幂，总字节容量不得超过 RV32 地址空间。 |
| `dmemDepth` | 64 个字 | 与 `imemDepth` 相同。 |
| `resetVector` | 0 | 非负、能用 32 位表示，并按 4 字节对齐；不要求落在零起始存储范围内，地址映射留给 IFU/IROM 实施时定义。 |
| `instructionInitFile` | `None` | 可选初始化文件路径；提供时不能是空白字符串。文件加载和存在性检查尚未接入。 |

与书中 `extend` 的差异：

- 原书输出固定 32 位；本实现允许配置更宽的符号扩展输出。
- 原书 `Instr[31:7]` 具有非零起始下标；Chisel 使用 `UInt(25.W)` 的零起始下标，并借助 `InstrFull` 恢复架构位号。
- 原书端口名为 `Instr/ImmSrc/ImmExt`；生成的 Verilog 使用 Chisel Bundle 的 `io_` 前缀。
- 原书为不匹配的四态选择值指定 `'x`；本实现覆盖全部四个二态选择值，源码默认值为 0。不将 X/Z 的四态传播作为接口保证，二态功能与书中一致。
- 为方便核验，四种扩展结果分别命名为内部信号。没有增加流水级或指令译码功能。

## 5. 验证方法与实际结果

验证日期：2026-10-03。工具链为 JDK 17、sbt 1.10.7、Scala 2.13.14、Chisel 3.6.1、chiseltest 0.6.2；功能仿真使用默认 Treadle 后端。

在项目目录执行：

```bash
./scripts/sbt-local.sh test \
  'runMain riscvsingle.GenerateExtend' \
  'runMain riscvsingle.GenerateExtend 64 generated/extend64'
```

**实际结果：2 个测试套件、8 项测试全部通过；两种输出宽度的 Verilog 均生成成功。**

| 验证内容 | 实际覆盖 |
| --- | --- |
| 定向功能测试 | 每种输出宽度验证四种格式的零、正负最小步长、最大正值、最小负值，并逐位验证有效立即数位。 |
| 非立即数字段隔离 | 对各格式的边界样本，将不参与该立即数的指令位全部置 1，确认结果不变。 |
| 随机参考对照 | 固定种子 `0x215`，每种输出宽度测试 1000 条随机 32 位指令，每条覆盖全部 4 个选择值；软件模型按位映射并计算补码有符号值，独立于 RTL 的 `Cat/Fill` 实现。 |
| 数据检查总量 | 32/64 位各 96 次定向检查和 4000 次随机检查，合计 **8192 次输出检查**。 |
| 生成接口 | 检查默认 Verilog 仅包含 25 位输入、2 位输入、32 位输出三个端口，无时钟/复位端口或 `always @` 时序块。 |
| 非法输出宽度 | −1、0、12、31 均在 elaboration 时被拒绝。 |
| CPU 配置 | 验证默认及自定义值，拒绝非 RV32 位宽、非法深度、负数/越界/非对齐复位地址和空白初始化路径。 |

chiseltest 0.6.2 要求测试顶层继承 `Module`，因此测试文件中使用 `ExtendHarness` 包装生产模块。时钟和复位只存在于测试包装层；功能测试不推进时钟，直接检查组合输出。交付的 `Extend.v` 不包含该包装层。

编译存在 `chisel3.stage.ChiselStage` 的弃用提示；此接口在当前 Chisel 3.6.1 工具链中可以正常编译并生成 Verilog。本轮沿用现有 Scala FIRRTL 流程，不引入额外的 CIRCT/firtool 工具依赖。未进行 FPGA 实现、时序分析或完整 CPU 仿真。

## 6. 核验停点

本轮交付到此停止。请核验端口映射、四种立即数位拼接、参数范围、命名和报告格式；如需修改，仅修改 Extend 及其配套资料并重新验证。收到明确核验通过并允许继续的回复后，下一轮实施 Cmp。
