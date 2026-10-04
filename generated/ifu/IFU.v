module IROM(
  input  [31:0] io_a, // @[src/main/scala/riscvsingle/ifu/IROM.scala 20:14]
  output [31:0] io_rd // @[src/main/scala/riscvsingle/ifu/IROM.scala 20:14]
);
  reg [31:0] ROM [0:63]; // @[src/main/scala/riscvsingle/ifu/IROM.scala 26:16]
  wire  ROM_io_rd_MPORT_en; // @[src/main/scala/riscvsingle/ifu/IROM.scala 26:16]
  wire [5:0] ROM_io_rd_MPORT_addr; // @[src/main/scala/riscvsingle/ifu/IROM.scala 26:16]
  wire [31:0] ROM_io_rd_MPORT_data; // @[src/main/scala/riscvsingle/ifu/IROM.scala 26:16]
  wire  _GEN_0 = 1'h0;
  assign ROM_io_rd_MPORT_en = 1'h1;
  assign ROM_io_rd_MPORT_addr = io_a[7:2];
  assign ROM_io_rd_MPORT_data = ROM[ROM_io_rd_MPORT_addr]; // @[src/main/scala/riscvsingle/ifu/IROM.scala 26:16]
  assign io_rd = ROM_io_rd_MPORT_data; // @[src/main/scala/riscvsingle/ifu/IROM.scala 30:9]
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
module IFU(
  input         clk, // @[src/main/scala/riscvsingle/ifu/IFU.scala 21:15]
  input         reset, // @[src/main/scala/riscvsingle/ifu/IFU.scala 22:17]
  input         io_PCSrc, // @[src/main/scala/riscvsingle/ifu/IFU.scala 23:14]
  input  [31:0] io_IEUAdr, // @[src/main/scala/riscvsingle/ifu/IFU.scala 23:14]
  output [31:0] io_Instr, // @[src/main/scala/riscvsingle/ifu/IFU.scala 23:14]
  output [31:0] io_PC, // @[src/main/scala/riscvsingle/ifu/IFU.scala 23:14]
  output [31:0] io_PCPlus4 // @[src/main/scala/riscvsingle/ifu/IFU.scala 23:14]
);
`ifdef RANDOMIZE_REG_INIT
  reg [31:0] _RAND_0;
`endif // RANDOMIZE_REG_INIT
  wire [31:0] irom_io_a; // @[src/main/scala/riscvsingle/ifu/IFU.scala 29:20]
  wire [31:0] irom_io_rd; // @[src/main/scala/riscvsingle/ifu/IFU.scala 29:20]
  reg [31:0] pcreg; // @[src/main/scala/riscvsingle/ifu/IFU.scala 27:12]
  IROM irom ( // @[src/main/scala/riscvsingle/ifu/IFU.scala 29:20]
    .io_a(irom_io_a),
    .io_rd(irom_io_rd)
  );
  assign io_Instr = irom_io_rd; // @[src/main/scala/riscvsingle/ifu/IFU.scala 36:12]
  assign io_PC = pcreg; // @[src/main/scala/riscvsingle/ifu/IFU.scala 31:9]
  assign io_PCPlus4 = pcreg + 32'h4; // @[src/main/scala/riscvsingle/ifu/IFU.scala 32:23]
  assign irom_io_a = pcreg; // @[src/main/scala/riscvsingle/ifu/IFU.scala 35:13]
  always @(posedge clk) begin
    if (reset) begin // @[src/main/scala/riscvsingle/ifu/IFU.scala 27:12]
      pcreg <= 32'h0; // @[src/main/scala/riscvsingle/ifu/IFU.scala 27:12]
    end else if (io_PCSrc) begin // @[src/main/scala/riscvsingle/ifu/IFU.scala 33:16]
      pcreg <= io_IEUAdr;
    end else begin
      pcreg <= io_PCPlus4;
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
`ifdef RANDOMIZE_REG_INIT
  _RAND_0 = {1{`RANDOM}};
  pcreg = _RAND_0[31:0];
`endif // RANDOMIZE_REG_INIT
  `endif // RANDOMIZE
end // initial
`ifdef FIRRTL_AFTER_INITIAL
`FIRRTL_AFTER_INITIAL
`endif
`endif // SYNTHESIS
endmodule
