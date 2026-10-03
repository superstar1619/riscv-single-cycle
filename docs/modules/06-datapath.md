# 模块 06：Datapath 数据通路核验报告

## 1. 依据与交付状态

对应《RISC-V System-on-Chip Design, Edition 1》Code Example 2.15 的 `datapath`，书中第 63 页。本模块将已核验的 RegFile、Extend、Cmp、ALU 连接起来，完成寄存器读取、立即数扩展、操作数选择、运算、地址输出及寄存器写回。

上一模块 Controller 已按用户指示提交，提交号为 `668d141`。本轮交付 **Datapath 一个硬件模块**及其生成入口、测试和报告。Controller 与 Datapath 的连接仅出现在测试辅助顶层；生产 IEU 留待下一轮。

状态：**Datapath 测试通过，尚未提交，等待用户核验。**

| 交付内容 | 路径 |
| --- | --- |
| 模块及端口定义 | [Datapath.scala](../../src/main/scala/riscvsingle/ieu/Datapath.scala) |
| Verilog 生成入口 | [GenerateDatapath.scala](../../src/main/scala/riscvsingle/GenerateDatapath.scala) |
| 模块测试 | [DatapathSpec.scala](../../src/test/scala/riscvsingle/ieu/DatapathSpec.scala) |
| 生成的硬件，含四个子模块 | [Datapath.v](../../generated/datapath/Datapath.v) |

## 2. 接口与参数配置

模块类为 `riscvsingle.ieu.Datapath`，继承 `RawModule`，端口 Bundle 为 `DatapathIO`。时钟和复位显式声明为 `clk`、`reset`；组合输入输出具有 `io_` 前缀。共有 13 个输入和 3 个输出。

构造参数为 `config: CpuConfig = CpuConfig()`。数据宽度由 `config.xlen` 提供，并传给 RegFile、Extend、Cmp、ALU。现有配置检查要求 `xlen=32`，因此本数据通路支持 RV32；指令宽度固定为 32 位，寄存器数量固定为 32，寄存器地址直接采用 5 位指令字段。

`imemDepth`、`dmemDepth`、`resetVector`、`instructionInitFile` 在本模块中不参与硬件生成。PC、PCPlus4 和 ReadData 均由外部提供；后续 IFU、IROM、LSU 将使用对应配置。本轮未修改配置类。

| Chisel 端口 | Verilog 端口 | 方向 | 类型 / 位宽 | 功能 |
| --- | --- | --- | --- | --- |
| `clk` | `clk` | 输入 | `Clock` / 1 | RegFile 的写入和同步复位时钟，上升沿触发。 |
| `reset` | `reset` | 输入 | `Bool` / 1 | 高有效同步复位，直接接入 RegFile，只清零 x0，并阻止该周期的普通写入。 |
| `io.Funct3` | `io_Funct3` | 输入 | `UInt(3.W)` | 传给 ALU 的功能码，通常由外部提供 Instr[14:12]。 |
| `io.ALUResultSrc` | `io_ALUResultSrc` | 输入 | `Bool` / 1 | 0 选 ALUResult，1 选 PCPlus4，产生 IEUResult。 |
| `io.ResultSrc` | `io_ResultSrc` | 输入 | `Bool` / 1 | 0 选 IEUResult，1 选 ReadData，产生最终写回 Result。 |
| `io.ALUSrc` | `io_ALUSrc` | 输入 | `UInt(2.W)` | 位 1 选择 SrcA 的 R1/PC，位 0 选择 SrcB 的 R2/ImmExt。 |
| `io.RegWrite` | `io_RegWrite` | 输入 | `Bool` / 1 | 请求在时钟上升沿向 Instr[11:7] 指定寄存器写入 Result。 |
| `io.ImmSrc` | `io_ImmSrc` | 输入 | `UInt(2.W)` | Extend 的 I/S/B/J 格式选择，依次为 00/01/10/11。 |
| `io.ALUControl` | `io_ALUControl` | 输入 | `UInt(2.W)` | ALU 的 `{Sub, ALUOp}` 控制。 |
| `io.Eq` | `io_Eq` | 输出 | `Bool` / 1 | 原始寄存器读数 R1 与 R2 是否相等，供 Controller 判断 beq。 |
| `io.PC` | `io_PC` | 输入 | `UInt(32.W)` | 当前指令字节地址，可选作 ALU 的 A 操作数。 |
| `io.PCPlus4` | `io_PCPlus4` | 输入 | `UInt(32.W)` | 外部计算的顺序地址，可选作 jal 的链接写回值。 |
| `io.Instr` | `io_Instr` | 输入 | `UInt(32.W)` | 当前指令，用于提取 rs1、rs2、rd 和立即数。 |
| `io.IEUAdr` | `io_IEUAdr` | 输出 | `UInt(32.W)` | ALU 的加减法结果，用于访问地址、分支或跳转目标。 |
| `io.WriteData` | `io_WriteData` | 输出 | `UInt(32.W)` | 始终输出 R2，作为存储器写数据。 |
| `io.ReadData` | `io_ReadData` | 输入 | `UInt(32.W)` | 外部内存读数据，可选为寄存器写回值。 |

`Funct3` 保留书中的独立输入接口，本模块不会自行改用 Instr[14:12]；调用者负责连接一致的指令字段。PCPlus4 同样直接使用输入值，本模块不会重新计算 PC+4。

## 3. 功能与时序

RegFile 的两个异步读地址分别为 Instr[19:15]、Instr[24:20]，写地址为 Instr[11:7]，写数据为 Result。Extend 输入为 Instr[31:7]，其输出是符号扩展后的 ImmExt。

| ALUSrc | SrcA | SrcB | 常见用途 |
| --- | --- | --- | --- |
| `00` | R1 | R2 | R 型寄存器运算。 |
| `01` | R1 | ImmExt | I 型运算及 lw/sw 地址。 |
| `10` | PC | R2 | 接口允许该组合，现有 Controller 不输出此值。 |
| `11` | PC | ImmExt | beq/jal 目标地址。 |

Cmp 始终比较 R1、R2，与 ALUSrc 无关。WriteData 始终取 R2，即使 ALU 的 B 输入选择立即数也如此。ALUResult 按既有 ALU 规则选择加减、有符号小于、或、与及默认零；IEUAdr 独立输出加减法结果。

写回链路由两级二选一组成：

```text
IEUResult = ALUResultSrc ? PCPlus4 : ALUResult
Result    = ResultSrc    ? ReadData : IEUResult
```

| ALUResultSrc | ResultSrc | 写回 Result |
| --- | --- | --- |
| 0 | 0 | ALUResult。 |
| 1 | 0 | PCPlus4。 |
| 0 | 1 | ReadData。 |
| 1 | 1 | ReadData，最终选择器优先选择内存。 |

所有选通、运算、比较与读端口均为组合逻辑。时钟上升沿时，RegFile 按以下优先级更新：

1. reset=1：仅将 rf(0) 清零；其他寄存器保持，普通写入被抑制。
2. reset=0 且 RegWrite=1 且 rd≠0：将 Result 写入 rf(rd)。
3. 其他情况：全部寄存器保持。

沿用用户已核验的完整 32 项寄存器及直接编号索引。x0 在首次 reset=1 的 clk 上升沿后保持零；x1–x31 没有初始值或复位值保证。读输出在写入上升沿后反映更新值，未添加写入旁路。

## 4. 内部信号与子模块名称

以下八个信号在 Chisel 源码中均显式定义为 `Wire(UInt(config.xlen.W))`，当前宽度为 32 位；Datapath 自身未新增状态寄存器。

| 内部信号 | 功能 | 当前生成 Verilog 中的名称 / 对应位置 |
| --- | --- | --- |
| `ImmExt` | Extend 的符号扩展立即数。 | `ImmExt`，来自 `ext_io_ImmExt`。 |
| `R1` | RegFile 第一个读端口值。 | `R1`，来自 `rf_io_RD1`。 |
| `R2` | RegFile 第二个读端口值。 | `R2`，来自 `rf_io_RD2`。 |
| `SrcA` | ALU A 操作数，选择 R1 或 PC。 | 别名经优化合并为 `alu_io_SrcA` 的赋值。 |
| `SrcB` | ALU B 操作数，选择 R2 或 ImmExt。 | 别名经优化合并为 `alu_io_SrcB` 的赋值。 |
| `ALUResult` | ALU 的功能结果。 | `ALUResult`，来自 `alu_io_ALUResult`。 |
| `IEUResult` | ALUResult 与 PCPlus4 的选择结果。 | `IEUResult`。 |
| `Result` | 最终寄存器写回数据。 | 别名经优化合并为 `rf_io_WD3` 的赋值。 |

| 子模块实例名 | 类型 | 功能及连接 |
| --- | --- | --- |
| `rf` | `RegFile(config.xlen, 32)` | 32 项寄存器；clk/reset 接外部，WE3 接 RegWrite，WD3 接 Result。 |
| `ext` | `Extend(config.xlen)` | 对 Instr[31:7] 按 ImmSrc 扩展立即数。 |
| `cmp` | `Cmp(config.xlen)` | 比较 R1、R2，输出 Eq。 |
| `alu` | `ALU(config.xlen)` | 接收 SrcA、SrcB、ALUControl、Funct3，输出 ALUResult 和 IEUAdr。 |

当前 Verilog 还包含子模块端口连接线：`rf_clk`、`rf_reset`、`rf_io_WE3`、`rf_io_A1/A2/A3`、`rf_io_WD3`、`rf_io_RD1/RD2`，`ext_io_Instr/ImmSrc/ImmExt`，`cmp_io_R1/R2/Eq`，`alu_io_SrcA/SrcB/ALUControl/Funct3/ALUResult/IEUAdr`。位宽与对应子模块端口一致。未使用 `dontTouch` 强制保留别名；后续编译或综合可能进一步优化名称。

## 5. 与原书的差异及验证结果

原书将四个二选一选择器实例化为通用 `mux2`，本实现直接使用 Chisel `Mux`。原书的 RegFile 不接收 reset，本实现将 Datapath 的 reset 接入已核验的 RegFile，落实用户指定的 x0 同步清零行为。组合接口增加 `io_` 前缀，其余连接关系保持书中定义。

验证日期：2026-10-03。工具链为 JDK 17、sbt 1.10.7、Scala 2.13.14、Chisel 3.6.1、chiseltest 0.6.2；功能仿真采用 Treadle 后端。

```bash
./scripts/sbt-local.sh test 'runMain riscvsingle.GenerateDatapath'
```

**实际结果：Datapath 新增 7 项测试全部通过；全项目 7 个套件、46 项测试全部通过。Datapath Verilog 已生成。**

| 验证内容 | 实际覆盖 |
| --- | --- |
| 操作数、比较和存储数据 | 全部四种 ALUSrc；PC/ImmExt 相等而寄存器不等、寄存器相等而 PC/ImmExt 不等；WriteData 始终取 R2。 |
| 写回选择 | 两个写回选择信号的四种组合；OR 结果为 15 而 IEUAdr 为 19；明确检查独立 Funct3 输入生效。 |
| 立即数及地址 | I/S/B/J 各一个负立即数，分别在两种 PC 下检查目标；包含 32 位地址下溢回绕。 |
| 时序、使能和复位 | 写入在上升沿生效；RegWrite=0 时保持；reset 优先于 x1 写入且保持 x1；向 x0 写入全 1 仍读出零。 |
| 随机连续操作 | 固定种子 `0x215`，先通过正常写入初始化 x1–x31，再执行 200 个周期的随机指令位、立即数格式、操作数选择、ALU 功能、写回选择和写使能；独立软件寄存器模型检查 Eq、WriteData、IEUAdr，最后逐项检查全部 32 个寄存器的两个读端口。 |
| 书中程序联调 | 连接已核验 Controller，执行 Code Example 2.16 程序；检查 19 条实际执行指令的 PC 顺序、两次存储及最终循环地址。地址 96 写入 7，地址 100 写入 25，最终 PC 为 0x50。 |
| 接口与层次 | 检查全部 16 个生产端口及位宽、四个子模块实例名和复位连接。 |

随机测试采用既有 Controller 可产生的 ALU 功能组合；ALUSrc 和写回选择则覆盖全部二态组合。测试还用 `CpuConfig(imemDepth=128, dmemDepth=256, resetVector=0x100)` 实例化 Datapath，确认有效的后续结构配置可以透传，但本模块不会据此生成 PC 或存储器。

测试辅助 `DatapathHarness` 用于满足 chiseltest 的 Module 顶层要求，`ControlledDatapathHarness` 只提供 Controller 连接。书中程序的 PC 与内存由 Scala 测试模型维护，因此联调结果验证当前数据通路与控制器配合；完整 CPU 硬件尚待后续模块实现。未进行 FPGA 实现或时序分析。

单独生成可执行 `make generate-datapath SBT=./scripts/sbt-local.sh`，输出目录由 `DATAPATH_TARGET_DIR` 指定。`GenerateDatapath` 仅接受一个可选输出目录参数；硬件配置通过 Scala 构造参数传入。

## 6. 核验停点

本轮到此停止，Datapath 及配套资料保留在工作区等待核验。收到修改意见时，仅修改当前模块及配套资料并重新验证；核验通过并允许继续后，下一模块为 IEU。
