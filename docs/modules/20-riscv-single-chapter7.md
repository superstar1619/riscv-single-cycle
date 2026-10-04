# RiscvSingle 第七章整机验收（第 16 轮）

验证日期：2026-10-05。第七章 16 轮全部完成，生产硬件均由 Chisel 生成。
教材依据为 §7.1、图 7.2、表 7.1；已阅读 CVW
`src/wally/wallypipelinedcore.sv` 的 IFU/IEU/LSU 连接，参考其 MemRW、Funct3、
IEUAdr、WriteData 数据流。本工程采用教材单周期组合读、上升沿写；流水线、
缓存、总线、异常、CSR 和扩展不纳入。本报告取代第二章历史报告的当前接口描述。

## 接口与复位

`RiscvSingle(config: CpuConfig = CpuConfig())` 为 RawModule，整机限定 RV32。

| Chisel / RTL | 方向 / 类型 / 位宽 | 含义 |
| --- | --- | --- |
| clk | Input Clock 1 | PC、RF 和 DTIM 上升沿更新 |
| reset | Input Bool 1 | 高有效；PC/x0 同步复位，正常写入屏蔽 |
| io.WriteData / io_WriteData | Output UInt 32 | 原始 R2；存储时有效的低位由 LSU 复制 |
| io.IEUAdr / io_IEUAdr | Output UInt 32 | IEU 加减地址结果，JALR bit 0 已清零 |
| io.MemWrite / io_MemWrite | Output Bool 1 | 实际合法、对齐且允许的存储，等于 ByteMask.orR |
| io.MemRW / io_MemRW | Output UInt 2 | {读请求,写请求}；复位期间为零 |
| io.Funct3 / io_Funct3 | Output UInt 3 | 原始 Instr[14:12]，包括非法指令和复位期间 |
| io.ByteMask / io_ByteMask | Output UInt 4 | 实际允许写入的字节通道；bit 0 对应最低字节 |

顶层共 8 端口。兼容原观察端口名称；MemWrite 由原 IEU 请求改为实际写入使能。
例如未对齐 SH：MemRW=01、Funct3=001，但 ByteMask=0000、MemWrite=0。
非法指令关闭 Controller 的写回、访存和跳转控制，不实现陷阱。
ByteMask=0 不表示读请求不存在；合法加载 MemRW=10 且 ByteMask=0。

LSU 新增同名 ByteMask 输出，也为 8 端口；内部 ByteMask 保留尺寸/偏移译码结果，
对外通过两位请求、兼容 MemWrite 和对齐条件门控。独立 LSU 无 reset。
顶层 reset 立即屏蔽 LSU 读写请求；PC/RF 状态仍只在上升沿复位。
寄存器复位仅清 x0、保留其他寄存器且禁止正常写入；RAM 无复位/初始化且内容保持。
未写入寄存器与字节的值未指定，验收程序先建立所需状态。

默认 CpuConfig 为 imemDepth=64、dmemDepth=64、resetVector=0、无指令初始化文件。
生成入口默认使用教材程序。容量以 32 位字计，地址按字节计，小端、容量高位回绕。
自然对齐检查位于 LSU，未对齐读零、不写的工程约定保持。

## 汇编、机器码与独立预期

[rv32-acceptance.s](../../programs/rv32-acceptance.s) 显式固定 RV32I、关闭压缩和松弛，
不使用栈。实际 114 条指令、110 个执行周期，包括最后 5 个自循环周期。
所有 37 类教材 RV32 指令至少执行一次；六种条件分支分别覆盖 taken/untaken。
另外覆盖移位量 33、正负边界、负立即数、LUI/AUIPC、JAL/JALR 链接、奇数 JALR 目标、
小端子字保留与符号/零扩展、未对齐访问、四种非法编码、最后一个字节。

| 配置 | IROM / DTIM 字数 | 复位地址 | 机器码 / 逐周期预期 |
| --- | --- | --- | --- |
| 教材原程序 | 64 / 64 | 0 | riscvtest.memfile；原地址 96=7、100=25 保持 |
| 容量程序 | 128 / 128 | 0x100 | rv32-configtest.memfile；区分 RAM 字 127/63 |
| 完整验收 | 128 / 64 | 0 | [memfile](../../programs/rv32-acceptance.memfile)、[trace](../../programs/rv32-acceptance.trace) |
| 完整扩容验收 | 256 / 128 | 0x100 | [memfile](../../programs/rv32-acceptance-expanded.memfile)、[trace](../../programs/rv32-acceptance-expanded.trace) |

[生成脚本](../../scripts/build-rv32-acceptance.py) 调用 GNU RISC-V as/ld/objcopy/nm/objdump，
独立软件模型按 RV32 语义执行，再用手工常量与汇编符号核对 33 个固定字签名。
签名区为 128..248，另有 RAM[0]=800100ff、RAM[252]=01000000。
JSON 记录覆盖、配置、符号与固定签名；反汇编和 ELF 位于 target/acceptance-images。
所有 ROM 条目明确填充，扩容前缀和结尾为 NOP。

trace 列为十六进制：PC、Instr、地址、地址有效、MemRW、Funct3、有效 ByteMask、原始数据。
Chisel 与 C++ 均在边沿前核对生产观察口，再推进真实 CPU；没有硬件参考模型。
LUI/非法指令的无关地址可能依赖未初始化 RF，此时地址有效列为零；其控制口仍核对。
数据仅在有效存储时核对。实际加载结果经生产 DTIM/LSU 写回，再由签名 SW 观察。

## 测试与命令

```bash
make build-acceptance
make test-acceptance SBT=./scripts/sbt-local.sh
./scripts/sbt-local.sh test
make generate-cpu SBT=./scripts/sbt-local.sh
make generate-cpu SBT=./scripts/sbt-local.sh IMEM_DEPTH=128 DMEM_DEPTH=128 CPU_TARGET_DIR=generated/riscv-single128 INSTRUCTION_INIT_FILE=programs/rv32-configtest.memfile RESET_VECTOR=0x100
make generate-acceptance SBT=./scripts/sbt-local.sh
make generate-lsu SBT=./scripts/sbt-local.sh
make generate-lsu SBT=./scripts/sbt-local.sh DMEM_DEPTH=128 LSU_TARGET_DIR=generated/lsu128
make test-rtl
make test-final-waves SBT=./scripts/sbt-local.sh
```

工具链前缀可通过 RISCV_TOOL_PREFIX 指定；默认 riscv64-unknown-elf-，生成 RV32 ELF。
test-rtl 使用已生成 RTL，不自动重新生成；修改硬件后先执行对应生成命令。

| 核验 | 结果 |
| --- | --- |
| 复位 RED→GREEN | 原硬件两项均失败；修复后两项通过：覆盖写 99 的复位保留旧 RAM=9，以及复位向量 SW/待写 RF 屏蔽 |
| CPU 模块 | 原 4 项保持，完整验收 2 项、复位 2 项，共 8/8 |
| LSU | 17/17；256 组请求/编码/偏移/兼容组合增加有效掩码检查 |
| 全工程 | 20 套件，188/188 |
| RTL | 四份完整 CPU，各种子 1/17/2026，共 12 次 PASS；验收每次 110 周期、37 次有效存储 |
| 结构 | 顶层 8 端口、16 模块层次、3 个 posedge 状态块；四字节 RAM 通道和配置传播检查通过 |
| 最终波形 | 12/12；两份完整 CPU 各 110 周期、33 签名和全部预选路径通过 |

日志位于 target/chapter7-final：cpu-reset-{red,green}.log、cpu-acceptance.log、
cpu-regression-generate.log、cpu-rtl.log、final-waves-verified.log、final-regression.log。
新 RTL 为 generated/rv32-acceptance{,-expanded}/RiscvSingle.v；原两份 CPU 和两份 LSU
同步重新生成。所有生产 Verilog 均来自 Chisel，无手写生产 RTL。

## 最终波形

全部硬件功能通过后，按用户要求统一生成和核对。一条命令：
`make test-final-waves SBT=./scripts/sbt-local.sh`。
产物在 target/waveforms，GTKWave 配置在 waves：
完整 CPU 两种配置、教材原程序、IROM、IFU、LSU、SubwordWrite32/64、
SubwordRead32/64、DTIM32/64，共 12 份。

[核对脚本](../../scripts/check-final-waves.py) 读取 VCD 稳定时间组及上升沿前数据，
检查两种完整 CPU 的 PC/Instr/PC+4/请求/掩码/数据/复位与 33 个固定签名，
并检查原程序签名、IFU 边沿/目标/回绕、IROM 复位保持、子字复制/扩展及 DTIM/LSU
字节写入与组合读取。所有 GTKWave 预选路径必须实际存在。Treadle 结束测试时归零输入但不重新求值，
脚本排除最后清理时间组；全部有效执行上升沿仍纳入核对。

查看示例：
`gtkwave target/waveforms/rv32-acceptance.vcd waves/rv32-acceptance.gtkw`。
生产组合模块没有观察时钟，VCD 时钟来自测试包装层。

验证为仿真功能与接口检查；尚未进行综合、布局布线、四态 X 传播或形式证明。
IROM 文件加载只用于仿真，FPGA/ASIC 程序固化仍需目标平台流程。


## 独立审查与可选改进

最终独立审查覆盖 886e708 之后的第 11–16 轮及未提交整机实现，核对硬件、
RTL 一致性、独立模型与签名、配置/复位和最终波形；无 Critical/Important 问题，
可提交。审查重新计算两份 trace/签名与检查 12 份波形，均匹配。

两项 Minor 留作可选测试增强，本轮不阻塞交付：

- 完整 CPU 汇编的非法指令目标均为 x0；未来可改用已初始化的非零目标并存储其保留值。
  当前非法寄存器写入抑制已由既有 IEU 测试直接覆盖，整机仍核对非法访存和跳转关闭。
- 波形脚本针对当前 Treadle 排除最后清理时间组；未来接入其他 VCD 生产者时可显式
  校验清理格式。当前所有 110 个执行上升沿已经核对，替换生产者需重新确认此约定。
