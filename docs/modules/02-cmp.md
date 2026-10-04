# 模块 02：Cmp 比较器核验报告（第七章第 2 轮）

## 1. 依据与本轮范围

依据本地《RISC-V System-on-Chip Design, Edition 1》第七章 §7.1、图 7.2 和表 7.1。比较器的具体依据为 §7.1.3.6（书中第 308–311 页）、图 7.6 和 Code Example 7.1（第 311 页），以相等和大小比较产生分支所需标志。

CVW 参考文件：`/home/unlastingstar/cvw/src/ieu/comparator.sv`。按用户核验意见，本实现改用教材 Code Example 7.1 的最高位翻转方法：有符号路径翻转两个操作数的最高位，再进行无符号比较。本阶段仍按计划同时输出 `Eq、LT、LTU`，分别对应教材的相等结果、`sgnd=1` 的比较结果和 `sgnd=0` 的比较结果；不增加运行时 `sgnd` 端口，不手工实现前缀比较网络。

第 1 轮 Extend 已通过用户核验，并按用户指示提交为 `db4ddc4`。本轮只完善 **Cmp** 及其测试、生成 RTL 和报告。Datapath 继续将原有 `Eq` 提供给 Controller；新增标志的整机连接与六种分支译码属于后续对应轮次，不在本轮提前实现。

| 交付内容 | 路径 |
| --- | --- |
| 模块与端口 | [Cmp.scala](../../src/main/scala/riscvsingle/ieu/Cmp.scala) |
| 生成入口 | [GenerateCmp.scala](../../src/main/scala/riscvsingle/GenerateCmp.scala) |
| 模块测试 | [CmpSpec.scala](../../src/test/scala/riscvsingle/ieu/CmpSpec.scala) |
| 32 位 RTL | [Cmp.v](../../generated/cmp/Cmp.v) |
| 64 位 RTL | [Cmp.v](../../generated/cmp64/Cmp.v) |

## 2. 参数与全部端口

类 `riscvsingle.ieu.Cmp` 继承 `RawModule`，端口定义为 `CmpIO`。它是纯组合模块，没有时钟、复位、使能或握手端口，无子模块、寄存器或锁存器。

| 参数 | 类型 | 默认值 | 合法范围与限制 |
| --- | --- | --- | --- |
| `dataWidth` | Scala `Int` | 32 | 必须大于 0，在 elaboration 时确定两个操作数的相同位宽；非法值抛出 `IllegalArgumentException`。本轮验证 1、4、32、64 位。 |

生成入口仍默认使用 `CpuConfig().xlen=32`，支持显式指定其他正位宽。独立 64 位比较器不代表整机支持 RV64I；其他共享配置字段 `imemDepth、dmemDepth、resetVector、instructionInitFile` 不参与本模块逻辑。

| Chisel 名称 | 实际 Verilog 端口 | 方向 | 类型 / 位宽 | 用途 |
| --- | --- | --- | --- | --- |
| `io.R1` | `io_R1` | 输入 | `UInt(dataWidth.W)` | 第一个操作数的位模式。 |
| `io.R2` | `io_R2` | 输入 | `UInt(dataWidth.W)` | 第二个操作数的位模式。 |
| `io.Eq` | `io_Eq` | 输出 | `Bool` / 1 位 | 全部位相同则为 1。 |
| `io.LT` | `io_LT` | 输出 | `Bool` / 1 位 | 将两输入按等宽补码解释，R1 严格小于 R2 则为 1。 |
| `io.LTU` | `io_LTU` | 输出 | `Bool` / 1 位 | 将两输入按无符号整数解释，R1 严格小于 R2 则为 1。 |

输入均为 `UInt`；有符号路径在内部翻转最高位，无符号路径直接使用原始输入。输入变化后输出经组合逻辑更新，无需时钟沿。X/Z 四态传播不作为接口保证。

## 3. 功能规则与内部信号

令 `W=dataWidth`、无符号值为 `u`。对应补码有符号值是 `u`（当 `u < 2^(W-1)`），或 `u - 2^W`（否则）。`LT` 比较这两个有符号值，`LTU` 直接比较两个无符号值；相等时两个严格小于标志均为 0。

教材将最高位翻转后，负数映射到无符号区间的下半部，非负数映射到上半部，保留有符号次序。因此 `af=R1 XOR 2^(W-1)`、`bf=R2 XOR 2^(W-1)`，用无符号 `af < bf` 就能得到有符号小于。源码使用等宽 XOR 掩码表达翻转，等价于教材的最高位拼接，同时避免 `W=1` 时出现空的低位切片。

| 32 位 R1 | 32 位 R2 | `Eq` | `LT` | `LTU` | 说明 |
| --- | --- | --- | --- | --- | --- |
| `0x00000000` | `0x00000000` | 1 | 0 | 0 | 相等不满足严格小于。 |
| `0xFFFFFFFF` | `0x00000000` | 0 | 1 | 0 | 有符号为 −1 < 0，无符号为最大值 > 0。 |
| `0x00000000` | `0x80000000` | 0 | 0 | 1 | 有符号 0 大于最小负数，无符号 0 小于最高位置一的值。 |
| `0x80000000` | `0x7FFFFFFF` | 0 | 1 | 0 | 有符号最小值 < 最大值，无符号次序相反。 |
| `0xFFFFFFFB` | `0xFFFFFFF6` | 0 | 0 | 0 | −5 大于 −10，同为负数时两种比较次序一致。 |

这里直接做关系运算，不通过截断后的减法结果符号判断大小，因此有符号减法溢出不影响比较。模块不输出大于等于标志、不选择分支、不计算地址；对应控制判断留在 Controller。

| Chisel 内部名称 | 类型 / 位宽 | 定义与输出连接 |
| --- | --- | --- |
| `signMask` | `UInt(W.W)` 常量 | `2^(W-1)`，仅最高位为 1；无运行时控制输入。 |
| `af` | `Wire(UInt(W.W))` | `io.R1 ^ signMask`，教材的最高位翻转操作数。 |
| `bf` | `Wire(UInt(W.W))` | `io.R2 ^ signMask`，教材的最高位翻转操作数。 |
| `EqResult` | `Wire(Bool())` / 1 位 | `io.R1 === io.R2`，驱动 `io.Eq`。 |
| `SignedLT` | `Wire(Bool())` / 1 位 | 无符号 `af < bf`，驱动 `io.LT`。 |
| `UnsignedLT` | `Wire(Bool())` / 1 位 | `io.R1 < io.R2`，驱动 `io.LTU`。 |

这些名称标识源码功能，不保证综合后保留为独立内部线网；本次实际生成名称在下节记录。无需 `dontTouch`，编译器临时信号不作为稳定接口。

## 4. 当前整机连接与导出范围

本轮不修改 Datapath、Controller 或 IEU 的 Scala 连接：Cmp 原有两个输入和相等输出保持兼容，`LT/LTU` 暂未向整机控制器提供。因此整机继续执行已核验的简化指令子集，新比较器输出的独立测试不代表整机已支持六种条件分支。

为保持实际导出层次与源码一致，重新生成以下目录中的 `.v、.fir、.anno.json` 文件：

- `generated/cmp`、`generated/cmp64`：独立模块 32/64 位操作数。
- `generated/datapath`、`generated/ieu`：包含 Cmp 的既有执行层次。
- `generated/riscv-single`：64 项指令/数据存储器，复位地址 0，原书程序。
- `generated/riscv-single128`：128 项指令/数据存储器，复位地址 0x100，原有配置测试程序。

整机公开端口和程序镜像保持原有约定；Verilator 验证脚本继续检查已有程序兼容性。

## 5. 实际生成名称与模块测试

验证日期：2026-10-04。沿用 JDK 17、sbt 1.10.7、Scala 2.13.14、Chisel 3.6.1、chiseltest 0.6.2；功能仿真使用 Treadle。

### 5.1 实际 RTL 名称与优化

32/64 位独立导出的两个输入分别为 `io_R1/io_R2[31:0]` 和 `io_R1/io_R2[63:0]`，三个输出为 1 位 `io_Eq、io_LT、io_LTU`。32 位版本实际生成：

```verilog
wire [31:0] af = io_R1 ^ 32'h80000000;
wire [31:0] bf = io_R2 ^ 32'h80000000;
assign io_Eq = io_R1 == io_R2;
assign io_LT = af < bf;
assign io_LTU = io_R1 < io_R2;
```

64 位版本保留同名 `af/bf[63:0]`，翻转掩码为 `64'h8000000000000000`；其余三个输出赋值相同。`signMask` 被直接展开为常量，没有独立 RTL 线网。

源码中的 `EqResult、SignedLT、UnsignedLT` 在 FIRRTL 中有对应命名线网，实际 Verilog 将它们合并到输出赋值，没有独立同名内部 `wire`。因此独立模块波形可观察五个端口及 `af/bf`。临时节点和综合后的结构不是稳定接口，不强制保留。

四份层次导出中，Datapath 尚未消费 `LT/LTU`，编译器已移除其未使用的逻辑和子模块输出；这些导出内的 `Cmp` 仍只有 `R1/R2/Eq`。这是当前连接导致的优化，独立模块导出与源码均提供完整三个比较标志。

### 5.2 先失败、再实现

先升级测试和包装层，运行：

```bash
cd riscv-single-cycle
./scripts/sbt-local.sh 'testOnly riscvsingle.ieu.CmpSpec'
```

初次因现有 `CmpIO` 缺少 `LT/LTU` 而产生 6 个编译错误，尚未执行测试。随后只增加端口并临时将两个新输出固定为零，再执行同一命令：**10 项测试中 8 项失败、2 项通过**。失败明确检测到 `LT/LTU=false` 而预期为 `true`，覆盖 32/64 位边界和随机对照，以及 1/4 位穷举；接口与非法参数验证通过。

实现有符号和无符号比较后，运行：

```bash
./scripts/sbt-local.sh 'testOnly riscvsingle.ieu.CmpSpec riscvsingle.ieu.DatapathSpec'
```

**实际结果：2 个套件、17 项测试全部通过**，其中 Cmp 10 项、Datapath 7 项。

| Cmp 验证内容 | 实际覆盖 |
| --- | --- |
| 边界模式 | 32/64 位分别交叉比较零、全一、最低位置一、最高位置一、最大有符号正值及交替位，共 72 对输入；每对检查三个标志。 |
| 任意位差异 | 每位分别验证全零与该位置一、全一与该位清零，共 192 对输入；检查相等和两种大小比较。 |
| 手工参考边界 | 每种 32/64 位宽 10 对有序输入，覆盖异号、最大/最小有符号值、−1、零和一及反向次序；另检查 5 种相等边界的 `Eq=1、LT=LTU=0`，共 30 对输入，预期标志由手工给定。 |
| 随机参考对照 | 每种位宽固定种子 `0x215C0 + dataWidth`，1000 组各检查相等、独立随机、随机翻转一位，合计 6000 对输入；BigInt 模型通过减去 `2^W` 解释负数，独立于 RTL 的最高位翻转实现。 |
| 小位宽穷举 | 1 位全部 4 对、4 位全部 256 对输入，检查三个标志，共 260 对。 |
| 检查总量 | 共 **6554 对输入、19,662 次标志输出检查**。32 位 3115 对、64 位 3179 对，加上小位宽 260 对。 |
| 导出接口 | 32/64 位均恰好五个组合端口，无时钟/复位或 `always @`。 |
| 非法参数 | `dataWidth=-1/0` 均在 elaboration 时被拒绝。 |

原有 Eq 样本全部保留，新测试同时核验两个严格小于输出。测试用 `CmpHarness` 满足 chiseltest 的 `Module` 顶层要求；功能检查不推进时钟，生产导出不包含测试包装层的时钟或复位。

## 6. 全工程回归、生成命令与 RTL 仿真

在 `riscv-single-cycle` 目录执行：

```bash
./scripts/sbt-local.sh test
```

**实际结果：12 个套件、83 项测试全部通过，无失败或中止。** Extend 提交前的基线为 81 项，本轮增加两项手工参考边界测试。已有合法程序、各模块与配置检查保持通过。

实际生成命令：

```bash
./scripts/sbt-local.sh \
  'runMain riscvsingle.GenerateCmp 32 generated/cmp' \
  'runMain riscvsingle.GenerateCmp 64 generated/cmp64' \
  'runMain riscvsingle.GenerateDatapath generated/datapath' \
  'runMain riscvsingle.GenerateIEU generated/ieu' \
  'runMain riscvsingle.GenerateRiscvSingle 64 64 generated/riscv-single programs/riscvtest.memfile 0' \
  'runMain riscvsingle.GenerateRiscvSingle 128 128 generated/riscv-single128 programs/rv32-configtest.memfile 0x100'
make test-rtl
```

六个生成入口均成功。Verilator 5.036 对重新生成的默认和 128 项完整 CPU 各使用种子 1、17、2026，**六次 RTL 仿真全部通过**。原书程序每次检查 19 个执行周期和 2 次存储事务；扩容程序每次检查 13 个执行周期和 5 次存储事务；两者均通过结束循环和同步复位验证。该整机回归核验既有 Eq 连接及程序兼容性，新增 LT/LTU 的行为由独立模块测试核验。

日志：`target/cmp-round2-{red,red-logic,green,generate,regression,rtl}.log`，为本地验证产物，不要求纳入版本控制。沿用现有 `ChiselStage` 和 FIRRTL 流程；已有弃用提示不影响当前工具链编译或生成。未进行 FPGA 实现、时序分析或六种分支的整机验收。

### 6.1 按教材修改后的复核

按用户核验意见，将最初的 `asSInt` 比较改为教材的最高位翻转加无符号比较，保持全部端口和数值语义。修改前单独运行 Cmp 的 10 项测试通过；修改后重新执行上述全工程测试及六个生成入口，实际结果仍为 **12 个套件、83 项测试全部通过**，六份 RTL 均生成成功。沿用已有边界、随机模型和穷举测试，全部 19,662 次标志检查通过。

修改前验证日志为 `target/cmp-round2-textbook-before.log`，修改后的全工程回归与生成日志为 `target/cmp-round2-textbook.log`。重新运行 `make test-rtl`，两种容量各三个种子的六次仿真全部通过，本次日志为 `target/cmp-round2-textbook-rtl.log`。

单独生成也可使用：

```bash
make generate-cmp SBT=./scripts/sbt-local.sh
make generate-cmp SBT=./scripts/sbt-local.sh WIDTH=64 CMP_TARGET_DIR=generated/cmp64
```

## 7. 核验停点

**本轮停止，等待用户核验。** 核验范围为 Cmp 的五个端口、Eq/LT/LTU 语义、符号边界、正位宽参数范围、内部信号与 RTL 优化关系，以及原有程序兼容性。

用户核验意见优先用于修订本模块。明确核验通过并允许继续后，才进入第 3 轮 RegFile。本轮不自动提交、合并或进入下一模块。
