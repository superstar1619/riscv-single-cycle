module Cmp(
  input  [31:0] io_R1, // @[src/main/scala/riscvsingle/ieu/Cmp.scala 20:14]
  input  [31:0] io_R2, // @[src/main/scala/riscvsingle/ieu/Cmp.scala 20:14]
  output        io_Eq, // @[src/main/scala/riscvsingle/ieu/Cmp.scala 20:14]
  output        io_LT, // @[src/main/scala/riscvsingle/ieu/Cmp.scala 20:14]
  output        io_LTU // @[src/main/scala/riscvsingle/ieu/Cmp.scala 20:14]
);
  wire [31:0] af = io_R1 ^ 32'h80000000; // @[src/main/scala/riscvsingle/ieu/Cmp.scala 27:21]
  wire [31:0] bf = io_R2 ^ 32'h80000000; // @[src/main/scala/riscvsingle/ieu/Cmp.scala 28:21]
  assign io_Eq = io_R1 == io_R2; // @[src/main/scala/riscvsingle/ieu/Cmp.scala 33:21]
  assign io_LT = af < bf; // @[src/main/scala/riscvsingle/ieu/Cmp.scala 34:24]
  assign io_LTU = io_R1 < io_R2; // @[src/main/scala/riscvsingle/ieu/Cmp.scala 35:23]
endmodule
