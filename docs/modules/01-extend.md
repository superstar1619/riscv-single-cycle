# 模块 01：Extend 立即数扩展核验报告（第七章第 1 轮）

## 1. 依据与本轮范围

依据本地《RISC-V System-on-Chip Design, Edition 1》第七章 §7.1（书中第 301–303 页）、图 7.2、表 7.1，具体位拼接与选择编码以 §7.1.2 和表 7.2（第 303 页）为准。第二章 Code Example 2.15 的 I/S/B/J 功能保持兼容。

CVW 参考文件：`/home/unlastingstar/cvw/src/ieu/extend.sv`。参考其 25 位指令字段、3 位选择编码和 U 型符号扩展；CVW 对未定义编码可输出 X，本实现按本阶段约定对 `101/110/111` 全部输出零，不引入原子指令或缓存维护扩展。

本轮只完善 **Extend**，配套调整测试、Datapath 连接及包含 Extend 的 RTL 导出。Controller、Datapath 的选择端口仍为原有 2 位；Datapath 在接入 Extend 时显式补高位零。整机仍执行原有简化 RV32 指令子集；LUI/AUIPC 的译码、操作数和写回选择待后续对应轮次实现。

| 交付内容 | 路径 |
| --- | --- |
| 模块与端口 | [Extend.scala](../../src/main/scala/riscvsingle/ieu/Extend.scala) |
| 生成入口 | [GenerateExtend.scala](../../src/main/scala/riscvsingle/GenerateExtend.scala) |
| 模块测试 | [ExtendSpec.scala](../../src/test/scala/riscvsingle/ieu/ExtendSpec.scala) |
| 连接适配 | [Datapath.scala](../../src/main/scala/riscvsingle/ieu/Datapath.scala) |
| 32 位 RTL | [Extend.v](../../generated/extend/Extend.v) |
| 64 位输出 RTL | [Extend.v](../../generated/extend64/Extend.v) |

## 2. 参数与全部端口

类 `riscvsingle.ieu.Extend` 继承 `RawModule`，端口定义为 `ExtendIO`。它是纯组合模块，无时钟、复位、使能或握手端口，无子模块、寄存器或锁存器。

| 参数 | 类型 | 默认值 | 合法范围与限制 |
| --- | --- | --- | --- |
| `outputWidth` | Scala `Int` | 32 | 不小于 32，在 elaboration 时确定；低于 32 抛出 `IllegalArgumentException`。更宽输出对全部五种格式做符号扩展；独立 64 位模块不代表整机支持 RV64I。 |

整机仍使用 `CpuConfig.xlen=32`；`imemDepth、dmemDepth、resetVector、instructionInitFile` 不参与本模块组合逻辑。生成入口无变化，默认读取 `CpuConfig().xlen`，也可显式指定更宽输出。

| Chisel 名称 | 实际 Verilog 端口 | 方向 | 类型 / 位宽 | 用途 |
| --- | --- | --- | --- | --- |
| `io.Instr` | `io_Instr` | 输入 | `UInt(25.W)` | 完整指令的 `Instr[31:7]`；输入位 24 对应架构位 31，输入位 0 对应架构位 7。 |
| `io.ImmSrc` | `io_ImmSrc` | 输入 | `UInt(3.W)` | 选择 I/S/B/J/U 或零输出。 |
| `io.ImmExt` | `io_ImmExt` | 输出 | `UInt(outputWidth.W)` | 补码表示的扩展立即数。 |

输入变化后输出经组合逻辑更新，无需时钟沿。对 X/Z 的四态传播不作接口保证；非法编码返回零指二态编码 `101/110/111`。

## 3. 编码、功能规则与内部信号

下表的 `instr[n]` 使用完整 32 位指令的架构位编号，`SExt(value, W)` 表示按该值最高位扩展为 W 位补码，`W=outputWidth`。

| `ImmSrc` | 格式 | 符号扩展前的位拼接（从高位到低位） | 有符号范围 |
| --- | --- | --- | --- |
| `000` | I | `{instr[31:20]}` | −2048 至 2047 |
| `001` | S | `{instr[31:25], instr[11:7]}` | −2048 至 2047 |
| `010` | B | `{instr[31], instr[7], instr[30:25], instr[11:8], 0}` | −4096 至 4094，步长 2 |
| `011` | J | `{instr[31], instr[19:12], instr[20], instr[30:21], 0}` | −1048576 至 1048574，步长 2 |
| `100` | U | `{instr[31:12], 12'b0}` | −2147483648 至 2147479552，步长 4096 |
| `101/110/111` | 未定义 | 全零 | 0，与指令内容无关 |

五种格式的符号来源都是位 31。B/J 输出已经是字节偏移，后续不得再次左移一位。U 输出低 12 位恒零，宽度大于 32 时将位 31 复制到更高位。例如 `Instr=0x80000037`、`ImmSrc=100`，32 位输出为 `0x80000000`，64 位输出为 `0xFFFFFFFF80000000`；`0x7FFFF037` 则输出 `0x7FFFF000` 或 `0x000000007FFFF000`。

| Chisel 内部名称 | 类型 / 位宽 | 定义 |
| --- | --- | --- |
| `InstrFull` | `UInt(32.W)` | `{io.Instr, 7'b0}`，恢复架构位索引；低 7 位不参与立即数提取。 |
| `ImmI` | `UInt(W.W)` | I 型 12 位立即数的符号扩展。 |
| `ImmS` | `UInt(W.W)` | S 型 12 位立即数的重组及符号扩展。 |
| `ImmB` | `UInt(W.W)` | B 型 13 位字节偏移的重组及符号扩展。 |
| `ImmJ` | `UInt(W.W)` | J 型 21 位字节偏移的重组及符号扩展。 |
| `ImmU` | `UInt(W.W)` | `SExt({InstrFull[31:12], 12'b0}, W)`。 |

以上为组合 `Wire`；`MuxLookup` 选择五路结果，默认全零，直接驱动 `io.ImmExt`。信号名是源码中的功能名称；编译器可合并、优化信号，实际导出名称在下节记录，临时名称不作为稳定接口。

## 4. 当前整机连接与导出范围

Datapath 在连接处使用 `Cat(0.U(1.W), io.ImmSrc)`，将旧的 2 位编码扩为 3 位。这保留 I/S/B/J 编码和所有已有程序行为，尚不能从整机控制器选择 U 型。其他模块控制行为、顶层端口和配置不变。

为保持导出层次一致，同步重新生成以下目录中的 `.v、.fir、.anno.json` 文件：

- `generated/extend`、`generated/extend64`：独立模块 32/64 位输出。
- `generated/datapath`、`generated/ieu`：现有执行层次中的 Extend。
- `generated/riscv-single`：64 项指令/数据存储器，复位地址 0，原书程序。
- `generated/riscv-single128`：128 项指令/数据存储器，复位地址 0x100，原有配置测试程序。

本轮保持已有 Verilator 验证脚本和测试程序；子字事务检查属于后续访存及整机轮次。

## 5. 实际生成名称与核验结果

验证日期：2026-10-04。工具链为 JDK 17、sbt 1.10.7、Scala 2.13.14、Chisel 3.6.1、chiseltest 0.6.2（Treadle）、Verilator 5.036。

### 5.1 实际 RTL 名称

已逐一检查 `generated/extend/Extend.v` 和 `generated/extend64/Extend.v`：

| 源码功能名 | 32 位实际 RTL | 64 位实际 RTL |
| --- | --- | --- |
| `InstrFull` | `wire [31:0] InstrFull` | `wire [31:0] InstrFull` |
| `ImmI/ImmS/ImmB/ImmJ/ImmU` | 对应同名 `wire [31:0]` | 对应同名 `wire [63:0]` |
| U 型拼接 | `{InstrFull[31], InstrFull[30:12], 12'h0}` | `{_ImmU_T_1, InstrFull[30:12], 12'h0}` |
| 输出 | `io_ImmExt[31:0]` | `io_ImmExt[63:0]` |

64 位版本的 `_ImmU_T_1[32:0]` 是 33 次位 31 的复制，其中包含 U 型结果本身的位 31 和更高 32 位的扩展。32 位版本没有该临时信号。I/S/B 共用 `_ImmI_T_1`，J 使用 `_ImmJ_T_1`；选择链产生 `_io_ImmExt_T_1/_3/_5/_7`。这些名称记录本次实际生成结果，**编译器临时名称不作为稳定接口**，不使用 `dontTouch` 强制保留。

两种独立 RTL 的输入均为 `io_Instr[24:0]`、`io_ImmSrc[2:0]`，仅有一个输出，没有时序块。四份包含 Extend 的层次导出均以 3 位连接子模块，Datapath 公开端口保持 2 位，生成连接为 `assign ext_io_ImmSrc = {1'h0, io_ImmSrc};`。

### 5.2 先失败、再实现

先增加新能力测试，运行：

```bash
cd riscv-single-cycle
./scripts/sbt-local.sh 'testOnly riscvsingle.ieu.ExtendSpec'
```

原实现实际运行 13 项测试：12 项失败、1 项通过，暴露原 2 位输入不能接受选择码 4–7 及 RTL 接口位宽不匹配。仅将输入扩为 3 位后重复同一命令：8 项失败、5 项通过，直接检查到 U 型输出零而预期为 `0x1000`、`0xFFFFF000` 等。随后实现 U 型拼接和连接适配，运行：

```bash
./scripts/sbt-local.sh 'testOnly riscvsingle.ieu.ExtendSpec riscvsingle.ieu.DatapathSpec'
```

**实际结果：2 个套件、20 项测试全部通过**，其中 Extend 13 项、Datapath 7 项。

| Extend 验证内容 | 每种输出宽度的覆盖 |
| --- | --- |
| 五种格式边界与有效立即数位 | 126 次检查：零、正负步长、最大正值、最小负值、有效位逐位变化及非立即数字段隔离；原 I/S/B/J 的 96 次检查保留。 |
| 指令位映射与忽略规则 | 320 次检查：五种格式分别遍历完整指令的 32 个架构位，使用 one-hot 和 one-cold；传入的是高 25 位，低 7 位不影响输出。 |
| U 型固定参考样本 | 12 次检查：零、4096、最大正值、最小负值、最小负值加 4096、−4096，分别改变低 12 位；预期值独立手工给定。 |
| 非法选择码 | 12 次定向检查：选择码 5/6/7，配合零、全一、最高位及混合指令，输出恒零。 |
| 随机软件模型对照 | 固定种子 `0x215`，1000 条随机指令各验证全部 8 个选择值，共 8000 次检查；模型逐位映射并计算有符号值，独立于 RTL 的 `Cat/Fill`。 |
| 输出检查总量 | 32/64 位各 8470 次，合计 **16,940 次**。 |
| 导出接口 | 两种宽度均验证恰好三个组合端口，选择输入为 3 位，无时钟/复位或 `always @`。 |
| 非法参数 | −1、0、12、31 均在 elaboration 时被拒绝。 |

测试使用 `ExtendHarness` 满足 chiseltest 对 `Module` 顶层的要求。时钟和复位只在测试包装层出现；立即数功能检查不推进时钟，生产 RTL 仍为 `RawModule`。

### 5.3 全工程回归、RTL 生成与仿真

在 `riscv-single-cycle` 目录运行：

```bash
./scripts/sbt-local.sh test
```

**实际结果：12 个套件、81 项测试全部通过，无失败或中止。** 基线为 74 项，本轮新增 7 项。已有合法程序、寄存器堆、访存、取指、执行层次和配置测试保持通过。

实际生成命令：

```bash
./scripts/sbt-local.sh \
  'runMain riscvsingle.GenerateExtend 32 generated/extend' \
  'runMain riscvsingle.GenerateExtend 64 generated/extend64' \
  'runMain riscvsingle.GenerateDatapath generated/datapath' \
  'runMain riscvsingle.GenerateIEU generated/ieu' \
  'runMain riscvsingle.GenerateRiscvSingle 64 64 generated/riscv-single programs/riscvtest.memfile 0' \
  'runMain riscvsingle.GenerateRiscvSingle 128 128 generated/riscv-single128 programs/rv32-configtest.memfile 0x100'
make test-rtl
```

六个生成入口全部退出成功。Verilator 直接编译两份重新生成的完整 CPU，每份使用种子 1、17、2026，**共六次 RTL 仿真全部通过**。原书程序每次检查 19 个执行周期和 2 次存储事务；扩容程序每次检查 13 个执行周期和 5 次存储事务；两者均通过结束循环与同步复位检查。该整机回归验证原有程序兼容性，新增 U 型由独立模块的 32/64 位测试核验。

日志保存在 `target/extend-round1-{red,red-u,green,generate,regression,rtl}.log`；它们是本地验证产物，不要求纳入版本控制。现有 `ChiselStage` 弃用提示不影响当前工具链编译与生成，本轮沿用现有 FIRRTL 流程。未做 FPGA 实现或时序分析。

## 6. 核验停点

**本轮停止，等待用户核验。** 核验范围为 Extend 的三位选择编码、五种立即数格式、32/64 位符号扩展、非法选择码零输出、内部命名及连接兼容性。用户的核验意见优先用于修订当前模块；明确核验通过并允许继续后，才进入第 2 轮 Cmp。

本轮不自动提交、合并或继续实施其他模块。
