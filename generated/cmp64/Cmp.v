module Cmp(
  input  [63:0] io_R1, // @[src/main/scala/riscvsingle/ieu/Cmp.scala 22:14]
  input  [63:0] io_R2, // @[src/main/scala/riscvsingle/ieu/Cmp.scala 22:14]
  output        io_Eq, // @[src/main/scala/riscvsingle/ieu/Cmp.scala 22:14]
  output        io_LT, // @[src/main/scala/riscvsingle/ieu/Cmp.scala 22:14]
  output        io_LTU // @[src/main/scala/riscvsingle/ieu/Cmp.scala 22:14]
);
  wire [63:0] af = io_R1 ^ 64'h8000000000000000; // @[src/main/scala/riscvsingle/ieu/Cmp.scala 29:15]
  wire [63:0] bf = io_R2 ^ 64'h8000000000000000; // @[src/main/scala/riscvsingle/ieu/Cmp.scala 30:15]
  assign io_Eq = io_R1 == io_R2; // @[src/main/scala/riscvsingle/ieu/Cmp.scala 35:21]
  assign io_LT = af < bf; // @[src/main/scala/riscvsingle/ieu/Cmp.scala 36:18]
  assign io_LTU = io_R1 < io_R2; // @[src/main/scala/riscvsingle/ieu/Cmp.scala 37:23]
endmodule
