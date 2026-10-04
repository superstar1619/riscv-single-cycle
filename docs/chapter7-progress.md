# 第七章单周期 CPU 进度与后续执行报告

记录日期：2026-10-04（Asia/Shanghai）。工程目录：`/home/unlastingstar/cvw-experiment/single-cycle/riscv-single-cycle`，分支：`main`。

## 1. 当前停点与提交定位

**第七章 16 轮计划已交付前 10 轮；用户已授权提交 SwByteMask、SubwordWrite，并继续第 11 轮 SubwordRead。** 第二章简化 CPU 已完成，第七章执行单元已接通表 7.1 的 RV32 指令控制、运算、分支和跳转；数据存储仍为第二章的整字 RAM，完整子字访存尚未接通。

第 8 轮 IEU 与原进度报告已提交为 `89b1e58`，其前置基线为 Datapath 提交 `ffd13e5`。本次按用户最新指令，将第 9 轮 SwByteMask、第 10 轮 SubwordWrite 及其可读性更新一起提交，再继续第 11 轮；不合并或推送。

SwByteMask 是教材图 7.9 和计划中 LSU/DTIM 所需的字节写使能模块，用于 SB/SH/SW 只更新指定字节，因此保留源码、生成入口、测试与 RTL。当前尚未接入整机，不能以暂未连接为由删除。

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
| 9 | SwByteMask | 独立组合掩码；32 位 SB/SH/SW、64 位增加 SD；完整 Funct3、自然对齐，非法/未对齐输出零。尚未接入 LSU。 | 本次经用户授权提交；[13-swbytemask.md](modules/13-swbytemask.md) |
| 10 | SubwordWrite | 纯组合低位数据复制；32 位 SB/SH/SW、64 位增加 SD；完整 Funct3 校验，非法编码输出零；提供两种宽度波形，尚未接入 LSU。 | 本次经用户授权提交；[14-subwordwrite.md](modules/14-subwordwrite.md) |

报告编号沿用既有模块目录，**不等于第七章执行轮次**：例如 Shifter 的报告是 `12-shifter.md`，其执行轮次为 4。新增模块应使用独立报告并更新 README 索引，不覆盖已有报告。

## 3. 后续必须保留的接口与行为

### 参数与状态

- 统一 `CpuConfig`：`xlen=32`、`imemDepth=64`、`dmemDepth=64`、`resetVector=0`、`instructionInitFile=None` 为默认值；当前整机仅允许 xlen=32。
- 存储深度以 32 位字计，须为不小于 2 的二次幂且总字节数不超过 2^32。resetVector 为 32 位范围内、4 字节对齐的字节地址。
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
| MemRW[1:0] | {MemRead,MemWrite}：00 空闲，10 加载，01 存储；兼容 MemWrite=MemRW[0]。 |
| PCSrc | 六种分支条件成立，或合法 JAL/JALR 时选择 IEUAdr。 |

Controller 使用完整 `Funct7[6:0]`，接收独立 Eq/LT/LTU；非法 opcode/funct3/funct7 不产生寄存器写、访存请求或跳转，不实现陷阱。

ALU 的 IEUAdr 始终为加减器结果，不能误用逻辑或移位运算结果作地址。Datapath 的 RawIEUAdr 与 ALUResult 分离；AUIPC 用当前 PC。JALR 地址仅在 Datapath 根据 opcode 清除位 0，保留位 1，IFU 不再重复处理。该地址清位是数据通路约定，非法 JALR 的执行副作用仍由 Controller 关闭。

### 第 8 轮交付与尚未接通的部分

独立 IEU 有 **12 个端口**（含 clk/reset）：

| 输入 | 输出 |
| --- | --- |
| clk、reset、Instr[31:0]、PC[31:0]、PCPlus4[31:0]、ReadData[31:0] | PCSrc、MemWrite、MemRW[1:0]、Funct3[2:0]、IEUAdr[31:0]、WriteData[31:0] |

Funct3 恒为 Instr[14:12]，包括非访存、非法指令和复位期间。WriteData 恒为完整原始 R2。ReadData 表示外部已经处理完成的加载结果，IEU 原样写回；字节提取、符号扩展和写数据复制由 LSU 负责；SubwordWrite 本轮独立完成，尚待第 13 轮接入。

当前 [RiscvSingle.scala](../src/main/scala/riscvsingle/RiscvSingle.scala) 只消费兼容 MemWrite，新 MemRW/Funct3 尚未接入旧 [LSU.scala](../src/main/scala/riscvsingle/lsu/LSU.scala)。两份 CPU 导出因此裁去未消费的 IEU 新输出，内部 IEU 为 10 端口；独立 [IEU.v](../generated/ieu/IEU.v) 保留 12 端口。CPU 和 LSU 生产端口各为 5 个，尚无顶层 ByteMask 观察输出。

旧 LSU 组合读取整个字、上升沿写整个字，忽略地址低两位；当前整机的 LB/LH/SB/SH 不能据此宣称正确实现。**计划要求的未对齐读取零、禁止写入尚待第 13 轮 LSU；顶层复位期间抑制存储写入尚待第 16 轮。** IEU 自身 reset 只阻止寄存器正常写入，不屏蔽组合 MemRW/MemWrite/PCSrc。

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

### 第 10 轮当前交付

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

可读性整理：SubwordWrite 源码增加中文说明与三步结构；测试显式列出 Funct3/期望值，并澄清参考模型及高位隔离输入名称；报告分表说明 CVW 差异、验证阶段和波形。功能与测试覆盖保持一致，整理后全工程 133 项及现有整机 RTL 六次运行再次通过。更新日志位于 `target/subwordwrite-readability/`；用户现已授权提交后继续第 11 轮。

## 5. 剩余轮次与验收要求

以下每行都是独立轮次，**完成该行的测试、RTL、报告后立即停止**。接口连线和生成入口适配归入当前轮，但不提前实现下一模块。

| 轮次 / 模块 | 本轮接口与功能 | 核验重点 |
| --- | --- | --- |
| **11 · SubwordRead（下一轮）** | 新模块；ReadDataWord、ByteOffset、Funct3 → ReadData；RV32 支持 LB/LBU/LH/LHU/LW，独立 RV64 增 LD/LWU。 | 合法偏移、符号边界、符号/零扩展；非法编码输出零。 |
| 12 · DTIM | 新模块；clk、Adr、MemRead、MemWrite、WriteDataWord、ByteMask → ReadDataWord；组合读、上升沿按字节写。 | 未选中字节保持、地址独立、容量、写边沿和禁止写入；无复位清空。 |
| 13 · LSU | 组合 DTIM 与三个辅助模块；消费 MemRW/Funct3/IEUAdr/WriteData，输出 ReadData；保留兼容 MemWrite，实际写入要求它与 MemRW[0] 同时有效。 | 混合 SB/SH/SW 与五种加载；字节隔离、符号读取；未对齐不写、读零。 |
| 14 · IROM | 核对取指存储；保留组合读取、容量、文件初始化、地址切片映射。 | 64/128 项、镜像、不同地址与高地址映射；说明仿真初始化边界。 |
| 15 · IFU | 核对 PC、PC+4、目标选择和 IROM；保留同步复位、resetVector，不重复清 JALR 目标位。 | 顺序/分支/跳转、回绕、无边沿 reset 脉冲。 |
| 16 · RiscvSingle | 接通升级 IFU/IEU/LSU；保留观察输出，新增 MemRW/Funct3/ByteMask；顶层复位期间禁止正常寄存器和存储写入。 | 第二章程序与第七章完整签名、子字事务、不同容量/复位地址、Chisel 和生成 RTL。 |

新增模块的源码与测试建议放在 `src/{main,test}/scala/riscvsingle/lsu`；按现有 `Generate*` 与 Makefile 风格提供独立生成入口。本阶段不加入旋转、RV64 字运算、流水线、总线、缓存或地址保护。

最终范围以表 7.1 的 RV32 列为准；FENCE、ECALL、EBREAK、CSR、异常、中断和扩展均不纳入。软件自然对齐；确定的未对齐读零/不写行为不代表异常处理。最终须确保每类指令具有定向和参考验证、原程序结果保持、子字写不破坏邻接字节、两种容量 Chisel 与 RTL 验证通过。

## 6. 恢复执行步骤与逐轮交付规则

后续执行者先读本报告、README、相关模块源码及对应核验报告，再确认 git 状态和最新用户授权。不要把第二章已实现的 IROM/IFU/LSU/RiscvSingle 当作已完成第七章的后续核验。

下一轮仅处理 **SubwordRead**，须先明确核验通过本轮并允许继续：

1. 重读教材 §7.1.6、图 7.9，以及 `/home/unlastingstar/cvw/src/lsu/subwordread.sv`；核对子字选择、符号/零扩展结构和 32/64 位独立模块范围。
2. 先增加 32/64 位定向与参考测试，再实现 ReadDataWord、ByteOffset、Funct3 → ReadData；核验合法偏移、符号边界和符号/零扩展，非法编码输出零。
3. 运行模块测试及 `./scripts/sbt-local.sh test`，生成两种宽度 RTL、简短中文测试说明和波形，核对实际端口；保持现有 CPU 合法程序通过。此轮不提前接入新 LSU 或实现 DTIM。
4. 在 `docs/modules` 新增独立报告，更新 README 索引及本进度表，记录实际命令/结果/RTL 路径/整机接通边界。
5. 标记“本轮停止，等待用户核验”，交付后停止；只有明确核验通过并允许继续，才进入下一轮。不自动提交、合并或连续实施；用户明确要求提交时执行相应提交。

每份模块报告须包含：教材/CVW 依据，参数默认值与合法范围，全部端口的方向/类型/位宽，功能编码和边界，内部信号及子模块连接，Chisel 名称与实际 RTL 名称区别，验证命令/结果/生成路径/接通状态，以及停点说明。

整机轮重新生成默认与扩容 RTL，更新 Verilator 验证脚本以检查子字写入，再运行 `make test-rtl`。程序先建立寄存器和 RAM 状态，再通过存储签名验收。

## 7. 资料入口

- 教材：`/home/unlastingstar/cvw-experiment/single-cycle/RISC-V System-on-Chip Design, Edition 1.pdf`；§7.1、图 7.2、表 7.1 为总依据，§7.1.6/图 7.9 为后续 LSU 依据。
- CVW：`/home/unlastingstar/cvw/src`；执行单元参照 `ieu/{ieu,controller,datapath}.sv`，访存参照 `lsu/{swbytemask,subwordwrite,subwordread,dtim,lsu}.sv`。只参考本轮相关结构，不移植其流水线或扩展接口。
- [README 与模块索引](../README.md)、[统一配置](../src/main/scala/riscvsingle/config/CpuConfig.scala)、[IEU 当前报告](modules/07-ieu.md)、[RTL 整机验证说明](12-rtl-validation.md)。

**当前授权：提交第 9/10 轮后继续 SubwordRead；第 11 轮交付后停止等待用户核验。**
