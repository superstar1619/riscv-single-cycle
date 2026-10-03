# 模块 10：LSU 访存单元核验报告

## 1. 依据与交付状态

对应《RISC-V System-on-Chip Design, Edition 1》Code Example 2.15 的 `lsu`，书中第 65 页。原书定义 64 项、每项 32 位的 RAM，以 IEUAdr[7:2] 组合读出，并在 clk 上升沿、MemWrite=1 时写入 WriteData。

上一模块 IFU 及统一同步复位修改已按用户指示提交，提交号为 `1c1cb34`。本轮交付 **LSU 一个硬件模块**及其生成入口、测试和报告。生产 CPU 顶层 RiscvSingle 留待下一轮实现。

状态：**LSU 测试通过，尚未提交，等待用户核验。**

| 交付内容 | 路径 |
| --- | --- |
| 模块及端口定义 | [LSU.scala](../../src/main/scala/riscvsingle/lsu/LSU.scala) |
| Verilog 生成入口 | [GenerateLSU.scala](../../src/main/scala/riscvsingle/GenerateLSU.scala) |
| 模块测试 | [LSUSpec.scala](../../src/test/scala/riscvsingle/lsu/LSUSpec.scala) |
| 生成硬件 | [64 项 LSU.v](../../generated/lsu/LSU.v)、[128 项 LSU.v](../../generated/lsu128/LSU.v) |

## 2. 端口与配置

模块类为 `riscvsingle.lsu.LSU`，继承 `RawModule`，端口 Bundle 为 `LSUIO`。显式声明 clk，其余端口具有 `io_` 前缀。共有 4 个输入和 1 个输出，沿用原书的无复位接口。

| Chisel 端口 | Verilog 端口 | 方向 | 类型 / 位宽 | 功能 |
| --- | --- | --- | --- | --- |
| `clk` | `clk` | 输入 | `Clock` / 1 | RAM 写入时钟，上升沿触发。 |
| `io.MemWrite` | `io_MemWrite` | 输入 | `Bool` / 1 | 为 1 时，在 clk 上升沿写入当前地址的数据字。 |
| `io.IEUAdr` | `io_IEUAdr` | 输入 | `UInt(config.xlen.W)` / 32 | IEU 提供的读写字节地址。 |
| `io.WriteData` | `io_WriteData` | 输入 | `UInt(config.xlen.W)` / 32 | 上升沿写入 RAM 的完整数据字。 |
| `io.ReadData` | `io_ReadData` | 输出 | `UInt(config.xlen.W)` / 32 | 当前地址对应的 RAM 数据，经组合逻辑输出，供 lw 写回。 |

构造参数为 `config: CpuConfig = CpuConfig()`。本轮未修改统一配置类。

| 配置项 | 默认值 | 在 LSU 中的作用 |
| --- | --- | --- |
| `xlen` | 32 | 地址和数据宽度；当前配置要求 RV32，每个存储字为 32 位。 |
| `dmemDepth` | 64 | RAM 的数据字数量，要求至少 2 的二次幂且容量不超过地址空间。 |
| `imemDepth`、`resetVector`、`instructionInitFile` | 64、0、None | LSU 不使用，分别由取指相关模块消费。 |

## 3. 功能、地址映射与时序

使用 `Mem(dmemDepth, UInt(config.xlen.W))` 保存数据，定义 `indexWidth = log2Ceil(dmemDepth)`。组合读与时钟更新关系为：

```text
WordIndex = IEUAdr[indexWidth + 1 : 2]
ReadData  = RAM[WordIndex]
clk 上升沿：若 MemWrite=1，则 RAM[WordIndex] := WriteData
```

| 深度 | 容量 | 索引宽度 | 地址字段 |
| --- | --- | --- | --- |
| 2 项 | 8 字节 | 1 位 | IEUAdr[2:2]。 |
| 64 项 | 256 字节 | 6 位 | IEUAdr[7:2]，与原书一致。 |
| 128 项 | 512 字节 | 7 位 | IEUAdr[8:2]。 |

地址低两位被忽略，同一字内四个字节地址访问同一数据字。索引以上的高位也被忽略，地址按 RAM 容量回绕。例如默认容量下，0、3、0x100、0x80000000 均访问 RAM[0]；0xFC 访问 RAM[63]。读写共用同一索引，因此通过任一别名写入后，可以通过其他别名读到相同内容。

MemWrite=0 时，时钟上升沿保持所有数据。MemWrite=1 时，写地址和数据按上升沿时的输入采样；上升沿前更改地址、数据或使能不会提前写入。读口没有等待周期，也没有写入旁路：写入上升沿前读取旧值，上升沿完成写入后组合读口反映新值。

LSU 仅处理完整的 32 位字，不提供字节使能、半字访问、符号扩展、对齐异常或地址范围异常。当前简化指令子集使用 lw/sw；其他访问宽度不是本模块的已实现功能。

## 4. 初始化与复位行为

沿用原书定义，RAM 无复位输入、无初始化文件和固定初值保证。首次写入前的数据未指定；测试通过正常写口建立已知内容后才检查对应读取值。生成器的仿真随机初始化模板不构成零初始化或其他已知数据保证。

项目的 PC 和 RegFile 复位继续采用高有效同步复位。LSU 自身没有需要响应 reset 的逻辑；RAM 仅由 clk 上升沿写入，没有独立复位事件。上层 reset 不会清空 RAM 或自动关闭 LSU 写口，MemWrite=1 的每个 clk 上升沿仍会执行写入。

## 5. 内部信号名称

| Chisel 名称 | 类型 / 位宽 | 功能 | 当前 Verilog 对应位置 |
| --- | --- | --- | --- |
| `RAM` | `Mem(dmemDepth, UInt(config.xlen.W))` | 数据存储数组。 | `reg [31:0] RAM [0:63]` 或 `[0:127]`。 |
| `WordIndex` | `Wire(UInt(indexWidth.W))` | 由字节地址提取读写字索引。 | 别名优化合并为 `RAM_io_ReadData_MPORT_addr` 和 `RAM_MPORT_addr` 的赋值。 |
| `indexWidth` | elaboration 时的 Scala Int | 计算索引宽度和地址切片，不是硬件信号。 | 不生成独立信号。 |

| 自动生成的辅助名称 | 位宽 | 功能 |
| --- | --- | --- |
| `RAM_io_ReadData_MPORT_addr` | 6 / 7 | 64 / 128 项 RAM 的组合读地址。 |
| `RAM_io_ReadData_MPORT_data` | 32 | RAM 读出的数据，驱动 io_ReadData。 |
| `RAM_io_ReadData_MPORT_en` | 1 | 固定为 1，读口始终使能。 |
| `RAM_MPORT_addr` | 6 / 7 | 与读口相同的字索引，供时钟写入。 |
| `RAM_MPORT_data` | 32 | 来自 io_WriteData 的写入值。 |
| `RAM_MPORT_en` | 1 | 来自 io_MemWrite 的写使能。 |
| `RAM_MPORT_mask` | 1 | 固定为 1，整字写入。 |
| `_RAND_0`、`initvar` | 32 / Verilog integer | RANDOMIZE_MEM_INIT 宏下的仿真初始化辅助变量。 |

生产 LSU 没有子模块实例。生成文件只有一个 LSU 定义，状态更新块仅由 `posedge clk` 触发。未使用 `dontTouch` 强制保留别名，辅助名称可能随编译或综合优化变化。

## 6. 与原书的差异及验证结果

原书固定 64 项，本实现通过 `dmemDepth` 配置；端口增加 `io_` 前缀。默认地址切片、组合读、上升沿写入、无 RAM 复位/初始化等行为与原书一致，本轮未新增生产 CPU 顶层。

验证日期：2026-10-04。工具链为 JDK 17、sbt 1.10.7、Scala 2.13.14、Chisel 3.6.1、chiseltest 0.6.2；功能仿真采用 Treadle 后端。

```bash
./scripts/sbt-local.sh test \
  'runMain riscvsingle.GenerateLSU' \
  'runMain riscvsingle.GenerateLSU 128 generated/lsu128'
```

**实际结果：LSU 新增 7 项测试全部通过；全项目 11 个套件、70 项测试全部通过。64 项、128 项 Verilog 均已生成。**

| 验证内容 | 实际覆盖 |
| --- | --- |
| 三种容量、组合读取和地址别名 | 2、64、128 项分别通过写口填满；覆盖全零、全 1 和随机数据，每项读取四种字内偏移及四种高位别名，读阶段不推进时钟，共 3104 个地址检查。 |
| 写入时序与使能 | 上升沿前读取旧值；变化后的最终地址/数据在上升沿被采样；MemWrite=0 时保持；通过未对齐、高位别名写入后检查原索引内容。 |
| 连续随机操作 | 固定种子 0x65，初始化全部 64 项后执行 300 个周期的随机地址、数据、使能；独立数组模型使用除法/取模确定索引，检查上升沿前后读数，最后检查全部条目。 |
| 硬件执行链路联调 | 测试顶层连接生产 IFU、IEU、LSU，执行 Code Example 2.16，不使用 Scala PC 或内存模型。检查 19 条实际执行指令的 PC 与控制输出，lw 从实际 LSU 读到 7，最终进入 0x50 循环并再运行三周期。 |
| 实际 RAM 内容 | 执行结束、时钟停止后，通过测试辅助读探针直接读取 RAM，确认地址 96 保存 7、地址 100 保存 25。 |
| 生成接口 | 对 64、128 项检查全部 5 个顶层端口和位宽、数组深度、仅 posedge clk 的写入事件以及无文件初始化。 |

`LSUHarness` 仅为 chiseltest 提供 Module 顶层。`ExecutionWithLSUHarness` 是测试辅助连接，程序执行时全部状态由生产模块提供；ProbeEnable/ProbeAdr 只用于执行结束后的 RAM 读回，未添加生产 LSU 调试端口，也未交付生产 RiscvSingle 模块。未进行 FPGA 实现或时序分析，已有 IROM 的仿真初始化限制继续适用。

单独生成可执行 `make generate-lsu SBT=./scripts/sbt-local.sh`。生成入口两个可选参数依次为 `dmemDepth`、`targetDir`，默认 64、`generated/lsu`；Makefile 对应 `DMEM_DEPTH`、`LSU_TARGET_DIR`。

## 7. 核验停点

本轮到此停止，LSU 及配套资料保留在工作区等待核验。收到修改意见时，仅修改当前模块及配套资料并重新验证；核验通过并允许继续后，下一模块为 RiscvSingle。
