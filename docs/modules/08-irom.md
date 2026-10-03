# 模块 08：IROM 指令只读存储器核验报告

## 1. 依据与交付状态

对应《RISC-V System-on-Chip Design, Edition 1》Code Example 2.15 的 `irom`，书中第 62 页。原书定义 64 项、每项 32 位的 ROM，通过 `$readmemh("riscvtest.memfile", ROM)` 初始化，以 `ROM[a[7:2]]` 异步输出指令。

上一模块 IEU 已按用户指示提交，提交号为 `da62b51`。本轮交付 **IROM 一个硬件模块**及其生成入口、测试、指令镜像和报告。IFU 留待下一轮实现。

状态：**IROM 测试通过，尚未提交，等待用户核验。**

| 交付内容 | 路径 |
| --- | --- |
| 模块及端口定义 | [IROM.scala](../../src/main/scala/riscvsingle/ifu/IROM.scala) |
| Verilog 生成入口 | [GenerateIROM.scala](../../src/main/scala/riscvsingle/GenerateIROM.scala) |
| 模块测试 | [IROMSpec.scala](../../src/test/scala/riscvsingle/ifu/IROMSpec.scala) |
| 生成的硬件 | [64 项 IROM.v](../../generated/irom/IROM.v)、[128 项 IROM.v](../../generated/irom128/IROM.v) |
| Code Example 2.16 程序镜像 | [riscvtest.memfile](../../programs/riscvtest.memfile) |

## 2. 端口与配置

模块类为 `riscvsingle.ifu.IROM`，继承 `RawModule`，端口 Bundle 为 `IROMIO`。只有地址输入和指令输出，无时钟、复位或写入端口。

| Chisel 端口 | Verilog 端口 | 方向 | 类型 / 位宽 | 功能 |
| --- | --- | --- | --- | --- |
| `io.a` | `io_a` | 输入 | `UInt(config.xlen.W)`，当前 32 位 | 指令字节地址。 |
| `io.rd` | `io_rd` | 输出 | `UInt(32.W)` | 对应存储字的完整 32 位指令，地址变化后经组合逻辑更新。 |

构造参数为 `config: CpuConfig = CpuConfig()`，本轮未修改配置类。

| 配置项 | 默认值 | 在 IROM 中的作用 |
| --- | --- | --- |
| `xlen` | 32 | 地址位宽；现有配置要求为 32，指令字宽固定为 32 位。 |
| `imemDepth` | 64 | ROM 的指令字数量，要求为至少 2 的二次幂且容量不超过地址空间。 |
| `instructionInitFile` | `None` | `Some(path)` 使用十六进制文件初始化；`None` 不加载文件，ROM 内容未指定。 |
| `dmemDepth`、`resetVector` | 64、0 | 本模块不使用，由后续 LSU、IFU 消费。 |

默认构造与生成入口的初始化选择不同：`new IROM(CpuConfig())` 不加载文件；`GenerateIROM` 为方便复现书中程序，默认显式设置 `Some("programs/riscvtest.memfile")`。

## 3. 功能与地址映射

使用 `Mem(imemDepth, UInt(32.W))` 保存 ROM 内容，并使用零延迟读端口；没有任何写入逻辑。定义 `indexWidth = log2Ceil(imemDepth)`，索引为：

```text
WordIndex = a[indexWidth + 1 : 2]
rd = ROM[WordIndex]
```

| 深度 | 容量 | 索引宽度 | 使用的地址字段 |
| --- | --- | --- | --- |
| 2 项 | 8 字节 | 1 位 | a[2:2]。 |
| 64 项 | 256 字节 | 6 位 | a[7:2]，与原书一致。 |
| 128 项 | 512 字节 | 7 位 | a[8:2]。 |

地址低两位被忽略，同一字内四个字节地址返回同一条指令。索引以上的地址位同样被忽略，地址按 ROM 容量回绕。例如默认 64 项时，`0x00000000`、`0x00000003`、`0x00000100`、`0x80000000` 都读取 ROM[0]；`0x000000FC` 读取 ROM[63]。

模块未添加地址范围或对齐异常检查，沿用书中的字段截取行为。改变地址不需要时钟上升沿；本模块也不保存 PC、不计算 PCPlus4。

## 4. 初始化镜像与加载语义

`instructionInitFile=Some(path)` 时调用 Chisel 的 `loadMemoryFromFileInline`，当前生成 Verilog 直接包含 `$readmemh(path, ROM)`。初始化发生在仿真启动阶段，不通过 reset 触发；ROM 没有运行期写接口。

提供的 `programs/riscvtest.memfile` 每行一个 8 位十六进制数，表示一个 32 位指令字，不需要逐字节拆分。前 21 项逐条对应 Code Example 2.16 的机器码，覆盖地址 0x00–0x50；其余 43 项填充 `00000013`，即 `addi x0, x0, 0`（NOP），使默认 64 项镜像完整。

路径原样写入生成的 Verilog，相对路径以仿真运行目录为基准。项目的默认测试与命令均在项目根目录运行。Treadle 1.6.0 的加载器要求相对路径，测试生成的临时镜像也放在 `target/irom-test-images` 下并使用相对路径。

`None` 不提供程序，也不保证零初始化。文件未覆盖的条目同样没有已知值保证；128 项生成示例使用同一份 64 项镜像，因此后 64 项未初始化。使用小于 64 项的深度时应提供较小的镜像；文件内容与路径由运行仿真的工具加载，本模块未增加文件格式或长度的宿主端校验。

**当前 Chisel 3.6.1 生成的 `$readmemh` 位于 `ifndef SYNTHESIS` 块内，验证范围是仿真文件加载。** FPGA 或 ASIC 实现时需要另行接入目标工具的 ROM 初始化或固化流程；本轮未验证综合后的程序内容。

## 5. 内部信号名称

| Chisel 名称 | 类型 / 位宽 | 功能 | 当前 Verilog 对应名称 |
| --- | --- | --- | --- |
| `ROM` | `Mem(imemDepth, UInt(32.W))` | 存储 32 位指令字。 | `reg [31:0] ROM [0:63]` 或 `ROM [0:127]`。 |
| `WordIndex` | `Wire(UInt(indexWidth.W))` | 从字节地址提取 ROM 字索引。 | 别名合并为 `ROM_io_rd_MPORT_addr` 的赋值。 |
| `indexWidth` | elaboration 时的 Scala `Int` | 计算索引宽度和截取字段，不是硬件信号。 | 不生成独立信号。 |

| 自动生成的辅助名称 | 位宽 | 功能 |
| --- | --- | --- |
| `ROM_io_rd_MPORT_addr` | 6 / 7 位 | 64 / 128 项 ROM 的读地址。 |
| `ROM_io_rd_MPORT_data` | 32 位 | ROM 异步读出的指令，驱动 io_rd。 |
| `ROM_io_rd_MPORT_en` | 1 位 | 常量 1，读端口始终使能。 |
| `_GEN_0` | 1 位 | 常量 0，由读端口的低电平时钟表达式生成；不参与读数选择。 |
| `initvar` | Verilog integer | 生成器初始化模板的辅助变量，不是运行期 CPU 状态。 |

Chisel 的 Mem 读端口接口需要时钟表达式，本模块显式使用 `0.B.asClock`。由于读延迟为零，该表达式不会引入时序读逻辑或外部 clock 端口，生成的读取仍是连续赋值。未使用 `dontTouch` 强制保留别名或辅助线，名称可能随编译或综合优化变化。

IROM 无子模块实例；生成文件只有一个 IROM 模块定义。

## 6. 与原书的差异及验证结果

原书固定 64 项并固定文件名，本实现由统一配置指定深度和可选文件路径；默认生成镜像复现原书程序。端口增加 `io_` 前缀，字内地址忽略、高位地址回绕及异步读关系与原书一致。镜像新增 NOP 填充，仿真初始化代码的综合宏行为见第 4 节。

验证日期：2026-10-03。工具链为 JDK 17、sbt 1.10.7、Scala 2.13.14、Chisel 3.6.1、chiseltest 0.6.2；功能仿真采用 Treadle 后端。

```bash
./scripts/sbt-local.sh test \
  'runMain riscvsingle.GenerateIROM' \
  'runMain riscvsingle.GenerateIROM 128 generated/irom128 programs/riscvtest.memfile'
```

**实际结果：IROM 新增 6 项测试全部通过；全项目 9 个套件、57 项测试全部通过。64 项、128 项 Verilog 均已生成。**

| 验证内容 | 实际覆盖 |
| --- | --- |
| 三种存储深度 | 2、64、128 项分别加载完整镜像；随机访问每项，包含全零及全 1 指令字。 |
| 异步读与地址映射 | 每项覆盖四种字内偏移和四种高位别名；不推进时钟，共检查 3104 个地址，确认切换地址即可读出正确指令。 |
| 默认程序镜像 | 检查全部 64 项，确认前 21 条机器码和后 43 条 NOP。 |
| 与已有 IEU 联调 | 实际 IROM 输出直接驱动 IEU 的 Instr，Scala 提供 PC 和数据内存模型；检查 19 条实际执行指令的 PC、机器码、PCSrc/MemWrite。地址 96 写入 7，地址 100 写入 25，最终 PC 为 0x50。 |
| 生成接口及初始化 | 检查只有 32 位地址输入和 32 位指令输出、64 项数组、无上升沿读逻辑；分别检查 Some/None 是否包含对应的内联 `$readmemh`。 |

`IROMHarness` 为 chiseltest 提供 Module 顶层；`IROMIEUHarness` 仅连接当前 IROM 和已核验 IEU，不包含硬件 PC 状态或数据存储器。完整 CPU、FPGA 实现及时序分析留待后续模块。ChiselStage 弃用提示与此前一致，本轮未变更工具链。

单独生成可执行 `make generate-irom SBT=./scripts/sbt-local.sh`。生成入口三个可选参数依次为 `imemDepth`、`targetDir`、`instructionInitFile`，默认 `64`、`generated/irom`、`programs/riscvtest.memfile`；文件参数 `-` 表示 None。Makefile 分别使用 `IMEM_DEPTH`、`IROM_TARGET_DIR`、`INSTRUCTION_INIT_FILE`。

## 7. 核验停点

本轮到此停止，IROM 及配套资料保留在工作区等待核验。收到修改意见时，仅修改当前模块及配套资料并重新验证；核验通过并允许继续后，下一模块为 IFU。
