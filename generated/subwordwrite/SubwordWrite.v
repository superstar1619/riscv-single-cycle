module SubwordWrite(
  input  [31:0] io_WriteData, // @[src/main/scala/riscvsingle/lsu/SubwordWrite.scala 23:14]
  input  [2:0]  io_Funct3, // @[src/main/scala/riscvsingle/lsu/SubwordWrite.scala 23:14]
  output [31:0] io_WriteDataWord // @[src/main/scala/riscvsingle/lsu/SubwordWrite.scala 23:14]
);
  wire [31:0] ByteCopies = {io_WriteData[7:0],io_WriteData[7:0],io_WriteData[7:0],io_WriteData[7:0]}; // @[src/main/scala/riscvsingle/lsu/SubwordWrite.scala 30:21]
  wire [31:0] HalfwordCopies = {io_WriteData[15:0],io_WriteData[15:0]}; // @[src/main/scala/riscvsingle/lsu/SubwordWrite.scala 31:25]
  wire [31:0] _io_WriteDataWord_T_1 = 3'h0 == io_Funct3 ? ByteCopies : 32'h0; // @[src/main/scala/riscvsingle/lsu/SubwordWrite.scala 42:61]
  wire [31:0] _io_WriteDataWord_T_3 = 3'h1 == io_Funct3 ? HalfwordCopies : _io_WriteDataWord_T_1; // @[src/main/scala/riscvsingle/lsu/SubwordWrite.scala 42:61]
  assign io_WriteDataWord = 3'h2 == io_Funct3 ? io_WriteData : _io_WriteDataWord_T_3; // @[src/main/scala/riscvsingle/lsu/SubwordWrite.scala 42:61]
endmodule
