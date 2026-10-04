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
  wire [3:0] _Aligned_T_1 = AccessBytes - 4'h1; // @[src/main/scala/riscvsingle/lsu/SwByteMask.scala 39:44]
  wire [3:0] _GEN_0 = {{2'd0}, io_ByteOffset}; // @[src/main/scala/riscvsingle/lsu/SwByteMask.scala 39:29]
  wire [3:0] _Aligned_T_2 = _GEN_0 & _Aligned_T_1; // @[src/main/scala/riscvsingle/lsu/SwByteMask.scala 39:29]
  wire  Aligned = _Aligned_T_2 == 4'h0; // @[src/main/scala/riscvsingle/lsu/SwByteMask.scala 39:52]
  wire [6:0] _GEN_1 = {{3'd0}, BaseMask}; // @[src/main/scala/riscvsingle/lsu/SwByteMask.scala 41:15]
  wire [6:0] _ByteMask_T_2 = _GEN_1 << io_ByteOffset; // @[src/main/scala/riscvsingle/lsu/SwByteMask.scala 41:15]
  assign io_ByteMask = AccessBytes != 4'h0 & Aligned ? _ByteMask_T_2[3:0] : 4'h0; // @[src/main/scala/riscvsingle/lsu/SwByteMask.scala 40:18]
endmodule
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
module DTIM(
  input         clk, // @[src/main/scala/riscvsingle/lsu/DTIM.scala 28:15]
  input  [31:0] io_Adr, // @[src/main/scala/riscvsingle/lsu/DTIM.scala 29:14]
  input         io_MemRead, // @[src/main/scala/riscvsingle/lsu/DTIM.scala 29:14]
  input         io_MemWrite, // @[src/main/scala/riscvsingle/lsu/DTIM.scala 29:14]
  input  [31:0] io_WriteDataWord, // @[src/main/scala/riscvsingle/lsu/DTIM.scala 29:14]
  input  [3:0]  io_ByteMask, // @[src/main/scala/riscvsingle/lsu/DTIM.scala 29:14]
  output [31:0] io_ReadDataWord // @[src/main/scala/riscvsingle/lsu/DTIM.scala 29:14]
);
`ifdef RANDOMIZE_MEM_INIT
  reg [31:0] _RAND_0;
  reg [31:0] _RAND_1;
  reg [31:0] _RAND_2;
  reg [31:0] _RAND_3;
`endif // RANDOMIZE_MEM_INIT
  reg [7:0] RAM_0 [0:63]; // @[src/main/scala/riscvsingle/lsu/DTIM.scala 37:33]
  wire  RAM_0_ReadBytes_en; // @[src/main/scala/riscvsingle/lsu/DTIM.scala 37:33]
  wire [5:0] RAM_0_ReadBytes_addr; // @[src/main/scala/riscvsingle/lsu/DTIM.scala 37:33]
  wire [7:0] RAM_0_ReadBytes_data; // @[src/main/scala/riscvsingle/lsu/DTIM.scala 37:33]
  wire [7:0] RAM_0_MPORT_data; // @[src/main/scala/riscvsingle/lsu/DTIM.scala 37:33]
  wire [5:0] RAM_0_MPORT_addr; // @[src/main/scala/riscvsingle/lsu/DTIM.scala 37:33]
  wire  RAM_0_MPORT_mask; // @[src/main/scala/riscvsingle/lsu/DTIM.scala 37:33]
  wire  RAM_0_MPORT_en; // @[src/main/scala/riscvsingle/lsu/DTIM.scala 37:33]
  reg [7:0] RAM_1 [0:63]; // @[src/main/scala/riscvsingle/lsu/DTIM.scala 37:33]
  wire  RAM_1_ReadBytes_en; // @[src/main/scala/riscvsingle/lsu/DTIM.scala 37:33]
  wire [5:0] RAM_1_ReadBytes_addr; // @[src/main/scala/riscvsingle/lsu/DTIM.scala 37:33]
  wire [7:0] RAM_1_ReadBytes_data; // @[src/main/scala/riscvsingle/lsu/DTIM.scala 37:33]
  wire [7:0] RAM_1_MPORT_data; // @[src/main/scala/riscvsingle/lsu/DTIM.scala 37:33]
  wire [5:0] RAM_1_MPORT_addr; // @[src/main/scala/riscvsingle/lsu/DTIM.scala 37:33]
  wire  RAM_1_MPORT_mask; // @[src/main/scala/riscvsingle/lsu/DTIM.scala 37:33]
  wire  RAM_1_MPORT_en; // @[src/main/scala/riscvsingle/lsu/DTIM.scala 37:33]
  reg [7:0] RAM_2 [0:63]; // @[src/main/scala/riscvsingle/lsu/DTIM.scala 37:33]
  wire  RAM_2_ReadBytes_en; // @[src/main/scala/riscvsingle/lsu/DTIM.scala 37:33]
  wire [5:0] RAM_2_ReadBytes_addr; // @[src/main/scala/riscvsingle/lsu/DTIM.scala 37:33]
  wire [7:0] RAM_2_ReadBytes_data; // @[src/main/scala/riscvsingle/lsu/DTIM.scala 37:33]
  wire [7:0] RAM_2_MPORT_data; // @[src/main/scala/riscvsingle/lsu/DTIM.scala 37:33]
  wire [5:0] RAM_2_MPORT_addr; // @[src/main/scala/riscvsingle/lsu/DTIM.scala 37:33]
  wire  RAM_2_MPORT_mask; // @[src/main/scala/riscvsingle/lsu/DTIM.scala 37:33]
  wire  RAM_2_MPORT_en; // @[src/main/scala/riscvsingle/lsu/DTIM.scala 37:33]
  reg [7:0] RAM_3 [0:63]; // @[src/main/scala/riscvsingle/lsu/DTIM.scala 37:33]
  wire  RAM_3_ReadBytes_en; // @[src/main/scala/riscvsingle/lsu/DTIM.scala 37:33]
  wire [5:0] RAM_3_ReadBytes_addr; // @[src/main/scala/riscvsingle/lsu/DTIM.scala 37:33]
  wire [7:0] RAM_3_ReadBytes_data; // @[src/main/scala/riscvsingle/lsu/DTIM.scala 37:33]
  wire [7:0] RAM_3_MPORT_data; // @[src/main/scala/riscvsingle/lsu/DTIM.scala 37:33]
  wire [5:0] RAM_3_MPORT_addr; // @[src/main/scala/riscvsingle/lsu/DTIM.scala 37:33]
  wire  RAM_3_MPORT_mask; // @[src/main/scala/riscvsingle/lsu/DTIM.scala 37:33]
  wire  RAM_3_MPORT_en; // @[src/main/scala/riscvsingle/lsu/DTIM.scala 37:33]
  wire [31:0] _io_ReadDataWord_T = {RAM_3_ReadBytes_data,RAM_2_ReadBytes_data,RAM_1_ReadBytes_data,RAM_0_ReadBytes_data}
    ; // @[src/main/scala/riscvsingle/lsu/DTIM.scala 39:48]
  assign RAM_0_ReadBytes_en = 1'h1;
  assign RAM_0_ReadBytes_addr = io_Adr[7:2];
  assign RAM_0_ReadBytes_data = RAM_0[RAM_0_ReadBytes_addr]; // @[src/main/scala/riscvsingle/lsu/DTIM.scala 37:33]
  assign RAM_0_MPORT_data = io_WriteDataWord[7:0];
  assign RAM_0_MPORT_addr = io_Adr[7:2];
  assign RAM_0_MPORT_mask = io_ByteMask[0];
  assign RAM_0_MPORT_en = io_MemWrite;
  assign RAM_1_ReadBytes_en = 1'h1;
  assign RAM_1_ReadBytes_addr = io_Adr[7:2];
  assign RAM_1_ReadBytes_data = RAM_1[RAM_1_ReadBytes_addr]; // @[src/main/scala/riscvsingle/lsu/DTIM.scala 37:33]
  assign RAM_1_MPORT_data = io_WriteDataWord[15:8];
  assign RAM_1_MPORT_addr = io_Adr[7:2];
  assign RAM_1_MPORT_mask = io_ByteMask[1];
  assign RAM_1_MPORT_en = io_MemWrite;
  assign RAM_2_ReadBytes_en = 1'h1;
  assign RAM_2_ReadBytes_addr = io_Adr[7:2];
  assign RAM_2_ReadBytes_data = RAM_2[RAM_2_ReadBytes_addr]; // @[src/main/scala/riscvsingle/lsu/DTIM.scala 37:33]
  assign RAM_2_MPORT_data = io_WriteDataWord[23:16];
  assign RAM_2_MPORT_addr = io_Adr[7:2];
  assign RAM_2_MPORT_mask = io_ByteMask[2];
  assign RAM_2_MPORT_en = io_MemWrite;
  assign RAM_3_ReadBytes_en = 1'h1;
  assign RAM_3_ReadBytes_addr = io_Adr[7:2];
  assign RAM_3_ReadBytes_data = RAM_3[RAM_3_ReadBytes_addr]; // @[src/main/scala/riscvsingle/lsu/DTIM.scala 37:33]
  assign RAM_3_MPORT_data = io_WriteDataWord[31:24];
  assign RAM_3_MPORT_addr = io_Adr[7:2];
  assign RAM_3_MPORT_mask = io_ByteMask[3];
  assign RAM_3_MPORT_en = io_MemWrite;
  assign io_ReadDataWord = io_MemRead ? _io_ReadDataWord_T : 32'h0; // @[src/main/scala/riscvsingle/lsu/DTIM.scala 39:25]
  always @(posedge clk) begin
    if (RAM_0_MPORT_en & RAM_0_MPORT_mask) begin
      RAM_0[RAM_0_MPORT_addr] <= RAM_0_MPORT_data; // @[src/main/scala/riscvsingle/lsu/DTIM.scala 37:33]
    end
    if (RAM_1_MPORT_en & RAM_1_MPORT_mask) begin
      RAM_1[RAM_1_MPORT_addr] <= RAM_1_MPORT_data; // @[src/main/scala/riscvsingle/lsu/DTIM.scala 37:33]
    end
    if (RAM_2_MPORT_en & RAM_2_MPORT_mask) begin
      RAM_2[RAM_2_MPORT_addr] <= RAM_2_MPORT_data; // @[src/main/scala/riscvsingle/lsu/DTIM.scala 37:33]
    end
    if (RAM_3_MPORT_en & RAM_3_MPORT_mask) begin
      RAM_3[RAM_3_MPORT_addr] <= RAM_3_MPORT_data; // @[src/main/scala/riscvsingle/lsu/DTIM.scala 37:33]
    end
  end
// Register and memory initialization
`ifdef RANDOMIZE_GARBAGE_ASSIGN
`define RANDOMIZE
`endif
`ifdef RANDOMIZE_INVALID_ASSIGN
`define RANDOMIZE
`endif
`ifdef RANDOMIZE_REG_INIT
`define RANDOMIZE
`endif
`ifdef RANDOMIZE_MEM_INIT
`define RANDOMIZE
`endif
`ifndef RANDOM
`define RANDOM $random
`endif
`ifdef RANDOMIZE_MEM_INIT
  integer initvar;
`endif
`ifndef SYNTHESIS
`ifdef FIRRTL_BEFORE_INITIAL
`FIRRTL_BEFORE_INITIAL
`endif
initial begin
  `ifdef RANDOMIZE
    `ifdef INIT_RANDOM
      `INIT_RANDOM
    `endif
    `ifndef VERILATOR
      `ifdef RANDOMIZE_DELAY
        #`RANDOMIZE_DELAY begin end
      `else
        #0.002 begin end
      `endif
    `endif
`ifdef RANDOMIZE_MEM_INIT
  _RAND_0 = {1{`RANDOM}};
  for (initvar = 0; initvar < 64; initvar = initvar+1)
    RAM_0[initvar] = _RAND_0[7:0];
  _RAND_1 = {1{`RANDOM}};
  for (initvar = 0; initvar < 64; initvar = initvar+1)
    RAM_1[initvar] = _RAND_1[7:0];
  _RAND_2 = {1{`RANDOM}};
  for (initvar = 0; initvar < 64; initvar = initvar+1)
    RAM_2[initvar] = _RAND_2[7:0];
  _RAND_3 = {1{`RANDOM}};
  for (initvar = 0; initvar < 64; initvar = initvar+1)
    RAM_3[initvar] = _RAND_3[7:0];
`endif // RANDOMIZE_MEM_INIT
  `endif // RANDOMIZE
end // initial
`ifdef FIRRTL_AFTER_INITIAL
`FIRRTL_AFTER_INITIAL
`endif
`endif // SYNTHESIS
endmodule
module SubwordRead(
  input  [31:0] io_ReadDataWord, // @[src/main/scala/riscvsingle/lsu/SubwordRead.scala 24:14]
  input  [1:0]  io_ByteOffset, // @[src/main/scala/riscvsingle/lsu/SubwordRead.scala 24:14]
  input  [2:0]  io_Funct3, // @[src/main/scala/riscvsingle/lsu/SubwordRead.scala 24:14]
  output [31:0] io_ReadData // @[src/main/scala/riscvsingle/lsu/SubwordRead.scala 24:14]
);
  wire [15:0] SelectedHalfword = io_ByteOffset[1] ? io_ReadDataWord[31:16] : io_ReadDataWord[15:0]; // @[src/main/scala/riscvsingle/lsu/SubwordRead.scala 36:26]
  wire [7:0] SelectedByte = io_ByteOffset[0] ? SelectedHalfword[15:8] : SelectedHalfword[7:0]; // @[src/main/scala/riscvsingle/lsu/SubwordRead.scala 37:22]
  wire [7:0] _T = io_ByteOffset[0] ? SelectedHalfword[15:8] : SelectedHalfword[7:0]; // @[src/main/scala/riscvsingle/lsu/SubwordRead.scala 41:30]
  wire [31:0] _T_2 = {{24{_T[7]}},_T}; // @[src/main/scala/riscvsingle/lsu/SubwordRead.scala 41:52]
  wire [15:0] _T_3 = io_ByteOffset[1] ? io_ReadDataWord[31:16] : io_ReadDataWord[15:0]; // @[src/main/scala/riscvsingle/lsu/SubwordRead.scala 42:34]
  wire [31:0] _T_5 = {{16{_T_3[15]}},_T_3}; // @[src/main/scala/riscvsingle/lsu/SubwordRead.scala 42:56]
  wire [31:0] _T_8 = {{24'd0}, SelectedByte}; // @[src/main/scala/riscvsingle/lsu/SubwordRead.scala 44:33]
  wire [31:0] _T_9 = {{16'd0}, SelectedHalfword}; // @[src/main/scala/riscvsingle/lsu/SubwordRead.scala 45:37]
  wire [31:0] _io_ReadData_T_1 = 3'h0 == io_Funct3 ? _T_2 : 32'h0; // @[src/main/scala/riscvsingle/lsu/SubwordRead.scala 52:56]
  wire [31:0] _io_ReadData_T_3 = 3'h1 == io_Funct3 ? _T_5 : _io_ReadData_T_1; // @[src/main/scala/riscvsingle/lsu/SubwordRead.scala 52:56]
  wire [31:0] _io_ReadData_T_5 = 3'h2 == io_Funct3 ? io_ReadDataWord : _io_ReadData_T_3; // @[src/main/scala/riscvsingle/lsu/SubwordRead.scala 52:56]
  wire [31:0] _io_ReadData_T_7 = 3'h4 == io_Funct3 ? _T_8 : _io_ReadData_T_5; // @[src/main/scala/riscvsingle/lsu/SubwordRead.scala 52:56]
  assign io_ReadData = 3'h5 == io_Funct3 ? _T_9 : _io_ReadData_T_7; // @[src/main/scala/riscvsingle/lsu/SubwordRead.scala 52:56]
endmodule
module LSU(
  input         clk, // @[src/main/scala/riscvsingle/lsu/LSU.scala 24:15]
  input         io_MemWrite, // @[src/main/scala/riscvsingle/lsu/LSU.scala 25:14]
  input  [1:0]  io_MemRW, // @[src/main/scala/riscvsingle/lsu/LSU.scala 25:14]
  input  [2:0]  io_Funct3, // @[src/main/scala/riscvsingle/lsu/LSU.scala 25:14]
  input  [31:0] io_IEUAdr, // @[src/main/scala/riscvsingle/lsu/LSU.scala 25:14]
  input  [31:0] io_WriteData, // @[src/main/scala/riscvsingle/lsu/LSU.scala 25:14]
  output [31:0] io_ReadData, // @[src/main/scala/riscvsingle/lsu/LSU.scala 25:14]
  output [3:0]  io_ByteMask // @[src/main/scala/riscvsingle/lsu/LSU.scala 25:14]
);
  wire [2:0] swByteMask_io_Funct3; // @[src/main/scala/riscvsingle/lsu/LSU.scala 40:26]
  wire [1:0] swByteMask_io_ByteOffset; // @[src/main/scala/riscvsingle/lsu/LSU.scala 40:26]
  wire [3:0] swByteMask_io_ByteMask; // @[src/main/scala/riscvsingle/lsu/LSU.scala 40:26]
  wire [31:0] subwordWrite_io_WriteData; // @[src/main/scala/riscvsingle/lsu/LSU.scala 41:28]
  wire [2:0] subwordWrite_io_Funct3; // @[src/main/scala/riscvsingle/lsu/LSU.scala 41:28]
  wire [31:0] subwordWrite_io_WriteDataWord; // @[src/main/scala/riscvsingle/lsu/LSU.scala 41:28]
  wire  dtim_clk; // @[src/main/scala/riscvsingle/lsu/LSU.scala 42:20]
  wire [31:0] dtim_io_Adr; // @[src/main/scala/riscvsingle/lsu/LSU.scala 42:20]
  wire  dtim_io_MemRead; // @[src/main/scala/riscvsingle/lsu/LSU.scala 42:20]
  wire  dtim_io_MemWrite; // @[src/main/scala/riscvsingle/lsu/LSU.scala 42:20]
  wire [31:0] dtim_io_WriteDataWord; // @[src/main/scala/riscvsingle/lsu/LSU.scala 42:20]
  wire [3:0] dtim_io_ByteMask; // @[src/main/scala/riscvsingle/lsu/LSU.scala 42:20]
  wire [31:0] dtim_io_ReadDataWord; // @[src/main/scala/riscvsingle/lsu/LSU.scala 42:20]
  wire [31:0] subwordRead_io_ReadDataWord; // @[src/main/scala/riscvsingle/lsu/LSU.scala 43:27]
  wire [1:0] subwordRead_io_ByteOffset; // @[src/main/scala/riscvsingle/lsu/LSU.scala 43:27]
  wire [2:0] subwordRead_io_Funct3; // @[src/main/scala/riscvsingle/lsu/LSU.scala 43:27]
  wire [31:0] subwordRead_io_ReadData; // @[src/main/scala/riscvsingle/lsu/LSU.scala 43:27]
  wire [1:0] ByteOffset = io_IEUAdr[1:0]; // @[src/main/scala/riscvsingle/lsu/LSU.scala 28:26]
  wire  HalfwordAligned = ~ByteOffset[0]; // @[src/main/scala/riscvsingle/lsu/LSU.scala 29:25]
  wire  WordAligned = ByteOffset == 2'h0; // @[src/main/scala/riscvsingle/lsu/LSU.scala 30:32]
  wire  _LoadAllowed_T_3 = 3'h1 == io_Funct3 ? HalfwordAligned : 3'h0 == io_Funct3; // @[src/main/scala/riscvsingle/lsu/LSU.scala 32:47]
  wire  _LoadAllowed_T_5 = 3'h2 == io_Funct3 ? WordAligned : _LoadAllowed_T_3; // @[src/main/scala/riscvsingle/lsu/LSU.scala 32:47]
  wire  LoadAllowed = 3'h5 == io_Funct3 ? HalfwordAligned : 3'h4 == io_Funct3 | _LoadAllowed_T_5; // @[src/main/scala/riscvsingle/lsu/LSU.scala 32:47]
  wire [3:0] ByteMask = swByteMask_io_ByteMask; // @[src/main/scala/riscvsingle/lsu/LSU.scala 51:22 56:12]
  wire  MemWrite = io_MemWrite & io_MemRW[0] & |ByteMask; // @[src/main/scala/riscvsingle/lsu/LSU.scala 55:45]
  SwByteMask swByteMask ( // @[src/main/scala/riscvsingle/lsu/LSU.scala 40:26]
    .io_Funct3(swByteMask_io_Funct3),
    .io_ByteOffset(swByteMask_io_ByteOffset),
    .io_ByteMask(swByteMask_io_ByteMask)
  );
  SubwordWrite subwordWrite ( // @[src/main/scala/riscvsingle/lsu/LSU.scala 41:28]
    .io_WriteData(subwordWrite_io_WriteData),
    .io_Funct3(subwordWrite_io_Funct3),
    .io_WriteDataWord(subwordWrite_io_WriteDataWord)
  );
  DTIM dtim ( // @[src/main/scala/riscvsingle/lsu/LSU.scala 42:20]
    .clk(dtim_clk),
    .io_Adr(dtim_io_Adr),
    .io_MemRead(dtim_io_MemRead),
    .io_MemWrite(dtim_io_MemWrite),
    .io_WriteDataWord(dtim_io_WriteDataWord),
    .io_ByteMask(dtim_io_ByteMask),
    .io_ReadDataWord(dtim_io_ReadDataWord)
  );
  SubwordRead subwordRead ( // @[src/main/scala/riscvsingle/lsu/LSU.scala 43:27]
    .io_ReadDataWord(subwordRead_io_ReadDataWord),
    .io_ByteOffset(subwordRead_io_ByteOffset),
    .io_Funct3(subwordRead_io_Funct3),
    .io_ReadData(subwordRead_io_ReadData)
  );
  assign io_ReadData = subwordRead_io_ReadData; // @[src/main/scala/riscvsingle/lsu/LSU.scala 69:15]
  assign io_ByteMask = MemWrite ? ByteMask : 4'h0; // @[src/main/scala/riscvsingle/lsu/LSU.scala 71:21]
  assign swByteMask_io_Funct3 = io_Funct3; // @[src/main/scala/riscvsingle/lsu/LSU.scala 45:24]
  assign swByteMask_io_ByteOffset = io_IEUAdr[1:0]; // @[src/main/scala/riscvsingle/lsu/LSU.scala 28:26]
  assign subwordWrite_io_WriteData = io_WriteData; // @[src/main/scala/riscvsingle/lsu/LSU.scala 48:29]
  assign subwordWrite_io_Funct3 = io_Funct3; // @[src/main/scala/riscvsingle/lsu/LSU.scala 47:26]
  assign dtim_clk = clk; // @[src/main/scala/riscvsingle/lsu/LSU.scala 58:12]
  assign dtim_io_Adr = io_IEUAdr; // @[src/main/scala/riscvsingle/lsu/LSU.scala 59:15]
  assign dtim_io_MemRead = io_MemRW[1] & LoadAllowed; // @[src/main/scala/riscvsingle/lsu/LSU.scala 54:29]
  assign dtim_io_MemWrite = io_MemWrite & io_MemRW[0] & |ByteMask; // @[src/main/scala/riscvsingle/lsu/LSU.scala 55:45]
  assign dtim_io_WriteDataWord = subwordWrite_io_WriteDataWord; // @[src/main/scala/riscvsingle/lsu/LSU.scala 52:27 57:17]
  assign dtim_io_ByteMask = swByteMask_io_ByteMask; // @[src/main/scala/riscvsingle/lsu/LSU.scala 51:22 56:12]
  assign subwordRead_io_ReadDataWord = dtim_io_ReadDataWord; // @[src/main/scala/riscvsingle/lsu/LSU.scala 53:26 64:16]
  assign subwordRead_io_ByteOffset = io_IEUAdr[1:0]; // @[src/main/scala/riscvsingle/lsu/LSU.scala 28:26]
  assign subwordRead_io_Funct3 = io_Funct3; // @[src/main/scala/riscvsingle/lsu/LSU.scala 68:25]
endmodule
