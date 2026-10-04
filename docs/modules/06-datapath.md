# 模块 06：Datapath 数据通路核验报告（第七章第 7 轮）

## 1. 依据与本轮范围

依据本地《RISC-V System-on-Chip Design, Edition 1》第七章 §7.1、图 7.2 和表 7.1 的 RV32 列。操作数、结果和地址的关系对应 §7.1.3 与表 7.3（第 304–305 页）；立即数扩展对应 §7.1.2；寄存器与比较器行为分别对应 §7.1.1、§7.1.3.6。CVW 参考文件为 `/home/unlastingstar/cvw/src/ieu/datapath.sv`，其中 `altresultmux` 与 `ieuresultmux` 对应本轮替代结果和执行结果选择；JALR 目标清位按当前逐轮计划放在 Datapath，IFU 后续不重复清位。

上一轮 Controller 已按用户指示提交为 `ffbb866`，提交前 13 个套件、102 项回归全部通过。本轮只完善 **Datapath**；IEU 接入新 Jump 输入、测试包装和生成 RTL 适配归入当前轮。IEU 的 MemRW/Funct3 外部交付和完整执行序列验证留待第 8 轮，子字访存与顶层完善仍按后续模块顺序进行。

| 交付内容 | 路径 |
| --- | --- |
| 模块及端口 | [Datapath.scala](../../src/main/scala/riscvsingle/ieu/Datapath.scala) |
| 生成入口 | [GenerateDatapath.scala](../../src/main/scala/riscvsingle/GenerateDatapath.scala) |
| 模块测试 | [DatapathSpec.scala](../../src/test/scala/riscvsingle/ieu/DatapathSpec.scala) |
| IEU 连接适配 | [IEU.scala](../../src/main/scala/riscvsingle/ieu/IEU.scala) |
| 联调测试 | [IEUSpec.scala](../../src/test/scala/riscvsingle/ieu/IEUSpec.scala) |
| 模块 RTL | [Datapath.v](../../generated/datapath/Datapath.v) |

## 2. 参数与全部端口

类 `riscvsingle.ieu.Datapath` 继承 `RawModule`，构造参数为 `config: CpuConfig = CpuConfig()`。固定 32 位指令、32 项整数寄存器、直接使用指令中的 5 位寄存器编号；本轮不改变共享配置约束。时钟和高有效同步复位显式提供，数据通路自身没有新增状态，状态仍在 RegFile。

| CpuConfig 字段 | 默认值 | 合法范围 | 在 Datapath 中的用途 |
| --- | --- | --- | --- |
| `xlen` | 32 | 当前仅允许 32 | 数据端口与 RegFile/Extend/Cmp/ALU 位宽。独立 64 位辅助模块不代表本模块支持 RV64。 |
| `imemDepth` | 64 | 不小于 2 的二次幂，32 位字容量对应的字节数不超过 2^32 | 不生成取指存储，随 config 保留。 |
| `dmemDepth` | 64 | 同上 | 不生成数据存储，随 config 保留。 |
| `resetVector` | 0 | 0 至 2^32−1，4 字节对齐 | 不生成 PC 状态，本模块使用外部 PC/PCPlus4。 |
| `instructionInitFile` | None | None 或非空白字符串 | 不初始化寄存器或存储。 |

生成入口 `GenerateDatapath` 默认构造 `CpuConfig()`，仅接受可选输出目录 `targetDir`，默认 `generated/datapath`。不同容量及复位地址由 Scala 配置对象传入，接口数据宽度仍为 32。

| Chisel 名称 | 实际 Verilog 端口 | 方向 | 类型 / 位宽 | 用途 |
| --- | --- | --- | --- | --- |
| `clk` | `clk` | 输入 | Clock / 1 | RegFile 写入及同步复位时钟，上升沿生效。 |
| `reset` | `reset` | 输入 | Bool / 1 | RegFile 高有效同步复位，只清零 x0，阻止普通写入。 |
| `io.Funct3` | `io_Funct3` | 输入 | UInt / 3 | 独立提供 ALU 功能码，通常接 Instr[14:12]。 |
| `io.ALUResultSrc` | `io_ALUResultSrc` | 输入 | Bool / 1 | 0 选择 ALUResult，1 选择 AltResult。 |
| `io.ResultSrc` | `io_ResultSrc` | 输入 | Bool / 1 | 0 选择 IEUResult，1 选择 ReadData。 |
| `io.ALUSrc` | `io_ALUSrc` | 输入 | UInt / 2 | {SrcA 选择 PC, SrcB 选择 ImmExt}。 |
| `io.RegWrite` | `io_RegWrite` | 输入 | Bool / 1 | 请求将 Result 写入 Instr[11:7] 指定寄存器。 |
| `io.ImmSrc` | `io_ImmSrc` | 输入 | UInt / 3 | 已在上一轮扩为 I/S/B/J/U 选择 000/001/010/011/100；其他值输出零立即数。 |
| `io.ALUControl` | `io_ALUControl` | 输入 | UInt / 2 | 保留 {SubArith, ALUOp}。 |
| `io.Jump` | `io_Jump` | 输入 | Bool / 1 | 新增输入，Jump 为 1 时 AltResult 取 PCPlus4，为 0 时取 ImmExt。 |
| `io.Eq` | `io_Eq` | 输出 | Bool / 1 | 原始 R1=R2。 |
| `io.LT` | `io_LT` | 输出 | Bool / 1 | 原始 R1<R2 的有符号比较。 |
| `io.LTU` | `io_LTU` | 输出 | Bool / 1 | 原始 R1<R2 的无符号比较。 |
| `io.PC` | `io_PC` | 输入 | UInt / 32 | 当前指令的字节地址，可选为 SrcA。 |
| `io.PCPlus4` | `io_PCPlus4` | 输入 | UInt / 32 | 外部提供的链接值；本模块不重新计算 PC+4。 |
| `io.Instr` | `io_Instr` | 输入 | UInt / 32 | 指令字段：rs1、rs2、rd、立即数及 JALR opcode 识别。 |
| `io.IEUAdr` | `io_IEUAdr` | 输出 | UInt / 32 | 原始加减地址；当 opcode=67 时仅清除位 0。 |
| `io.WriteData` | `io_WriteData` | 输出 | UInt / 32 | 恒为 R2，不受 ALUSrc/写回选择影响。 |
| `io.ReadData` | `io_ReadData` | 输入 | UInt / 32 | 外部 LSU/测试环境提供的加载数据。 |

共 **19 个端口：14 个输入、5 个输出**。Funct3 和 PCPlus4 仍为独立输入，调用者负责和指令、PC 保持一致；接口测试也刻意提供不同值确认它们生效。数据端口采用 UInt 位模式，有符号运算由子模块解释为 32 位补码。X/Z 四态传播不作为保证。

## 3. 功能、控制与边界

RegFile 两个读地址为 Instr[19:15]、Instr[24:20]，写地址为 Instr[11:7]。Extend 使用 Instr[31:7] 与 3 位 ImmSrc，五种格式包括 U 型低 12 位补零。Cmp 恒比较 R1/R2，不用被选择的 PC/ImmExt。

| ALUSrc | SrcA | SrcB | 常见用途 |
| --- | --- | --- | --- |
| 00 | R1 | R2 | 寄存器 ALU 运算。 |
| 01 | R1 | ImmExt | 立即数 ALU、加载/存储、JALR。 |
| 10 | PC | R2 | 接口允许的组合，当前 Controller 不输出。 |
| 11 | PC | ImmExt | 条件分支、JAL、AUIPC。 |

ALU 提供完整整数结果 ALUResult 与独立加减结果 RawIEUAdr。替代结果和写回使用教材图 7.2 的三级选择：

```text
AltResult = Jump         ? PCPlus4  : ImmExt
IEUResult = ALUResultSrc ? AltResult : ALUResult
Result    = ResultSrc    ? ReadData  : IEUResult
```

| ResultSrc | ALUResultSrc | Jump | 最终写回 Result |
| --- | --- | --- | --- |
| 1 | 任意 | 任意 | ReadData，优先于 ALU 与两个替代结果。 |
| 0 | 0 | 任意 | ALUResult。 |
| 0 | 1 | 0 | ImmExt，用于 LUI。 |
| 0 | 1 | 1 | 外部 PCPlus4，用于 JAL/JALR。 |

LUI 使用 Controller 的 ImmSrc=100、ALUResultSrc=1、Jump=0，不受 SrcA 中可能读取的寄存器内容影响。AUIPC 使用 ALUSrc=11、ImmSrc=100、ALUOp=0，ALUResultSrc=0，计算 **当前 PC + U 型立即数**，按 2^32 回绕，不采用 PCPlus4。JAL 的目标由 PC+J 型立即数计算，JALR 的原始目标由 R1+I 型立即数计算；两者链接写回均为外部 PCPlus4。

JALR 的目标规则按本阶段接口约定直接识别 opcode，而不依赖 Jump 或指令合法性译码：

```text
IsJalr    = (Instr[6:0] == 0x67)
IEUAdr    = IsJalr ? (RawIEUAdr & 0xFFFFFFFE) : RawIEUAdr
```

仅清除位 0，位 1 保留；本模块不实现指令地址未对齐异常。非 JALR 的奇数加减地址保留，存储地址、分支/JAL 地址不通过该掩码。ALUResult 和写回结果不受目标地址掩码影响。非法 JALR 功能码是否执行由 Controller 关闭 RegWrite/PCSrc；Datapath 仍按 opcode 提供组合地址。

当 JALR 的 rs1=rd 时，目标在上升沿前使用旧 R1，时钟沿将链接值写入 rd；沿后的组合输出会根据新 R1 更新。这符合当前两读一写异步读取、同步写入的寄存器堆，未新增旁路。rd=x0 时链接写入被 RegFile 禁止。

时序保持原约定：reset=1 的上升沿仅清零 rf(0)，x1–x31 保持，普通写入被屏蔽；reset=0 且 RegWrite=1 且 rd≠0 才写入 Result。无上升沿的复位电平脉冲不改变状态。x1–x31 的首次写入前内容未指定，测试先建立状态再观察。

## 4. 子模块连接与当前整机范围

| 子模块实例 | 类型与参数 | 连接规则 |
| --- | --- | --- |
| `rf` | RegFile(xlen=32, registerCount=32) | clk/reset 接显式端口；WE3 接 RegWrite；WD3 接 Result；A1/A2/A3 直接取 rs1/rs2/rd，不减一。 |
| `ext` | Extend(outputWidth=32) | 输入 Instr[31:7] 和 3 位 ImmSrc，输出 ImmExt。 |
| `cmp` | Cmp(dataWidth=32) | 输入原始 R1/R2；Eq/LT/LTU 接三个比较输出。 |
| `alu` | ALU(dataWidth=32) | SrcA/SrcB 来自操作数选择；独立 Funct3 与 ALUControl 传入；ALUResult 用于结果选择，IEUAdr 提供 RawIEUAdr。内部 Shifter 沿用第 5 轮接入。 |

Datapath 不增加 PC、存储器、译码器或流水寄存器。LUI、AUIPC、JAL、JALR 的控制依赖已经完善的 Controller；独立模块测试也直接驱动所有输入组合以验证接口语义。

IEU 本轮只增加 Jump 内部连接：Controller.Jump 送往 Datapath.Jump，保持原生产外部端口。完整 MemRW/Funct3 交付仍留待下一轮 IEU。因为 Jump 现在被消费，IEU 与整机导出中的 Controller.io_Jump 不再是上一轮未连接的输出，需重新生成这些依赖 RTL。

当前整机已接通新写回与跳转地址数据通路，但旧 LSU 仍为字访存，不具备子字选择、字节写掩码或未对齐禁止访问；此轮不宣称整机已经支持表 7.1 的全部访存指令。复位保持现有行为：PC 与 x0 同步复位，数据 RAM 不清空；顶层在复位期间屏蔽存储写入的计划项仍留待对应整机轮次。

## 5. 内部信号与实际 RTL 名称

十个数据功能信号显式定义为 `Wire(UInt(config.xlen.W))`，本阶段均为 32 位；JALR 识别为一个 Bool。它们不引入新状态。

| Chisel 名称 | 位宽 | 定义 | 实际生成 RTL |
| --- | --- | --- | --- |
| `ImmExt` | 32 | 按 ImmSrc 从 Instr 扩展的立即数 | `ImmExt=ext_io_ImmExt` |
| `R1` | 32 | rs1 读数 | `R1=rf_io_RD1` |
| `R2` | 32 | rs2 读数 | `R2=rf_io_RD2` |
| `SrcA` | 32 | ALUSrc[1] 选择 PC/R1 | 别名折叠为 `alu_io_SrcA` 的选择赋值 |
| `SrcB` | 32 | ALUSrc[0] 选择 ImmExt/R2 | 别名折叠为 `alu_io_SrcB` 的选择赋值 |
| `ALUResult` | 32 | ALU 功能结果，不进行目标清位 | `ALUResult=alu_io_ALUResult` |
| `RawIEUAdr` | 32 | ALU 独立加减器结果 | `RawIEUAdr=alu_io_IEUAdr` |
| `IsJalr` | 1 | Instr[6:0]=67 | `IsJalr` |
| `AltResult` | 32 | Jump 选择 PCPlus4/ImmExt | `AltResult` |
| `IEUResult` | 32 | ALUResultSrc 选择 AltResult/ALUResult | `IEUResult` |
| `Result` | 32 | ResultSrc 选择 ReadData/IEUResult | 别名折叠为 `rf_io_WD3` 的选择赋值 |

目标掩码在当前生成 RTL 中表现为 32 位辅助线网 `_io_IEUAdr_T=RawIEUAdr & 32'hFFFFFFFE`，io_IEUAdr 再按 IsJalr 选择该值或 RawIEUAdr。该临时名字仅描述本次编译输出，不是稳定观察接口。

子模块连接线采用 `rf_*、ext_*、cmp_*、alu_*`，位宽与对应端口一致；没有使用 dontTouch 强制保留别名。IEU 的 Chisel Jump Wire 在实际 RTL 中折叠为 `dp_io_Jump=c_io_Jump`；Controller.io_Jump 在依赖 RTL 中保留。本轮完整 Datapath 的 19 个生产端口保持，IEU/CPU 外部端口不变。

## 6. 测试先行与模块验证

先在已有公共接口的 ControlledDatapathHarness 上增加 LUI/JALR 行为测试，实际运行：

```bash
./scripts/sbt-local.sh 'testOnly riscvsingle.ieu.DatapathSpec'
```

红阶段 **9 项中 7 通过、2 失败，退出码 1**；LUI 错误写回 PCPlus4，奇数 JALR 目标未清位，两个失败均为预期功能缺失。日志为 `target/datapath-round7-red.log`。随后新增 Jump 接口和相应测试，完善写回选择与目标处理。

绿色验证命令：

```bash
./scripts/sbt-local.sh 'testOnly riscvsingle.ieu.DatapathSpec riscvsingle.ieu.IEUSpec'
```

**两个套件、20 项测试全部通过，退出码 0**：DatapathSpec 13 项、IEUSpec 7 项；日志 `target/datapath-round7-focused.log`。

| 验证内容 | 实际覆盖 |
| --- | --- |
| 操作数与比较 | 全部四种 ALUSrc；原始寄存器相等/不等、−1 与 1 的正负顺序互换，确认 Eq/LT/LTU 与 WriteData 独立于所选 PC/ImmExt。 |
| 写回组合 | ALUResultSrc、Jump、ResultSrc 的全部八种组合，结果来自 ALU、立即数、外部链接值或加载数据；加载最终优先。独立 Funct3 指定 OR=15，地址加法=19。 |
| LUI/AUIPC | U 型立即数 0/1/7FFFF/80000/FFFFF，十六进制高 20 位；LUI 含 rd=x0 与非零寄存器，共 10 组；AUIPC 配合四种当前 PC 共 20 组，包含高位、奇数 PC 输入及回绕，PCPlus4 故意取不同值。 |
| JAL | 正负偏移各一种、三个 PC、rd=x0/非零，共 12 组；目标与外部链接值独立，包含地址回绕。 |
| JALR 与别名 | 八种 base/立即数组合 × 两种 Jump × 两种 ALUResultSrc × rd=x0/rs1，共 64 组；检查沿前旧源目标、沿后更新目标、最终寄存器值，覆盖奇偶、负偏移、回绕、位 1 保持及 ALU 写回不被清位。 |
| opcode 掩码边界 | 十种 opcode × 两种 Jump 共 20 组，JALR 即使提供非法 Funct3 仍按 opcode 清位；其余 opcode 保留奇数地址，并确认算术结果原值写回。非法指令是否执行仍归 Controller。 |
| 原立即数与复位 | 既有 I/S/B/J 负立即数、两种 PC 的地址测试继续通过；验证写入边沿、RegWrite=0、同步复位优先且非零寄存器保持、x0 禁止写入。 |
| 随机模型 | 固定种子 0x215，先初始化 x1–x31，再运行 **640 周期**。ImmSrc=0–7 各 80 次，包括三种零立即数选择；214 次 JALR opcode，18 次高有效同步复位周期。随机操作数选择、八路 ALU 功能、Jump、替代/加载写回和使能。 |
| 联调与接口 | 原书 Code Example 2.16 的 19 条执行指令、地址 96 写 7 与地址 100 写 25 保持；新增 IEU 单指令检查 LUI/AUIPC/JALR、rs1=rd 与 rd=x0，确认生产 Jump 接线。19 端口的方向/位宽、四个子模块实例与显式复位连接均核对。 |

随机模型使用 BigInt 直接运算和比较，单独计算原始加减地址与目标；JALR 参考清位用整数除二再乘二，不复用硬件掩码表达式。每个周期在沿前和沿后各检查 Eq、LT、LTU、WriteData、IEUAdr 五个输出，共 **6,400 次输出断言**；最终检查全部 32 项寄存器的两个观察路径，共 64 次，总计 **6,464 次随机模型输出断言**。写回模型区分 AltResult 和 ALUResult，并在同步复位周期阻止非零寄存器更新。

随机测试直接驱动数据通路控制，因此指令位模式不必对应合法的 Controller 译码；它验证独立接口与选择规则，不替代指令合法性测试。实例使用 `CpuConfig(imemDepth=128,dmemDepth=256,resetVector=0x100)` 验证配置透传，本模块仍不生成 PC 或 RAM。测试的参考计算与结果观察只依赖已写入的非零寄存器；原程序的 Scala 内存模型先建立存储状态，不依赖未初始化内容。

全工程最终回归命令：

```bash
./scripts/sbt-local.sh test
```

**13 个套件、109 项测试全部通过，退出码 0**，无失败或中止；既有模块、配置、原书程序及扩容程序回归均通过。日志 `target/datapath-round7-regression.log`。

## 7. RTL 生成与整机验证

实际生成命令：

```bash
./scripts/sbt-local.sh \
  'runMain riscvsingle.GenerateDatapath generated/datapath' \
  'runMain riscvsingle.GenerateIEU generated/ieu' \
  'runMain riscvsingle.GenerateRiscvSingle 64 64 generated/riscv-single programs/riscvtest.memfile 0' \
  'runMain riscvsingle.GenerateRiscvSingle 128 128 generated/riscv-single128 programs/rv32-configtest.memfile 0x100'
make test-rtl
```

四个生成入口全部成功，退出码 0，日志 `target/datapath-round7-generate.log`；产物如下：

| 配置 | RTL 路径 |
| --- | --- |
| Datapath | [generated/datapath/Datapath.v](../../generated/datapath/Datapath.v) |
| IEU | [generated/ieu/IEU.v](../../generated/ieu/IEU.v) |
| 默认 64 项 CPU、复位地址 0 | [generated/riscv-single/RiscvSingle.v](../../generated/riscv-single/RiscvSingle.v) |
| 128 项 CPU、复位地址 0x100 | [generated/riscv-single128/RiscvSingle.v](../../generated/riscv-single128/RiscvSingle.v) |

核对实际 RTL 的 19 个 Datapath 端口、Jump 接线、三级写回选择与 JALR 地址掩码；RegFile 之外没有新增状态，其他模块算法保持原实现。IEU/CPU 生产外部端口不变，Controller.Jump 在依赖 RTL 中保留并被消费。

`make test-rtl` 直接编译重新生成的两份完整 CPU，使用 Verilator 5.036，对 book/expanded 配置各运行随机初值种子 1、17、2026，**六次全部 PASS，退出码 0**。book 每次执行 19 个周期、2 次存储；expanded 每次执行 13 个周期、5 次存储，均检查结束循环与同步复位。日志 `target/datapath-round7-rtl.log`。

本轮 RTL 整机回归验证既有合法程序；LUI/AUIPC/JALR 的新增路径由 Datapath 定向/随机模型及 IEU 接线测试验证。完整表 7.1 的整机签名程序、子字存储检查和最终容量验收留待计划对应轮次，不把本轮结果解释为全部 RV32 整机能力已经完成。

## 8. 核验停点

**本轮停止，等待用户核验。** Datapath 源码、Jump 连接适配、测试、四份生成 RTL 和报告已交付。本轮改动保留在工作树；明确核验通过并允许继续后，提交本轮并进入第 8 轮 IEU，不自动连续实施其他模块。
