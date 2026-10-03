module LSU(
  input         clk, // @[src/main/scala/riscvsingle/lsu/LSU.scala 21:15]
  input         io_MemWrite, // @[src/main/scala/riscvsingle/lsu/LSU.scala 22:14]
  input  [31:0] io_IEUAdr, // @[src/main/scala/riscvsingle/lsu/LSU.scala 22:14]
  input  [31:0] io_WriteData, // @[src/main/scala/riscvsingle/lsu/LSU.scala 22:14]
  output [31:0] io_ReadData // @[src/main/scala/riscvsingle/lsu/LSU.scala 22:14]
);
`ifdef RANDOMIZE_MEM_INIT
  reg [31:0] _RAND_0;
`endif // RANDOMIZE_MEM_INIT
  reg [31:0] RAM [0:63]; // @[src/main/scala/riscvsingle/lsu/LSU.scala 27:33]
  wire  RAM_io_ReadData_MPORT_en; // @[src/main/scala/riscvsingle/lsu/LSU.scala 27:33]
  wire [5:0] RAM_io_ReadData_MPORT_addr; // @[src/main/scala/riscvsingle/lsu/LSU.scala 27:33]
  wire [31:0] RAM_io_ReadData_MPORT_data; // @[src/main/scala/riscvsingle/lsu/LSU.scala 27:33]
  wire [31:0] RAM_MPORT_data; // @[src/main/scala/riscvsingle/lsu/LSU.scala 27:33]
  wire [5:0] RAM_MPORT_addr; // @[src/main/scala/riscvsingle/lsu/LSU.scala 27:33]
  wire  RAM_MPORT_mask; // @[src/main/scala/riscvsingle/lsu/LSU.scala 27:33]
  wire  RAM_MPORT_en; // @[src/main/scala/riscvsingle/lsu/LSU.scala 27:33]
  assign RAM_io_ReadData_MPORT_en = 1'h1;
  assign RAM_io_ReadData_MPORT_addr = io_IEUAdr[7:2];
  assign RAM_io_ReadData_MPORT_data = RAM[RAM_io_ReadData_MPORT_addr]; // @[src/main/scala/riscvsingle/lsu/LSU.scala 27:33]
  assign RAM_MPORT_data = io_WriteData;
  assign RAM_MPORT_addr = io_IEUAdr[7:2];
  assign RAM_MPORT_mask = 1'h1;
  assign RAM_MPORT_en = io_MemWrite;
  assign io_ReadData = RAM_io_ReadData_MPORT_data; // @[src/main/scala/riscvsingle/lsu/LSU.scala 29:15]
  always @(posedge clk) begin
    if (RAM_MPORT_en & RAM_MPORT_mask) begin
      RAM[RAM_MPORT_addr] <= RAM_MPORT_data; // @[src/main/scala/riscvsingle/lsu/LSU.scala 27:33]
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
    RAM[initvar] = _RAND_0[31:0];
`endif // RANDOMIZE_MEM_INIT
  `endif // RANDOMIZE
end // initial
`ifdef FIRRTL_AFTER_INITIAL
`FIRRTL_AFTER_INITIAL
`endif
`endif // SYNTHESIS
endmodule
