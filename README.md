# RISC-V 单周期 CPU（Chisel）

依据本地《RISC-V System-on-Chip Design, Edition 1》分模块实现，沿用 IFU、IEU、LSU 层次。第二章 Code Example 2.15 的简化 RV32 单周期 CPU 已完成；当前按第七章 §7.1、图 7.2 和表 7.1 逐轮完善 RV32 整数指令支持。

## 当前交付

**第七章第 10 轮：SubwordWrite。** 新增独立组合写数据复制模块，32 位支持 SB/SH/SW，独立 64 位增加 SD；校验完整 Funct3，非法编码输出零。源码、中文测试说明、两种宽度 RTL 和波形见 [SubwordWrite 核验报告](docs/modules/14-subwordwrite.md)。**用户已授权提交后继续第 11 轮 SubwordRead；下一轮交付后停止等待核验。** SwByteMask 与 SubwordWrite 尚未接入 LSU，整机仍采用原有字 RAM。

后续恢复执行请先读 [第七章进度与后续执行报告](docs/chapter7-progress.md)：包含前 10 轮交付状态、当前接口和整机边界、验证复现、剩余 6 轮验收与逐轮停点规则。

第 10 轮验证：SubwordWrite 的 12 项测试通过；全工程 15 个套件、133 项全部通过；现有两种 CPU 配置的六次 RTL 仿真全部 PASS。本次将第 9 轮 SwByteMask 一并提交；该模块是后续子字存储的字节写使能逻辑，予以保留。

本轮可读性更新：源码按三步展示复制与选择，测试显式列出功能编码和期望常量，报告用对照表说明 CVW 差异及波形各阶段。

第 8 轮 IEU 已提交为 `89b1e58`：导出 `MemRW={MemRead,MemWrite}` 与原始 `Funct3`，独立验证全部 37 类 RV32 指令；加载数据由外部提供，详见 [IEU 核验报告](docs/modules/07-ieu.md)。

第 7 轮 Datapath 已核验并按用户指示提交为 `ffd13e5`：完善 LUI/AUIPC 写回、JAL/JALR 链接值及 JALR 目标位 0 清零，详见 [Datapath 核验报告](docs/modules/06-datapath.md)。

第 6 轮 Controller 已核验并按用户指示提交为 `ffbb866`：完善 RV32 表 7.1 译码、六种分支、完整 Funct7 合法性检查，以及 Jump/MemRW 控制，详见 [Controller 核验报告](docs/modules/05-controller.md)。第 7 轮已接入 Jump 与替代结果写回。

第 5 轮 ALU 已核验并按用户指示提交为 `715b7b0`：补齐 XOR、SLTU、SLL、SRL、SRA 并接入教材漏斗 Shifter，保留独立加减地址输出，详见 [ALU 核验报告](docs/modules/03-alu.md)。第 6 轮已完善其指令控制。

第 4 轮 Shifter 已核验并按用户指示提交为 `d9d7861`：教材漏斗结构支持 SLL/SRL/SRA，独立模块验证 32/64 位，详见 [Shifter 核验报告](docs/modules/12-shifter.md)。第 5 轮 ALU 已接入该模块。

第 3 轮 RegFile 已核验并按用户指示提交为 `96576f0`：核对教材要求后保留既有硬件，补充全部非零寄存器复位保值、写入抑制和无时钟沿脉冲验证，详见 [RegFile 核验报告](docs/modules/04-regfile.md)。

第 2 轮 Cmp 已核验并按用户指示提交为 `c8cec38`：保留 `R1/R2/Eq`，新增 `LT/LTU`，按教材 Code Example 7.1 翻转最高位后进行无符号比较，实现 `LT`。该轮整机沿用原有 `Eq` 连接，新增比较标志及六种分支已在后续 Controller/Datapath 轮次接通，详见 [Cmp 核验报告](docs/modules/02-cmp.md)。

第 1 轮 Extend 已核验并按用户指示提交为 `db4ddc4`：`ImmSrc` 升级为 3 位，编码 `000/001/010/011/100` 对应 I/S/B/J/U，其他编码输出零，支持 32/64 位符号扩展。该轮 Datapath 将原有 2 位选择码补零后连接 Extend；3 位选择码与 LUI/AUIPC 已在后续 Controller/Datapath 轮次接通，详见 [Extend 核验报告](docs/modules/01-extend.md)。

以下为各模块索引，其中历史提交记录属于第二章；当前接口及第七章新增模块以更新报告和生成文件为准。

**Extend**、**Cmp**、**ALU**、**RegFile**、**Controller**、**Datapath**、**IEU**、**IROM**、**IFU**、**LSU** 已核验并提交，提交号分别为 `d6cf8cc`、`4b96fd7`、`aa69b21`、`24f885f`、`668d141`、`b77a5cd`、`da62b51`、`58232c4`、`1c1cb34`、`b882f2d`。第 11 个模块 **RiscvSingle（CPU 顶层）** 已按用户指示提交，提交号为 `d218f06`。CPU 的 `XLEN` 仍限定为 32。

| 模块 | 报告 | 源码 | 测试 | 生成的 Verilog |
| --- | --- | --- | --- | --- |
| Extend | [01-extend.md](docs/modules/01-extend.md) | [Extend.scala](src/main/scala/riscvsingle/ieu/Extend.scala) | [ExtendSpec.scala](src/test/scala/riscvsingle/ieu/ExtendSpec.scala) | [32 位](generated/extend/Extend.v)、[64 位输出](generated/extend64/Extend.v) |
| Cmp | [02-cmp.md](docs/modules/02-cmp.md) | [Cmp.scala](src/main/scala/riscvsingle/ieu/Cmp.scala) | [CmpSpec.scala](src/test/scala/riscvsingle/ieu/CmpSpec.scala) | [32 位](generated/cmp/Cmp.v)、[64 位](generated/cmp64/Cmp.v) |
| ALU | [03-alu.md](docs/modules/03-alu.md) | [ALU.scala](src/main/scala/riscvsingle/ieu/ALU.scala) | [ALUSpec.scala](src/test/scala/riscvsingle/ieu/ALUSpec.scala) | [32 位](generated/alu/ALU.v)、[64 位](generated/alu64/ALU.v) |
| RegFile | [04-regfile.md](docs/modules/04-regfile.md) | [RegFile.scala](src/main/scala/riscvsingle/ieu/RegFile.scala) | [RegFileSpec.scala](src/test/scala/riscvsingle/ieu/RegFileSpec.scala) | [32 位](generated/regfile/RegFile.v)、[64 位](generated/regfile64/RegFile.v) |
| Shifter | [12-shifter.md](docs/modules/12-shifter.md) | [Shifter.scala](src/main/scala/riscvsingle/ieu/Shifter.scala) | [ShifterSpec.scala](src/test/scala/riscvsingle/ieu/ShifterSpec.scala) | [32 位](generated/shifter/Shifter.v)、[64 位](generated/shifter64/Shifter.v) |
| SwByteMask | [13-swbytemask.md](docs/modules/13-swbytemask.md) | [SwByteMask.scala](src/main/scala/riscvsingle/lsu/SwByteMask.scala) | [SwByteMaskSpec.scala](src/test/scala/riscvsingle/lsu/SwByteMaskSpec.scala) | [32 位](generated/swbytemask/SwByteMask.v)、[64 位](generated/swbytemask64/SwByteMask.v) |
| SubwordWrite | [14-subwordwrite.md](docs/modules/14-subwordwrite.md) | [SubwordWrite.scala](src/main/scala/riscvsingle/lsu/SubwordWrite.scala) | [SubwordWriteSpec.scala](src/test/scala/riscvsingle/lsu/SubwordWriteSpec.scala) | [32 位](generated/subwordwrite/SubwordWrite.v)、[64 位](generated/subwordwrite64/SubwordWrite.v) |
| Controller | [05-controller.md](docs/modules/05-controller.md) | [Controller.scala](src/main/scala/riscvsingle/ieu/Controller.scala) | [ControllerSpec.scala](src/test/scala/riscvsingle/ieu/ControllerSpec.scala) | [Controller.v](generated/controller/Controller.v) |
| Datapath | [06-datapath.md](docs/modules/06-datapath.md) | [Datapath.scala](src/main/scala/riscvsingle/ieu/Datapath.scala) | [DatapathSpec.scala](src/test/scala/riscvsingle/ieu/DatapathSpec.scala) | [Datapath.v](generated/datapath/Datapath.v) |
| IEU | [07-ieu.md](docs/modules/07-ieu.md) | [IEU.scala](src/main/scala/riscvsingle/ieu/IEU.scala) | [IEUSpec.scala](src/test/scala/riscvsingle/ieu/IEUSpec.scala) | [IEU.v](generated/ieu/IEU.v) |
| IROM | [08-irom.md](docs/modules/08-irom.md) | [IROM.scala](src/main/scala/riscvsingle/ifu/IROM.scala) | [IROMSpec.scala](src/test/scala/riscvsingle/ifu/IROMSpec.scala) | [64 项](generated/irom/IROM.v)、[128 项](generated/irom128/IROM.v) |
| IFU | [09-ifu.md](docs/modules/09-ifu.md) | [IFU.scala](src/main/scala/riscvsingle/ifu/IFU.scala) | [IFUSpec.scala](src/test/scala/riscvsingle/ifu/IFUSpec.scala) | [默认](generated/ifu/IFU.v)、[128 项、复位地址 0x100](generated/ifu128/IFU.v) |
| LSU | [10-lsu.md](docs/modules/10-lsu.md) | [LSU.scala](src/main/scala/riscvsingle/lsu/LSU.scala) | [LSUSpec.scala](src/test/scala/riscvsingle/lsu/LSUSpec.scala) | [64 项](generated/lsu/LSU.v)、[128 项](generated/lsu128/LSU.v) |
| RiscvSingle | [11-riscv-single.md](docs/modules/11-riscv-single.md) | [RiscvSingle.scala](src/main/scala/riscvsingle/RiscvSingle.scala) | [RiscvSingleSpec.scala](src/test/scala/riscvsingle/RiscvSingleSpec.scala) | [默认 CPU](generated/riscv-single/RiscvSingle.v)、[128 项、复位地址 0x100](generated/riscv-single128/RiscvSingle.v) |

[统一配置](src/main/scala/riscvsingle/config/CpuConfig.scala) 提供 CPU 结构参数。Extend、Cmp、ALU、RegFile 可单独配置数据位宽，RegFile 还可配置寄存器数量；Shifter 位宽须为不小于 2 的二次幂，默认 32。Controller 的端口位宽遵循固定的 RISC-V 指令字段和控制编码。Datapath、IEU 接受 `CpuConfig`，将 `xlen` 传递给端口与子模块，当前限定为 RV32 和 32 项寄存器。

RegFile 默认使用完整的 32 项 `Reg(Vec(...))`，下标直接对应寄存器编号。高有效同步 `reset` 仅将 `rf(0)` 清零，其他寄存器保持；正常写入跳过 0 号寄存器。读写编号均不减一。

IROM 接受同一配置，以 `imemDepth` 设置 32 位指令字数量，以 `instructionInitFile` 设置十六进制初始化文件。默认 `CpuConfig()` 不初始化 ROM；生成入口默认加载 [书中程序镜像](programs/riscvtest.memfile)，包含 21 条程序指令和 43 条填充 NOP。当前生成的 `$readmemh` 位于仿真初始化块中，FPGA 初始化需在后续实现流程中另行接入。

IFU 接受统一配置，用 `resetVector` 设置 PC 的复位字节地址，并将配置传给 IROM。项目统一使用高有效同步复位：reset=1 的时钟上升沿将 PC 加载为 resetVector，并将 RegFile 的 x0 清零；reset 电平变化本身不更新寄存器。正常时钟上升沿选择 PCPlus4 或 IEUAdr 更新 PC。

LSU 以 `dmemDepth` 配置 32 位数据字数量，组合读取、在 MemWrite=1 的上升沿写入。按书中接口不设 RAM 复位或初始化；首次写入前的内容未指定，读写地址低两位及超出容量的高位均忽略。

RiscvSingle 接入同一 `CpuConfig` 并连接 IFU、IEU、LSU，保留书中的 clk/reset 输入及 WriteData/IEUAdr/MemWrite 输出。生产顶层已执行书中程序，确认地址 96 写入 7、地址 100 写入 25。全项目 12 个套件、74 项测试通过。

**预定的 11 个硬件模块均已实现并提交。** 后续补充的生成 RTL 整机验证已通过：Verilator 直接编译默认及 128 项配置，各使用三个随机初值种子，共六次运行通过。检查逐周期地址、存储事务、结束循环与同步复位；详见 [RTL 整机验证报告](docs/12-rtl-validation.md)。验证脚本、测试与报告已提交为 `b983c22`。

**第二章项目已完成收尾。** 当时的实现范围、模块交付、参数、用户核验要求、验证结果及复现方式见 [项目总结报告](docs/project-summary.md)；第七章的当前进度以本 README 和各模块更新报告为准。

后续按用户要求补充 [Code Example 2.16 汇编程序](programs/riscvtest.s)和整机波形入口；其机器码与已有 `programs/riscvtest.memfile` 的前 21 项一致。运行 `make test-wave SBT=./scripts/sbt-local.sh` 可生成 `target/waveforms/code-example-2.16.vcd`，再用 `gtkwave target/waveforms/code-example-2.16.vcd waves/code-example-2.16.gtkw` 查看预选信号。详见 [程序与波形报告](docs/13-code-example-2.16.md)。

## 构建与验证

版本：JDK 17、sbt 1.10.7、Scala 2.13.14、Chisel 3.6.1、chiseltest 0.6.2。

在本项目目录执行；已有 Java/sbt 的环境使用：

```bash
make test
make test-rtl
make generate
make generate WIDTH=64 TARGET_DIR=generated/extend64
make generate-cmp
make generate-cmp WIDTH=64 CMP_TARGET_DIR=generated/cmp64
make generate-shifter
make generate-shifter WIDTH=64 SHIFTER_TARGET_DIR=generated/shifter64
make generate-swbytemask
make generate-swbytemask WIDTH=64 SWBYTEMASK_TARGET_DIR=generated/swbytemask64
make generate-subwordwrite
make generate-subwordwrite WIDTH=64 SUBWORDWRITE_TARGET_DIR=generated/subwordwrite64
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
make generate-cpu
make generate-cpu IMEM_DEPTH=128 DMEM_DEPTH=128 CPU_TARGET_DIR=generated/riscv-single128 INSTRUCTION_INIT_FILE=programs/rv32-configtest.memfile RESET_VECTOR=0x100
```

当前工作区的工具链位于 `../../.tools/chisel`，尚未加入 PATH，可使用：

```bash
make test SBT=./scripts/sbt-local.sh
make test-rtl
make generate SBT=./scripts/sbt-local.sh
make generate SBT=./scripts/sbt-local.sh WIDTH=64 TARGET_DIR=generated/extend64
make generate-cmp SBT=./scripts/sbt-local.sh
make generate-cmp SBT=./scripts/sbt-local.sh WIDTH=64 CMP_TARGET_DIR=generated/cmp64
make generate-shifter SBT=./scripts/sbt-local.sh
make generate-shifter SBT=./scripts/sbt-local.sh WIDTH=64 SHIFTER_TARGET_DIR=generated/shifter64
make generate-swbytemask SBT=./scripts/sbt-local.sh
make generate-swbytemask SBT=./scripts/sbt-local.sh WIDTH=64 SWBYTEMASK_TARGET_DIR=generated/swbytemask64
make generate-subwordwrite SBT=./scripts/sbt-local.sh
make generate-subwordwrite SBT=./scripts/sbt-local.sh WIDTH=64 SUBWORDWRITE_TARGET_DIR=generated/subwordwrite64
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
make generate-cpu SBT=./scripts/sbt-local.sh
make generate-cpu SBT=./scripts/sbt-local.sh IMEM_DEPTH=128 DMEM_DEPTH=128 CPU_TARGET_DIR=generated/riscv-single128 INSTRUCTION_INIT_FILE=programs/rv32-configtest.memfile RESET_VECTOR=0x100
```

`scripts/sbt-local.sh` 读取共享 JDK 和 sbt，将共享依赖缓存复制到本项目 `.cache/chisel` 后使用，避免修改共享工具目录。可以通过 `CHISEL_TOOLCHAIN_DIR` 和 `CHISEL_CACHE_DIR` 指定绝对路径。脚本关闭 sbt 构建服务器，并允许在无法创建启动套接字时继续批处理。

`sbt run` 默认生成 Extend。也可以使用 `runMain` 指定 `riscvsingle.GenerateExtend`、`riscvsingle.GenerateCmp`、`riscvsingle.GenerateShifter`、`riscvsingle.GenerateALU`、`riscvsingle.GenerateRegFile`、`riscvsingle.GenerateController`、`riscvsingle.GenerateDatapath` 或 `riscvsingle.GenerateIEU`。例如 `sbt "runMain riscvsingle.GenerateRegFile 64 generated/regfile64 32"`；RegFile 的第三个可选参数为寄存器数量，Makefile 对应 `REGISTER_COUNT`，默认 32。Shifter 使用 `sbt "runMain riscvsingle.GenerateShifter 64 generated/shifter64"`，两个可选参数依次为位宽和输出目录，Makefile 对应 `WIDTH` 和 `SHIFTER_TARGET_DIR`。Controller、Datapath、IEU 各自接受一个可选的输出目录参数，如 `sbt "runMain riscvsingle.GenerateIEU generated/ieu"`，Makefile 对应 `IEU_TARGET_DIR`。`make clean` 调用 `sbt clean`，保留已生成的 Verilog 和模块报告。

IROM 使用 `sbt "runMain riscvsingle.GenerateIROM 64 generated/irom programs/riscvtest.memfile"`。三个可选参数依次为深度、输出目录、初始化文件，Makefile 分别对应 `IMEM_DEPTH`、`IROM_TARGET_DIR`、`INSTRUCTION_INIT_FILE`；将文件设为 `-` 表示不初始化。相对文件路径以仿真运行目录为基准；在项目根目录运行可直接使用默认镜像。默认镜像为 64 项，128 项生成示例的后 64 项未初始化；如需读取全部条目，提供与深度匹配的镜像。

IFU 使用 `sbt "runMain riscvsingle.GenerateIFU 64 generated/ifu programs/riscvtest.memfile 0"`。四个可选参数依次为深度、输出目录、初始化文件、复位字节地址，Makefile 对应 `IMEM_DEPTH`、`IFU_TARGET_DIR`、`INSTRUCTION_INIT_FILE`、`RESET_VECTOR`。复位地址支持十进制和 `0x`/`0X` 十六进制。128 项、复位地址 0x100 的生成示例用于展示配置；默认镜像在该地址没有初始化指令，运行时需提供覆盖该地址的镜像。

LSU 使用 `sbt "runMain riscvsingle.GenerateLSU 64 generated/lsu"`。两个可选参数依次为数据字深度、输出目录，Makefile 对应 `DMEM_DEPTH`、`LSU_TARGET_DIR`。LSU 联调测试在程序运行后直接读回 RAM 确认地址 100 保存 25；生产 RiscvSingle 通过公开顶层端口检查同一程序的执行与存储结果。

完整 CPU 使用 `sbt "runMain riscvsingle.GenerateRiscvSingle 64 64 generated/riscv-single programs/riscvtest.memfile 0"`。五个可选参数依次为指令深度、数据深度、输出目录、初始化文件、复位字节地址，分别对应 Makefile 的 `IMEM_DEPTH`、`DMEM_DEPTH`、`CPU_TARGET_DIR`、`INSTRUCTION_INIT_FILE`、`RESET_VECTOR`。文件参数 `-` 表示不初始化指令内容；复位地址支持十进制和 `0x`/`0X` 十六进制。默认生成加载书中程序镜像。

128 项 CPU 示例使用完整的 [配置测试镜像](programs/rv32-configtest.memfile)，从 0x100 执行测试程序，验证 RAM 字 127 与字 63 独立、负偏移加载、jal 跳转与链接值。单独验证整机可执行 `./scripts/sbt-local.sh 'testOnly riscvsingle.RiscvSingleSpec'`。当前 IEU 已支持第七章表 7.1 的 RV32 指令译码与执行，整机 LSU 仍为原有字访存，子字提取、扩展及字节写掩码待后续轮次接入。异常、CSR 和外部总线接口不在本阶段范围；程序文件加载仍属于仿真初始化，FPGA/ASIC 程序固化尚未实现。

`make test-rtl` 使用 Verilator 和 C++17 编译器直接验证两份现有完整 CPU Verilog，测试仅通过五个生产顶层端口进行。可用 `make test-rtl VERILATOR=/path/to/verilator` 指定工具。脚本自动切换到项目根目录以加载镜像，将编译产物及日志放入 `target/rtl-test/{book,expanded}`，ccache 写入 `.cache/ccache`。修改 Chisel 后先执行上述两个 `generate-cpu` 命令，更新两份 RTL，再运行 `test-rtl`；该目标本身不重新生成硬件。

SubwordWrite 模块测试：`./scripts/sbt-local.sh 'testOnly riscvsingle.lsu.SubwordWriteSpec'`。一条命令生成两个宽度的 VCD：

```bash
make test-subwordwrite-wave SBT=./scripts/sbt-local.sh
gtkwave target/waveforms/subwordwrite32.vcd waves/subwordwrite32.gtkw
gtkwave target/waveforms/subwordwrite64.vcd waves/subwordwrite64.gtkw
```

观察时钟仅在测试包装层；生产模块没有时钟或复位。VCD 位于被忽略的 `target/waveforms`，可用上述命令重建。

## 配置与目录

SwByteMask 使用 `sbt "runMain riscvsingle.GenerateSwByteMask 64 generated/swbytemask64"`。两个可选参数依次为原生数据位宽和输出目录，默认 32、`generated/swbytemask`；Makefile 对应 `WIDTH` 和 `SWBYTEMASK_TARGET_DIR`。位宽只允许 32/64，模块没有时钟、复位或存储状态，不使用存储深度等共享配置；独立 64 位模块不改变 CPU 的 RV32 限制。

SubwordWrite 使用 `sbt "runMain riscvsingle.GenerateSubwordWrite 64 generated/subwordwrite64"`。两个可选参数依次为数据位宽和输出目录，默认 32、`generated/subwordwrite`；Makefile 对应 `WIDTH` 和 `SUBWORDWRITE_TARGET_DIR`。位宽仅允许 32/64，不改变整机 RV32 范围。

`CpuConfig` 默认值为 `xlen=32`、`imemDepth=64`、`dmemDepth=64`、`resetVector=0`、`instructionInitFile=None`。存储深度以 32 位字计，复位地址以字节计。配置类负责 elaboration 前的参数检查。RiscvSingle 将同一配置对象传给三个子模块：IEU 再传给 Datapath，当前仅消费 `xlen`；IROM 消费 `imemDepth` 和 `instructionInitFile`，地址宽度采用 `xlen`；IFU 使用 `xlen`、`resetVector` 并将配置传给 IROM；LSU 使用 `xlen` 和 `dmemDepth`。

`src/main/scala/riscvsingle/ieu` 放置执行单元相关模块，`ifu` 放置取指相关模块，`lsu` 放置访存相关模块，`config` 放置共享配置，`src/test/scala` 放置测试，`docs/modules` 放置每轮核验报告，`generated` 放置实际生成的硬件文件，`programs` 放置指令初始化镜像。

第七章完善顺序：`Extend → Cmp → RegFile → Shifter → ALU → Controller → Datapath → IEU → SwByteMask → SubwordWrite → SubwordRead → DTIM → LSU → IROM → IFU → RiscvSingle`。每轮仅完善一个硬件模块；连接适配归入当前轮，完成测试、生成 RTL 和更新报告后停止等待核验，不自动提交或进入下一模块。
