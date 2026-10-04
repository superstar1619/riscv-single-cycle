# IROM 第七章核验（第 14 轮）

验证日期：2026-10-05。教材 §7.1.5、图 7.2；已阅读 CVW `src/ifu/irom.sv`。
现有 Chisel 实现符合单周期需求，保留硬件行为，完善中文说明和边界测试。
第二章历史记录见 [08-irom.md](08-irom.md)。

## 接口与配置

`IROM(config: CpuConfig = CpuConfig())` 为 RawModule，无时钟和复位。

| Chisel / RTL 端口 | 方向 / 类型 / 位宽 | 含义 |
| --- | --- | --- |
| io.a / io_a | Input UInt 32 | 字节地址，保留公开名称 |
| io.rd / io_rd | Output UInt 32 | 组合指令读取 |

imemDepth 默认 64，单位为 32 位指令字，须为不小于 2 的二次幂且容量不超过 2^32 字节。
instructionInitFile 默认 None；指定文件时只在仿真初始化加载，复位不重载。
没有文件或文件未覆盖的条目，其值未指定。xlen 保持 32，不消费数据存储配置。

`WordIndex = a[log2Ceil(imemDepth)+1:2]`：默认切片 [7:2]，128 项 [8:2]。
低两位和容量外高位被忽略。Mem 的零延迟读端口使用常低占位时钟；RTL 没有地址寄存器，
读数不依赖时钟沿。内部 ROM 为 32 位数组，WordIndex 在 RTL 中合并到读口地址。

CVW 通过 rom1p1r 和 ce 实现同步读取，支持原生 64 位及压缩指令半字选择。
本工程只参考索引结构，采用教材组合读取，不移植这些流水线/扩展路径。
Chisel 生成的 `$readmemh` 位于仿真条件块，不能当作 FPGA/ASIC 程序固化保证。

## 命令与结果

```bash
./scripts/sbt-local.sh 'testOnly riscvsingle.ifu.IROMSpec'
./scripts/sbt-local.sh test
make generate-irom SBT=./scripts/sbt-local.sh
make generate-irom SBT=./scripts/sbt-local.sh IMEM_DEPTH=128 IROM_TARGET_DIR=generated/irom128 INSTRUCTION_INIT_FILE=programs/rv32-configtest.memfile
```

| 检查 | 结果 |
| --- | --- |
| IROM 模块 | 8/8；原 6 项保持，增加完整扩容镜像边界与包装层复位保持 |
| 地址映射 | 深度 2/64/128、全部条目、四个字节偏移、四类高位别名，无时钟推进 |
| 镜像 | 原程序全部指令/NOP，扩容镜像 0xfc/0x100/0x108/0x134/0x1fc 及高位别名固定机器码 |
| 接口 | 两端口、组合读取、可选文件初始化；包装层 reset 不改变生产 ROM |
| 全工程 | 18 套件，181/181 |
| RTL | [64 项](../../generated/irom/IROM.v)、[128 项](../../generated/irom128/IROM.v) 已生成；后者使用完整 128 项镜像 |

日志：`target/chapter7-final/irom-{focused,regression,generate}.log`。
由于硬件已符合要求，新增验证直接通过；本轮未引入功能变更，不人为制造失败。
按用户最新指令，测试通过后提交并进入 IFU；波形统一留到全部模块完成后生成和核对。


最终第 16 轮统一生成并核对了本模块波形：`target/waveforms/irom32.vcd`、
`waves/irom32.gtkw`；命令 `make test-final-waves SBT=./scripts/sbt-local.sh`。
结果及其他最终波形见 [整机验收报告](20-riscv-single-chapter7.md)。
