# 模块 09：IFU 取指单元核验报告

## 1. 依据与交付状态

对应《RISC-V System-on-Chip Design, Edition 1》Code Example 2.15 的 `ifu`，书中第 61–62 页。IFU 保存当前 PC，生成 PCPlus4，选择下一 PC，并通过 IROM 输出当前指令。

上一模块 IROM 已按用户指示提交，提交号为 `58232c4`。本轮交付 **IFU 一个硬件模块**及其生成入口、测试和报告。LSU 留待下一轮实现。

状态：**IFU 测试通过，尚未提交，等待用户核验。**

| 交付内容 | 路径 |
| --- | --- |
| 模块及端口定义 | [IFU.scala](../../src/main/scala/riscvsingle/ifu/IFU.scala) |
| Verilog 生成入口 | [GenerateIFU.scala](../../src/main/scala/riscvsingle/GenerateIFU.scala) |
| 模块测试 | [IFUSpec.scala](../../src/test/scala/riscvsingle/ifu/IFUSpec.scala) |
| 默认生成硬件 | [IFU.v](../../generated/ifu/IFU.v) |
| 128 项、复位地址 0x100 的配置示例 | [IFU.v](../../generated/ifu128/IFU.v) |

## 2. 端口与参数配置

模块类为 `riscvsingle.ifu.IFU`，继承 `RawModule`，端口 Bundle 为 `IFUIO`。显式声明 clk/reset，其余端口具有 `io_` 前缀。共有 4 个输入和 3 个输出。

| Chisel 端口 | Verilog 端口 | 方向 | 类型 / 位宽 | 功能 |
| --- | --- | --- | --- | --- |
| `clk` | `clk` | 输入 | `Clock` / 1 | PC 寄存器在上升沿加载选定的下一地址。 |
| `reset` | `reset` | 输入 | `Bool` / 1 | 高有效同步复位，在 clk 上升沿将 PC 加载为 resetVector，优先于正常更新。 |
| `io.PCSrc` | `io_PCSrc` | 输入 | `Bool` / 1 | 0 选择 PCPlus4，1 选择 IEUAdr，通常来自 IEU。 |
| `io.IEUAdr` | `io_IEUAdr` | 输入 | `UInt(config.xlen.W)` / 32 | IEU 提供的分支或跳转目标字节地址。 |
| `io.Instr` | `io_Instr` | 输出 | `UInt(32.W)` | IROM 根据当前 PC 异步读出的完整指令。 |
| `io.PC` | `io_PC` | 输出 | `UInt(config.xlen.W)` / 32 | 当前指令的字节地址，即 PC 寄存器值。 |
| `io.PCPlus4` | `io_PCPlus4` | 输出 | `UInt(config.xlen.W)` / 32 | 当前 PC 加 4，供顺序执行与 jal 链接写回。 |

构造参数为 `config: CpuConfig = CpuConfig()`。本轮未修改配置类。

| 配置项 | 默认值 | 功能 |
| --- | --- | --- |
| `xlen` | 32 | PC、PCPlus4、IEUAdr 和 PCNext 的宽度，当前配置要求为 RV32。 |
| `resetVector` | 0 | PC 同步复位时加载的字节地址，需满足 32 位范围和 4 字节对齐。 |
| `imemDepth` | 64 | 传给子模块 IROM，设置指令字数量。 |
| `instructionInitFile` | `None` | 传给 IROM，指定仿真启动时加载的十六进制镜像。 |
| `dmemDepth` | 64 | IFU 不使用，留给后续 LSU。 |

默认构造 `new IFU(CpuConfig())` 不初始化指令内容；生成入口默认显式加载 `programs/riscvtest.memfile`，以便复现书中程序。

## 3. 功能与时序

正常执行时的组合关系为：

```text
PCPlus4 = (PC + 4) mod 2^32
PCNext  = PCSrc ? IEUAdr : PCPlus4
Instr   = IROM[PC 的字索引]
```

clk 上升沿且 reset=0 时，PC 加载 PCNext；没有暂停或写使能输入。修改 PCSrc 或 IEUAdr 只改变下一地址选择，当前 PC 和 Instr 在下一上升沿前保持当前状态。

| reset | PCSrc | PC 更新行为 |
| --- | --- | --- |
| 1 | 任意 | 下一个 clk 上升沿复位为 resetVector；复位保持期间的上升沿也保持该值。 |
| 0 | 0 | 下一个时钟上升沿加载 PCPlus4。 |
| 0 | 1 | 下一个时钟上升沿加载 IEUAdr。 |

按本次用户核验意见，项目统一使用高有效同步复位，PC 寄存器的 Bool reset 直接传入 `withClockAndReset(clk, reset)`。reset 的置位、释放及未跨越时钟上升沿的短脉冲均不改变 PC。reset=1 时的 clk 上升沿将 PC 恢复为 resetVector，同时 RegFile 将 x0 清零；复位优先于跳转或顺序更新。这一复位方式是本项目对书中 PC 寄存器的调整。

IEUAdr 原样加载，未增加对齐掩码或地址异常检查。例如目标 3 会使 PC=3、PCPlus4=7；IROM 忽略地址低两位，仍读取第 0 项。高位地址别名也沿用已核验 IROM 的行为。PCPlus4 在 `0xFFFFFFFC` 时回绕为 0。

PC 在首次有效复位前没有已知值保证。复位不清除或重新加载 ROM，指令内容仍由 IROM 的初始化配置决定。

## 4. 内部信号名称与层次

| 内部名称 | 类型 / 位宽 | 功能 | 当前生成 Verilog 中的名称 / 位置 |
| --- | --- | --- | --- |
| `pcreg` | `RegInit(UInt(config.xlen.W))` / 32 | 保存当前 PC，同步复位值为 resetVector。 | `reg [31:0] pcreg`，驱动 io_PC；更新块仅由 `posedge clk` 触发。 |
| `PCNext` | `Wire(UInt(config.xlen.W))` / 32 | PCPlus4 与 IEUAdr 的选择结果。 | 别名经优化合并为 pcreg 更新块中的 `if (io_PCSrc)` 分支。 |
| `io.PCPlus4` | 输出 / 32 | PC 加 4 的组合结果。 | `assign io_PCPlus4 = pcreg + 32'h4`。 |
| `irom` | `IROM(config)` 实例 | 根据当前 PC 输出指令，接受同一配置。 | `IROM irom`。 |
| `irom_io_a` | 自动连接线 / 32 | 子模块地址输入，接 pcreg。 | 同名，赋值为 pcreg。 |
| `irom_io_rd` | 自动连接线 / 32 | 子模块指令输出，接 io_Instr。 | 同名。 |

生成器在 `RANDOMIZE_REG_INIT` 宏下还声明 `_RAND_0`，供仿真随机初始化使用；它不属于运行期 CPU 状态。未使用 `dontTouch` 强制保留别名。

IFU 直接包含一个已核验的 IROM 子模块，生成文件包含 IFU 和 IROM 两个模块定义。书中的 pcreg、pcadd4、pcmux 分别以 Chisel 寄存器、加法表达式和 Mux 表达式实现，本轮未引入额外的通用基础模块。

## 5. 生成配置与已有初始化限制

生成入口四个可选参数依次为深度、输出目录、初始化文件、复位字节地址，默认 `64`、`generated/ifu`、`programs/riscvtest.memfile`、`0`。文件参数 `-` 表示 None；复位地址支持十进制及 `0x`/`0X` 十六进制。

Makefile 对应 `IMEM_DEPTH`、`IFU_TARGET_DIR`、`INSTRUCTION_INIT_FILE`、`RESET_VECTOR`，例如：

```bash
make generate-ifu SBT=./scripts/sbt-local.sh
make generate-ifu SBT=./scripts/sbt-local.sh \
  IMEM_DEPTH=128 IFU_TARGET_DIR=generated/ifu128 RESET_VECTOR=0x100
```

128 项、复位地址 0x100 的生成示例用于检查配置结构。其默认镜像只有 64 项，0x100 对应第 64 项，未初始化；实际运行该复位地址需提供覆盖该项的镜像。复位测试使用完整的 128 项独立镜像验证该位置的读取。

IFU 继承 IROM 的加载语义：相对文件路径以仿真运行目录为基准，未覆盖的条目和 None 配置的指令内容未指定。当前 `$readmemh` 位于 `ifndef SYNTHESIS` 块内，FPGA/ASIC 的程序固化流程尚未实现或验证。本轮未修改 IROM。

## 6. 验证结果

验证日期：2026-10-04。工具链为 JDK 17、sbt 1.10.7、Scala 2.13.14、Chisel 3.6.1、chiseltest 0.6.2；功能仿真采用 Treadle 后端。

```bash
./scripts/sbt-local.sh test \
  'runMain riscvsingle.GenerateIFU' \
  'runMain riscvsingle.GenerateIFU 128 generated/ifu128 programs/riscvtest.memfile 0x100'
```

**实际结果：IFU 新增 6 项测试全部通过；全项目 10 个套件、63 项测试全部通过。默认及 128 项、复位地址 0x100 的 Verilog 均已生成。**

| 验证内容 | 实际覆盖 |
| --- | --- |
| 顺序取指 | 依次检查书中 21 条机器码、当前 PC 及 PCPlus4；PCSrc=0 时忽略改变的 IEUAdr；下一项读取填充 NOP。 |
| 下一 PC 选择与地址边界 | 输入变化不立即更新 PC，上升沿采样最终目标；检查目标 0x18、未对齐目标 3、高位目标 0x80000100 及 0xFFFFFFFC 后的顺序回绕。 |
| 默认/非零同步复位 | 两项测试分别使用 resetVector=0 和 0x100；检查置位而无上升沿时 PC/PCPlus4/Instr 保持，未跨越上升沿的短脉冲无效，上升沿复位优先于跳转，保持期间不推进，释放后按上升沿推进；完整 128 项镜像验证对应指令。 |
| 与 IEU 联调 | 硬件 IFU 的 PC、PCPlus4、Instr 驱动生产 IEU，IEU 的 PCSrc/IEUAdr 反馈驱动 IFU。检查 19 条实际执行指令及控制输出，地址 96 写入 7、地址 100 写入 25；进入 0x50 循环后再运行三个周期确认 PC 保持。 |
| 生成接口与层次 | 检查全部 7 个顶层端口和位宽，所有状态更新事件仅为 `posedge clk`，以及配置的复位常量、IROM 实例名。 |

`IFUHarness` 仅为 chiseltest 提供 Module 顶层。`IFUIEUHarness` 为测试辅助连接，PC 和指令存储器已经由硬件实现，数据内存仍由 Scala 模型提供；生产 LSU 与 RiscvSingle 尚待实现。未进行 FPGA 实现或时序分析，ChiselStage 弃用提示与此前一致。

## 7. 核验停点

本轮到此停止，IFU 及配套资料保留在工作区等待核验。收到修改意见时，仅修改当前模块及配套资料并重新验证；核验通过并允许继续后，下一模块为 LSU。
