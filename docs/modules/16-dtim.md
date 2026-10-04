# DTIM 核验报告（第七章第 12 轮）

记录日期：2026-10-04。**本轮完成独立 DTIM，停止等待核验；下一轮为 LSU。**

## 1. 教材与 CVW 对应

教材 §7.1.6（印刷页 314–316）、图 7.9 要求单周期数据存储器组合读取、时钟上升沿写入；
RAM 按原生字访问，子字存储通过 ByteMask 选择字节通道，不使用读改写。
参考文件为 `/home/unlastingstar/cvw/src/lsu/dtim.sv` 和
`/home/unlastingstar/cvw/src/generic/mem/ram1p1rwbe.sv`。

| 项目 | CVW | 本工程 |
| --- | --- | --- |
| 字索引 | 从 DTIMAdr 截取容量内的字地址 | 从 Adr 截取相同用途的字索引；容量外高位回绕 |
| 写通道 | ram1p1rwbe 的 bwe 逐字节控制 | Chisel `Mem(depth, Vec(byteCount, UInt(8.W)))` 掩码写 |
| 读取时序 | ce 有效的上升沿保存读地址，随后输出所保存地址的数据 | 不保存读地址，组合读取，符合教材 §7.1.6 |
| 读禁用 | ce=0 保持读地址及相应输出 | MemRead=0 输出零；独立项目接口约定 |
| 写请求 | ce、MemRWM[0] 和 ~FlushW 门控 | MemWrite 与 ByteMask；不引入流水线或陷阱信号 |
| 配置 | LLEN、DTIM_RANGE、PA_BITS、USE_SRAM 等 | dataWidth、depth；无 SRAM 宏、缓存或总线接口 |

生产硬件全部由 Chisel 编写，Verilog 由生成入口输出。参考的是地址映射和字节使能结构，
读取时序采用教材单周期要求。没有复制 CVW 的流水级命名、ce、FlushW 或扩展接口。

## 2. 参数与完整接口

源码：[DTIM.scala](../../src/main/scala/riscvsingle/lsu/DTIM.scala)。
构造接口为 `DTIM(dataWidth: Int = 32, depth: Int = 64)`。

| 参数 | 默认值 | 单位与约束 |
| --- | --- | --- |
| dataWidth | 32 | 原生数据位宽，只允许 32 或 64 |
| depth | 64 | 原生字数量；不小于 2 的二次幂；总字节容量不超过 2^32 |
| 容量 | 256 字节 | depth × dataWidth / 8；默认 64 位配置为 512 字节 |

32 位配置的最大合法深度为 2^30，64 位为 2^29；大容量仅由参数约束规定，
本轮实际功能测试深度为 2、4、64、128。默认位宽和深度与 RV32 的 CpuConfig 一致；
独立 64 位 DTIM 的每字为 8 字节，不改变整机 RV32 范围。

模块继承 RawModule，恰好 **7 个端口**，无 reset。

| Chisel 端口 | 方向 / 类型 | 32 位配置 | 64 位配置 | 含义 |
| --- | --- | --- | --- | --- |
| clk | Input / Clock | 1 | 1 | 写入上升沿 |
| io.Adr | Input / UInt | 32 | 64 | 字节地址 |
| io.MemRead | Input / Bool | 1 | 1 | 读取使能，0 时输出零 |
| io.MemWrite | Input / Bool | 1 | 1 | 上升沿写入使能 |
| io.WriteDataWord | Input / UInt | 32 | 64 | 已复制的原生字写数据 |
| io.ByteMask | Input / UInt | 4 | 8 | 每位控制一个字节，bit 0 对应最低字节 |
| io.ReadDataWord | Output / UInt | 32 | 64 | 原生字读数据，尚未做子字提取或扩展 |

DTIM 不接收 Funct3，也不检查合法编码或自然对齐。写数据复制、字节掩码和读取扩展
分别由 SubwordWrite、SwByteMask、SubwordRead 完成，下一轮 LSU 负责连接和对齐门控。

## 3. 数据与时序

`byteCount = dataWidth / 8`，`indexWidth = log2Ceil(depth)`，
`byteOffsetWidth = log2Ceil(byteCount)`。

```text
WordIndex = Adr[indexWidth + byteOffsetWidth - 1 : byteOffsetWidth]
ReadDataWord = MemRead ? RAM[WordIndex] : 0
在 clk 上升沿：若 MemWrite && ByteMask(i)，更新 RAM[WordIndex] 的 byte i
```

低地址位只表示字内偏移，不用于 RAM 索引；容量外高位被忽略。
默认 32 位使用 Adr[7:2]，默认 64 位使用 Adr[8:3]。
例如深度 4 时，32 位的地址 0、3、16、19 访问同一原生字；这不是对齐检查。

字节排列采用小端：WriteBytes(0) = WriteDataWord[7:0]，
ReadBytes(0) 恢复到 ReadDataWord[7:0]。ByteMask 可以是任意位组合，零掩码不会写入。
MemRead 与 MemWrite 独立：读取禁用仍允许写入，两者同时有效也合法。
同地址写请求在上升沿前仍读到旧内容，写入并组合稳定后读到新内容，没有写数据旁路。

RAM 没有复位、清空或镜像初始化。首次写入前的字节内容未指定；局部写入不会使未写字节
变为已知值。测试全部通过生产写口初始化待观察内容，包装层 reset 不连接 DTIM。
生成 RTL 中 Chisel 标准 `RANDOMIZE_MEM_INIT` 条件块仅用于仿真随机初值，
不能视为 RAM 的功能初始化或 FPGA/ASIC 上电保证。

## 4. 固定常量示例与波形

测试：[DTIMSpec.scala](../../src/test/scala/riscvsingle/lsu/DTIMSpec.scala)。
首个用例给出下面的固定常量，按“掩码、写数据、完整读回字”排列，便于逐步观察。
这些例子直接驱动 DTIM 的原生字接口；SB/SH/SW/SD 标注表示所模拟的字节更新范围。

| 配置 / 步骤 | ByteMask | WriteDataWord | 上升沿后 ReadDataWord |
| --- | --- | --- | --- |
| 32 位初始化 | f | 11223344 | 11223344 |
| SB：byte 1 | 2 | a5a5a5a5 | 1122a544 |
| SH：高半字 | c | f00df00d | f00da544 |
| SW：整字 | f | 89abcdef | 89abcdef |
| 64 位初始化 | ff | 1122334455667788 | 1122334455667788 |
| SB：byte 1 | 02 | a5a5a5a5a5a5a5a5 | 112233445566a588 |
| SH：bytes 2–3 | 0c | f00df00df00df00d | 11223344f00da588 |
| SW：高字 | f0 | 89abcdef89abcdef | 89abcdeff00da588 |
| SD：整个双字 | ff | deadbeef01234567 | deadbeef01234567 |

随后展示 MemWrite=0 和零掩码保持、MemRead=0 时写入 byte 0、邻接字读取及容量回绕。
32 位最终字为 89abcd5a，64 位为 deadbeef0123455a；邻接字保持原值。

一条命令生成两份 VCD：

```bash
make test-dtim-wave SBT=./scripts/sbt-local.sh
```

查看预选信号：

```bash
gtkwave target/waveforms/dtim32.vcd waves/dtim32.gtkw
gtkwave target/waveforms/dtim64.vcd waves/dtim64.gtkw
```

配置包含包装层 clock、MemRead、MemWrite、ByteMask、Adr、WriteDataWord、ReadDataWord
及 dut.WordIndex，两个宽度各 8 条路径均与实际 VCD 核对。clock 驱动生产模块的 clk。
本轮 VCD 时间单位为 1 ns：32 位记录至 296 ns，64 位至 326 ns。
首次读取已初始化字在 46 ns；两种宽度的局部写读回变化位于 71、101、131 ns，
64 位整双字写入在 161 ns；禁用读取期间的 byte 0 更新重新使能后分别在 216、246 ns 可见。
仿真时间仅是测试激励安排，不代表硬件延迟或时序收敛结果。

## 5. 生成与实际 RTL

入口：[GenerateDTIM.scala](../../src/main/scala/riscvsingle/GenerateDTIM.scala)。
可选参数依次为位宽、原生字深度、输出目录，默认 `32 64 generated/dtim`。
Makefile 对应 WIDTH、DMEM_DEPTH、DTIM_TARGET_DIR。

```bash
make generate-dtim SBT=./scripts/sbt-local.sh
make generate-dtim SBT=./scripts/sbt-local.sh WIDTH=64 DTIM_TARGET_DIR=generated/dtim64
# 可选深度：128 个原生字
make generate-dtim SBT=./scripts/sbt-local.sh DMEM_DEPTH=128 DTIM_TARGET_DIR=target/dtim128
```

交付 RTL：[32 位 DTIM.v](../../generated/dtim/DTIM.v)、
[64 位 DTIM.v](../../generated/dtim64/DTIM.v)，两者深度均为 64。

| Chisel 名称 | 实际 RTL 表达 |
| --- | --- |
| io.Adr 等 IO | io_Adr、io_MemRead、io_MemWrite、io_WriteDataWord、io_ByteMask、io_ReadDataWord |
| RAM | 32 位 RAM_0…RAM_3，64 位 RAM_0…RAM_7；各为 64 × 8 位数组 |
| WordIndex | 独立 RTL 优化为 io_Adr 的索引切片；测试 VCD 中保留 dut.WordIndex |
| ReadBytes | RAM_i_ReadBytes_data，按高字节到低字节拼接 |
| WriteBytes | 写口数据直接取 io_WriteDataWord 对应字节 |
| 写门控 | RAM_i_MPORT_en = io_MemWrite，mask = io_ByteMask[i]；posedge clk 中逐通道写 |

读数组访问与 MemRead 输出选择均为连续赋值，无读地址寄存器、读延迟或锁存器。
独立 RTL 没有子模块实例；原生 Mem 的 Vec 字节通道由生成器展开为字节数组。
RTL 与仿真证明接口和功能行为，不证明综合后 SRAM 映射或时序收敛。

## 6. 验证结果

```bash
./scripts/sbt-local.sh 'testOnly riscvsingle.lsu.DTIMSpec'
./scripts/sbt-local.sh test
verilator --lint-only --top-module DTIM generated/dtim/DTIM.v
verilator --lint-only --top-module DTIM generated/dtim64/DTIM.v
make test-rtl
```

| 验证 | 实际结果 |
| --- | --- |
| 测试先行 | 常零骨架运行 20 项，18 项功能和 1 项 RAM 结构测试失败，参数测试通过 |
| DTIM 模块 | 20/20 通过；其中 18 项功能测试、接口/RTL 结构与参数测试各 1 项 |
| 地址映射 | 两种宽度、深度 2/64/128 的全部字，每个字节偏移及四类高位别名；读检查不推进时钟 |
| 字节使能 | 32 位全部 16 种掩码、64 位全部 256 种；选中字节更新、其他字节和邻接字保持 |
| 边沿与控制 | 沿前旧值、沿后新值；地址/数据/掩码沿前改变；无沿写脉冲；读写独立、零掩码 |
| 复位边界 | 包装层复位保持 RAM，复位期间 RAM 写口仍可正常写入；生产无 reset |
| 随机参考 | 每种宽度 300 笔，种子 0xd712 + width；四种读写使能各 75 笔；独立字节数组模型 |
| 接口与参数 | 两种宽度、深度 2/64/128 均为 7 端口；字节 RAM 和 posedge clk；21 个非法配置拒绝 |
| RTL / 波形 | 两种 RTL 成功生成、lint 通过；两份 VCD 和 GTKWave 信号路径通过核对 |
| 全工程 | 17 个套件，169/169 通过 |
| 现有 CPU RTL | 默认/扩容配置各三个种子，共六次 PASS |
| 独立审查 | 功能、接口、测试与报告审查通过，无剩余发现；最终源码复验日志已核对 |

日志位于 `target/dtim-round12/{red,focused,regression,generate32,generate64,wave,rtl,lint32,lint64}.log`。
target、test_run_dir、生成 .fir/.anno.json 被忽略；源码、测试、生成 .v、报告和 GTKWave 配置为交付文件。
工具链沿用 JDK 17、sbt 1.10.7、Scala 2.13.14、Chisel 3.6.1、chiseltest 0.6.2、Verilator 5.036。

## 7. 接通边界与停点

本轮没有修改旧 LSU 或 CPU。SwByteMask、SubwordWrite、SubwordRead 和 DTIM 均已独立完成，
整机仍使用第二章整字 RAM；现有 CPU RTL 回归验证已有字访存程序。
下一轮 LSU 连接四个模块，实施自然对齐检查、非法编码抑制以及 MemRW/Funct3 处理。
顶层复位期间屏蔽写入仍归第 16 轮。**本轮停止，等待用户核验；未自动提交、合并或推送。**
