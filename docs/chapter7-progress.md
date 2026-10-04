# 第七章单周期 CPU 进度与后续执行报告

记录日期：2026-10-05（Asia/Shanghai）；第 13 轮功能验证于 10-04 完成。工程目录：`/home/unlastingstar/cvw-experiment/single-cycle/riscv-single-cycle`，分支：`main`。

## 1. 当前停点与提交定位

**第七章 16/16 轮全部完成。** 第 11–13 轮提交 `f826665`，IROM 提交 `5cbc622`，IFU 提交 `a212288`。第 16 轮完成 8 端口 CPU、实际字节写掩码、复位写入屏蔽、全部 37 类 RV32 指令的整机验收；全工程 188/188、12 次 RTL 仿真及 12 份最终波形通过。详见 [整机验收报告](modules/20-riscv-single-chapter7.md)。

第 8 轮 IEU 已提交为 `89b1e58`。此前按用户指令将第 9 轮 SwByteMask、第 10 轮 SubwordWrite 及可读性更新提交为 `886e708`，随后完成第 11 轮 SubwordRead、第 12 轮 DTIM。第 11–13 轮现已补提交为 `f826665`。用户 10-05 最新要求：后续每轮测试通过后 git commit，连续完成剩余模块；全部结束后统一生成/核对波形，不再逐轮等待波形检查。未合并或推送。

SwByteMask 是教材图 7.9 中 LSU/DTIM 所需的字节写使能模块，用于 SB/SH/SW 只更新指定字节，因此保留源码、生成入口、测试与 RTL；第 13 轮已经接入 LSU 和 CPU。

通过提交日志、工作树状态及模块报告定位交付：

```bash
cd /home/unlastingstar/cvw-experiment/single-cycle/riscv-single-cycle
git log -1 --format='%h %s' -- docs/chapter7-progress.md
git status --short
git diff -- docs/chapter7-progress.md
```

后续更新本报告时，应同时更新完成表、停点、验证证据和下一轮范围。旧的 [第二章总结](project-summary.md)、历史模块报告及 README 历史测试数量不能替代当前进度；第七章接口以对应已更新模块报告和实际源码为准。

## 2. 已完成轮次

| 轮次 | 模块 | 已完成能力 | 提交 / 核验报告 |
| --- | --- | --- | --- |
| 1 | Extend | 25 位 Instr、3 位 ImmSrc；I/S/B/J/U，非法选择输出零；32/64 位扩展。 | `db4ddc4`；[01-extend.md](modules/01-extend.md) |
| 2 | Cmp | Eq、LT、LTU；有符号比较采用教材最高位翻转后的无符号比较结构；32/64 位。 | `c8cec38`；[02-cmp.md](modules/02-cmp.md) |
| 3 | RegFile | 保留完整数组、直接编号索引和两读一写；补充 x0、边沿、复位保持与参数验证。 | `96576f0`；[04-regfile.md](modules/04-regfile.md) |
| 4 | Shifter | 新增教材漏斗结构，SLL/SRL/SRA；32/64 位，全部移位量。 | `d9d7861`；[12-shifter.md](modules/12-shifter.md) |
| 5 | ALU | 十种整数运算，接入 Shifter；独立加减地址输出；32/64 位验证。 | `715b7b0`；[03-alu.md](modules/03-alu.md) |
| 6 | Controller | 表 7.1 的 37 类 RV32 指令、六种分支；完整 Funct7、Jump、MemRW、3 位 ImmSrc；非法指令关闭副作用。 | `ffbb866`；[05-controller.md](modules/05-controller.md) |
| 7 | Datapath | LUI/AUIPC、JAL/JALR 链接值；三级写回选择；JALR 目标位 0 清零。 | `ffd13e5`；[06-datapath.md](modules/06-datapath.md) |
| 8 | IEU | Controller/Datapath 完整连接；导出 MemRW/Funct3；独立程序和参考模型验证全部 37 类指令。 | `89b1e58`；[07-ieu.md](modules/07-ieu.md) |
| 9 | SwByteMask | 独立组合掩码；32 位 SB/SH/SW、64 位增加 SD；完整 Funct3、自然对齐，非法/未对齐输出零；第 13 轮接入 LSU。 | `886e708`；[13-swbytemask.md](modules/13-swbytemask.md) |
| 10 | SubwordWrite | 纯组合低位数据复制；32 位 SB/SH/SW、64 位增加 SD；完整 Funct3 校验，非法编码输出零；两种宽度波形，第 13 轮接入 LSU。 | `886e708`；[14-subwordwrite.md](modules/14-subwordwrite.md) |
| 11 | SubwordRead | 逐级小端子字选择与符号/零扩展；RV32 五种加载，独立 RV64 增 LD/LWU；非法编码输出零；两种宽度波形，第 13 轮接入 LSU。 | `f826665`；[15-subwordread.md](modules/15-subwordread.md) |
| 12 | DTIM | 独立 32/64 位 RAM；组合读、上升沿逐字节掩码写；读取禁用输出零，无复位/初始化；容量回绕，第 13 轮接入 LSU。 | `f826665`；[16-dtim.md](modules/16-dtim.md) |
| 13 | LSU | RV32 三种存储、五种加载，四模块连接；非法/未对齐读零、不写；双写使能、组合读/边沿写；CPU 必要接口适配、64/128 项 RTL 与波形。 | `f826665`；[17-lsu-chapter7.md](modules/17-lsu-chapter7.md) |
| 14 | IROM | 保留组合取指、字节映射/容量回绕；补充完整扩容镜像边界与复位保持，8 项模块/181 项全工程通过。 | `5cbc622`；[18-irom-chapter7.md](modules/18-irom-chapter7.md) |
| 15 | IFU | 保留同步复位、PC+4、目标选择与组合取指；新增三组900周期参考，9项模块/184项全工程通过。 | `a212288`；[19-ifu-chapter7.md](modules/19-ifu-chapter7.md) |
| 16 | RiscvSingle | 8 观察端口、复位屏蔽、全部 37 类 RV32 整机验收；188 项、12 次 RTL、12 份最终波形通过。 | 见提交日志；[20-riscv-single-chapter7.md](modules/20-riscv-single-chapter7.md) |

报告编号沿用既有模块目录，**不等于第七章执行轮次**：例如 Shifter 的报告是 `12-shifter.md`，其执行轮次为 4。新增模块应使用独立报告并更新 README 索引，不覆盖已有报告。

## 3. 后续必须保留的接口与行为

### 参数与状态

- 统一 `CpuConfig`：`xlen=32`、`imemDepth=64`、`dmemDepth=64`、`resetVector=0`、`instructionInitFile=None` 为默认值；当前整机仅允许 xlen=32。
- CpuConfig 的存储深度以 32 位字计，须为不小于 2 的二次幂且总字节数不超过 2^32。独立 DTIM 的 depth 以 dataWidth 位原生字计，采用相同深度/容量限制；64 位每字 8 字节。resetVector 为 32 位范围内、4 字节对齐的字节地址。
- Extend 输出宽度至少 32；Cmp、Shifter、ALU 和访存辅助模块独立验证 32/64 位。独立模块 64 位配置不表示整机已支持 RV64I。
- 指令固定 32 位，小端存储，地址按字节计；保留存储容量回绕映射。
- 显式 clk，高有效同步 reset；RegFile 使用完整数组和直接索引，复位仅清零 x0、保留非零寄存器，正常写入禁止 x0。PC 在复位上升沿加载 resetVector；RAM 不复位清空。

### 控制语义

| 信号 | 当前约定 |
| --- | --- |
| ImmSrc[2:0] | 000/001/010/011/100 对应 I/S/B/J/U，其他编码输出零。 |
| ALUSrc[1:0] | {选择 PC 作为 SrcA，选择 ImmExt 作为 SrcB}。 |
| ALUControl[1:0] | {SubArith,ALUOp}；保留独立 Funct3，ALU 内部生成 ALUSelect。 |
| ALUResultSrc | 1 选 AltResult，0 选 ALUResult。 |
| Jump | 合法 JAL/JALR 为 1；用于 AltResult 选择外部 PCPlus4，否则 AltResult 为 ImmExt。 |
| ResultSrc | 1 选外部 ReadData，0 选 IEUResult。 |
| MemRW[1:0] | {MemRead,MemWrite}：00 空闲，10 加载，01 存储；IEU 兼容 MemWrite=MemRW[0]；CPU MemWrite 为实际有效掩码的归约或。 |
| PCSrc | 六种分支条件成立，或合法 JAL/JALR 时选择 IEUAdr。 |

Controller 使用完整 `Funct7[6:0]`，接收独立 Eq/LT/LTU；非法 opcode/funct3/funct7 不产生寄存器写、访存请求或跳转，不实现陷阱。

ALU 的 IEUAdr 始终为加减器结果，不能误用逻辑或移位运算结果作地址。Datapath 的 RawIEUAdr 与 ALUResult 分离；AUIPC 用当前 PC。JALR 地址仅在 Datapath 根据 opcode 清除位 0，保留位 1，IFU 不再重复处理。该地址清位是数据通路约定，非法 JALR 的执行副作用仍由 Controller 关闭。

### 当前连接与观察接口

独立 IEU 有 **12 个端口**（含 clk/reset）：

| 输入 | 输出 |
| --- | --- |
| clk、reset、Instr[31:0]、PC[31:0]、PCPlus4[31:0]、ReadData[31:0] | PCSrc、MemWrite、MemRW[1:0]、Funct3[2:0]、IEUAdr[31:0]、WriteData[31:0] |

Funct3 恒为 Instr[14:12]，包括非访存、非法指令和复位期间。WriteData 恒为完整原始 R2。ReadData 表示外部已经处理完成的加载结果，IEU 原样写回；字节提取、符号扩展和写数据复制由 LSU 负责，第 13 轮已连接四个访存模块。

当前 [RiscvSingle.scala](../src/main/scala/riscvsingle/RiscvSingle.scala) 已完整连接 IFU/IEU/LSU。IEU 保留 12 端口，LSU 新增有效 ByteMask 输出共 8 端口；CPU 共 8 端口。顶层 MemRW 在 reset 有效时为零，Funct3 保留原指令字段；顶层 ByteMask/MemWrite 为实际合法、自然对齐、请求有效的写入通道/使能。

第 16 轮在顶层立即屏蔽复位期间访存；IEU 的同步复位禁止 RF 正常写入，PC/x0 上升沿复位，RAM 内容保持。非法指令关闭副作用，未对齐读零、不写，不实现异常处理。独立 LSU 没有 reset，包装层 reset 不禁止其写入；CPU 的复位门控由父模块负责。

## 4. 验证证据与复现

### 第 8 轮已提交的历史证据

第 8 轮先修改生成接口断言，旧实现实际运行 7 项测试：6 通过、1 因缺少 MemRW/Funct3 失败；随后增加功能测试并实现接口。最终独立 IEU 为 **11/11 通过**。详细证据见 [IEU 核验报告](modules/07-ieu.md)。

| 验证 | 结果 |
| --- | --- |
| 访存请求 | 20 组加载、6 组存储；检查请求、原始 Funct3、地址、完整写数据和外部加载值写回。 |
| 非法及复位 | 32 组非法编码；五类复位期间指令；非零寄存器保持、x0 和写入边沿。 |
| 固定 PC 驱动程序 | 142 个指令字，执行 135 条，覆盖全部 37 类 RV32 指令；六种分支各 taken/untaken，25 个独立常量字存储签名。 |
| 独立参考模型 | 固定种子 0x1e008，564 条；37 类合法和 10 类非法各 12 次；检查全部输出和沿前/沿后状态。 |
| 全工程回归 | 13 个套件、113 项测试全部通过。 |
| 生成 RTL 整机回归 | 默认/扩容 CPU 各以种子 1、17、2026 执行，六次 PASS；保留原书与容量配置程序结果。 |

固定程序的非零寄存器通过真实指令初始化，加载只使用已建立的外部数据；参考模型不依赖未初始化寄存器或 RAM。完整指令覆盖发生在独立 IEU，整机 RTL 验证当前仍使用既有合法字访存程序。完整 RV32 子字整机签名验收留待第 16 轮。

第 8 轮提交前再次执行全工程与两种配置 RTL 回归。历史复现命令：

```bash
cd /home/unlastingstar/cvw-experiment/single-cycle/riscv-single-cycle
./scripts/sbt-local.sh 'testOnly riscvsingle.ieu.IEUSpec'
./scripts/sbt-local.sh test
./scripts/sbt-local.sh \
  'runMain riscvsingle.GenerateIEU generated/ieu' \
  'runMain riscvsingle.GenerateRiscvSingle 64 64 generated/riscv-single programs/riscvtest.memfile 0' \
  'runMain riscvsingle.GenerateRiscvSingle 128 128 generated/riscv-single128 programs/rv32-configtest.memfile 0x100'
make test-rtl
```

工具链：JDK 17、sbt 1.10.7、Scala 2.13.14、Chisel 3.6.1、chiseltest 0.6.2、Verilator 5.036。`sbt-local.sh` 使用项目内缓存；`make test-rtl` 不自动生成硬件，修改 Chisel 后需先重新生成对应 RTL。

日志位于 `target/ieu-round8-{red,focused,generate,regression,rtl}.log`；第 8 轮提交前复验为 `target/ieu-round8-precommit.log` 与 `target/ieu-round8-precommit-rtl.log`。target、缓存、test_run_dir 及生成的 .fir/.anno.json 被忽略，不随提交保存；源码、测试、Markdown 和生成 .v 随提交保存，新环境使用上述命令重建证据。

### 第 9 轮历史交付

本轮 8 项 SwByteMask 测试全部通过，包含 32/64 位全部 96 种 Funct3/偏移组合的双向遍历、22 组手工定向及 12 组无时钟沿切换，共 226 次功能断言。恰好三个组合端口；位宽参数仅允许 32/64。完整证据、实际 RTL 命名与教材/CVW 边界差异见 [SwByteMask 核验报告](modules/13-swbytemask.md)。

全工程回归为 **14 个套件、121 项全部通过**；两份新独立 RTL 均成功生成；现有默认/扩容 CPU 各三个种子的六次 RTL 仿真全部 PASS。SwByteMask 未接入 CPU，整机 RTL 本轮没有变化，CPU 回归仍验证既有字访存程序。

```bash
./scripts/sbt-local.sh 'testOnly riscvsingle.lsu.SwByteMaskSpec'
./scripts/sbt-local.sh test \
  'runMain riscvsingle.GenerateSwByteMask' \
  'runMain riscvsingle.GenerateSwByteMask 64 generated/swbytemask64'
make test-rtl
```

本轮日志为 `target/swbytemask-round9-{red-interface,red,focused,regression-generate,rtl}.log`。实际产物为 `generated/swbytemask/SwByteMask.v` 与 `generated/swbytemask64/SwByteMask.v`。

### 第 10 轮历史交付

SubwordWrite 采用 Chisel `Fill` 复制低 8/16/32 位，并按完整 Funct3 选择结果；64 位的 SD 原样输出。非法编码输出零，生产模块恰好三个组合端口，无时钟、复位或状态。CVW 的 32/64 位分支只译码 Funct3 低两位，本工程保留完整编码校验。依据和全部接口见 [SubwordWrite 核验报告](modules/14-subwordwrite.md)。

测试先行：常零接口骨架运行 12 项，10 项功能测试按预期失败、2 项接口/参数测试通过；实现后 **12/12 通过**。本轮基线为 121 项；最终全工程 **15 个套件、133 项全部通过**。现有默认/扩容 CPU 各三个种子的六次 RTL 仿真全部 PASS。CPU 与 LSU 硬件未改变，整机回归仍验证已有字访存程序。

```bash
./scripts/sbt-local.sh 'testOnly riscvsingle.lsu.SubwordWriteSpec'
./scripts/sbt-local.sh test
make generate-subwordwrite SBT=./scripts/sbt-local.sh
make generate-subwordwrite SBT=./scripts/sbt-local.sh WIDTH=64 SUBWORDWRITE_TARGET_DIR=generated/subwordwrite64
make test-subwordwrite-wave SBT=./scripts/sbt-local.sh
make test-rtl
```

实际 RTL：`generated/subwordwrite/SubwordWrite.v`、`generated/subwordwrite64/SubwordWrite.v`。实际 VCD：`target/waveforms/subwordwrite32.vcd`、`target/waveforms/subwordwrite64.vcd`；预选信号配置：`waves/subwordwrite32.gtkw`、`waves/subwordwrite64.gtkw`。VCD 时钟仅来自测试包装层。日志为 `target/subwordwrite-round10-{baseline,red,focused,regression,generate32,generate64,wave,rtl}.log`。

可读性整理：SubwordWrite 源码增加中文说明与三步结构；测试显式列出 Funct3/期望值，并澄清参考模型及高位隔离输入名称；报告分表说明 CVW 差异、验证阶段和波形。功能与测试覆盖保持一致，整理后全工程 133 项及现有整机 RTL 六次运行再次通过。更新日志位于 `target/subwordwrite-readability/`；第 9/10 轮及可读性更新现已提交为 `886e708`。

### 第 11 轮历史交付

先阅读教材 §7.1.6/图 7.9 和 CVW `lsu/subwordread.sv`，再用 Chisel 实现 SubwordRead 的逐级 Word → Halfword → Byte 选择、符号/零扩展与完整 Funct3 译码。只有四个组合端口，无时钟/复位。32 位支持 LB/LH/LW/LBU/LHU；64 位另支持 LD/LWU。详见 [SubwordRead 核验报告](modules/15-subwordread.md)。

大小相关低偏移位沿用 CVW 的选择行为：半字忽略 bit 0，32 位字忽略 bits 1:0；64 位的 LW/LWU 用 bit 2 选低/高字，LD 不使用偏移。SubwordRead 自身不检查自然对齐；整机未对齐读零仍由第 13 轮 LSU 门控，不提前承诺接通行为。

测试先行：常零骨架实际运行 16 项，14 项功能失败、2 项接口/参数通过；实现后 **16/16 通过**。最终全工程 **16 个套件、149 项全部通过**，现有整机 RTL 六次 PASS。固定常量、符号边界、邻接字节隔离、组合切换，以及固定种子下全部 96 种 Funct3/偏移组合均已覆盖。

```bash
./scripts/sbt-local.sh 'testOnly riscvsingle.lsu.SubwordReadSpec'
./scripts/sbt-local.sh test
make generate-subwordread SBT=./scripts/sbt-local.sh
make generate-subwordread SBT=./scripts/sbt-local.sh WIDTH=64 SUBWORDREAD_TARGET_DIR=generated/subwordread64
make test-subwordread-wave SBT=./scripts/sbt-local.sh
make test-rtl
```

RTL 位于 `generated/subwordread{,64}/SubwordRead.v`；VCD 位于 `target/waveforms/subwordread{32,64}.vcd`，GTKWave 配置位于 `waves/subwordread{32,64}.gtkw`。日志在 `target/subwordread-round11/`。观察时钟仅在测试包装层；CPU/LSU 仍使用原字 RAM。

### 第 12 轮历史交付

先阅读教材 §7.1.6/图 7.9、CVW `lsu/dtim.sv` 和 `generic/mem/ram1p1rwbe.sv`，再用 Chisel 的字节 Vec 存储器实现掩码写入。保持教材组合读、上升沿写，区别于 CVW 的同步读地址接口。独立接口共 7 端口：clk、Adr、MemRead、MemWrite、WriteDataWord、ByteMask → ReadDataWord。详见 [DTIM 核验报告](modules/16-dtim.md)。

MemRead=0 输出零；MemWrite 独立控制写入，ByteMask(0) 对应最低字节；零掩码和未选中字节保持。地址忽略低字节偏移及容量外高位。没有 reset 或初始化；对齐和编码检查仍交由下一轮 LSU。

测试先行：常零骨架实际运行 20 项，19 项失败、参数测试通过；实现后 **20/20 通过**。最终全工程 **17 个套件、169 项全部通过**，现有 CPU RTL 六次 PASS；两种宽度独立 RTL lint 通过。测试覆盖全部 16/256 种掩码、深度 2/64/128 的全部地址与偏移别名，以及每种宽度 300 笔固定种子字节模型事务。

```bash
./scripts/sbt-local.sh 'testOnly riscvsingle.lsu.DTIMSpec'
./scripts/sbt-local.sh test
make generate-dtim SBT=./scripts/sbt-local.sh
make generate-dtim SBT=./scripts/sbt-local.sh WIDTH=64 DTIM_TARGET_DIR=generated/dtim64
make test-dtim-wave SBT=./scripts/sbt-local.sh
make test-rtl
```

RTL 位于 `generated/dtim{,64}/DTIM.v`，两者深度为 64 原生字；VCD 位于 `target/waveforms/dtim{32,64}.vcd`，GTKWave 配置位于 `waves/dtim{32,64}.gtkw`。日志在 `target/dtim-round12/`。旧 LSU/CPU 硬件和此前交付保持，RAM 内容只由生产写口建立，不假定上电值。

### 第 13 轮历史交付

先核对教材 §7.1.6/图 7.9 和 CVW `lsu/lsu.sv` 的 DTIM/Subword Accesses 连接，再完善 RV32 LSU。7 端口包含 clk、兼容 MemWrite、MemRW、Funct3、IEUAdr、WriteData、ReadData；三种存储和五种加载均已连接，非法/未对齐读零、不写。详见 [LSU 第七章核验报告](modules/17-lsu-chapter7.md)，原第二章报告保留为历史。

测试先行：新接口仍使用旧 RAM 时 17 项中 12 失败、5 通过；实现后 **17/17 通过**。包括原 7 项测试的接口适配与新 10 项子字测试；覆盖 256 组编码/偏移/读写/兼容控制、600 笔固定种子字节模型、IEU 写回签名。默认/扩容 LSU 均通过 lint，波形 15 条路径核对通过。

首次全回归 178/179，定位扩容镜像在地址 511 的未对齐 SW。将 PC=0x108 的 `00112023` 改为 `fe112ea3`（sw x1,-3(x2)，有效地址 508），同步 Scala/C++ 预期地址，保留负偏移加载和容量/JAL 签名。最终全工程 **18 个套件、179 项全部通过**，重新生成两种 CPU RTL 的六次仿真 PASS。

```bash
./scripts/sbt-local.sh 'testOnly riscvsingle.lsu.LSUSpec riscvsingle.lsu.LSUSubwordSpec'
./scripts/sbt-local.sh test
make generate-lsu SBT=./scripts/sbt-local.sh
make generate-lsu SBT=./scripts/sbt-local.sh DMEM_DEPTH=128 LSU_TARGET_DIR=generated/lsu128
make generate-cpu SBT=./scripts/sbt-local.sh
make generate-cpu SBT=./scripts/sbt-local.sh IMEM_DEPTH=128 DMEM_DEPTH=128 CPU_TARGET_DIR=generated/riscv-single128 INSTRUCTION_INIT_FILE=programs/rv32-configtest.memfile RESET_VECTOR=0x100
make test-rtl
make test-lsu-wave SBT=./scripts/sbt-local.sh
```

独立 RTL 为 `generated/lsu/LSU.v`、`generated/lsu128/LSU.v`，均为 RV32；两份 CPU RTL 同步更新。VCD 为 `target/waveforms/lsu32.vcd`，GTKWave 配置为 `waves/lsu32.gtkw`。日志在 `target/lsu-round13/`，首次失败保留 regression.log，最终通过为 regression-final.log。上述为第 13 轮历史交付；第 11–13 轮现已补提交 f826665，本地规范文件继续保持。

## 5. 最终整机验收

第 16 轮报告：[20-riscv-single-chapter7.md](modules/20-riscv-single-chapter7.md)。完整程序 114 条指令、110 个执行周期、37 次有效存储，覆盖全部 37 类 RV32、六种分支各两个结果、33 个固定签名；提供汇编、两种配置完整镜像、逐周期 trace、覆盖/签名 JSON。

新增 CpuResetSpec 两项在原硬件均失败，顶层门控修复后通过；原整机 4 项保持，新增完整验收 2 项。全工程最终 20 套件、188/188；四种生成 CPU RTL 各三个随机初值种子，共 12 次 PASS。完整 CPU 波形额外核对真实 PC、Instr、PC+4；所有 12 份波形及预选路径核对通过。

```bash
make build-acceptance
make generate-acceptance SBT=./scripts/sbt-local.sh
./scripts/sbt-local.sh test
make test-rtl
make test-final-waves SBT=./scripts/sbt-local.sh
```

日志在 target/chapter7-final；汇编/镜像在 programs，生成 RTL 在 generated，VCD 在 target/waveforms，GTKWave 配置在 waves。

## 6. 完成范围与提交规则

用户的最后规则已执行：IROM/IFU 逐轮测试后提交；全部模块完成后统一生成和核对波形；第 16 轮验收与报告纳入本轮提交，提交号见 git log。保留此前未提交的本地 Chisel 规范文件，不将其混入模块提交，不自动合并或推送。

SwByteMask 必须保留：已连接 LSU/DTIM 并决定 SB/SH/SW 更新的字节通道。整机为 RV32；辅助模块独立支持 32/64 位不表示整机支持 RV64。FENCE、ECALL、EBREAK、CSR、异常、中断、流水线、缓存与总线均不纳入教材本阶段的 37 类范围。仿真加载不等于 FPGA/ASIC 固化，也尚无综合/时序验证。

## 7. 资料入口

- 教材：`/home/unlastingstar/cvw-experiment/single-cycle/RISC-V System-on-Chip Design, Edition 1.pdf`；§7.1、图 7.2、表 7.1 为总依据，§7.1.6/图 7.9 为后续 LSU 依据。
- CVW：`/home/unlastingstar/cvw/src`；执行单元参照 `ieu/{ieu,controller,datapath}.sv`，访存参照 `lsu/{swbytemask,subwordwrite,subwordread,dtim,lsu}.sv`。只参考本轮相关结构，不移植其流水线或扩展接口。
- [README 与模块索引](../README.md)、[统一配置](../src/main/scala/riscvsingle/config/CpuConfig.scala)、[IEU 当前报告](modules/07-ieu.md)、[RTL 整机验证说明](12-rtl-validation.md)。

**当前状态：16/16 轮完成；测试、RTL 和最终波形核对均通过，本轮验收与报告纳入 git commit。**
