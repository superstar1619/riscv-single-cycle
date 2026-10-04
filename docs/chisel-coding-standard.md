# Chisel 编码规范：教材 §4.3 的语言适配

本规范依据本地《RISC-V System-on-Chip Design, Edition 1》§4.3
“Wally Style Guidelines”，印刷页 181–190，涵盖 Guideline 4.1–4.44。
它保留教材强调的可读性、可追踪接口、同步设计和可配置性，按 Chisel 的硬件构造语义改写。
这是中文归纳与适配，不是原文翻译；下文明确区分教材原则、语言适配和补充规则。

当前工程基线：Scala 2.13.14、Chisel 3.6.1、chiseltest 0.6.2。
其他工程使用本规范时先核对实际版本，不能因为最新文档推荐某个 API 就升级依赖。
规范主要约束可综合硬件；测试、生成入口和参考模型采用适合软件的 Scala 风格。
文中“必须”用于正确性和已确定的工程约定，“建议”用于可按理由调整的风格。
明确的用户要求及已有接口契约优先；偏离时说明原因和硬件影响，不做无关重构。

## 1. 命名与源码组织

**教材原则：**名称一致，跨层次便于追踪，有流水级的信号应标明所属级。

**Chisel 适配：**区分硬件信号、模块类型与 Scala elaboration 数据，不强行采用单一大小写。

| 对象 | 本工程约定 | 示例 |
| --- | --- | --- |
| 模块、Bundle、配置类 | 大驼峰；已有缩写保持原样 | `ALU`、`ALUIO`、`CpuConfig` |
| 数据/控制端口、重要内部硬件节点 | 教材大驼峰 | `SrcA`、`ALUResult`、`PCNext` |
| 时钟、复位 | `clk`、`reset`；兼容已有公开接口 | `IO(Input(Clock()))` |
| Scala 参数、配置字段、辅助函数与模块实例 | 小驼峰 | `dataWidth`、`config.xlen`、`shifter` |
| Scala 固定架构常量 | 大写下划线 | `XLEN`、`OP_LOAD` |
| 硬件状态与下一状态 | 可读名称，明确区别 | `State`、`StateNext` |

配置字段不是 SystemVerilog 宏，不要求把现有 `config.xlen` 改成 `config.XLEN`。
固定 Scala 常量与硬件常量表达式应区分；`val OP_LOAD = 3` 是主机数据，
`val LoadOpcode = 3.U(7.W)` 是硬件字面量。
已有通用模块的 `a`、`b`、`y`、`rd` 等公开端口保持兼容，不为风格更名。

仅在真实流水设计中使用 `F/D/E/M/W` 后缀，例如 `PCF`、`InstrD`。
流水线产生的信号以产生级命名；危险处理等外部控制按使用级命名。
单周期 CPU 不添加虚假的流水级后缀。
确需区分不同来源时使用 `IEU`、`LSU`、`MDU` 等前缀，例如 `IEUResultM`。

一个主要模块放入同名 `.scala` 文件；同模块的 IO Bundle、紧密相关的小辅助类可同文件。
包名采用小写。按 IFU、IEU、LSU、配置等职责组织目录，测试结构对应生产源码。

## 2. 排版与注释

**教材保留：**使用空格，每层缩进两格；源码行宽上限 95 字符，合理分组使用空行。
代码中的二元运算符两侧、逗号之后留空格；`if (`、`for (` 使用 Scala 常规形式。
方法调用使用 `Mux(...)`、`when(...)`，圆括号内部不加无意义空格。
左花括号与声明或条件同一行；Chisel 条件块即使只有一条连接也建议保留花括号。
Scala 不需要行末分号。长参数列表和表达式换行，遵守已有 formatter 配置。

**语言适配：**不要求把多个端口塞到一行；Bundle 每个字段一个 `val`。
不强制靠空格对齐等号，避免改动一个类型造成整块排版变更。
不把 SystemVerilog 的 `begin/end`、端口声明和分号规则照搬到 Scala。
95 字符约束针对新增 Scala 示例和代码；Markdown 表格、链接及不可拆分标识符可例外。

文件/模块开头说明目的和必要的教材出处；较复杂模块补充接口单位、组合/时序属性、
输入到输出延迟、复位极性与同步性。已有版权/许可证头保持原样。
作者、邮箱、版权和许可证只能来自真实项目资料，AI 不得虚构，也不每次改动都刷新日期。
中文说明配合英文代码标识符。注释解释设计意图和边界，不逐行复述运算符。

```scala
// 错：翻转每一位，再加一。
// 好：以固定字宽的二补码表示取负，结果按该字宽回绕。
```

## 3. 硬件类型、位宽与运算

**教材适配：**用 Chisel 类型表达意图，而不是统一声明为 SystemVerilog `logic`。

- 单比特条件使用 `Bool`；无符号数据/编码使用 `UInt`；有符号算术使用 `SInt`。
- 用 `Wire`、`WireDefault`、`Reg`、`RegInit` 表明硬件类别；简单表达式可以直接绑定到 `val`。
  不强迫每个表达式都增加 `Wire`，但重要中间节点应有可追踪名称。
- `Int`、`BigInt`、`Boolean`、`Seq` 是 Scala 数据，用于生成硬件；不能当作运行时信号。
  `val` 固定的是 Scala 引用，不表示对应硬件信号永远不变；`var` 不是寄存器。
- 接口和架构状态明确位宽。表达式可推断位宽，但必须理解运算结果和连接端的宽度。
  关键字面量明确使用 `0.U(dataWidth.W)`、`1.S(dataWidth.W)`、`true.B`。

**Chisel 补充 C1：进位和截断。**`+` 不自动保留额外进位，扩大目的端也无法恢复已丢失的位。
要保留进位使用 `+&`；架构要求回绕时明确选取低位。乘法、动态移位和连续运算
同样需要检查中间结果宽度，不能仅检查最终端口。
参见 [官方位宽说明](https://www.chisel-lang.org/docs/explanations/width-inference)。

```scala
// 错：io.Sum 是 9 位，也无法挽回 8 位加法丢失的进位。
io.Sum := io.A + io.B

// 好：A、B 为 UInt(8.W)，完整结果为 9 位。
io.Sum := io.A +& io.B
```

**Chisel 补充 C2：符号解释。**`UInt` 的 `<` 是无符号比较；对已有二补码位型使用
`asSInt` 做有符号解释。`asUInt/asSInt` 改变解释，不是任意宽度的数值转换。
算术右移必须先保证左操作数为有符号类型；逻辑右移使用 `UInt`。

```scala
// 错：0xff 会被当作 255，而不是 -1。
io.LT := io.A < io.B

// 好：同宽二补码有符号比较。
io.LT := io.A.asSInt < io.B.asSInt
```

**Chisel 补充 C3：扩展与拼接。**切片使用 `Data(high, low)`，bit 0 是最低有效位。
`Cat(High, Low)` 将第一个参数放到高位。用 `pad` 或明确 `Cat/Fill` 表达扩展，
有意截断使用切片，不依赖隐式连接掩盖信息丢失。
教材的 `[31:0]` 约定在这里是位编号，不等于存储器字节序；字节序另由地址与字节通道定义。

```scala
// 前提：Src 为 8 位 UInt；明确输出位宽为 16 位。
val ZeroExtended = Cat(0.U(8.W), Src)
val SignExtended = Cat(Fill(8, Src(7)), Src)
val LowByte = WideData(7, 0)
```

拼接示例需要 `import chisel3.util.{Cat, Fill}`。
可参数化切片先检查边界，避免 `dataWidth == 1`、深度为 1 等场景产生负下标或零位地址。
只支持特定配置时用 `require` 明确限制，不假装已支持所有正整数。
`&&/||/!` 用于 `Bool` 条件；`&/|/^/~` 用于位运算；硬件相等/不等使用 `===/=/=`。
位掩码与单比特使能组合时明确扩展或使用 `Mux`，不要依赖隐式类型混合。

以下是完整可编译的组合示例，所有配置都保留相同字段。
短示例采用匿名 Bundle，显式导入 `reflectiveCalls` 避免 Scala 结构类型访问警告；
生产模块的较大或复用接口建议采用单独命名的 IO Bundle，与现有工程保持一致。

```scala
import chisel3._
import chisel3.util.{Cat, Fill}
import scala.language.reflectiveCalls

/** 同宽加法与比较；无时钟、复位或状态，输出纯组合。 */
final class ArithmeticExample(dataWidth: Int = 8) extends RawModule {
  require(dataWidth > 0, "dataWidth must be positive")

  val io = IO(new Bundle {
    val A = Input(UInt(dataWidth.W))
    val B = Input(UInt(dataWidth.W))
    val Sum = Output(UInt((dataWidth + 1).W))
    val WrappedSum = Output(UInt(dataWidth.W))
    val LT = Output(Bool())
    val LTU = Output(Bool())
    val ZeroExtended = Output(UInt((2 * dataWidth).W))
    val SignExtended = Output(UInt((2 * dataWidth).W))
  })

  val SumExt = io.A +& io.B
  io.Sum := SumExt
  io.WrappedSum := SumExt(dataWidth - 1, 0)
  io.LT := io.A.asSInt < io.B.asSInt
  io.LTU := io.A < io.B
  io.ZeroExtended := Cat(0.U(dataWidth.W), io.A)
  io.SignExtended := Cat(Fill(dataWidth, io.A(dataWidth - 1)), io.A)
}
```

## 4. 条件、连接与组合逻辑

**Chisel 补充 C4：区分生成阶段与硬件运行阶段。**

| 写法 | 执行阶段与含义 |
| --- | --- |
| Scala `if (config.enabled)` | elaboration 时决定生成哪种硬件 |
| Scala `for (i <- 0 until n)` | elaboration 时重复构造；可能生成 n 份硬件，不表示执行 n 周期 |
| `when(Enable)` / `Mux(Select, A, B)` | 用运行时硬件条件生成控制逻辑或选择器 |
| Scala `match` | 对 Scala 数据选择；不替代硬件 `switch/is` 或硬件译码 |
| Scala `require(...)` | 检查构造参数，不生成硬件运行时断言 |
| Chisel `assert(...)` | 检查硬件运行时条件，需要适当时钟域与复位上下文 |

硬件条件不能传给 Scala `if`；也不能认为 Scala 循环创建了多周期控制器。
只有需求本来是静态裁剪时才使用配置条件；数据选择使用 `Mux`、`MuxLookup`、
`when/.elsewhen/.otherwise` 或 `switch/is`。后两者是生成硬件，不是运行 Scala 分支。
辅助 `def` 可以构造逻辑，但调用多次可能生成多份硬件；注释/命名应让成本可理解。

**Chisel 补充 C5：组合驱动完整性和覆盖顺序。**
组合输出/`Wire` 必须在所有合法输入下有驱动。建议先默认赋值，再按条件覆盖。
Chisel 的后续连接可以有意覆盖前面的连接；这是优先级语义，不是软件变量顺序执行。
独立 `when` 条件可同时为真时明确谁优先；互斥选择优先使用一条条件链。
不能把不完整组合连接解释成寄存器保持或有意推断锁存器。
参见 [官方连接和条件说明](https://www.chisel-lang.org/docs/explanations/combinational-circuits)。

```scala
// 错：Enable 为假时没有驱动。
val Y = Wire(UInt(8.W))
when(io.Enable) { Y := io.A }

// 好：禁用时输出确定的零；不是自动增加状态。
val Y = WireDefault(0.U(8.W))
when(io.Enable) { Y := io.A }
```

完整示例把优先级直接表达在一条条件链中：

```scala
import chisel3._
import scala.language.reflectiveCalls

/** Clear 优先于 Enable；组合结果不保存上一周期的值。 */
final class CombSelectExample extends RawModule {
  val io = IO(new Bundle {
    val A = Input(UInt(8.W))
    val Enable = Input(Bool())
    val Clear = Input(Bool())
    val Y = Output(UInt(8.W))
  })

  val Y = WireDefault(0.U(8.W))
  when(io.Clear) {
    Y := 0.U(8.W)
  }.elsewhen(io.Enable) {
    Y := io.A
  }
  io.Y := Y
}
```

`DontCare` 不是确定的零，也不是教材四态 `x` 的直接替代。
需要安全值的控制输出、非法译码默认值和禁用功能输出必须显式赋值。
输入不能因为暂时没使用就随意悬空；未使用子模块输出可以不连接到其他逻辑。

## 5. 时钟、复位、寄存器与存储器

**教材保留：**每个时钟域内采用上升沿同步设计；组合逻辑、寄存器和标准存储器是主要构件。
不自行生成组合门控时钟、负沿寄存器或锁存器。寄存器更新条件用使能表达。
必要的异步复位或跨时钟接口要显式记录并采用合适方案；本规范不提供 CDC 实现。

**教材适配：**Chisel 原生 `Reg/RegInit/RegNext/RegEnable` 就是状态构造工具，
无需把每个触发器或 mux 强制封装成独立 Module。标准存储器可以用原生构造，
工艺存储器、特殊复位或独立复用需求再通过专用模块/BlackBox 封装。

本工程纯组合模块采用 `RawModule`，没有为了测试而添加生产时钟端口。
有状态模块沿用显式 `clk: Clock`、`reset: Bool`，在 `withClockAndReset` 域中创建状态。
不要把已验收的显式端口改成 `Module` 的默认 `clock/reset`。
其他工程若已有 `Module` 隐式时钟约定，则保持其接口；不能让本项目约定扩大成普遍禁令。
在 `RawModule` 内实例化带隐式时钟的子模块时，也必须提供正确的时钟/复位上下文。

**Chisel 补充 C6：复位与保持分开规定。**

- `RegInit` 指定复位值；复位类型取决于所在域。`Bool` 域是同步复位，`AsyncReset` 域是
  异步复位，抽象 `Reset` 由上下文推断。不能只看到 `RegInit` 就宣称同步复位。
- 普通 `Reg` 没有复位清零逻辑；不能依赖其上电值。没有更新赋值的周期保持旧值。
- 普通 `Reg` 若在复位期间仍满足写条件，仍可能更新。需求是“复位期间保持”时明确抑制写入；
  不得以添加 `RegInit(0.U)` 实现保持，因为它改变了行为。
- `RegNext` 在不传初始化值时不自动复位。使能、flush/stall 和复位优先级必须符合模块契约。

以上语义依据 [官方复位说明](https://www.chisel-lang.org/docs/explanations/reset)
及 [时序电路说明](https://www.chisel-lang.org/docs/explanations/sequential-circuits)。

```scala
import chisel3._
import scala.language.reflectiveCalls

/** Q 同步复位为零；HeldQ 无复位值，且复位期间禁止更新。 */
final class RegisterExample extends RawModule {
  val clk = IO(Input(Clock()))
  val reset = IO(Input(Bool()))
  val io = IO(new Bundle {
    val D = Input(UInt(8.W))
    val Enable = Input(Bool())
    val Q = Output(UInt(8.W))
    val HeldQ = Output(UInt(8.W))
  })

  val Q = withClockAndReset(clk, reset) {
    RegInit(0.U(8.W))
  }
  val HeldQ = withClock(clk) {
    Reg(UInt(8.W))
  }

  when(io.Enable) {
    Q := io.D
  }
  when(!reset && io.Enable) {
    HeldQ := io.D
  }
  io.Q := Q
  io.HeldQ := HeldQ
}
```

`HeldQ` 只有一次有效写入后才有可验证的已知值；测试不得先假设它为零。
生产逻辑不使用模拟延时、`Thread.sleep` 或测试 `clock.step` 表达硬件等待。
多周期操作必须有寄存器、计数或状态机；测试的周期推进只写在测试代码。

**Chisel 补充 C7：存储器时序属于接口契约。**
`Mem` 表达组合读、同步写存储器；`SyncReadMem` 表达同步读、同步写存储器。
前者读取时地址直接影响数据，后者读取存在时钟沿/周期延迟；切换构造会改变系统时序。
不要为了追随书中的同步存储器偏好，直接替换当前单周期 CPU 的组合读 IROM/LSU。
同步存储器的无效读周期、读写同址和多写冲突要按实际契约处理，不能默认读输出保持。
参见 [官方存储器说明](https://www.chisel-lang.org/docs/explanations/memories)。

存储器深度用“字/条目数”，地址注明“字节地址/字索引”；字节掩码注明各位对应的通道。
`Vec` 表达硬件数组，`Seq` 表达 elaboration 集合，二者不是可互换的存储器。
复位并不自动清空 `Mem/SyncReadMem`；镜像加载、FPGA 初始化和 ASIC 上电保证需分开说明。
只在需求规定时初始化全部寄存器或存储器，避免无谓增加复位网络和改变推断结果。

## 6. 接口、连接与层次

**教材保留：**非通用模块跨边界尽量保持名称一致；层次应使功能和数据流容易理解。
叶模块可行为描述；数据通路按运算、选择和状态块组织，复杂控制独立。
Chisel 原生 `Mux`、运算符和 `Reg` 可以直接使用，不为“结构式”形式增加无意义薄模块。

**语言适配：**IO 使用 `Bundle` 并明确 `Input/Output`，数组接口可直接使用 `Vec`。
无需沿用教材对 SystemVerilog 数组端口的限制；接口是否采用数组由硬件契约决定。
普通内部总线使用单向信号；只有外部引脚确需双向电气语义时才使用顶层 `Analog` 和相应封装。
`Analog` 不是内部数据网络或普通 `UInt` 的替代。

**Chisel 补充 C8：选择可理解的连接方式。**

- 对普通信号使用 `sink := source`，命名连接即 `child.io.SrcA := io.SrcA`。
  不机械模仿 SystemVerilog 实例化的按位置连线或 `.*`。
- 对方向清晰、兼容的接口可以使用 `<>`；混合方向 Bundle 不能随意用整体 `:=` 替代。
  不把 `<>` 当作电气双向总线，也不认为它自动提供协议控制或缓冲。
- `Decoupled` 的 `valid/ready/bits` 保留标准名称；使用 `Flipped` 表达对端方向，
  按协议定义握手与等待期间行为，不为了大驼峰约定重命名库字段。
- 使用聚合连接前检查字段、方向、位宽和数组长度；非对应接口显式适配。
  功能禁用时也要驱动其接口中需要驱动的字段。

参见 [官方接口与连接说明](https://www.chisel-lang.org/docs/explanations/interfaces-and-connections)。
该网站会更新；本工程保持 Chisel 3.6.1 可用的 `:=`、`<>`，
不引入新版本推荐但当前未核验的 Connectable 操作符。

辅助函数可以封装明确的组合变换、常量或构造模式，这是 Scala/Chisel 的正常用法。
含状态、存储器或子模块实例的辅助函数应在调用处让这些构件和复制次数可见。
不用深层继承、隐式副作用或复杂集合变换掩盖优先级、资源数量与关键数据路径。

## 7. 配置与稳定接口

**教材保留：**共用参数集中管理，局部参数独立，可选功能不让外部接口随开关消失。

**语言适配：**共享配置使用不可变 `case class`，如当前 `CpuConfig`，
由上层把同一配置传递给子模块；简单独立模块使用 `dataWidth: Int` 等构造参数。
不用 SystemVerilog `config.vh`、文本宏或全局可变状态替代 Scala 配置对象。
参数名称、单位、支持集合及限制明确；新增限制先确认与已有契约一致。

**Chisel 补充 C9：参数合法性与层次可追踪。**
用 Scala `require` 检查正位宽、深度、对齐、值域与支持的架构配置。
若允许单条目集合，要明确处理 `log2Ceil(1) == 0`；否则在构造时明确拒绝。
配置分支/循环在 elaboration 时展开；重要子模块和节点用稳定的 `val` 名称。
必要时使用 `suggestName` 改善 RTL 调试，但不保证优化后所有节点都存在，
也不依靠自动生成的层次名作为公共硬件接口。

**Chisel 补充 C10：功能开关保持字段与宽度契约。**
可选功能的端口在条件外声明；开关只选择内部逻辑，禁用时输出确定的约定值。
字段集合和方向稳定；参数化位宽可以按已声明的配置契约变化，并不要求所有配置位宽相同。
需求明确要配置相关 IO 时需另定接口约定，不能悄悄删除字段。

```scala
// 错：关闭功能时，端口会在 elaboration 阶段消失。
if (config.enableFeature) {
  val FeatureOut = IO(Output(UInt(8.W)))
  FeatureOut := 0.U(8.W)
}
```

以下完整示例在两个配置下都有 `A` 和 `FeatureOut`：

```scala
import chisel3._
import scala.language.reflectiveCalls

final case class FeatureConfig(dataWidth: Int = 8, enableFeature: Boolean = true) {
  require(dataWidth > 0, "dataWidth must be positive")
}

/** 功能关闭时输出零；两种配置的端口名称、方向和同宽配置的位宽一致。 */
final class FeatureExample(config: FeatureConfig) extends RawModule {
  val io = IO(new Bundle {
    val A = Input(UInt(config.dataWidth.W))
    val FeatureOut = Output(UInt(config.dataWidth.W))
  })

  if (config.enableFeature) {
    io.FeatureOut := io.A
  } else {
    io.FeatureOut := 0.U(config.dataWidth.W)
  }
}
```

## 8. 生成、测试与评审

**Chisel 补充 C11：验证生成结果，区分验证层级。**
Scala 编译通过只证明类型与语法，elaboration/RTL 生成检查硬件合法性，
仿真检查指定输入与时序下的行为。代码评审应核对生成的端口、位宽、时钟、复位和状态，
不能仅凭源码形式宣称硬件等价，也不能把仿真通过当作时序收敛或 SRAM 映射的证明。

针对改动选择有意义的边界：进位/回绕、正负数边界、非法译码、使能保持、复位时序、
参数最小值与功能开关。使用独立预期值，不把 RTL 表达式原样抄入测试充当参考模型。
本项目已有测试入口为 `./scripts/sbt-local.sh 'testOnly <完整测试类名>'`；
执行前确认工具链可用。无关文档编辑无需重新生成已有 RTL。
纯组合模块如需时钟驱动的测试工具，可添加测试包装模块，生产模块维持无时钟接口。

评审记录建议使用“位置—条款—影响—建议”的形式：

| 类别 | 判断与报告 |
| --- | --- |
| 功能问题 | 给出触发输入、错误结果或时序影响，以及最小修正 |
| 接口/配置问题 | 给出受影响配置、字段/方向/宽度变化和连接风险 |
| 风格建议 | 标明不改变功能；已有兼容接口不自动更名 |
| 尚未验证 | 说明未运行的检查及原因，不声称通过 |

## 9. 教材 44 条指南逐条映射

“保留”指原则与做法基本一致；“适配”指保留意图并改写语言/工程形式；
“不适用”仅指 SystemVerilog 专属语法，下表给出 Chisel 对应处理。

| 指南 | 原主题（归纳） | 处理 | 本规范对应 |
| --- | --- | --- | --- |
| 4.1 | 时钟、复位小写 | 保留 | §1、§5：`clk/reset`，兼容既有接口 |
| 4.2 | 信号大驼峰 | 保留 | §1：本工程硬件命名，不扩展到库字段 |
| 4.3 | 常量、参数大写 | 适配 | §1、§7：固定常量大写，Scala 参数/配置小驼峰 |
| 4.4 | 流水级后缀 | 适配 | §1：仅真实流水级使用，单周期不添加 |
| 4.5 | 来源前缀 | 保留 | §1：需要区分来源时使用单元缩写 |
| 4.6 | 内部信号显式声明 | 适配 | §3：命名绑定与硬件类型，允许直接表达式 |
| 4.7 | 双向引脚只在芯片顶层 | 适配 | §6：外部 `Analog`，内部单向逻辑 |
| 4.8 | 普通信号统一用 logic | 适配 | §3：`Bool/UInt/SInt` 与 Wire/Reg |
| 4.9 | 总线 `[N-1:0]` | 适配 | §3 C3：bit 0 为 LSB，区分字节序 |
| 4.10 | 避免数组端口及 var logic | 适配 | §6：允许合理 `Vec` 接口，无 SV 语法限制 |
| 4.11 | 简单模块按位置连端口 | 不适用 | §6 C8：原生构造或字段显式连接 |
| 4.12 | 其他模块按名称连接 | 适配 | §6 C8：`child.io.Field := Source` |
| 4.13 | 单次使用模块跨边界同名 | 保留 | §6：名称便于追踪，确有变换才改名 |
| 4.14 | 避免 .*，包装模块例外 | 适配 | §6 C8：按兼容性和方向判断聚合连接 |
| 4.15 | 一行放多个端口连接 | 适配 | §2、§6：优先清晰字段连接，不强求压缩 |
| 4.16 | 用空格而非 tab | 保留 | §2：空格缩进 |
| 4.17 | 两空格缩进 | 保留 | §2：每层两格 |
| 4.18 | 对齐信号声明 | 适配 | §2：不强制手工对齐，沿用 formatter |
| 4.19 | 运算符和条件语句间距 | 适配 | §2：Scala/Chisel 对应语法习惯 |
| 4.20 | 标点前后留白 | 适配 | §2：Scala 标点和方法调用，无行末分号 |
| 4.21 | 单句省略 begin/end | 不适用 | §2：Chisel 条件保留花括号 |
| 4.22 | begin 与条件同行 | 适配 | §2：左花括号同行 |
| 4.23 | 95 字符行宽 | 保留 | §2：Scala 源码上限，Markdown 合理例外 |
| 4.24 | 合理使用空行分组 | 保留 | §2：按接口、状态、逻辑段分组 |
| 4.25 | 少用大段分隔注释 | 保留 | §2：复杂模块必要时分组，优先职责清晰 |
| 4.26 | 同步时序设计 | 保留 | §5：每域上升沿，必要异步边界明确说明 |
| 4.27 | 存储器、触发器用模块实例 | 适配 | §5：原生 Reg/存储器，必要时专用封装 |
| 4.28 | 延时仅用于测试 | 适配 | §5：生产硬件无模拟延时，测试按周期推进 |
| 4.29 | 模块与文件同名 | 保留 | §1：主要模块同名文件，关联 Bundle 可同文件 |
| 4.30 | 模块名、括号和端口排版 | 不适用 | §1、§2、§6：Scala class 与 IO Bundle |
| 4.31 | 参数化模块声明排版 | 不适用 | §2、§7：Scala 构造参数列表 |
| 4.32 | 同方向同宽端口一行并对齐 | 适配 | §2、§6：每字段一个 val，明确方向和宽度 |
| 4.33 | 叶模块行为描述 | 保留 | §6：原生运算、条件和状态构造 |
| 4.34 | 数据通路尽量结构描述 | 适配 | §6：可理解的数据流，不要求薄 mux/flop 模块 |
| 4.35 | 小而清晰的层次 | 保留 | §1、§6：按职责分层，避免过度拆分 |
| 4.36 | 避免隐藏硬件的编程结构 | 适配 | §4、§6：允许 def，说明状态/资源与复制成本 |
| 4.37 | 合理使用可配置通用模块 | 保留 | §7：参数化需有实际复用目的 |
| 4.38 | 参数结构与顶层配置文件 | 适配 | §7：不可变 case class，由上层传递 |
| 4.39 | 模块局部参数 | 适配 | §7：Scala 构造参数与 require |
| 4.40 | 功能开关不改变接口 | 保留 | §7 C10：端口在条件外定义，禁用值明确 |
| 4.41 | 省略 generate/endgenerate | 不适用 | §4：Scala if/for 做 elaboration |
| 4.42 | generate 块有稳定名称 | 适配 | §7 C9：稳定 val 名称，必要时 suggestName |
| 4.43 | 文件头目的、作者、日期、许可 | 适配 | §2：目的与出处保留，身份/许可证不得虚构 |
| 4.44 | 注释解释大意而非复述代码 | 保留 | §2：意图、时序、单位和边界 |

## 10. 来源与使用

主要依据是本地 PDF §4.3；上表是摘要，设计语义补充不是对教材的新归因。
Chisel API 额外参考 [官方数据类型说明](https://www.chisel-lang.org/docs/explanations/data-types)
和 [Scala 类型与 Chisel 类型区别](https://www.chisel-lang.org/docs/explanations/chisel-type-vs-scala-type)。
在线文档可能包含新版本接口，本规范完整示例以本项目版本验证为准。

后续 AI 可使用项目本地 [chisel-coding-standard skill](../.agents/skills/chisel-coding-standard/SKILL.md)。
示例请求：`使用 $chisel-coding-standard 编写或审查这个 Chisel 模块，保留现有接口。`
如果工具未自动发现项目技能，直接要求读取该 `SKILL.md`。
skill 引用本文件；迁移到其他项目时同时复制规范并保持或调整相对路径。
