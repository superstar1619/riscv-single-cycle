module Extend(
  input  [24:0] io_Instr, // @[src/main/scala/riscvsingle/ieu/Extend.scala 19:14]
  input  [1:0]  io_ImmSrc, // @[src/main/scala/riscvsingle/ieu/Extend.scala 19:14]
  output [31:0] io_ImmExt // @[src/main/scala/riscvsingle/ieu/Extend.scala 19:14]
);
  wire [31:0] InstrFull = {io_Instr,7'h0}; // @[src/main/scala/riscvsingle/ieu/Extend.scala 23:19]
  wire [19:0] _ImmI_T_1 = InstrFull[31] ? 20'hfffff : 20'h0; // @[src/main/scala/riscvsingle/ieu/Extend.scala 30:19]
  wire [31:0] ImmI = {_ImmI_T_1,InstrFull[31:20]}; // @[src/main/scala/riscvsingle/ieu/Extend.scala 30:14]
  wire [31:0] ImmS = {_ImmI_T_1,InstrFull[31:25],InstrFull[11:7]}; // @[src/main/scala/riscvsingle/ieu/Extend.scala 31:14]
  wire [31:0] ImmB = {_ImmI_T_1,InstrFull[7],InstrFull[30:25],InstrFull[11:8],1'h0}; // @[src/main/scala/riscvsingle/ieu/Extend.scala 32:14]
  wire [11:0] _ImmJ_T_1 = InstrFull[31] ? 12'hfff : 12'h0; // @[src/main/scala/riscvsingle/ieu/Extend.scala 34:19]
  wire [31:0] ImmJ = {_ImmJ_T_1,InstrFull[19:12],InstrFull[20],InstrFull[30:21],1'h0}; // @[src/main/scala/riscvsingle/ieu/Extend.scala 34:14]
  wire [31:0] _io_ImmExt_T_1 = 2'h1 == io_ImmSrc ? ImmS : ImmI; // @[src/main/scala/riscvsingle/ieu/Extend.scala 39:56]
  wire [31:0] _io_ImmExt_T_3 = 2'h2 == io_ImmSrc ? ImmB : _io_ImmExt_T_1; // @[src/main/scala/riscvsingle/ieu/Extend.scala 39:56]
  assign io_ImmExt = 2'h3 == io_ImmSrc ? ImmJ : _io_ImmExt_T_3; // @[src/main/scala/riscvsingle/ieu/Extend.scala 39:56]
endmodule
