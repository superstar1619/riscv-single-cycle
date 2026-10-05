module Shifter(
  input  [31:0] io_A, // @[src/main/scala/riscvsingle/ieu/Shifter.scala 22:14]
  input  [4:0]  io_Amt, // @[src/main/scala/riscvsingle/ieu/Shifter.scala 22:14]
  input         io_Right, // @[src/main/scala/riscvsingle/ieu/Shifter.scala 22:14]
  input         io_SubArith, // @[src/main/scala/riscvsingle/ieu/Shifter.scala 22:14]
  output [31:0] io_Y // @[src/main/scala/riscvsingle/ieu/Shifter.scala 22:14]
);
  wire  Sign = io_A[31] & io_SubArith; // @[src/main/scala/riscvsingle/ieu/Shifter.scala 29:31]
  wire [30:0] _Z_T = Sign ? 31'h7fffffff : 31'h0; // @[src/main/scala/riscvsingle/ieu/Shifter.scala 30:30]
  wire [62:0] _Z_T_1 = {_Z_T,io_A}; // @[src/main/scala/riscvsingle/ieu/Shifter.scala 30:25]
  wire [62:0] _Z_T_2 = {io_A,31'h0}; // @[src/main/scala/riscvsingle/ieu/Shifter.scala 31:8]
  wire [62:0] Z = io_Right ? _Z_T_1 : _Z_T_2; // @[src/main/scala/riscvsingle/ieu/Shifter.scala 30:11]
  wire [4:0] _Offset_T = ~io_Amt; // @[src/main/scala/riscvsingle/ieu/Shifter.scala 33:35]
  wire [4:0] Offset = io_Right ? io_Amt : _Offset_T; // @[src/main/scala/riscvsingle/ieu/Shifter.scala 33:16]
  wire [62:0] ZShift = Z >> Offset; // @[src/main/scala/riscvsingle/ieu/Shifter.scala 34:15]
  assign io_Y = ZShift[31:0]; // @[src/main/scala/riscvsingle/ieu/Shifter.scala 35:17]
endmodule
