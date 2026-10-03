# RISC-V 单周期 CPU（Chisel）

依据本地《RISC-V System-on-Chip Design, Edition 1》Code Example 2.15（书中第 61–65 页）分模块实现。第一版采用书中的简化 RV32 指令子集，沿用 IFU、IEU、LSU 层次。

## 当前交付

**Extend**、**Cmp**、**ALU**、**RegFile**、**Controller**、**Datapath**、**IEU** 已核验并提交，提交号分别为 `d6cf8cc`、`4b96fd7`、`aa69b21`、`24f885f`、`668d141`、`b77a5cd`、`da62b51`。第 8 个模块 **IROM（指令只读存储器）** 已实现，等待核验。CPU 的 `XLEN` 仍限定为 32。

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

[统一配置](src/main/scala/riscvsingle/config/CpuConfig.scala) 提供 CPU 结构参数。Extend、Cmp、ALU、RegFile 可单独配置数据位宽，RegFile 还可配置寄存器数量；Controller 的端口位宽遵循固定的 RISC-V 指令字段和控制编码。Datapath、IEU 接受 `CpuConfig`，将 `xlen` 传递给端口与子模块，当前限定为 RV32 和 32 项寄存器。

RegFile 默认使用完整的 32 项 `Reg(Vec(...))`，下标直接对应寄存器编号。高有效同步 `reset` 仅将 `rf(0)` 清零，其他寄存器保持；正常写入跳过 0 号寄存器。读写编号均不减一。

IROM 接受同一配置，以 `imemDepth` 设置 32 位指令字数量，以 `instructionInitFile` 设置十六进制初始化文件。默认 `CpuConfig()` 不初始化 ROM；生成入口默认加载 [书中程序镜像](programs/riscvtest.memfile)，包含 21 条程序指令和 43 条填充 NOP。当前生成的 `$readmemh` 位于仿真初始化块中，FPGA 初始化需在后续实现流程中另行接入。

**当前停在 IROM 核验阶段；收到明确核验通过并允许继续的回复后，才实施 IFU。**

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
```

`scripts/sbt-local.sh` 读取共享 JDK 和 sbt，将共享依赖缓存复制到本项目 `.cache/chisel` 后使用，避免修改共享工具目录。可以通过 `CHISEL_TOOLCHAIN_DIR` 和 `CHISEL_CACHE_DIR` 指定绝对路径。脚本关闭 sbt 构建服务器，并允许在无法创建启动套接字时继续批处理。

`sbt run` 默认生成 Extend。也可以使用 `runMain` 指定 `riscvsingle.GenerateExtend`、`riscvsingle.GenerateCmp`、`riscvsingle.GenerateALU`、`riscvsingle.GenerateRegFile`、`riscvsingle.GenerateController`、`riscvsingle.GenerateDatapath` 或 `riscvsingle.GenerateIEU`。例如 `sbt "runMain riscvsingle.GenerateRegFile 64 generated/regfile64 32"`；RegFile 的第三个可选参数为寄存器数量，Makefile 对应 `REGISTER_COUNT`，默认 32。Controller、Datapath、IEU 各自接受一个可选的输出目录参数，如 `sbt "runMain riscvsingle.GenerateIEU generated/ieu"`，Makefile 对应 `IEU_TARGET_DIR`。`make clean` 调用 `sbt clean`，保留已生成的 Verilog 和模块报告。

IROM 使用 `sbt "runMain riscvsingle.GenerateIROM 64 generated/irom programs/riscvtest.memfile"`。三个可选参数依次为深度、输出目录、初始化文件，Makefile 分别对应 `IMEM_DEPTH`、`IROM_TARGET_DIR`、`INSTRUCTION_INIT_FILE`；将文件设为 `-` 表示不初始化。相对文件路径以仿真运行目录为基准；在项目根目录运行可直接使用默认镜像。默认镜像为 64 项，128 项生成示例的后 64 项未初始化；如需读取全部条目，提供与深度匹配的镜像。

## 配置与目录

`CpuConfig` 默认值为 `xlen=32`、`imemDepth=64`、`dmemDepth=64`、`resetVector=0`、`instructionInitFile=None`。存储深度以 32 位字计，复位地址以字节计。配置类负责 elaboration 前的参数检查。IEU 将配置传给 Datapath，当前仅消费 `xlen`；IROM 消费 `imemDepth` 和 `instructionInitFile`，地址宽度采用 `xlen`；数据存储深度、复位地址将在后续模块接入。

`src/main/scala/riscvsingle/ieu` 放置执行单元相关模块，`ifu` 放置取指相关模块，`config` 放置共享配置，`src/test/scala` 放置测试，`docs/modules` 放置每轮核验报告，`generated` 放置实际生成的硬件文件，`programs` 放置指令初始化镜像。

实施顺序：`Extend → Cmp → ALU → RegFile → Controller → Datapath → IEU → IROM → IFU → LSU → RiscvSingle`。每轮仅完成一个硬件模块及其测试、报告，然后停止等待核验。
