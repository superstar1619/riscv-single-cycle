# 模块 04：RegFile 寄存器堆核验报告

## 1. 依据与交付状态

对应《RISC-V System-on-Chip Design, Edition 1》Code Example 2.15 中的 `regfile`，书中第 63–64 页。实现两个组合读端口、一个上升沿写端口，供 Datapath 读取源寄存器和写回结果。

上一模块 ALU 已按用户指示提交，提交号为 `aa69b21`。本轮仅新增 **RegFile 一个硬件模块**、生成入口、测试及报告，没有实施 Controller 或后续模块。

状态：**RegFile 测试通过，尚未提交，等待用户核验；核验通过并允许继续后才实施 Controller。**

| 交付内容 | 路径 |
| --- | --- |
| 模块及端口定义 | [RegFile.scala](../../src/main/scala/riscvsingle/ieu/RegFile.scala) |
| Verilog 生成入口 | [GenerateRegFile.scala](../../src/main/scala/riscvsingle/GenerateRegFile.scala) |
| 模块测试 | [RegFileSpec.scala](../../src/test/scala/riscvsingle/ieu/RegFileSpec.scala) |
| 默认 32 位硬件 | [RegFile.v](../../generated/regfile/RegFile.v) |
| 64 位数据版本 | [RegFile.v](../../generated/regfile64/RegFile.v) |

## 2. 参数与端口

模块类为 `riscvsingle.ieu.RegFile`，继承 `RawModule`，通过显式 `clk` 和 `withClock` 建立时序逻辑。数据端口 Bundle 为 `RegFileIO`。令 `W=dataWidth`、`N=registerCount`、`A=log2Ceil(N)`。

| 参数 | 类型 | 默认值 | 约束及作用 |
| --- | --- | --- | --- |
| `dataWidth` | Scala `Int` | 32 | 必须大于 0，决定寄存器和数据端口位宽。 |
| `registerCount` | Scala `Int` | 32 | 必须是不小于 2 的 2 次幂；决定时序寄存器数量和地址位宽，包含通过复位清零的 x0。 |

参数在 elaboration 时检查。按用户最新核验意见，`rf` 改为默认 **32 项时序寄存器**的 `Reg(Vec(...))`，`rf(i)` 直接对应 xi，读写编号不减一。新增高有效同步复位端口，仅在复位上升沿将 `rf(0)` 清零；正常写入不更新 `rf(0)`。生成入口的数据位宽默认来自 `CpuConfig().xlen`；寄存器数量为独立模块参数，默认 32，没有修改共享配置或实现 RV32E/RV64 CPU。

| Chisel 端口 | Verilog 端口 | 方向 | 类型 / 位宽 | 功能 |
| --- | --- | --- | --- | --- |
| `clk` | `clk` | 输入 | `Clock` / 1 位 | 写入时钟，上升沿采样写端口。 |
| `reset` | `reset` | 输入 | `Bool` / 1 位 | 高有效同步复位，在 clk 上升沿只清零 x0，优先于正常写入。 |
| `io.WE3` | `io_WE3` | 输入 | `Bool` / 1 位 | 写使能；目的寄存器为 x0 时写入仍被抑制。 |
| `io.A1` | `io_A1` | 输入 | `UInt(A.W)`，默认 5 位 | 第一个读端口的架构寄存器编号。 |
| `io.A2` | `io_A2` | 输入 | `UInt(A.W)`，默认 5 位 | 第二个读端口的架构寄存器编号。 |
| `io.A3` | `io_A3` | 输入 | `UInt(A.W)`，默认 5 位 | 写端口的架构寄存器编号。 |
| `io.WD3` | `io_WD3` | 输入 | `UInt(W.W)` | 写入数据。 |
| `io.RD1` | `io_RD1` | 输出 | `UInt(W.W)` | `A1` 指定寄存器的当前内容；x0 完成复位后恒为 0。 |
| `io.RD2` | `io_RD2` | 输出 | `UInt(W.W)` | `A2` 指定寄存器的当前内容；x0 完成复位后恒为 0。 |

复位为同步行为，需要在 reset 为 1 时至少经过一个 clk 上升沿。完成首次复位后，x0 始终保持 0；首次复位前其值不作保证。x1–xN−1 没有初始值，复位不会清空它们，复位期间也不执行正常写入。

未来 Datapath 应连接 `rf.clk := clock`、`rf.reset := reset.asBool`，将指令 `[19:15]`、`[24:20]`、`[11:7]` 分别接到默认配置的 A1/A2/A3，`RegWrite` 接 WE3，写回结果接 WD3。

## 3. 功能与时序

| 场景 | 行为 |
| --- | --- |
| 改变 A1/A2 | 两个输出通过各自组合选择路径更新，不等待时钟。两个端口可读取同一寄存器。 |
| 时钟上升沿且 reset=1 | 仅将 x0 清零，x1–xN−1 保持；即使 WE3=1 也不执行正常写入。 |
| 时钟上升沿且 reset=0、WE3=1、A3≠0 | 将 WD3 写入指定寄存器，其他寄存器保持。 |
| reset=0 且 WE3=0 | 上升沿不修改任何寄存器，改变 A3/WD3 不影响存储值。 |
| A3=0 | 正常写入无效，即使 WE3=1 也不更新 x0；复位后的零值保持。 |
| 读地址等于写地址 | 上升沿前读出旧值；写入生效并经组合逻辑传播后读出新值。没有沿前写数据旁路。 |
| 数据或写地址在两个上升沿之间变化 | 不修改存储值；上升沿采样当时的 WE3/A3/WD3。 |

本实现使用完整的 `Reg(Vec(N, UInt(W.W)))` 保存所有寄存器，包括 x0；没有 `RegInit`，通过显式更新分支只复位第 0 项。两个读端口直接使用 `rf(io.A1)`、`rf(io.A2)`，写端口直接使用 `rf(io.A3)`，编号不作减法。

```scala
val rf = withClock(clk) { Reg(Vec(registerCount, UInt(dataWidth.W))) }
val WriteEnable = io.WE3 && (io.A3 =/= 0.U)
withClock(clk) {
  when(reset) {
    rf(0) := 0.U(dataWidth.W)
  }.elsewhen(WriteEnable) {
    rf(io.A3) := io.WD3
  }
}
io.RD1 := rf(io.A1)
io.RD2 := rf(io.A2)
```

生成的 Verilog 包含工具链提供的、受宏控制且位于非综合部分的随机初始化代码；实际硬件复位由上面的同步 reset 分支实现，随机初始化代码不表示上电值保证。仿真先执行复位初始化 x0，再通过写端口初始化需要观察的非零寄存器。

## 4. 内部信号名称

| 内部名称 | 类型 / 位宽 | 功能 |
| --- | --- | --- |
| `rf` | `Reg(Vec(N, UInt(W.W)))` | 保存全部 N 个寄存器，包括 x0；`rf(i)` 直接对应 xi。 |
| `rf_0`–`rf_(N-1)` | 生成的 Verilog 中各为 W 位 `reg` | `rf` 展开后的同编号状态寄存器，默认名称为 `rf_0`–`rf_31`。 |
| `WriteEnable` | `Wire(Bool())` | `WE3 && A3 != 0`，控制物理写入。 |

`addressWidth` 是端口 Bundle 中的 Scala 常量 A，不是硬件端口或信号。写入通过 A3 动态索引译码，仅在 reset=0、WriteEnable=1 时允许更新；x0 的正常写条件被 A3≠0 屏蔽。

生成的 FIRRTL 中保留完整 32 项 `reg rf`，并在 `when reset` 分支中执行 `rf[0] <= 0`；读端口为 `rf[io.A1]`、`rf[io.A2]`。默认 Verilog 保留了 **`rf_0` 至 `rf_31` 共 32 个状态寄存器**，每个 32 位；64 位版本同名、每个 64 位。x0 为真正的时序寄存器，而不是组合常量槽位。

原来的 `ReadIndex1`、`ReadIndex2`、`WriteIndex` 已移除，源码和生成逻辑均不计算编号减一。默认生成的读选择器包含 W 位辅助信号 `_GEN_97` 至 `_GEN_126`（RD1 路径）和 `_GEN_129` 至 `_GEN_158`（RD2 路径）。`_RAND_0` 至 `_RAND_31` 属于可选仿真随机初始化辅助变量，不是架构寄存器。自动信号名称可能随配置、编译器及综合优化变化；本轮没有使用 `dontTouch` 强制保留。

## 5. 与原书的差异及验证结果

- 原书数据位宽固定 32、寄存器数固定 32；本实现提供两个 elaboration 参数，默认值保持一致。
- 原书状态数组使用 `rf[31:1]`；按用户意见，本实现提供 `rf[0:31]` 的完整时序寄存器数组，直接使用架构编号索引。
- 原书直接在 WE3 下执行数组写入，未对 A3=0 单独加写门控；本实现显式屏蔽 x0 写入，保持架构可见行为。
- 原书没有寄存器堆复位端口；本实现按用户意见新增高有效同步 reset，仅清零 x0，其他寄存器保持，复位优先于写入。
- 数据端口具有 `io_` 前缀，时钟端口仍为 `clk`，新增复位端口为 `reset`。无读数据寄存级、无额外写旁路。

验证日期：2026-10-03。沿用 JDK 17、sbt 1.10.7、Scala 2.13.14、Chisel 3.6.1、chiseltest 0.6.2；功能仿真使用默认 Treadle 后端。

```bash
./scripts/sbt-local.sh test \
  'runMain riscvsingle.GenerateRegFile' \
  'runMain riscvsingle.GenerateRegFile 64 generated/regfile64'
```

**修改后实际结果：RegFile 的 10 项测试全部通过；全项目 5 个套件、36 项测试全部通过，原有模块及配置检查回归无失败。32/64 位 Verilog 均已重新生成。**

| 验证内容 | 实际覆盖 |
| --- | --- |
| x0 与双读端口 | 32/64 位先复位 x0，再初始化 x1–x31 并遍历全部 32×32 对读地址，不推进时钟；复位后读 x0 保持为 0，两个端口独立且可读同址。 |
| x0 写入无效 | 用 1、全一和最高位置一分别尝试写 x0，检查沿前/沿后读值及所有非零寄存器保持。 |
| 上升沿采样 | 验证写数据在沿前变化不影响旧读值，沿后取最后采样数据；覆盖 x1、x31 和同地址双读。 |
| 禁写及部分复位 | 禁写后推进多个时钟沿，寄存器保持；同时拉高 reset 和 WE3 尝试写非零寄存器，验证 x0 为 0、其余值保持且正常写入被阻止，撤销 reset 后可恢复写入。 |
| 随机顺序模型 | 32/64 位各进行 300 个随机周期，覆盖禁写、x0 写入、读写碰撞及每 29 周期一次复位，在沿前、沿后和改变读地址后对照数组模型；最后检查所有寄存器。固定种子为 `0x215F1 + dataWidth*100 + registerCount`。 |
| 参数化配置 | `(dataWidth, registerCount)=(8,16)` 和 `(1,2)` 各运行 100 个随机周期，验证较小地址宽度及最小物理存储配置。 |
| 生成接口 | 对 32×32、64×32、8×16、1×2 四种配置检查时钟、reset 和七个数据端口，验证地址/数据位宽和同步上升沿更新逻辑。 |
| 完整时序数组与直接索引 | 四种配置均检查 FIRRTL 包含完整 N 项 `reg rf`、reset 分支清零第 0 项、直接读写地址索引，且不包含地址减法或旧的 Index 信号。 |
| 非法参数 | 数据位宽 −1/0、寄存器数 −2/0/1/3/17 均在 elaboration 时被拒绝。 |

所有需要观察的非零寄存器均先通过正常写端口初始化，参考模型只在有效时钟沿更新，并体现复位优先级。测试用 `RegFileHarness` 将生产模块的 clk、reset 接到 chiseltest 时钟和复位信号。

编译中的 `ChiselStage` 弃用提示与此前一致，本轮没有进行 FPGA 实现、时序分析或完整 CPU 仿真。异步双读结构是否映射为具体器件上的存储资源，由后续综合决定。

单独生成可执行：

```bash
make generate-regfile SBT=./scripts/sbt-local.sh
make generate-regfile SBT=./scripts/sbt-local.sh WIDTH=64 REGFILE_TARGET_DIR=generated/regfile64
# 可复用的 16 个 8 位寄存器配置：
make generate-regfile SBT=./scripts/sbt-local.sh WIDTH=8 REGISTER_COUNT=16 REGFILE_TARGET_DIR=generated/regfile16
```

## 6. 核验停点

本轮到此停止，RegFile 变更保留在工作区等待核验。收到修改意见时，仅修改当前模块及配套资料并重新验证；明确核验通过并允许继续后，下一模块为 Controller。
