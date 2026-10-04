# 模块 13：SwByteMask 字节写掩码核验报告（第七章第 9 轮）

## 1. 依据与交付范围

依据本地《RISC-V System-on-Chip Design, Edition 1》§7.1.6（书中第 314–315 页）及图 7.9：DTIM 按原生字访问，子字存储通过 ByteMask 选择实际更新的字节。CVW 参考文件为 `/home/unlastingstar/cvw/src/lsu/swbytemask.sv`。

沿用访问大小生成连续掩码、按字内偏移选择字节位置的结构。本项目按第七章逐轮计划增加完整 Funct3 合法性检查与自然对齐检查；非法大小或未对齐输出零。教材图中 SH/SW/SD 的低地址位标为 x，CVW 当前公式允许移位后的部分或跨字掩码；本模块不直接沿用这些边界行为，也不提供 CVW 的 EXTEND 或 ByteMaskExtended 接口。

前置提交为第 8 轮 IEU 的 `89b1e58`。本轮新增 **SwByteMask 一个硬件模块**、生成入口、测试与独立 RTL，更新 Makefile、README 和进度报告。新模块尚未接入 LSU，不提前实施 SubwordWrite。独立 64 位模块不表示 CPU 支持 RV64I。

| 交付 | 路径 |
| --- | --- |
| 模块与端口 | [SwByteMask.scala](../../src/main/scala/riscvsingle/lsu/SwByteMask.scala) |
| 生成入口 | [GenerateSwByteMask.scala](../../src/main/scala/riscvsingle/GenerateSwByteMask.scala) |
| 测试 | [SwByteMaskSpec.scala](../../src/test/scala/riscvsingle/lsu/SwByteMaskSpec.scala) |
| 32 位 RTL | [SwByteMask.v](../../generated/swbytemask/SwByteMask.v) |
| 64 位 RTL | [SwByteMask.v](../../generated/swbytemask64/SwByteMask.v) |

## 2. 参数与全部端口

类 `riscvsingle.lsu.SwByteMask` 继承 `RawModule`，端口 Bundle 为 `SwByteMaskIO`。构造参数 `dataWidth: Int = 32` 仅允许 32 或 64，其他值在 elaboration 时抛出 `IllegalArgumentException`。令 `W=dataWidth`、`B=W/8`、`K=log2Ceil(B)`。

模块只有三个组合端口，没有时钟、复位、使能、寄存器、存储器或子模块。生成入口的默认宽度取 `CpuConfig().xlen=32`；其他共享配置字段不参与本模块。

| Chisel 端口 | 实际 Verilog 端口 | 方向 / 类型 | 位宽（32 / 64 配置） | 用途 |
| --- | --- | --- | --- | --- |
| `io.Funct3` | `io_Funct3` | 输入 / UInt | 3 / 3 | 完整存储功能字段，不仅译码低两位。 |
| `io.ByteOffset` | `io_ByteOffset` | 输入 / UInt | K：2 / 3 | 原生字内字节偏移，范围 0–3 / 0–7。 |
| `io.ByteMask` | `io_ByteMask` | 输出 / UInt | B：4 / 8 | 每位使能一个字节，bit 0 对应最低字节地址。 |

输入变化经组合传播更新输出，不等待时钟沿。模块不接收完整地址，也不解释高地址映射、访存使能或 reset。后续 LSU 从地址低 K 位提供偏移，并结合有效存储请求使用掩码；单独输出非零掩码不代表发生写入。X/Z 四态传播不作为接口保证。

## 3. 功能规则与边界

| Funct3 | 操作 / AccessBytes | 32 位合法偏移 → 掩码 | 64 位合法偏移 → 掩码 |
| --- | --- | --- | --- |
| 000 | SB / 1 | 0/1/2/3 → 1/2/4/8 | 0–7 → 01/02/04/08/10/20/40/80 |
| 001 | SH / 2 | 0/2 → 3/C | 0/2/4/6 → 03/0C/30/C0 |
| 010 | SW / 4 | 0 → F | 0/4 → 0F/F0 |
| 011 | SD / 8 | 非法，输出零 | 0 → FF |
| 100–111 | 非法 / 0 | 所有偏移输出零 | 所有偏移输出零 |

表中偏移使用十进制，掩码使用十六进制。任意合法大小的其他偏移均未自然对齐，输出零；包括跨原生字边界的 SH/SW/SD，不产生截断后的部分写掩码。完整 Funct3 的高位为 1 时均非法，不能把加载用的 LBU/LHU/LWU 编码解释为存储大小。

组合规则：译码完整 Funct3 得到 AccessBytes，选择低 AccessBytes 位为 1 的 BaseMask；当 AccessBytes 非零且 `ByteOffset & (AccessBytes - 1)` 为零时，将 BaseMask 左移 ByteOffset 并取低 B 位，否则输出零。支持的大小均为二次幂且不超过原生字，因此自然对齐也保证不会跨字。

这只是确定的掩码行为，不生成未对齐异常或陷阱；整机未对齐读零／不写的约定仍需后续 LSU 实施。

## 4. 内部信号与实际 RTL 名称

| Chisel 名称 | 类型 / 位宽 | 定义 | 本次实际 RTL |
| --- | --- | --- | --- |
| `AccessBytes` | Wire / UInt(4.W) | 1/2/4，64 位另含 8；非法为 0。 | 同名 4 位 wire。 |
| `BaseMask` | Wire / UInt(B.W) | 1/3/F，64 位另含 FF；非法为 0。 | 同名 4/8 位 wire。 |
| `Aligned` | Wire / Bool | 检查自然对齐。 | 同名 1 位 wire。 |
| `ByteMask` | Wire / UInt(B.W) | 合法且对齐时取移位结果低 B 位，否则零。 | 别名折叠到 `io_ByteMask` 的连续赋值，无独立同名 wire。 |

`storeSizes` 与 `byteCount` 是 elaboration 时的 Scala 常量，不是运行时信号。RTL 的 `_Aligned_T_1` 为 4 位大小减一，`_GEN_0` 将偏移零扩展到 4 位；`_ByteMask_T_2` 为移位后的 7/15 位中间结果，输出显式取 `[3:0]` / `[7:0]`。这些自动名称只记录本次生成结果，不作为稳定接口；不使用 dontTouch 强制保留别名。

每份独立 RTL 只有一个 `SwByteMask` 模块定义、三个端口、组合 wire 和连续赋值，无时序块、寄存器或子模块连接。

## 5. 测试先行与验证证据

验证日期：2026-10-04（Asia/Shanghai）。工具链沿用 JDK 17、sbt 1.10.7、Scala 2.13.14、Chisel 3.6.1、chiseltest 0.6.2（Treadle）与 Verilator 5.036。

先新增测试并运行模块命令：首次因缺少 `SwByteMask` / `SwByteMaskIO` 出现四个编译错误，没有执行功能测试。随后只补接口和参数校验，将输出固定为零，再运行同一命令：**8 项中 6 项功能测试失败、2 项接口与参数测试通过，sbt 退出码 1**。失败明确为期望 1/F 而实际为零。实现掩码与对齐门控后，同一命令 **8/8 通过，退出码 0**。

| 验证 | 实际覆盖 |
| --- | --- |
| 手工定向 | 32 位 7 组、64 位 15 组，检查全部合法大小与偏移，预期值为独立常量。 |
| 穷举参考模型 | 32 位 8×4、64 位 8×8，共 96 种输入；正序和逆序各一遍，共 192 次断言。软件模型按字节是否处于访问区间逐位设置结果，不复用硬件移位掩码。 |
| 组合切换 | 每种宽度 6 组，覆盖合法→非法→合法及对齐→未对齐；所有检查均不推进时钟，确认不会保持旧掩码。 |
| 功能断言总量 | 定向 22 + 穷举 192 + 切换 12 = **226 次**。 |
| 端口与状态 | 两种宽度各检查恰好三个端口、全部方向/位宽以及无时序块或寄存器。 |
| 参数校验 | −1、0、1、8、16、31、33、63、65、128 均在 elaboration 时被拒绝。 |
| 全工程 | **14 个套件、121 项测试全部通过**，既有原书程序、容量配置、复位与执行验证保持通过。 |
| 现有整机 RTL | 默认与扩容 CPU 各使用种子 1、17、2026，**六次全部 PASS**，退出码 0。 |

实际命令（在项目根目录运行）：

```bash
./scripts/sbt-local.sh 'testOnly riscvsingle.lsu.SwByteMaskSpec'
./scripts/sbt-local.sh test \
  'runMain riscvsingle.GenerateSwByteMask' \
  'runMain riscvsingle.GenerateSwByteMask 64 generated/swbytemask64'
make test-rtl
```

默认及 64 位生成入口均成功，实际端口与内部信号已对照本节和第 4 节核查。Makefile 入口为：

```bash
make generate-swbytemask SBT=./scripts/sbt-local.sh
make generate-swbytemask SBT=./scripts/sbt-local.sh \
  WIDTH=64 SWBYTEMASK_TARGET_DIR=generated/swbytemask64
```

日志：`target/swbytemask-round9-red-interface.log`、`target/swbytemask-round9-red.log`、`target/swbytemask-round9-focused.log`、`target/swbytemask-round9-regression-generate.log`、`target/swbytemask-round9-rtl.log`。这些本地测试产物及生成的 .fir/.anno.json 已被忽略；交付保留源码、测试、Markdown 和两份 .v。沿用项目现有 ChiselStage/FIRRTL 流程，其弃用提示与此前一致。

## 6. 接通边界与核验停点

本轮 SwByteMask 尚未接入旧 LSU 或 RiscvSingle。原生产硬件和两份 CPU RTL 没有变化，因此整机 RTL 回归验证已有程序兼容性；新掩码的行为由独立模块测试验证，不能据此宣称整机已完成 SB/SH 或未对齐禁止写入。未进行 FPGA/ASIC 实现或时序分析。

**本轮停止，等待用户核验。** 改动保留在当前工作区，未自动提交或合并；核验通过并明确允许继续后，进入第 10 轮 SubwordWrite。

### 后续状态（2026-10-04）

第 10 轮 SubwordWrite 已完成；用户现已授权将第 9/10 轮一起提交并继续 SubwordRead。SwByteMask 负责生成后续 LSU/DTIM 使用的字节写使能，是图 7.9 所需模块，保留本轮全部交付。首轮测试结果与接通边界仍如上所述。
