# RISC-V 单周期 CPU（Chisel）

依据本地《RISC-V System-on-Chip Design, Edition 1》Code Example 2.15（书中第 61–65 页）分模块实现。第一版采用书中的简化 RV32 指令子集，沿用 IFU、IEU、LSU 层次。

## 当前交付

已完成第 1 个模块 **Extend（立即数扩展）**，支持 I/S/B/J 格式，默认输出 32 位，可配置为更宽的符号扩展结果。CPU 的 `XLEN` 仍限定为 32。

- [模块报告](docs/modules/01-extend.md)：端口、功能、内部信号、参数及验证结果。
- [模块源码](src/main/scala/riscvsingle/ieu/Extend.scala)。
- [统一配置](src/main/scala/riscvsingle/config/CpuConfig.scala)。
- [模块测试](src/test/scala/riscvsingle/ieu/ExtendSpec.scala)。
- [32 位 Verilog](generated/extend/Extend.v) 和 [64 位 Verilog](generated/extend64/Extend.v)。

**当前停在 Extend 核验阶段；收到明确核验通过并允许继续的回复后，才实施 Cmp。**

## 构建与验证

版本：JDK 17、sbt 1.10.7、Scala 2.13.14、Chisel 3.6.1、chiseltest 0.6.2。

在本项目目录执行；已有 Java/sbt 的环境使用：

```bash
make test
make generate
make generate WIDTH=64 TARGET_DIR=generated/extend64
```

当前工作区的工具链位于 `../../.tools/chisel`，尚未加入 PATH，可使用：

```bash
make test SBT=./scripts/sbt-local.sh
make generate SBT=./scripts/sbt-local.sh
make generate SBT=./scripts/sbt-local.sh WIDTH=64 TARGET_DIR=generated/extend64
```

`scripts/sbt-local.sh` 读取共享 JDK 和 sbt，将共享依赖缓存复制到本项目 `.cache/chisel` 后使用，避免修改共享工具目录。可以通过 `CHISEL_TOOLCHAIN_DIR` 和 `CHISEL_CACHE_DIR` 指定绝对路径。脚本关闭 sbt 构建服务器，并允许在无法创建启动套接字时继续批处理。

也可以直接执行 `sbt run`，或 `sbt "runMain riscvsingle.GenerateExtend 64 generated/extend64"`。`make clean` 调用 `sbt clean`，保留已生成的 Verilog 和模块报告。

## 配置与目录

`CpuConfig` 默认值为 `xlen=32`、`imemDepth=64`、`dmemDepth=64`、`resetVector=0`、`instructionInitFile=None`。存储深度以 32 位字计，复位地址以字节计；初始化文件将在 IROM 模块实现时接入。配置类负责 elaboration 前的参数检查，目前不实例化存储器或 CPU。

`src/main/scala/riscvsingle/ieu` 放置执行单元相关模块，`config` 放置共享配置，`src/test/scala` 放置测试，`docs/modules` 放置每轮核验报告，`generated` 放置实际生成的硬件文件。

实施顺序：`Extend → Cmp → ALU → RegFile → Controller → Datapath → IEU → IROM → IFU → LSU → RiscvSingle`。每轮仅完成一个硬件模块及其测试、报告，然后停止等待核验。
