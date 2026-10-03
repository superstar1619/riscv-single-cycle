# RISC-V 单周期 CPU（Chisel）

依据本地《RISC-V System-on-Chip Design, Edition 1》Code Example 2.15（书中第 61–65 页）分模块实现。第一版采用书中的简化 RV32 指令子集，沿用 IFU、IEU、LSU 层次。

## 当前交付

**Extend**、**Cmp**、**ALU**、**RegFile** 已核验并提交，提交号分别为 `d6cf8cc`、`4b96fd7`、`aa69b21`、`24f885f`。第 5 个模块 **Controller（控制器）** 已实现，等待核验。CPU 的 `XLEN` 仍限定为 32。

| 模块 | 报告 | 源码 | 测试 | 生成的 Verilog |
| --- | --- | --- | --- | --- |
| Extend | [01-extend.md](docs/modules/01-extend.md) | [Extend.scala](src/main/scala/riscvsingle/ieu/Extend.scala) | [ExtendSpec.scala](src/test/scala/riscvsingle/ieu/ExtendSpec.scala) | [32 位](generated/extend/Extend.v)、[64 位输出](generated/extend64/Extend.v) |
| Cmp | [02-cmp.md](docs/modules/02-cmp.md) | [Cmp.scala](src/main/scala/riscvsingle/ieu/Cmp.scala) | [CmpSpec.scala](src/test/scala/riscvsingle/ieu/CmpSpec.scala) | [32 位](generated/cmp/Cmp.v)、[64 位](generated/cmp64/Cmp.v) |
| ALU | [03-alu.md](docs/modules/03-alu.md) | [ALU.scala](src/main/scala/riscvsingle/ieu/ALU.scala) | [ALUSpec.scala](src/test/scala/riscvsingle/ieu/ALUSpec.scala) | [32 位](generated/alu/ALU.v)、[64 位](generated/alu64/ALU.v) |
| RegFile | [04-regfile.md](docs/modules/04-regfile.md) | [RegFile.scala](src/main/scala/riscvsingle/ieu/RegFile.scala) | [RegFileSpec.scala](src/test/scala/riscvsingle/ieu/RegFileSpec.scala) | [32 位](generated/regfile/RegFile.v)、[64 位](generated/regfile64/RegFile.v) |
| Controller | [05-controller.md](docs/modules/05-controller.md) | [Controller.scala](src/main/scala/riscvsingle/ieu/Controller.scala) | [ControllerSpec.scala](src/test/scala/riscvsingle/ieu/ControllerSpec.scala) | [Controller.v](generated/controller/Controller.v) |

[统一配置](src/main/scala/riscvsingle/config/CpuConfig.scala) 提供 CPU 结构参数。Extend、Cmp、ALU、RegFile 可单独配置数据位宽，RegFile 还可配置寄存器数量；Controller 的端口位宽遵循固定的 RISC-V 指令字段和控制编码。

RegFile 默认使用完整的 32 项 `Reg(Vec(...))`，下标直接对应寄存器编号。高有效同步 `reset` 仅将 `rf(0)` 清零，其他寄存器保持；正常写入跳过 0 号寄存器。读写编号均不减一。

**当前停在 Controller 核验阶段；收到明确核验通过并允许继续的回复后，才实施 Datapath。**

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
```

`scripts/sbt-local.sh` 读取共享 JDK 和 sbt，将共享依赖缓存复制到本项目 `.cache/chisel` 后使用，避免修改共享工具目录。可以通过 `CHISEL_TOOLCHAIN_DIR` 和 `CHISEL_CACHE_DIR` 指定绝对路径。脚本关闭 sbt 构建服务器，并允许在无法创建启动套接字时继续批处理。

`sbt run` 默认生成 Extend。也可以使用 `runMain` 指定 `riscvsingle.GenerateExtend`、`riscvsingle.GenerateCmp`、`riscvsingle.GenerateALU`、`riscvsingle.GenerateRegFile` 或 `riscvsingle.GenerateController`。例如 `sbt "runMain riscvsingle.GenerateRegFile 64 generated/regfile64 32"`；RegFile 的第三个可选参数为寄存器数量，Makefile 对应 `REGISTER_COUNT`，默认 32。Controller 仅接受一个可选的输出目录参数，如 `sbt "runMain riscvsingle.GenerateController generated/controller"`。`make clean` 调用 `sbt clean`，保留已生成的 Verilog 和模块报告。

## 配置与目录

`CpuConfig` 默认值为 `xlen=32`、`imemDepth=64`、`dmemDepth=64`、`resetVector=0`、`instructionInitFile=None`。存储深度以 32 位字计，复位地址以字节计；初始化文件将在 IROM 模块实现时接入。配置类负责 elaboration 前的参数检查，目前不实例化存储器或 CPU。

`src/main/scala/riscvsingle/ieu` 放置执行单元相关模块，`config` 放置共享配置，`src/test/scala` 放置测试，`docs/modules` 放置每轮核验报告，`generated` 放置实际生成的硬件文件。

实施顺序：`Extend → Cmp → ALU → RegFile → Controller → Datapath → IEU → IROM → IFU → LSU → RiscvSingle`。每轮仅完成一个硬件模块及其测试、报告，然后停止等待核验。
