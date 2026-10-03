module IROM(
  input  [31:0] io_a, // @[src/main/scala/riscvsingle/ifu/IROM.scala 21:14]
  output [31:0] io_rd // @[src/main/scala/riscvsingle/ifu/IROM.scala 21:14]
);
  reg [31:0] ROM [0:127]; // @[src/main/scala/riscvsingle/ifu/IROM.scala 27:16]
  wire  ROM_io_rd_MPORT_en; // @[src/main/scala/riscvsingle/ifu/IROM.scala 27:16]
  wire [6:0] ROM_io_rd_MPORT_addr; // @[src/main/scala/riscvsingle/ifu/IROM.scala 27:16]
  wire [31:0] ROM_io_rd_MPORT_data; // @[src/main/scala/riscvsingle/ifu/IROM.scala 27:16]
  wire  _GEN_0 = 1'h0;
  assign ROM_io_rd_MPORT_en = 1'h1;
  assign ROM_io_rd_MPORT_addr = io_a[8:2];
  assign ROM_io_rd_MPORT_data = ROM[ROM_io_rd_MPORT_addr]; // @[src/main/scala/riscvsingle/ifu/IROM.scala 27:16]
  assign io_rd = ROM_io_rd_MPORT_data; // @[src/main/scala/riscvsingle/ifu/IROM.scala 32:9]
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
  integer initvar;
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
  `endif // RANDOMIZE
  $readmemh("programs/riscvtest.memfile", ROM);
end // initial
`ifdef FIRRTL_AFTER_INITIAL
`FIRRTL_AFTER_INITIAL
`endif
`endif // SYNTHESIS
endmodule
