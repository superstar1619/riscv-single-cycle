module SwByteMask(
  input  [2:0] io_Funct3, // @[src/main/scala/riscvsingle/lsu/SwByteMask.scala 20:14]
  input  [1:0] io_ByteOffset, // @[src/main/scala/riscvsingle/lsu/SwByteMask.scala 20:14]
  output [3:0] io_ByteMask // @[src/main/scala/riscvsingle/lsu/SwByteMask.scala 20:14]
);
  wire [3:0] _AccessBytes_T_1 = 3'h0 == io_Funct3 ? 4'h1 : 4'h0; // @[src/main/scala/chisel3/util/Mux.scala 77:13]
  wire [3:0] _AccessBytes_T_3 = 3'h1 == io_Funct3 ? 4'h2 : _AccessBytes_T_1; // @[src/main/scala/chisel3/util/Mux.scala 77:13]
  wire [3:0] AccessBytes = 3'h2 == io_Funct3 ? 4'h4 : _AccessBytes_T_3; // @[src/main/scala/chisel3/util/Mux.scala 77:13]
  wire [3:0] _BaseMask_T_1 = 4'h1 == AccessBytes ? 4'h1 : 4'h0; // @[src/main/scala/chisel3/util/Mux.scala 77:13]
  wire [3:0] _BaseMask_T_3 = 4'h2 == AccessBytes ? 4'h3 : _BaseMask_T_1; // @[src/main/scala/chisel3/util/Mux.scala 77:13]
  wire [3:0] BaseMask = 4'h4 == AccessBytes ? 4'hf : _BaseMask_T_3; // @[src/main/scala/chisel3/util/Mux.scala 77:13]
  wire [3:0] _Aligned_T_1 = AccessBytes - 4'h1; // @[src/main/scala/riscvsingle/lsu/SwByteMask.scala 38:44]
  wire [3:0] _GEN_0 = {{2'd0}, io_ByteOffset}; // @[src/main/scala/riscvsingle/lsu/SwByteMask.scala 38:29]
  wire [3:0] _Aligned_T_2 = _GEN_0 & _Aligned_T_1; // @[src/main/scala/riscvsingle/lsu/SwByteMask.scala 38:29]
  wire  Aligned = _Aligned_T_2 == 4'h0; // @[src/main/scala/riscvsingle/lsu/SwByteMask.scala 38:52]
  wire [6:0] _GEN_1 = {{3'd0}, BaseMask}; // @[src/main/scala/riscvsingle/lsu/SwByteMask.scala 40:15]
  wire [6:0] _ByteMask_T_2 = _GEN_1 << io_ByteOffset; // @[src/main/scala/riscvsingle/lsu/SwByteMask.scala 40:15]
  assign io_ByteMask = AccessBytes != 4'h0 & Aligned ? _ByteMask_T_2[3:0] : 4'h0; // @[src/main/scala/riscvsingle/lsu/SwByteMask.scala 39:18]
endmodule
