module Cmp(
  input  [63:0] io_R1, // @[src/main/scala/riscvsingle/ieu/Cmp.scala 19:14]
  input  [63:0] io_R2, // @[src/main/scala/riscvsingle/ieu/Cmp.scala 19:14]
  output        io_Eq // @[src/main/scala/riscvsingle/ieu/Cmp.scala 19:14]
);
  assign io_Eq = io_R1 == io_R2; // @[src/main/scala/riscvsingle/ieu/Cmp.scala 22:21]
endmodule
