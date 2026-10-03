# Chisel 单周期 CPU 项目总结报告

完成日期：2026-10-04。设计依据：《RISC-V System-on-Chip Design, Edition 1》Code Example 2.15，书中第 61–65 页。

## 1. 完成情况

已用 Chisel 完成书中单周期 CPU 的 11 个硬件模块，并按模块交付源码、测试、生成的 Verilog 和中文核验报告。整机接通取指、译码、寄存器读写、运算、访存与下一 PC 选择，可运行书中测试程序。

项目位于 `riscv-single-cycle`，统一配置由 [CpuConfig.scala](../src/main/scala/riscvsingle/config/CpuConfig.scala) 提供。CPU 顶层已提交为 `d218f06`，补充的生成 RTL 验证已提交为 `b983c22`。本总结报告及 README 的交付状态更新另行提交。

**原定实现与验证任务已完成，按用户指示结束。** 各模块报告保留分轮核验时的交付记录；最终交付状态以本总结和 README 为准。

## 2. 模块划分与交付记录

| 模块 | 功能 | 实现提交 | 核验报告 |
| --- | --- | --- | --- |
| Extend | I、S、B、J 型立即数提取与符号扩展。 | `d6cf8cc` | [01-extend.md](modules/01-extend.md) |
| Cmp | 寄存器操作数比较，提供相等与小于结果。 | `4b96fd7` | [02-cmp.md](modules/02-cmp.md) |
| ALU | 加减、逻辑运算及比较结果选择。 | `aa69b21` | [03-alu.md](modules/03-alu.md) |
| RegFile | 两路组合读取、一路上升沿写入，直接按寄存器编号索引。 | `24f885f` | [04-regfile.md](modules/04-regfile.md) |
| Controller | 译码指令字段，生成执行、写回、访存与 PC 选择控制。 | `668d141` | [05-controller.md](modules/05-controller.md) |
| Datapath | 连接寄存器堆、立即数扩展、比较与 ALU，完成结果写回。 | `b77a5cd` | [06-datapath.md](modules/06-datapath.md) |
| IEU | 连接 Controller 与 Datapath，完成指令执行。 | `da62b51` | [07-ieu.md](modules/07-ieu.md) |
| IROM | 组合读取指令，支持配置容量和仿真程序镜像。 | `58232c4` | [08-irom.md](modules/08-irom.md) |
| IFU | PC 状态、顺序地址、跳转选择与取指；该轮统一同步复位。 | `1c1cb34` | [09-ifu.md](modules/09-ifu.md) |
| LSU | 数据 RAM 的组合读取与上升沿写入。 | `b882f2d` | [10-lsu.md](modules/10-lsu.md) |
| RiscvSingle | 连接 IFU、IEU、LSU，形成完整 CPU。 | `d218f06` | [11-riscv-single.md](modules/11-riscv-single.md) |

每份模块报告均说明端口、功能、内部信号名称及验证结果。生产顶层为 [RiscvSingle.scala](../src/main/scala/riscvsingle/RiscvSingle.scala)，层次如下：

```text
RiscvSingle
├── ifu: IFU
│   └── irom: IROM
├── ieu: IEU
│   ├── c: Controller
│   └── dp: Datapath
│       ├── rf: RegFile
│       ├── ext: Extend
│       ├── cmp: Cmp
│       └── alu: ALU
└── lsu: LSU
```

生产模块采用 RawModule 显式声明所需时钟和复位。测试用 Module 包装连接生产硬件。

## 3. 顶层端口与内部信号

| Verilog 端口 | 方向 / 位宽 | 功能 |
| --- | --- | --- |
| `clk` | 输入 / 1 | 共用时钟，上升沿更新状态。 |
| `reset` | 输入 / 1 | 高有效同步复位，复位 PC 和 x0。 |
| `io_WriteData` | 输出 / 32 | 第二路寄存器读数，同时作为 LSU 写数据。 |
| `io_IEUAdr` | 输出 / 32 | 地址运算结果，同时用于 LSU 访问及 IFU 跳转目标。 |
| `io_MemWrite` | 输出 / 1 | 数据存储写使能。 |

顶层内部信号为 `PC`、`PCPlus4`、`Instr`、`ReadData`、`PCSrc`。IFU 向 IEU 提供当前地址、顺序地址和指令；IEU 向 LSU 提供地址、写数据和写使能；LSU 将读数据返回 IEU；IEU 将跳转选择与目标地址返回 IFU。单周期内完成组合运算，在上升沿更新 PC、目的寄存器或 RAM。

生成器会合并部分 Wire 别名，对应 RTL 连接名称见模块 11 报告。输出端口用于观察生产 CPU 的执行与存储事务，内部已包含指令与数据存储器。

## 4. 参数化配置与用户核验要求

| CpuConfig 字段 | 默认值 | 约束与用途 |
| --- | --- | --- |
| `xlen` | 32 | 当前 CPU 限定为 RV32。 |
| `imemDepth` | 64 | 指令存储深度，以 32 位字计；为至少 2 的二次幂，容量须符合地址空间。 |
| `dmemDepth` | 64 | 数据存储深度，以 32 位字计；约束同上。 |
| `resetVector` | 0 | PC 复位字节地址，须为非负、符合 XLEN 范围且 4 字节对齐。 |
| `instructionInitFile` | None | 可选十六进制指令初始化文件，设置时路径须非空白。 |

Extend、Cmp、ALU、RegFile 可独立配置数据位宽，RegFile 还支持独立配置寄存器数量。整机仍固定使用 32 位数据和 32 项寄存器；独立模块的 64 位生成示例不代表整机支持 RV64。

已落实用户核验中的具体要求：

- 寄存器堆采用完整的 32 项 `Reg(Vec(...))`，索引直接使用寄存器编号，不减一。
- 同步复位仅清零 `rf(0)`；x1–x31 保持原值，没有预设复位值。
- 正常写入要求 `WE3 && A3 != 0`，禁止修改 x0；读端口直接读取 `rf(A1)`、`rf(A2)`。
- PC 与寄存器堆均采用高有效同步复位；源代码、测试、生成 RTL 及文档已统一。复位电平变化本身不更新状态。
- LSU 沿用书中无复位接口，RAM 不清空；MemWrite 在复位期间仍直接控制时钟边沿写入。

## 5. 验证结果

工具链为 JDK 17、sbt 1.10.7、Scala 2.13.14、Chisel 3.6.1、chiseltest 0.6.2、Verilator 5.036。

| 验证层次 | 已完成的结果 |
| --- | --- |
| Chisel/Treadle 全项目测试 | 12 个套件、74 项测试全部通过，包含配置检查、各模块功能、联调与生产顶层整机执行。 |
| 默认整机程序 | 书中程序实际执行 19 条指令；地址 96 写入 7，地址 100 写入 25，最终在 0x50 循环。 |
| 扩容配置整机程序 | IROM/RAM 均为 128 项，从 0x100 执行；检查 RAM 字 127 与 63 独立、负偏移加载、jal 链接值与跳过指令。 |
| 整机同步复位及 RAM 保留 | Chisel 测试确认无边沿复位脉冲不重启，复位上升沿重启，实际 RAM 数据在 CPU 复位后仍可加载。 |
| 生成 RTL 仿真 | 两份完整 CPU Verilog 均由 Verilator 编译；每份采用种子 1、17、2026 随机初始化未定义状态，共六次运行全部通过。 |
| RTL 接口与结构检查 | 顶层包含五个端口和完整 11 模块层次；PC、寄存器和 RAM 三个状态更新块均仅由 `posedge clk` 触发。 |

RTL 仿真逐周期核对地址与写使能，在写入周期核对写数据，并检查最终循环及同步复位。详见 [RTL 整机验证报告](12-rtl-validation.md)。Verilator 使用二态仿真，随机初值检查不能替代四态 X 传播验证或形式验证。

本次收尾仅提交已有验证文件和编写总结，生产代码与测试逻辑没有修改；上述结果为实现及 RTL 验证阶段的实际通过记录。

## 6. 复现与交付文件

从 `riscv-single-cycle` 目录运行：

```bash
# 全项目 Chisel 测试
make test SBT=./scripts/sbt-local.sh

# 生成默认完整 CPU，加载书中程序
make generate-cpu SBT=./scripts/sbt-local.sh

# 生成 128 项完整 CPU，从 0x100 执行配置测试程序
make generate-cpu SBT=./scripts/sbt-local.sh \
  IMEM_DEPTH=128 DMEM_DEPTH=128 CPU_TARGET_DIR=generated/riscv-single128 \
  INSTRUCTION_INIT_FILE=programs/rv32-configtest.memfile RESET_VECTOR=0x100

# 编译并验证上述两份生成的 RTL
make test-rtl
```

已有 sbt 在 PATH 中时可省略 `SBT=./scripts/sbt-local.sh`。Verilator 可通过 `VERILATOR=/path/to/verilator` 指定。`test-rtl` 验证现有导出文件，修改 Chisel 后须先重新生成两份 CPU。`make generate` 仍是独立 Extend 生成入口，整机使用 `generate-cpu`。

默认 [完整 RTL](../generated/riscv-single/RiscvSingle.v) 与扩容 [完整 RTL](../generated/riscv-single128/RiscvSingle.v) 各自包含全部 11 个模块，编译时选择其中一份即可。程序镜像位于 [programs](../programs)，模块报告位于 [docs/modules](modules)。`target/`、`.cache/`、测试产物及中间生成文件由 `.gitignore` 排除，交付保留 Verilog、源码、镜像和文档。

## 7. 当前边界与后续扩展方向

实现覆盖书中的简化 RV32 子集：add/sub/slt/or/and、所支持的对应 I 型运算、lw、sw、beq、jal。控制器按 opcode 分组译码，未提供完整 RV32I 合法性检查、异常、CSR、中断、压缩指令或外部总线。

指令和数据存储器采用字寻址切片，忽略地址低两位及超出容量的高位，访问按容量回绕。未初始化寄存器、RAM 和未覆盖的 ROM 项没有已知值保证；软件必须先建立需要的状态。

当前 IROM 的 `$readmemh` 位于 `ifndef SYNTHESIS`，已验证仿真程序加载；FPGA/ASIC 程序固化、综合、资源利用率及实现时序尚未验证。模块化层次与统一配置可作为后续完整 ISA、存储接口或 FPGA 实现的起点，具体扩展另行确定范围。
