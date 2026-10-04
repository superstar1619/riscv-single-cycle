# 模块 05：Controller 控制器核验报告（第七章第 6 轮）

## 1. 依据与本轮范围

依据本地《RISC-V System-on-Chip Design, Edition 1》第七章 §7.1、图 7.2 与表 7.1 的 RV32 列。控制结构依据 §7.1.4（第 311–314 页）、图 7.7；主译码对应表 7.6；分支标志选择及取反对应图 7.8 与表 7.7；ALU 译码对应 §7.1.4.3。CVW 参考文件为 `/home/unlastingstar/cvw/src/ieu/controller.sv`，特别是完整功能字段的 `legalcheck` 与主译码控制。未移植 CVW 的流水线、CSR、异常或扩展指令控制。

上一轮 ALU 已按用户指示提交为 `715b7b0`，提交前 13 个套件、99 项回归全部通过。本轮只完善 **Controller**。接口变化所需的 Datapath/IEU 连接和测试包装适配归入本轮；LUI 的 AltResult 写回、JALR 目标位 0 清零及 Jump 接入 Datapath 留待第 7 轮，完整 LSU 控制接入留待后续轮次。

| 交付内容 | 路径 |
| --- | --- |
| 模块及端口 | [Controller.scala](../../src/main/scala/riscvsingle/ieu/Controller.scala) |
| 生成入口 | [GenerateController.scala](../../src/main/scala/riscvsingle/GenerateController.scala) |
| 模块测试 | [ControllerSpec.scala](../../src/test/scala/riscvsingle/ieu/ControllerSpec.scala) |
| 连接适配 | [Datapath.scala](../../src/main/scala/riscvsingle/ieu/Datapath.scala)、[IEU.scala](../../src/main/scala/riscvsingle/ieu/IEU.scala) |
| 适配测试 | [DatapathSpec.scala](../../src/test/scala/riscvsingle/ieu/DatapathSpec.scala)、[IEUSpec.scala](../../src/test/scala/riscvsingle/ieu/IEUSpec.scala) |
| 模块 RTL | [Controller.v](../../generated/controller/Controller.v) |

## 2. 参数与全部端口

类 `riscvsingle.ieu.Controller` 继承 `RawModule`，端口 Bundle 为 `ControllerIO`。Controller 没有构造参数，指令字段和控制协议的位宽固定；本轮不改变 `CpuConfig`。整机仍限定 `xlen=32`。没有时钟、复位或使能端口，所有输出均为组合逻辑；控制器自身不负责同步复位期间的写入屏蔽。

生成入口仅有可选输出目录参数 `targetDir`，默认 `generated/controller`；不存在独立 RV64 译码配置。`imemDepth、dmemDepth、resetVector、instructionInitFile` 不参与本模块。

| Chisel 名称 | 实际 Verilog 端口 | 方向 | 类型 / 位宽 | 用途 |
| --- | --- | --- | --- | --- |
| `io.Op` | `io_Op` | 输入 | UInt / 7 | Instr[6:0] opcode。 |
| `io.Eq` | `io_Eq` | 输入 | Bool / 1 | 原始两个寄存器相等。 |
| `io.LT` | `io_LT` | 输入 | Bool / 1 | 原始两个寄存器有符号小于。 |
| `io.LTU` | `io_LTU` | 输入 | Bool / 1 | 原始两个寄存器无符号小于。 |
| `io.Funct3` | `io_Funct3` | 输入 | UInt / 3 | Instr[14:12]，功能码或立即数的一部分。 |
| `io.Funct7` | `io_Funct7` | 输入 | UInt / 7 | Instr[31:25]，替代旧 Funct7b5；普通立即数指令中为数据。 |
| `io.ALUResultSrc` | `io_ALUResultSrc` | 输出 | Bool / 1 | 1 选择 AltResult，0 选择 ALUResult；当前 Datapath 的替代结果仍仅为 PCPlus4。 |
| `io.ResultSrc` | `io_ResultSrc` | 输出 | Bool / 1 | 1 选择加载 ReadData，0 选择 IEUResult。 |
| `io.MemWrite` | `io_MemWrite` | 输出 | Bool / 1 | 保留兼容写使能，恒等于 MemRW[0]。 |
| `io.MemRW` | `io_MemRW` | 输出 | UInt / 2 | {MemRead, MemWrite}，00 空闲、10 加载、01 存储。 |
| `io.PCSrc` | `io_PCSrc` | 输出 | Bool / 1 | Jump 或已满足条件的 Branch 选择 IEUAdr 作为下一 PC。 |
| `io.Jump` | `io_Jump` | 输出 | Bool / 1 | 合法 JAL/JALR 标志，后续 Datapath 用于选择链接值。 |
| `io.RegWrite` | `io_RegWrite` | 输出 | Bool / 1 | 合法写寄存器指令使能。 |
| `io.ALUSrc` | `io_ALUSrc` | 输出 | UInt / 2 | {SrcA 选择 PC, SrcB 选择 ImmExt}。 |
| `io.ImmSrc` | `io_ImmSrc` | 输出 | UInt / 3 | 000/001/010/011/100 选择 I/S/B/J/U。 |
| `io.ALUControl` | `io_ALUControl` | 输出 | UInt / 2 | {SubArith, ALUOp}，保持原 ALU 接口。 |

共 6 个输入、10 个输出。比较输入通常来自同一 Cmp，测试也遍历不一致的标志组合，以验证分支电路确实选择对应输入。X/Z 四态传播不作为接口保证。

## 3. 主译码、合法性与控制规则

表 7.6 的 RV32 控制采用确定的二态值。R 型不用的立即数选择固定为 I；LUI 不用的 ALUSrc 固定为 00、ALUOp 固定为 0。加载、存储、分支不用的结果选择固定为 0。

| 指令组 | Op | RegWrite | ImmSrc | ALUSrc | ALUOp | ALUResultSrc | MemRW | ResultSrc | Branch | Jump |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| R 型 ALU | 33 | 1 | 000 | 00 | 1 | 0 | 00 | 0 | 0 | 0 |
| I 型 ALU | 13 | 1 | 000 | 01 | 1 | 0 | 00 | 0 | 0 | 0 |
| 加载 | 03 | 1 | 000 | 01 | 0 | 0 | 10 | 1 | 0 | 0 |
| 存储 | 23 | 0 | 001 | 01 | 0 | 0 | 01 | 0 | 0 | 0 |
| 条件分支 | 63 | 0 | 010 | 11 | 0 | 0 | 00 | 0 | 1 | 0 |
| JAL | 6F | 1 | 011 | 11 | 0 | 1 | 00 | 0 | 0 | 1 |
| JALR | 67 | 1 | 000 | 01 | 0 | 1 | 00 | 0 | 0 | 1 |
| LUI | 37 | 1 | 100 | 00 | 0 | 1 | 00 | 0 | 0 | 0 |
| AUIPC | 17 | 1 | 100 | 11 | 0 | 0 | 00 | 0 | 0 | 0 |
| 非法指令 | 任意 | 0 | 000 | 00 | 0 | 0 | 00 | 0 | 0 | 0 |

Op 列使用十六进制，其余控制列为二进制。只在满足下列合法性条件时启用指令组控制：

| 指令组 | 合法功能字段 |
| --- | --- |
| R 型 | Funct7=00 时八种 Funct3 均合法；Funct7=20 时仅 Funct3=000（SUB）或 101（SRA）。 |
| I 型 ALU | 非移位 Funct3=000/010/011/100/110/111 不限制 Funct7，因为它属于立即数；SLLI 要求 Funct7=00；SRLI/SRAI 要求 Funct7=00/20。 |
| 加载 | Funct3=000/001/010/100/101：LB/LH/LW/LBU/LHU。 |
| 存储 | Funct3=000/001/010：SB/SH/SW。 |
| 分支 | Funct3=000/001/100/101/110/111：BEQ/BNE/BLT/BGE/BLTU/BGEU。 |
| JALR | Funct3=000，Funct7 不限制（立即数数据）。 |
| JAL、LUI、AUIPC | 两个功能字段均不限制（立即数字段）。 |

Funct7=00/20 使用十六进制。RV32 移位立即数的 shamt 只允许低 5 位，Instr[25]=1 对应 Funct7[0]=1，不能误当作合法 RV64 移位。M、位操作、RV64 的 1B/3B opcode，以及 FENCE、ECALL、EBREAK、CSR 等不在范围，控制全零。非法指令关闭 RegWrite、MemRW、Jump 和 PCSrc，不产生异常或陷阱；已有 IFU 按 PC+4 前进。

教材 ALU 译码只检查 Funct7[5]；本轮依用户计划及 CVW 的严格合法性路径检查完整字段，再生成相同有效 ALU 控制，避免接受扩展或保留编码。

分支按图 7.8 先由 Funct3[2:1] 选择 Eq/0/LT/LTU，再 XOR Funct3[0]。非法 010/011 的 Branch 已关闭，即使组合选择中间值为 1 也不跳转：

| 指令 | Funct3 | 分支条件 |
| --- | --- | --- |
| BEQ | 000 | Eq |
| BNE | 001 | NOT Eq |
| BLT | 100 | LT |
| BGE | 101 | NOT LT |
| BLTU | 110 | LTU |
| BGEU | 111 | NOT LTU |

```text
PCSrc = Jump OR (Branch AND BranchTaken)
SubArith = ALUOp AND (Funct3=010 OR Funct3=011
           OR (Funct3=101 AND Funct7[5])
           OR (Op=33 AND Funct3=000 AND Funct7[5]))
ALUControl = {SubArith, ALUOp}
MemWrite = MemRW[0]
```

SUB、SLT/SLTI、SLTU/SLTIU、SRA/SRAI 的控制为 11；其他合法 ALU 运算为 01。非 ALU 指令与非法编码为 00，地址始终使用加法，AUIPC 强制 ALUSelect=000；ADDI 即使立即数位 30=1 也不会变为减法。

## 4. 当前连接适配与整机边界

本轮将 Datapath 的 ImmSrc 输入扩为 3 位，直接连接已经支持 I/S/B/J/U 的 Extend，取消原来的高位补零包装。向外暴露已经实现的 Cmp.LT/LTU，并在 IEU 中反馈给 Controller；Controller 的 Funct7 接 Instr[31:25]。比较始终针对原始 R1/R2，与 ALUSrc 选择的 PC/ImmExt 无关。接口变化所需的包装和端口测试同步适配，其他模块的硬件算法不在本轮重写。

IEU 的生产外部端口仍保留现有接口，MemWrite 使用 Controller 的兼容输出。Controller 新增 Jump 与 MemRW 的完整下游消费分别留待 Datapath/IEU/LSU 轮次；本轮不新增 Datapath.Jump 输入、不实现 AltResult 选择、不清除 JALR 目标位 0、不改 LSU 的字 RAM。

因此 Controller 已能生成表 7.1 的控制，并不代表当前整机已完整执行所有指令。当前 LUI 的 ALUResultSrc 仍会选择旧 Datapath 的 PCPlus4，JALR 目标仍未清位；SB/SH 仍通过旧 LSU 写整个字，LB/LH/LBU/LHU 仍得到旧字读取结果。完整指令签名程序留待各轮接通完成后验证。本轮继续用已有合法程序验证整机回归，并针对新控制与比较连接作独立测试。

同步复位行为保持既有 RegFile/IFU 约定；Controller 无复位端口，独立 IEU 在复位高电平时仍组合译码 MemWrite。计划中的顶层复位期间存储写入屏蔽留待对应整机轮次，本轮不宣称已经完成该行为。

## 5. 内部信号与实际 RTL 名称

控制字保持教材主译码结构，增加立即数选择和读写控制的位宽后共 13 位：

```text
RegWrite_ImmSrc_ALUSrc_ALUOp_ALUResultSrc_MemRW_ResultSrc_Branch_Jump
   12     11:9    8:7    6        5        4:3       2       1     0
```

合法组对应常量依次为 R=`0x1040`、I=`0x10C0`、加载=`0x1094`、存储=`0x0288`、分支=`0x0582`、JAL=`0x17A1`、JALR=`0x10A1`、LUI=`0x1820`、AUIPC=`0x1980`。LegalInstr=0 时整个控制字为零；ALU 解码还受其中 ALUOp 门控。

| Chisel 名称 | 类型 / 位宽 | 定义 | 实际生成 RTL |
| --- | --- | --- | --- |
| `controls` | UInt / 13 | 合法性门控后的主译码控制字 | `controls` |
| `LegalInstr` | Bool / 1 | Op 与功能字段满足本轮 RV32 范围 | `LegalInstr` |
| `Branch` | Bool / 1 | controls[1]，合法分支组标志 | `Branch` |
| `Jump` | Bool / 1 | controls[0]，合法 JAL/JALR 标志 | `Jump`；io_Jump 直接连接 controls[0] |
| `BranchFlag` | Bool / 1 | Funct3[2:1] 四选一的 Eq/0/LT/LTU | `BranchFlag` |
| `BranchTaken` | Bool / 1 | BranchFlag XOR Funct3[0]，尚未与 Branch 门控 | `BranchTaken` |
| `ALUOp` | Bool / 1 | controls[6]，合法 R/I 型 ALU 标志 | `ALUOp` |
| `SubArith` | Bool / 1 | 减法、比较及算术右移控制 | `SubArith` |
| `RLegal` | Bool / 1 | R 型完整 Funct7 合法性 | `RLegal` |
| `ILegal` | Bool / 1 | I 型移位与普通立即数合法性 | `ILegal` |
| `LoadLegal` | Bool / 1 | 五种 RV32 加载宽度 | `LoadLegal` |
| `StoreLegal` | Bool / 1 | 三种 RV32 存储宽度 | 独立名称被优化，复用 `_LoadLegal_T_4` 的三种编码判定 |
| `BranchLegal` | Bool / 1 | 排除 Funct3=010/011 | `BranchLegal` |

所有信号均为组合逻辑。没有新增 Controller 子模块或状态。生成器的 `_RLegal_T*、_ILegal_T*、_LegalInstr_T*、_controls_T*、_BranchFlag_T*、_SubArith_T*` 为当前编译产生的辅助线网，不作为稳定接口。

独立 Controller RTL 保留全部 16 个端口、13 位控制字和上述主功能名称；主译码和分支四选一展开为选择链，最终 PCSrc 使用 `Jump | Branch & BranchTaken`。IEU 和两份整机导出内，尚未消费的 Controller.io_Jump 输出被编译器裁去，因此该层次中的 Controller 为 15 个端口；内部 Jump 仍用于 PCSrc，MemRW 因 MemWrite 使用其位 0 而保留。这不改变 Chisel 的完整接口，后续 Jump 接入时应重新生成依赖 RTL。

Datapath 生成 RTL 的 io_ImmSrc 为 3 位，io_LT/io_LTU 直接来自 Cmp；IEU 中 `c_io_Funct7=io_Instr[31:25]`，`c_io_LT=dp_io_LT`、`c_io_LTU=dp_io_LTU`，`dp_io_ImmSrc=c_io_ImmSrc`。Chisel 的中间 Eq/LT/LTU/ImmSrc Wire 别名可能直接折叠为这些连接，不能把辅助名称视为稳定观察端口。

## 6. 测试先行与模块验证

先在旧公共接口上新增行为测试，实际运行：

```bash
./scripts/sbt-local.sh 'testOnly riscvsingle.ieu.ControllerSpec'
```

红阶段 **8 项中 3 通过、5 失败，退出码 1**；五个预期失败分别暴露 BNE 条件、非法 R 型移位的写入抑制、SLTU 减法控制、SRA 算术控制和非法 RV32 加载宽度。日志为 `target/controller-round6-red.log`。之后升级接口与测试参考模型，实施合法性门控、分支和 ALU 译码及必要连接适配。

绿色验证命令：

```bash
./scripts/sbt-local.sh 'testOnly riscvsingle.ieu.ControllerSpec riscvsingle.ieu.DatapathSpec riscvsingle.ieu.IEUSpec'
```

**三个套件、18 项测试全部通过，退出码 0**，其中 ControllerSpec 5 项、DatapathSpec 7 项、IEUSpec 6 项。日志 `target/controller-round6-focused.log`。测试包装使用 Module 的测试时钟，生产 Controller 仍为无时钟的组合 RawModule。

| 验证内容 | 实际覆盖 |
| --- | --- |
| 全功能字段扫描 | 128 个 Op × 8 个 Funct3 × 128 个 Funct7，共 **131,072** 组；每组检查全部 10 个输出。软件以具名字段行为表和独立合法性模型建立期望，不复用硬件打包常量。 |
| 全分支选择 | 8 个 Funct3 × 128 个 Funct7 × 8 个 Eq/LT/LTU 组合，共 **8,192** 组；包含非法 010/011 的全控制关闭。期望直接判断各分支条件，不使用硬件四选一/XOR 表达式。 |
| 明确合法性样本 | **33** 组显式合法/非法样本，覆盖普通负立即数、移位保留位、M/位操作编码、RV64 opcode、FENCE/CSR、JALR 的 Funct3、JAL/U 型功能字段数据。 |
| Controller→ALU | **25** 组手工结果向量，覆盖十种寄存器、九种立即数运算和六类地址生成，共 **50** 次结果/地址检查；验证比较溢出、借位、算术右移和 ADDI 高位不误触发减法。 |
| IEU 比较反馈 | **40** 组分支场景，五种原始寄存器对 × 八种 Funct3；−1/1 的正负顺序互换、相等、零，确认有符号/无符号条件独立、目标 PC+16、非法分支不跳转且不改寄存器。 |
| RTL 接口 | 检查 6 输入/10 输出及位宽，完整 Funct7、3 位 ImmSrc、LT/LTU、Jump/MemRW，无 clock/reset 或时序块。Datapath 端口测试适配为 3 位立即数选择和新增比较输出；IEU/CPU 的外部端口不变。 |

前三类共 **139,297 组控制输入、1,392,970 次输出断言**。全功能字段扫描中，合法指令字段组合为 5,773 组，非法组合为 125,299 组；立即数字段不受限制的指令占多数，这一统计用于核对扫描范围，不代表不同指令条数。所有加载宽度、存储宽度、九类控制以及非法 opcode/funct3/funct7 都包含在扫描内。

IEU 原“未定义 opcode”测试中的 LUI/JALR 现已被 Controller 合法识别，因此改用本阶段不支持的 00/0F/73/7F；仍验证不写寄存器、不存储、不跳转。原书程序和现有控制手动驱动的 Datapath 随机参考测试继续保留。

全工程最终回归命令：

```bash
./scripts/sbt-local.sh test
```

**13 个套件、102 项测试全部通过，退出码 0**，无中止或失败；原书程序、扩容配置、寄存器与 PC 同步复位、数据 RAM 保持及先前各模块回归通过。日志 `target/controller-round6-regression.log`。

## 7. RTL 生成与整机验证

实际重新生成命令：

```bash
./scripts/sbt-local.sh \
  'runMain riscvsingle.GenerateController generated/controller' \
  'runMain riscvsingle.GenerateDatapath generated/datapath' \
  'runMain riscvsingle.GenerateIEU generated/ieu' \
  'runMain riscvsingle.GenerateRiscvSingle 64 64 generated/riscv-single programs/riscvtest.memfile 0' \
  'runMain riscvsingle.GenerateRiscvSingle 128 128 generated/riscv-single128 programs/rv32-configtest.memfile 0x100'
make test-rtl
```

五个生成入口全部成功，退出码 0，日志 `target/controller-round6-generate.log`。产物如下，生成依赖模块均包含本轮的接口和连接适配：

| 配置 | RTL 路径 |
| --- | --- |
| Controller | [generated/controller/Controller.v](../../generated/controller/Controller.v) |
| Datapath | [generated/datapath/Datapath.v](../../generated/datapath/Datapath.v) |
| IEU | [generated/ieu/IEU.v](../../generated/ieu/IEU.v) |
| 默认 64 项 CPU、复位地址 0 | [generated/riscv-single/RiscvSingle.v](../../generated/riscv-single/RiscvSingle.v) |
| 128 项 CPU、复位地址 0x100 | [generated/riscv-single128/RiscvSingle.v](../../generated/riscv-single128/RiscvSingle.v) |

`make test-rtl` 直接编译重新生成的两份整机 Verilog。Verilator 5.036 对 book/expanded 分别使用随机初值种子 1、17、2026，**六次运行全部 PASS，退出码 0**；book 每次执行 19 个周期、2 次存储，expanded 每次执行 13 个周期、5 次存储，并检查结束循环与同步复位。日志 `target/controller-round6-rtl.log`。

当前整机 RTL 验证运行既有合法程序；新增控制的全部功能字段和比较条件由独立模块、Controller→ALU 与 IEU 分支测试覆盖，不把既有程序的 RTL 回归解释为全部 RV32 指令整机验收。两种存储容量及初始化文件保持原配置；独立 ALU 的 64 位测试不改变 Controller 的 RV32 范围。

## 8. 核验停点

**本轮停止，等待用户核验。** Controller 源码、接口适配、测试、五份生成 RTL 和报告已交付。当前轮改动保留在工作树；明确核验通过并允许继续后，提交本轮并进入第 7 轮 Datapath，不自动连续实施其他模块。
