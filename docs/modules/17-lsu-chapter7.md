# LSU 核验报告（第七章第 13 轮）

验证日期：2026-10-04；交付记录：2026-10-05。**完成 RV32 子字访存连接，本轮停止等待核验；下一轮为 IROM。**
第二章接口与历史结果保留在 [原 LSU 报告](10-lsu.md)，当前接口以本报告和源码为准。

## 1. 教材与 CVW 对应

依据教材 §7.1.6（印刷页 314–316）、图 7.9；参考
`/home/unlastingstar/cvw/src/lsu/lsu.sv` 的 DTIM 实例与 Subword Accesses 连接。
生产逻辑采用 Chisel，生成 Verilog。

| 结构 | 本轮采用的部分 | 与 CVW 的边界 |
| --- | --- | --- |
| SubwordWrite | 复制低字节/半字/字作为原生写数据 | 完整 Funct3 校验，非法编码输出零 |
| SwByteMask | 由大小和字内偏移产生写字节使能 | 保留自然对齐检查，不实现跨字拼接 |
| DTIM | 原生字 RAM，按 ByteMask 更新字节 | 教材组合读、上升沿写；不移植同步读、停顿或冲刷接口 |
| SubwordRead | 小端子字提取、符号/零扩展 | LSU 增加合法性/对齐门控；不移植端序、浮点或流水接口 |

只连接已交付的四个生产模块，其源码与独立 32/64 位测试保持。LSU 与 CpuConfig 仍限定
RV32，不加入缓存、MMU、总线、原子操作、异常或未对齐跨字访问。

## 2. 参数与完整接口

源码：[LSU.scala](../../src/main/scala/riscvsingle/lsu/LSU.scala)。
构造接口保持 `LSU(config: CpuConfig = CpuConfig())`，继承 RawModule。
配置 xlen 固定为 32，dmemDepth 默认 64，单位为 32 位数据字；须为不小于 2 的二次幂，
总容量不超过 2^32 字节。其他 CpuConfig 字段不由 LSU 消费。

恰好 **7 个生产端口**，无 reset：

| Chisel 名称 | RTL 名称 | 方向 / 类型 | 位宽 | 含义 |
| --- | --- | --- | --- | --- |
| clk | clk | Input / Clock | 1 | RAM 写入上升沿 |
| io.MemWrite | io_MemWrite | Input / Bool | 1 | 保留的兼容存储使能 |
| io.MemRW | io_MemRW | Input / UInt | 2 | {读取请求，写入请求} |
| io.Funct3 | io_Funct3 | Input / UInt | 3 | 完整加载/存储编码 |
| io.IEUAdr | io_IEUAdr | Input / UInt | 32 | 字节地址 |
| io.WriteData | io_WriteData | Input / UInt | 32 | 寄存器原始写数据 |
| io.ReadData | io_ReadData | Output / UInt | 32 | 完成子字处理的加载值，禁用/非法/未对齐为零 |

MemRW 的两位独立：00 空闲、10 读取、01 写入；11 允许同时读写，用于接口与同址边沿测试，
不代表原子操作。Controller 正常指令只产生前三种请求。写入必须同时满足
`io.MemWrite && io.MemRW(0)`，仅任一信号有效不能写入。

## 3. 编码、对齐与时序

| Funct3 | 加载 | 存储 | 对齐要求 |
| --- | --- | --- | --- |
| 000 | LB，8 位符号扩展 | SB，只写低 8 位 | 任意字节地址 |
| 001 | LH，16 位符号扩展 | SH，只写低 16 位 | 地址 bit 0 = 0 |
| 010 | LW，完整 32 位 | SW，完整 32 位 | 地址 bits 1:0 = 00 |
| 100 | LBU，8 位零扩展 | 非法，不写 | 加载任意字节地址 |
| 101 | LHU，16 位零扩展 | 非法，不写 | 加载地址 bit 0 = 0 |
| 011/110/111 | 非法，读零 | 非法，不写 | 不访问 RAM 的有效读写路径 |

自然对齐按实际有效地址检查；地址寄存器的基值可以未对齐，只要加偏移后的有效地址满足要求。
RAM 索引仍忽略容量外高位，默认 64 字为 IEUAdr[7:2]，128 字为 IEUAdr[8:2]。
例如地址 0x100 的 LW 在默认容量中读字 0；地址 0x101 的 LW 输出零，LBU 则读取字 0 的 byte 1。
此确定行为是本阶段接口约定，不实现异常。

| 内部信号 | 功能与连接 |
| --- | --- |
| ByteOffset[1:0] | IEUAdr[1:0]，送给 SwByteMask 和 SubwordRead |
| HalfwordAligned / WordAligned | 半字/字自然对齐条件 |
| LoadAllowed | 完整 Funct3 与加载对齐同时合法 |
| MemRead | MemRW[1] && LoadAllowed，驱动 DTIM.MemRead |
| ByteMask[3:0] | SwByteMask 的大小/偏移掩码；不含请求门控，bit 0 对应最低字节 |
| MemWrite | 兼容 MemWrite && MemRW[0] && ByteMask 非零，驱动 DTIM.MemWrite |
| WriteDataWord[31:0] | SubwordWrite 输出，送 DTIM 写口 |
| ReadDataWord[31:0] | DTIM 输出，送 SubwordRead 进行加载扩展 |

ByteMask 是内部信号，本轮未新增生产观察端口。即使 ByteMask 非零，写入仍要求 MemWrite。
组合读取没有等待周期；同址同时读写时，上升沿前是旧值，写入稳定后是新值。
非法/未对齐加载将 DTIM 读输出置零，再经子字模块输出零；存储不能更新任何字节。

RAM 没有复位、清空或初始化；未写字节内容未指定。测试先通过合法 SW 建立待观察状态。
包装层 reset 不连接 LSU；复位期间独立 LSU 写口仍按请求工作。

## 4. 简短固定常量测试与波形

[LSUSubwordSpec.scala](../../src/test/scala/riscvsingle/lsu/LSUSubwordSpec.scala)
的首个用例用中文注释与固定常量演示以下操作；表内数据均为十六进制：

| 操作 | 字节地址 / 数据 | 期望结果 |
| --- | --- | --- |
| SW 初始化 | 0 / 11223344；4 / aabbccdd | 两字独立建立内容 |
| SB | 1 / deadbeef | 字 0 为 1122ef44，仅 byte 1 更新 |
| SH | 2 / face8001 | 字 0 为 8001ef44，仅高半字更新 |
| LB / LBU | 1 | ffffffef / 000000ef |
| LH / LHU | 2 | ffff8001 / 00008001 |
| LW | 0 | 8001ef44 |
| 未对齐 LH / LW、非法 LD | 1 / 2 / 0 | 读取零 |
| 未对齐 SH / SW、非法存储编码 100 | 1 / 2 / 0 | 字 0 保持 8001ef44 |
| 高位别名 LW / 邻接字 LW | 0x100 / 4 | 8001ef44 / aabbccdd |
| MemWrite=1、MemRW=00 | 0 | 不写；随后读回原字 |

在工程目录一条命令生成 VCD：

```bash
make test-lsu-wave SBT=./scripts/sbt-local.sh
gtkwave target/waveforms/lsu32.vcd waves/lsu32.gtkw
```

预选 15 条信号：时钟、MemRW、兼容/实际 MemWrite、Funct3、实际 MemRead、LoadAllowed、
ByteMask、地址/字节偏移/字索引、原始与复制写数据、原生读字和最终 ReadData。
全部路径与实际 VCD 核对；时间单位 1 ns，记录至 426 ns。
56 ns 读回 SB 后的 1122ef44，86 ns 读回 SH 后的 8001ef44；
106/126 ns 展示 LB/LBU 的符号区别，146/166 ns 展示 LH/LHU；206 ns 起未对齐/非法读取为零。
326 ns 的回绕读确认被拒绝的存储未破坏原字。时间只表示激励顺序，不代表硬件延迟。

## 5. 生成结果与 CPU 接口适配

[GenerateLSU.scala](../../src/main/scala/riscvsingle/GenerateLSU.scala) 的接口保持不变：
可选参数依次为数据字深度和输出目录，默认 `64 generated/lsu`。
Makefile 对应 DMEM_DEPTH、LSU_TARGET_DIR。

```bash
make generate-lsu SBT=./scripts/sbt-local.sh
make generate-lsu SBT=./scripts/sbt-local.sh DMEM_DEPTH=128 LSU_TARGET_DIR=generated/lsu128
```

交付 [64 项 LSU.v](../../generated/lsu/LSU.v)、[128 项 LSU.v](../../generated/lsu128/LSU.v)，
两者都为 RV32。每份导出包含 LSU、SwByteMask、SubwordWrite、DTIM、SubwordRead 五个定义，
实例名为 swByteMask、subwordWrite、dtim、subwordRead。DTIM 展开四个 8 位 RAM 通道。
独立 RTL 保留 ByteOffset、HalfwordAligned、WordAligned、LoadAllowed、ByteMask；
MemRead/MemWrite 优化为 dtim_io_MemRead/dtim_io_MemWrite 的连接表达式，
WriteDataWord/ReadDataWord 别名合并到子模块 IO。测试 VCD 保留上述 Chisel 信号名。
只有 DTIM 有上升沿状态更新，其他三个辅助模块和 LSU 的控制/连接均为组合逻辑。

[RiscvSingle.scala](../../src/main/scala/riscvsingle/RiscvSingle.scala) 只增加 IEU.MemRW/Funct3
到 LSU 的两条必要连接；重新生成默认/扩容 CPU RTL，内部 IEU 保留完整 12 端口，LSU 为 7 端口。
CPU 顶层仍为原 5 端口。io.MemWrite 是 IEU 的存储请求，不能单独代表对齐门控后的实际写入。
新增顶层 MemRW/Funct3/ByteMask 观察及复位期间写入屏蔽仍归第 16 轮。

扩容镜像原 PC=0x108 的 `00112023`（sw x1,0(x2)，x2=511）依赖旧 RAM 忽略低地址位。
本轮改为 `fe112ea3`（sw x1,-3(x2)），实际地址为对齐的 508；Scala/C++ 期望地址同步改为 508。
原负偏移加载、字 127/63 独立、JAL 链接与最终签名保持。历史报告中的地址 511 属于旧接口记录。

## 6. 验证证据

```bash
./scripts/sbt-local.sh 'testOnly riscvsingle.lsu.LSUSpec riscvsingle.lsu.LSUSubwordSpec'
./scripts/sbt-local.sh test
./scripts/sbt-local.sh \
  'runMain riscvsingle.GenerateLSU' \
  'runMain riscvsingle.GenerateLSU 128 generated/lsu128' \
  'runMain riscvsingle.GenerateRiscvSingle 64 64 generated/riscv-single programs/riscvtest.memfile 0' \
  'runMain riscvsingle.GenerateRiscvSingle 128 128 generated/riscv-single128 programs/rv32-configtest.memfile 0x100'
make test-rtl
verilator --lint-only --top-module LSU generated/lsu/LSU.v
verilator --lint-only --top-module LSU generated/lsu128/LSU.v
```

| 验证 | 实际结果 |
| --- | --- |
| 测试先行 | 新接口仍接旧整字 RAM：17 项中 12 失败、5 通过；实现后 17/17 通过 |
| 原字访存测试 | 保留 7 项并适配新合法请求；深度 2/64/128 全部字、LBU 字节偏移/高位别名、边沿、300 笔整字模型、原书程序及接口 |
| 新子字测试 | 10/10：固定常量、全部编码/偏移/控制、符号边界、最终沿前输入、双使能、复位保持、无沿脉冲、随机模型、IEU 写回、容量边界 |
| 全组合矩阵 | 8 Funct3 × 4 字节偏移 × 4 MemRW × 2 兼容 MemWrite = 256 组；检查沿前/沿后输出、完整字与邻接字 |
| 字节参考模型 | 种子 0x713，600 笔；枚举控制组合并随机地址/数据，最后检查全部 256 字节 |
| IEU/LSU 子字写回 | 15 个固定机器码，覆盖三种存储与五种加载，六个独立常量 RAM 签名 |
| 扩容镜像兼容修正 | 首次全回归 178/179，定位未对齐 SW；修正夹具后 CPU 4/4 通过 |
| 最终全工程 | 18 个套件，179/179 通过 |
| 生成 CPU RTL | 两种容量各种子 1/17/2026，共六次 PASS；原程序 7/25、扩容签名 31/7/链接值保持 |
| RTL 与波形 | LSU 64/128 项 lint 通过；VCD 与 15 条 GTKWave 路径核对通过 |
| 独立审查 | 功能、接口、测试模型、夹具解码与文档均通过，无剩余发现 |

日志为 `target/lsu-round13/{red,focused,cpu-focused,regression,regression-final,generate,rtl,wave,lint64,lint128,wave-check}.log`。
regression.log 保留首次失败，regression-final.log 为夹具修正后的最终全回归。
target、test_run_dir、生成 .fir/.anno.json 被忽略；源码、测试、生成 .v、报告、镜像和 GTKWave 配置为交付。
沿用 JDK 17、sbt 1.10.7、Scala 2.13.14、Chisel 3.6.1、chiseltest 0.6.2、Verilator 5.036。
没有进行 SRAM 映射、FPGA/ASIC 实现或时序分析。

## 7. 停点

第 13 轮 LSU 已完成，子字数据路径已通过必要连接接入 CPU；完整 RV32 整机程序、
公开观察接口和复位写入屏蔽仍留在后续轮次。**本轮停止等待检查，下一轮 IROM；未提交、合并或推送。**


第 16 轮接口补充：LSU 新增有效 `io.ByteMask` 输出，共 8 端口，原内部 ByteMask 保留尺寸/偏移译码。
输出仅在兼容 MemWrite、MemRW(0)、合法尺寸/对齐同时满足时有效；否则为零。
CPU 使用此输出生成实际 MemWrite，复位门控由顶层完成；独立 LSU 仍无 reset。
256 组组合测试新增有效掩码核对，详细当前接口见 [整机报告](20-riscv-single-chapter7.md)。
