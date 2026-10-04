# IFU 第七章核验（第 15 轮）

验证日期：2026-10-05。教材 §7.1.5、图 7.2；已阅读 CVW `src/ifu/ifu.sv` 中的
PCNext、pcreg、PC 递增、无预测器目标选择及 IROM 连接。
现有 Chisel 已符合单周期要求，保留行为并完善中文说明与参考模型验证。
第二章历史见 [09-ifu.md](09-ifu.md)。

## 接口与状态

`IFU(config: CpuConfig = CpuConfig())` 为 RawModule，7 个端口：

| Chisel / RTL | 方向 / 类型 / 位宽 | 功能 |
| --- | --- | --- |
| clk | Input Clock 1 | PC 更新上升沿 |
| reset | Input Bool 1 | 高有效同步复位 |
| io.PCSrc / io_PCSrc | Input Bool 1 | 0 顺序，1 目标 |
| io.IEUAdr / io_IEUAdr | Input UInt 32 | IEU 已处理的跳转/分支目标 |
| io.Instr / io_Instr | Output UInt 32 | 当前 PC 的组合取指 |
| io.PC / io_PC | Output UInt 32 | 当前 PC |
| io.PCPlus4 / io_PCPlus4 | Output UInt 32 | PC+4，按 32 位回绕 |

config 默认 xlen=32、imemDepth=64、resetVector=0、instructionInitFile=None。
容量与镜像传给 IROM，resetVector 是 4 字节对齐的 32 位字节地址；不消费 dmemDepth。

`PCNext = PCSrc ? IEUAdr : PCPlus4`。pcreg 在上升沿更新，reset 优先加载 resetVector。
reset 的电平变化或无沿脉冲不改变 PC；释放 reset 也不立即推进 PC。
目标低位原样保留，JALR 清除 bit 0 已由 IEU 完成，IFU 不重复清位。
IROM 忽略低两位及容量外高位，这不表示实现了指令对齐异常。

CVW 增加停顿、冲刷、预测、陷阱、压缩指令与同步存储；本轮只参考单周期所需的数据流。
不移植 CVW 的统一 PC 清位路径。本工程 PC 为完整 32 位，增量固定为 4。
内部 pcreg、PCNext、irom 在 RTL 中保留；只有 pcreg 有 posedge clk 更新。

## 命令与结果

```bash
./scripts/sbt-local.sh 'testOnly riscvsingle.ifu.IFUSpec'
./scripts/sbt-local.sh test
make generate-ifu SBT=./scripts/sbt-local.sh
make generate-ifu SBT=./scripts/sbt-local.sh IMEM_DEPTH=128 IFU_TARGET_DIR=generated/ifu128 INSTRUCTION_INIT_FILE=programs/rv32-configtest.memfile RESET_VECTOR=0x100
```

| 检查 | 结果 |
| --- | --- |
| IFU 模块 | 9/9，保留原 6 项并增加 3 项固定种子 PC 模型 |
| 参考模型 | 深度/复位地址 2/0、64/0x100、128/0x100，各 300 周期；沿前/沿后 PC、PC+4 和镜像字全部检查 |
| 边界 | 顺序/目标选择、沿前改变目标、奇数目标不清位、32 位回绕、同步复位优先及无沿脉冲 |
| 原程序 | 实际 IFU/IEU 执行，保留 7/25 存储结果 |
| 接口 | 7 端口，单一同步 PC 更新与 IROM 实例 |
| 全工程 | 18 套件，184/184 |
| RTL | [默认](../../generated/ifu/IFU.v)、[128 项/复位 0x100](../../generated/ifu128/IFU.v) 已生成；后者使用完整扩容镜像 |

日志：`target/chapter7-final/ifu-{focused,regression,generate}.log`。
硬件没有新增行为，新增核验直接通过；没有人为制造失败。
按最新用户指令，测试通过后提交并进入 RiscvSingle，波形在全部模块完成后统一生成/核对。


最终第 16 轮统一生成并核对了本模块波形：`target/waveforms/ifu32.vcd`、
`waves/ifu32.gtkw`；命令 `make test-final-waves SBT=./scripts/sbt-local.sh`。
结果及其他最终波形见 [整机验收报告](20-riscv-single-chapter7.md)。
