# 模块 05：Controller 控制器核验报告

## 1. 依据与交付状态

对应《RISC-V System-on-Chip Design, Edition 1》Code Example 2.15 中的 `controller`，书中第 62–63 页及 Table 2.4。模块根据 opcode、功能字段和相等标志，生成寄存器写入、立即数格式、操作数选择、ALU 运算、内存写入、写回及 PC 选择信号。

上一模块 RegFile 已按用户指示提交，提交号为 `24f885f`，采用完整的 32 项时序寄存器、x0 专用同步复位和直接编号索引。本轮仅新增 **Controller 一个硬件模块**、生成入口、测试及报告，没有实施 Datapath 或后续模块。

状态：**Controller 测试通过，尚未提交，等待用户核验；核验通过并允许继续后才实施 Datapath。**

| 交付内容 | 路径 |
| --- | --- |
| 模块及端口定义 | [Controller.scala](../../src/main/scala/riscvsingle/ieu/Controller.scala) |
| Verilog 生成入口 | [GenerateController.scala](../../src/main/scala/riscvsingle/GenerateController.scala) |
| 模块测试 | [ControllerSpec.scala](../../src/test/scala/riscvsingle/ieu/ControllerSpec.scala) |
| 生成的硬件 | [Controller.v](../../generated/controller/Controller.v) |

## 2. 接口与配置

模块类为 `riscvsingle.ieu.Controller`，继承 `RawModule`，端口 Bundle 为 `ControllerIO`。无时钟、复位或寄存器，所有输出随输入经组合逻辑传播更新。

端口位宽由 RISC-V 指令字段及既有控制协议确定，没有数据通路位宽参数；本轮未修改 `CpuConfig`。生成入口仅接受可选输出目录，默认 `generated/controller`。

| Chisel 端口 | Verilog 端口 | 方向 | 类型 / 位宽 | 功能 |
| --- | --- | --- | --- | --- |
| `io.Op` | `io_Op` | 输入 | `UInt(7.W)` | 指令 opcode，对应 `Instr[6:0]`。 |
| `io.Eq` | `io_Eq` | 输入 | `Bool` / 1 | Cmp 输出的两个寄存器相等标志。 |
| `io.Funct3` | `io_Funct3` | 输入 | `UInt(3.W)` | 指令 `Instr[14:12]`，用于 ALU 功能选择。 |
| `io.Funct7b5` | `io_Funct7b5` | 输入 | `Bool` / 1 | 指令 `Instr[30]`；R 型加减法区分位，I 型中属于立即数。 |
| `io.ALUResultSrc` | `io_ALUResultSrc` | 输出 | `Bool` / 1 | 0 选择 ALUResult，1 选择 PCPlus4 作为 IEUResult。 |
| `io.ResultSrc` | `io_ResultSrc` | 输出 | `Bool` / 1 | 0 选择 IEUResult，1 选择内存 ReadData 写回。 |
| `io.MemWrite` | `io_MemWrite` | 输出 | `Bool` / 1 | LSU 存储写使能。 |
| `io.PCSrc` | `io_PCSrc` | 输出 | `Bool` / 1 | 0 选择顺序 PCPlus4，1 选择 IEUAdr 作为下一 PC。 |
| `io.RegWrite` | `io_RegWrite` | 输出 | `Bool` / 1 | RegFile 写使能。 |
| `io.ALUSrc` | `io_ALUSrc` | 输出 | `UInt(2.W)` | 位 1：0 选 R1、1 选 PC；位 0：0 选 R2、1 选 ImmExt。 |
| `io.ImmSrc` | `io_ImmSrc` | 输出 | `UInt(2.W)` | `00/01/10/11` 分别选择 Extend 的 I/S/B/J 格式。 |
| `io.ALUControl` | `io_ALUControl` | 输出 | `UInt(2.W)` | `{Sub, ALUOp}`，编码与已核验 ALU 一致。 |

## 3. 功能与控制表

主译码按书中 opcode 分组进行，控制字排列为：

```text
RegWrite_ImmSrc_ALUSrc_ALUOp_ALUResultSrc_MemWrite_ResultSrc_Branch_Jump
   10      9:8    7:6    5        4          3        2       1     0
```

| 指令组 | Op | RegWrite | ImmSrc | ALUSrc | ALUOp | ALUResultSrc | MemWrite | ResultSrc | Branch | Jump |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| lw | `0000011` | 1 | `00` | `01` | 0 | 0 | 0 | 1 | 0 | 0 |
| sw | `0100011` | 0 | `01` | `01` | 0 | 0 | 1 | 0 | 0 | 0 |
| R 型 ALU | `0110011` | 1 | `00` | `00` | 1 | 0 | 0 | 0 | 0 | 0 |
| I 型 ALU | `0010011` | 1 | `00` | `01` | 1 | 0 | 0 | 0 | 0 | 0 |
| beq | `1100011` | 0 | `10` | `11` | 0 | 0 | 0 | 0 | 1 | 0 |
| jal | `1101111` | 1 | `11` | `11` | 0 | 1 | 0 | 0 | 0 | 1 |
| 其他 opcode | 其他 | 0 | `00` | `00` | 0 | 0 | 0 | 0 | 0 | 0 |

R 型的 ImmSrc 在原书中为 `xx`，因为其操作数不使用立即数；本实现固定为 `00`。ALUSrc 的实际输出为 `00`（R1/R2）、`01`（R1/ImmExt）或 `11`（PC/ImmExt）。

ALU 控制计算与原书一致：

```text
Sub = ALUOp AND ((Funct3==000 AND Funct7b5 AND Op[5]) OR Funct3==010)
ALUControl = {Sub, ALUOp}
PCSrc = (Branch AND Eq) OR Jump
```

| 场景 | ALUControl | 行为 |
| --- | --- | --- |
| lw/sw/beq/jal | `00` | 强制 ALU 加法，生成访问地址或分支/跳转目标。 |
| R 型 add、I 型 addi | `01` | 加法；addi 不因立即数位 30 为 1 而变成减法。 |
| R 型 sub（Funct3=000、位 30=1） | `11` | 减法。 |
| R/I 型 slt/slti（Funct3=010） | `11` | 启用减法及有符号小于比较。 |
| R/I 型其余功能码 | `01` | ALU 按 Funct3 选择逻辑结果或其未实现功能的默认结果。 |

Controller 不生成 `ALUControl=10`。beq 组仅在 Eq=1 时令 PCSrc=1；jal 的 PCSrc 始终为 1，并选择 PCPlus4 写回；其余组不受 Eq 影响。

与书中一样，**本模块只按 opcode 分组译码，没有完整指令合法性检查**。例如 lw/sw 组不检查访问宽度，branch 组不按 Funct3 区分其他分支；R/I 型已识别 opcode 下的未实现功能码也不会自动关闭 RegWrite。这些行为不是完整 RV32I 支持或非法指令异常处理。

## 4. 内部信号名称

| 信号名称 | 类型 / 位宽 | 功能 |
| --- | --- | --- |
| `controls` | `Wire(UInt(11.W))` | 主译码的打包控制字；默认 0。六个 opcode 对应值依次为 `0x444/0x148/0x420/0x460/0x2C2/0x7D1`。 |
| `Branch` | `Wire(Bool())` | `controls[1]`，branch 组的标志。 |
| `Jump` | `Wire(Bool())` | `controls[0]`，jal 组的标志。 |
| `ALUOp` | `Wire(Bool())` | `controls[5]`，R/I 型 ALU 功能选择使能。 |
| `Sub` | `Wire(Bool())` | 加减法选择位，按上述公式生成。 |

五个命名信号均保留在当前生成的 Verilog 中，全部为组合信号。其他输出直接由控制字切片或组合表达式驱动。

当前 Verilog 还包含以下自动辅助信号：

| 辅助名称 | 位宽 | 功能 |
| --- | --- | --- |
| `_controls_T_1` | 11 | lw 控制字或默认 0。 |
| `_controls_T_3` | 11 | sw 控制字或上一层结果。 |
| `_controls_T_5` | 11 | R 型控制字或上一层结果。 |
| `_controls_T_7` | 11 | I 型控制字或上一层结果。 |
| `_controls_T_9` | 11 | beq 控制字或上一层结果；最后一层再选择 jal，得到 controls。 |
| `_Sub_T_4` | 1 | `Funct3==010` 的结果。 |

未使用 `dontTouch` 强制保留，辅助名称可能随源码、编译器或综合优化变化。

## 5. 与原书的差异及验证结果

原书对未实现 opcode 输出 `'x`，本实现使用全零控制字，使寄存器写入、内存写入和 PC 跳转均关闭。R 型不用的 ImmSrc 固定为 0。端口具有 `io_` 前缀，其他二态逻辑保持书中定义，X/Z 四态传播不作为本轮验证保证。

验证日期：2026-10-03。工具链为 JDK 17、sbt 1.10.7、Scala 2.13.14、Chisel 3.6.1、chiseltest 0.6.2；功能仿真使用默认 Treadle 后端。

```bash
./scripts/sbt-local.sh test 'runMain riscvsingle.GenerateController'
```

**实际结果：Controller 新增 3 项测试全部通过；全项目 6 个套件、39 项测试全部通过，原有模块和配置检查回归无失败。Controller Verilog 已生成。**

| 验证内容 | 实际覆盖 |
| --- | --- |
| 完整二态输入空间 | `128 opcode × 8 Funct3 × 2 Funct7b5 × 2 Eq = 4096` 组输入；逐组检查全部 8 个输出，共 32768 次输出检查。软件参考使用独立的行为字段表及指令组语义，不复用 RTL 打包常量。 |
| 分支、跳转及减法规则 | 穷举同时覆盖 beq 两种 Eq、jal 无条件跳转、addi 位 30 不触发减法、sub 区分位及 slt/slti 始终启用减法。 |
| 未实现 opcode | 穷举覆盖其全部功能字段及 Eq 组合，确认所有输出均为 0；已识别 opcode 内的粗粒度译码保持原书行为。 |
| 与已有 ALU 连接 | 11 组具体向量覆盖 add/sub/addi/slt/slti/or/and/lw/sw/beq/jal，检查实际 ALUResult 和 IEUAdr，包括负 addi、SLT 溢出边界及地址功能码屏蔽，共 22 次输出检查。 |
| 生成接口 | 检查只有 4 个输入和 8 个输出，全部位宽匹配，无 clock/reset 端口或时序 always 块。 |

chiseltest 的 `ControllerHarness` 仅用于满足 `Module` 测试顶层要求；`ControllerALUHarness` 仅连接当前 Controller 和已核验 ALU 做测试，没有交付新的 Datapath 模块。功能检查不推进时钟，生产 Controller 仍为组合 RawModule。

`ChiselStage` 弃用提示与此前一致；本轮没有进行 FPGA 实现、时序分析或完整 CPU 仿真。可单独执行 `make generate-controller SBT=./scripts/sbt-local.sh`，输出目录由 `CONTROLLER_TARGET_DIR` 指定。

## 6. 核验停点

本轮到此停止，Controller 变更保留在工作区等待核验。收到修改意见时，仅修改当前模块及配套资料并重新验证；明确核验通过并允许继续后，下一模块为 Datapath。
