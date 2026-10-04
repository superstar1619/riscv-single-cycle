module DTIM(
  input         clk, // @[src/main/scala/riscvsingle/lsu/DTIM.scala 28:15]
  input  [63:0] io_Adr, // @[src/main/scala/riscvsingle/lsu/DTIM.scala 29:14]
  input         io_MemRead, // @[src/main/scala/riscvsingle/lsu/DTIM.scala 29:14]
  input         io_MemWrite, // @[src/main/scala/riscvsingle/lsu/DTIM.scala 29:14]
  input  [63:0] io_WriteDataWord, // @[src/main/scala/riscvsingle/lsu/DTIM.scala 29:14]
  input  [7:0]  io_ByteMask, // @[src/main/scala/riscvsingle/lsu/DTIM.scala 29:14]
  output [63:0] io_ReadDataWord // @[src/main/scala/riscvsingle/lsu/DTIM.scala 29:14]
);
`ifdef RANDOMIZE_MEM_INIT
  reg [31:0] _RAND_0;
  reg [31:0] _RAND_1;
  reg [31:0] _RAND_2;
  reg [31:0] _RAND_3;
  reg [31:0] _RAND_4;
  reg [31:0] _RAND_5;
  reg [31:0] _RAND_6;
  reg [31:0] _RAND_7;
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
  reg [7:0] RAM_4 [0:63]; // @[src/main/scala/riscvsingle/lsu/DTIM.scala 37:33]
  wire  RAM_4_ReadBytes_en; // @[src/main/scala/riscvsingle/lsu/DTIM.scala 37:33]
  wire [5:0] RAM_4_ReadBytes_addr; // @[src/main/scala/riscvsingle/lsu/DTIM.scala 37:33]
  wire [7:0] RAM_4_ReadBytes_data; // @[src/main/scala/riscvsingle/lsu/DTIM.scala 37:33]
  wire [7:0] RAM_4_MPORT_data; // @[src/main/scala/riscvsingle/lsu/DTIM.scala 37:33]
  wire [5:0] RAM_4_MPORT_addr; // @[src/main/scala/riscvsingle/lsu/DTIM.scala 37:33]
  wire  RAM_4_MPORT_mask; // @[src/main/scala/riscvsingle/lsu/DTIM.scala 37:33]
  wire  RAM_4_MPORT_en; // @[src/main/scala/riscvsingle/lsu/DTIM.scala 37:33]
  reg [7:0] RAM_5 [0:63]; // @[src/main/scala/riscvsingle/lsu/DTIM.scala 37:33]
  wire  RAM_5_ReadBytes_en; // @[src/main/scala/riscvsingle/lsu/DTIM.scala 37:33]
  wire [5:0] RAM_5_ReadBytes_addr; // @[src/main/scala/riscvsingle/lsu/DTIM.scala 37:33]
  wire [7:0] RAM_5_ReadBytes_data; // @[src/main/scala/riscvsingle/lsu/DTIM.scala 37:33]
  wire [7:0] RAM_5_MPORT_data; // @[src/main/scala/riscvsingle/lsu/DTIM.scala 37:33]
  wire [5:0] RAM_5_MPORT_addr; // @[src/main/scala/riscvsingle/lsu/DTIM.scala 37:33]
  wire  RAM_5_MPORT_mask; // @[src/main/scala/riscvsingle/lsu/DTIM.scala 37:33]
  wire  RAM_5_MPORT_en; // @[src/main/scala/riscvsingle/lsu/DTIM.scala 37:33]
  reg [7:0] RAM_6 [0:63]; // @[src/main/scala/riscvsingle/lsu/DTIM.scala 37:33]
  wire  RAM_6_ReadBytes_en; // @[src/main/scala/riscvsingle/lsu/DTIM.scala 37:33]
  wire [5:0] RAM_6_ReadBytes_addr; // @[src/main/scala/riscvsingle/lsu/DTIM.scala 37:33]
  wire [7:0] RAM_6_ReadBytes_data; // @[src/main/scala/riscvsingle/lsu/DTIM.scala 37:33]
  wire [7:0] RAM_6_MPORT_data; // @[src/main/scala/riscvsingle/lsu/DTIM.scala 37:33]
  wire [5:0] RAM_6_MPORT_addr; // @[src/main/scala/riscvsingle/lsu/DTIM.scala 37:33]
  wire  RAM_6_MPORT_mask; // @[src/main/scala/riscvsingle/lsu/DTIM.scala 37:33]
  wire  RAM_6_MPORT_en; // @[src/main/scala/riscvsingle/lsu/DTIM.scala 37:33]
  reg [7:0] RAM_7 [0:63]; // @[src/main/scala/riscvsingle/lsu/DTIM.scala 37:33]
  wire  RAM_7_ReadBytes_en; // @[src/main/scala/riscvsingle/lsu/DTIM.scala 37:33]
  wire [5:0] RAM_7_ReadBytes_addr; // @[src/main/scala/riscvsingle/lsu/DTIM.scala 37:33]
  wire [7:0] RAM_7_ReadBytes_data; // @[src/main/scala/riscvsingle/lsu/DTIM.scala 37:33]
  wire [7:0] RAM_7_MPORT_data; // @[src/main/scala/riscvsingle/lsu/DTIM.scala 37:33]
  wire [5:0] RAM_7_MPORT_addr; // @[src/main/scala/riscvsingle/lsu/DTIM.scala 37:33]
  wire  RAM_7_MPORT_mask; // @[src/main/scala/riscvsingle/lsu/DTIM.scala 37:33]
  wire  RAM_7_MPORT_en; // @[src/main/scala/riscvsingle/lsu/DTIM.scala 37:33]
  wire [63:0] _io_ReadDataWord_T = {RAM_7_ReadBytes_data,RAM_6_ReadBytes_data,RAM_5_ReadBytes_data,RAM_4_ReadBytes_data,
    RAM_3_ReadBytes_data,RAM_2_ReadBytes_data,RAM_1_ReadBytes_data,RAM_0_ReadBytes_data}; // @[src/main/scala/riscvsingle/lsu/DTIM.scala 39:48]
  assign RAM_0_ReadBytes_en = 1'h1;
  assign RAM_0_ReadBytes_addr = io_Adr[8:3];
  assign RAM_0_ReadBytes_data = RAM_0[RAM_0_ReadBytes_addr]; // @[src/main/scala/riscvsingle/lsu/DTIM.scala 37:33]
  assign RAM_0_MPORT_data = io_WriteDataWord[7:0];
  assign RAM_0_MPORT_addr = io_Adr[8:3];
  assign RAM_0_MPORT_mask = io_ByteMask[0];
  assign RAM_0_MPORT_en = io_MemWrite;
  assign RAM_1_ReadBytes_en = 1'h1;
  assign RAM_1_ReadBytes_addr = io_Adr[8:3];
  assign RAM_1_ReadBytes_data = RAM_1[RAM_1_ReadBytes_addr]; // @[src/main/scala/riscvsingle/lsu/DTIM.scala 37:33]
  assign RAM_1_MPORT_data = io_WriteDataWord[15:8];
  assign RAM_1_MPORT_addr = io_Adr[8:3];
  assign RAM_1_MPORT_mask = io_ByteMask[1];
  assign RAM_1_MPORT_en = io_MemWrite;
  assign RAM_2_ReadBytes_en = 1'h1;
  assign RAM_2_ReadBytes_addr = io_Adr[8:3];
  assign RAM_2_ReadBytes_data = RAM_2[RAM_2_ReadBytes_addr]; // @[src/main/scala/riscvsingle/lsu/DTIM.scala 37:33]
  assign RAM_2_MPORT_data = io_WriteDataWord[23:16];
  assign RAM_2_MPORT_addr = io_Adr[8:3];
  assign RAM_2_MPORT_mask = io_ByteMask[2];
  assign RAM_2_MPORT_en = io_MemWrite;
  assign RAM_3_ReadBytes_en = 1'h1;
  assign RAM_3_ReadBytes_addr = io_Adr[8:3];
  assign RAM_3_ReadBytes_data = RAM_3[RAM_3_ReadBytes_addr]; // @[src/main/scala/riscvsingle/lsu/DTIM.scala 37:33]
  assign RAM_3_MPORT_data = io_WriteDataWord[31:24];
  assign RAM_3_MPORT_addr = io_Adr[8:3];
  assign RAM_3_MPORT_mask = io_ByteMask[3];
  assign RAM_3_MPORT_en = io_MemWrite;
  assign RAM_4_ReadBytes_en = 1'h1;
  assign RAM_4_ReadBytes_addr = io_Adr[8:3];
  assign RAM_4_ReadBytes_data = RAM_4[RAM_4_ReadBytes_addr]; // @[src/main/scala/riscvsingle/lsu/DTIM.scala 37:33]
  assign RAM_4_MPORT_data = io_WriteDataWord[39:32];
  assign RAM_4_MPORT_addr = io_Adr[8:3];
  assign RAM_4_MPORT_mask = io_ByteMask[4];
  assign RAM_4_MPORT_en = io_MemWrite;
  assign RAM_5_ReadBytes_en = 1'h1;
  assign RAM_5_ReadBytes_addr = io_Adr[8:3];
  assign RAM_5_ReadBytes_data = RAM_5[RAM_5_ReadBytes_addr]; // @[src/main/scala/riscvsingle/lsu/DTIM.scala 37:33]
  assign RAM_5_MPORT_data = io_WriteDataWord[47:40];
  assign RAM_5_MPORT_addr = io_Adr[8:3];
  assign RAM_5_MPORT_mask = io_ByteMask[5];
  assign RAM_5_MPORT_en = io_MemWrite;
  assign RAM_6_ReadBytes_en = 1'h1;
  assign RAM_6_ReadBytes_addr = io_Adr[8:3];
  assign RAM_6_ReadBytes_data = RAM_6[RAM_6_ReadBytes_addr]; // @[src/main/scala/riscvsingle/lsu/DTIM.scala 37:33]
  assign RAM_6_MPORT_data = io_WriteDataWord[55:48];
  assign RAM_6_MPORT_addr = io_Adr[8:3];
  assign RAM_6_MPORT_mask = io_ByteMask[6];
  assign RAM_6_MPORT_en = io_MemWrite;
  assign RAM_7_ReadBytes_en = 1'h1;
  assign RAM_7_ReadBytes_addr = io_Adr[8:3];
  assign RAM_7_ReadBytes_data = RAM_7[RAM_7_ReadBytes_addr]; // @[src/main/scala/riscvsingle/lsu/DTIM.scala 37:33]
  assign RAM_7_MPORT_data = io_WriteDataWord[63:56];
  assign RAM_7_MPORT_addr = io_Adr[8:3];
  assign RAM_7_MPORT_mask = io_ByteMask[7];
  assign RAM_7_MPORT_en = io_MemWrite;
  assign io_ReadDataWord = io_MemRead ? _io_ReadDataWord_T : 64'h0; // @[src/main/scala/riscvsingle/lsu/DTIM.scala 39:25]
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
    if (RAM_4_MPORT_en & RAM_4_MPORT_mask) begin
      RAM_4[RAM_4_MPORT_addr] <= RAM_4_MPORT_data; // @[src/main/scala/riscvsingle/lsu/DTIM.scala 37:33]
    end
    if (RAM_5_MPORT_en & RAM_5_MPORT_mask) begin
      RAM_5[RAM_5_MPORT_addr] <= RAM_5_MPORT_data; // @[src/main/scala/riscvsingle/lsu/DTIM.scala 37:33]
    end
    if (RAM_6_MPORT_en & RAM_6_MPORT_mask) begin
      RAM_6[RAM_6_MPORT_addr] <= RAM_6_MPORT_data; // @[src/main/scala/riscvsingle/lsu/DTIM.scala 37:33]
    end
    if (RAM_7_MPORT_en & RAM_7_MPORT_mask) begin
      RAM_7[RAM_7_MPORT_addr] <= RAM_7_MPORT_data; // @[src/main/scala/riscvsingle/lsu/DTIM.scala 37:33]
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
  _RAND_4 = {1{`RANDOM}};
  for (initvar = 0; initvar < 64; initvar = initvar+1)
    RAM_4[initvar] = _RAND_4[7:0];
  _RAND_5 = {1{`RANDOM}};
  for (initvar = 0; initvar < 64; initvar = initvar+1)
    RAM_5[initvar] = _RAND_5[7:0];
  _RAND_6 = {1{`RANDOM}};
  for (initvar = 0; initvar < 64; initvar = initvar+1)
    RAM_6[initvar] = _RAND_6[7:0];
  _RAND_7 = {1{`RANDOM}};
  for (initvar = 0; initvar < 64; initvar = initvar+1)
    RAM_7[initvar] = _RAND_7[7:0];
`endif // RANDOMIZE_MEM_INIT
  `endif // RANDOMIZE
end // initial
`ifdef FIRRTL_AFTER_INITIAL
`FIRRTL_AFTER_INITIAL
`endif
`endif // SYNTHESIS
endmodule
