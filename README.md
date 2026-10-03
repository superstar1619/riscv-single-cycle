# RISC-V 单周期 CPU（Chisel）

依据本地《RISC-V System-on-Chip Design, Edition 1》Code Example 2.15（书中第 61–65 页）分模块实现。第一版采用书中的简化 RV32 指令子集，沿用 IFU、IEU、LSU 层次。

## 当前交付

**Extend**、**Cmp**、**ALU**、**RegFile**、**Controller**、**Datapath**、**IEU**、**IROM**、**IFU** 已核验并提交，提交号分别为 `d6cf8cc`、`4b96fd7`、`aa69b21`、`24f885f`、`668d141`、`b77a5cd`、`da62b51`、`58232c4`、`1c1cb34`。第 10 个模块 **LSU（访存单元）** 已实现，等待核验。CPU 的 `XLEN` 仍限定为 32。

| 模块 | 报告 | 源码 | 测试 | 生成的 Verilog |
| --- | --- | --- | --- | --- |
| Extend | [01-extend.md](docs/modules/01-extend.md) | [Extend.scala](src/main/scala/riscvsingle/ieu/Extend.scala) | [ExtendSpec.scala](src/test/scala/riscvsingle/ieu/ExtendSpec.scala) | [32 位](generated/extend/Extend.v)、[64 位输出](generated/extend64/Extend.v) |
| Cmp | [02-cmp.md](docs/modules/02-cmp.md) | [Cmp.scala](src/main/scala/riscvsingle/ieu/Cmp.scala) | [CmpSpec.scala](src/test/scala/riscvsingle/ieu/CmpSpec.scala) | [32 位](generated/cmp/Cmp.v)、[64 位](generated/cmp64/Cmp.v) |
| ALU | [03-alu.md](docs/modules/03-alu.md) | [ALU.scala](src/main/scala/riscvsingle/ieu/ALU.scala) | [ALUSpec.scala](src/test/scala/riscvsingle/ieu/ALUSpec.scala) | [32 位](generated/alu/ALU.v)、[64 位](generated/alu64/ALU.v) |
| RegFile | [04-regfile.md](docs/modules/04-regfile.md) | [RegFile.scala](src/main/scala/riscvsingle/ieu/RegFile.scala) | [RegFileSpec.scala](src/test/scala/riscvsingle/ieu/RegFileSpec.scala) | [32 位](generated/regfile/RegFile.v)、[64 位](generated/regfile64/RegFile.v) |
| Controller | [05-controller.md](docs/modules/05-controller.md) | [Controller.scala](src/main/scala/riscvsingle/ieu/Controller.scala) | [ControllerSpec.scala](src/test/scala/riscvsingle/ieu/ControllerSpec.scala) | [Controller.v](generated/controller/Controller.v) |
| Datapath | [06-datapath.md](docs/modules/06-datapath.md) | [Datapath.scala](src/main/scala/riscvsingle/ieu/Datapath.scala) | [DatapathSpec.scala](src/test/scala/riscvsingle/ieu/DatapathSpec.scala) | [Datapath.v](generated/datapath/Datapath.v) |
| IEU | [07-ieu.md](docs/modules/07-ieu.md) | [IEU.scala](src/main/scala/riscvsingle/ieu/IEU.scala) | [IEUSpec.scala](src/test/scala/riscvsingle/ieu/IEUSpec.scala) | [IEU.v](generated/ieu/IEU.v) |
| IROM | [08-irom.md](docs/modules/08-irom.md) | [IROM.scala](src/main/scala/riscvsingle/ifu/IROM.scala) | [IROMSpec.scala](src/test/scala/riscvsingle/ifu/IROMSpec.scala) | [64 项](generated/irom/IROM.v)、[128 项](generated/irom128/IROM.v) |
| IFU | [09-ifu.md](docs/modules/09-ifu.md) | [IFU.scala](src/main/scala/riscvsingle/ifu/IFU.scala) | [IFUSpec.scala](src/test/scala/riscvsingle/ifu/IFUSpec.scala) | [默认](generated/ifu/IFU.v)、[128 项、复位地址 0x100](generated/ifu128/IFU.v) |
| LSU | [10-lsu.md](docs/modules/10-lsu.md) | [LSU.scala](src/main/scala/riscvsingle/lsu/LSU.scala) | [LSUSpec.scala](src/test/scala/riscvsingle/lsu/LSUSpec.scala) | [64 项](generated/lsu/LSU.v)、[128 项](generated/lsu128/LSU.v) |

[统一配置](src/main/scala/riscvsingle/config/CpuConfig.scala) 提供 CPU 结构参数。Extend、Cmp、ALU、RegFile 可单独配置数据位宽，RegFile 还可配置寄存器数量；Controller 的端口位宽遵循固定的 RISC-V 指令字段和控制编码。Datapath、IEU 接受 `CpuConfig`，将 `xlen` 传递给端口与子模块，当前限定为 RV32 和 32 项寄存器。

RegFile 默认使用完整的 32 项 `Reg(Vec(...))`，下标直接对应寄存器编号。高有效同步 `reset` 仅将 `rf(0)` 清零，其他寄存器保持；正常写入跳过 0 号寄存器。读写编号均不减一。

IROM 接受同一配置，以 `imemDepth` 设置 32 位指令字数量，以 `instructionInitFile` 设置十六进制初始化文件。默认 `CpuConfig()` 不初始化 ROM；生成入口默认加载 [书中程序镜像](programs/riscvtest.memfile)，包含 21 条程序指令和 43 条填充 NOP。当前生成的 `$readmemh` 位于仿真初始化块中，FPGA 初始化需在后续实现流程中另行接入。

IFU 接受统一配置，用 `resetVector` 设置 PC 的复位字节地址，并将配置传给 IROM。项目统一使用高有效同步复位：reset=1 的时钟上升沿将 PC 加载为 resetVector，并将 RegFile 的 x0 清零；reset 电平变化本身不更新寄存器。正常时钟上升沿选择 PCPlus4 或 IEUAdr 更新 PC。

LSU 以 `dmemDepth` 配置 32 位数据字数量，组合读取、在 MemWrite=1 的上升沿写入。按书中接口不设 RAM 复位或初始化；首次写入前的内容未指定，读写地址低两位及超出容量的高位均忽略。

**当前停在 LSU 核验阶段；收到明确核验通过并允许继续的回复后，才实施 RiscvSingle。**

## 构建与验证

版本：JDK 17、sbt 1.10.7、Scala 2.13.14、Chisel 3.6.1、chiseltest 0.6.2。

在本项目目录执行；已有 Java/sbt 的环境使用：

```bash
make test
make generate
make generate WIDTH=64 TARGET_DIR=generated/extend64
make generate-cmp
make generate-cmp WIDTH=64 CMP_TARGET_DIR=generated/cmp64
make generate-alu
make generate-alu WIDTH=64 ALU_TARGET_DIR=generated/alu64
make generate-regfile
make generate-regfile WIDTH=64 REGFILE_TARGET_DIR=generated/regfile64
make generate-controller
make generate-datapath
make generate-ieu
make generate-irom
make generate-irom IMEM_DEPTH=128 IROM_TARGET_DIR=generated/irom128
make generate-ifu
make generate-ifu IMEM_DEPTH=128 IFU_TARGET_DIR=generated/ifu128 RESET_VECTOR=0x100
make generate-lsu
make generate-lsu DMEM_DEPTH=128 LSU_TARGET_DIR=generated/lsu128
```

当前工作区的工具链位于 `../../.tools/chisel`，尚未加入 PATH，可使用：

```bash
make test SBT=./scripts/sbt-local.sh
make generate SBT=./scripts/sbt-local.sh
make generate SBT=./scripts/sbt-local.sh WIDTH=64 TARGET_DIR=generated/extend64
make generate-cmp SBT=./scripts/sbt-local.sh
make generate-cmp SBT=./scripts/sbt-local.sh WIDTH=64 CMP_TARGET_DIR=generated/cmp64
make generate-alu SBT=./scripts/sbt-local.sh
make generate-alu SBT=./scripts/sbt-local.sh WIDTH=64 ALU_TARGET_DIR=generated/alu64
make generate-regfile SBT=./scripts/sbt-local.sh
make generate-regfile SBT=./scripts/sbt-local.sh WIDTH=64 REGFILE_TARGET_DIR=generated/regfile64
make generate-controller SBT=./scripts/sbt-local.sh
make generate-datapath SBT=./scripts/sbt-local.sh
make generate-ieu SBT=./scripts/sbt-local.sh
make generate-irom SBT=./scripts/sbt-local.sh
make generate-irom SBT=./scripts/sbt-local.sh IMEM_DEPTH=128 IROM_TARGET_DIR=generated/irom128
make generate-ifu SBT=./scripts/sbt-local.sh
make generate-ifu SBT=./scripts/sbt-local.sh IMEM_DEPTH=128 IFU_TARGET_DIR=generated/ifu128 RESET_VECTOR=0x100
make generate-lsu SBT=./scripts/sbt-local.sh
make generate-lsu SBT=./scripts/sbt-local.sh DMEM_DEPTH=128 LSU_TARGET_DIR=generated/lsu128
```

`scripts/sbt-local.sh` 读取共享 JDK 和 sbt，将共享依赖缓存复制到本项目 `.cache/chisel` 后使用，避免修改共享工具目录。可以通过 `CHISEL_TOOLCHAIN_DIR` 和 `CHISEL_CACHE_DIR` 指定绝对路径。脚本关闭 sbt 构建服务器，并允许在无法创建启动套接字时继续批处理。

`sbt run` 默认生成 Extend。也可以使用 `runMain` 指定 `riscvsingle.GenerateExtend`、`riscvsingle.GenerateCmp`、`riscvsingle.GenerateALU`、`riscvsingle.GenerateRegFile`、`riscvsingle.GenerateController`、`riscvsingle.GenerateDatapath` 或 `riscvsingle.GenerateIEU`。例如 `sbt "runMain riscvsingle.GenerateRegFile 64 generated/regfile64 32"`；RegFile 的第三个可选参数为寄存器数量，Makefile 对应 `REGISTER_COUNT`，默认 32。Controller、Datapath、IEU 各自接受一个可选的输出目录参数，如 `sbt "runMain riscvsingle.GenerateIEU generated/ieu"`，Makefile 对应 `IEU_TARGET_DIR`。`make clean` 调用 `sbt clean`，保留已生成的 Verilog 和模块报告。

IROM 使用 `sbt "runMain riscvsingle.GenerateIROM 64 generated/irom programs/riscvtest.memfile"`。三个可选参数依次为深度、输出目录、初始化文件，Makefile 分别对应 `IMEM_DEPTH`、`IROM_TARGET_DIR`、`INSTRUCTION_INIT_FILE`；将文件设为 `-` 表示不初始化。相对文件路径以仿真运行目录为基准；在项目根目录运行可直接使用默认镜像。默认镜像为 64 项，128 项生成示例的后 64 项未初始化；如需读取全部条目，提供与深度匹配的镜像。

IFU 使用 `sbt "runMain riscvsingle.GenerateIFU 64 generated/ifu programs/riscvtest.memfile 0"`。四个可选参数依次为深度、输出目录、初始化文件、复位字节地址，Makefile 对应 `IMEM_DEPTH`、`IFU_TARGET_DIR`、`INSTRUCTION_INIT_FILE`、`RESET_VECTOR`。复位地址支持十进制和 `0x`/`0X` 十六进制。128 项、复位地址 0x100 的生成示例用于展示配置；默认镜像在该地址没有初始化指令，运行时需提供覆盖该地址的镜像。

LSU 使用 `sbt "runMain riscvsingle.GenerateLSU 64 generated/lsu"`。两个可选参数依次为数据字深度、输出目录，Makefile 对应 `DMEM_DEPTH`、`LSU_TARGET_DIR`。测试辅助顶层已将 IFU、IEU、LSU 接成硬件执行链路，书中程序运行后直接读回 RAM 确认地址 100 保存 25；生产 CPU 顶层将在下一轮实现。

## 配置与目录

`CpuConfig` 默认值为 `xlen=32`、`imemDepth=64`、`dmemDepth=64`、`resetVector=0`、`instructionInitFile=None`。存储深度以 32 位字计，复位地址以字节计。配置类负责 elaboration 前的参数检查。IEU 将配置传给 Datapath，当前仅消费 `xlen`；IROM 消费 `imemDepth` 和 `instructionInitFile`，地址宽度采用 `xlen`；IFU 使用 `xlen`、`resetVector` 并将配置传给 IROM；LSU 使用 `xlen` 和 `dmemDepth`。

`src/main/scala/riscvsingle/ieu` 放置执行单元相关模块，`ifu` 放置取指相关模块，`lsu` 放置访存相关模块，`config` 放置共享配置，`src/test/scala` 放置测试，`docs/modules` 放置每轮核验报告，`generated` 放置实际生成的硬件文件，`programs` 放置指令初始化镜像。

实施顺序：`Extend → Cmp → ALU → RegFile → Controller → Datapath → IEU → IROM → IFU → LSU → RiscvSingle`。每轮仅完成一个硬件模块及其测试、报告，然后停止等待核验。
