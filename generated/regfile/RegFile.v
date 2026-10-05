module RegFile(
  input         clk, // @[src/main/scala/riscvsingle/ieu/RegFile.scala 27:15]
  input         reset, // @[src/main/scala/riscvsingle/ieu/RegFile.scala 28:17]
  input         io_WE3, // @[src/main/scala/riscvsingle/ieu/RegFile.scala 29:14]
  input  [4:0]  io_A1, // @[src/main/scala/riscvsingle/ieu/RegFile.scala 29:14]
  input  [4:0]  io_A2, // @[src/main/scala/riscvsingle/ieu/RegFile.scala 29:14]
  input  [4:0]  io_A3, // @[src/main/scala/riscvsingle/ieu/RegFile.scala 29:14]
  input  [31:0] io_WD3, // @[src/main/scala/riscvsingle/ieu/RegFile.scala 29:14]
  output [31:0] io_RD1, // @[src/main/scala/riscvsingle/ieu/RegFile.scala 29:14]
  output [31:0] io_RD2 // @[src/main/scala/riscvsingle/ieu/RegFile.scala 29:14]
);
`ifdef RANDOMIZE_REG_INIT
  reg [31:0] _RAND_0;
  reg [31:0] _RAND_1;
  reg [31:0] _RAND_2;
  reg [31:0] _RAND_3;
  reg [31:0] _RAND_4;
  reg [31:0] _RAND_5;
  reg [31:0] _RAND_6;
  reg [31:0] _RAND_7;
  reg [31:0] _RAND_8;
  reg [31:0] _RAND_9;
  reg [31:0] _RAND_10;
  reg [31:0] _RAND_11;
  reg [31:0] _RAND_12;
  reg [31:0] _RAND_13;
  reg [31:0] _RAND_14;
  reg [31:0] _RAND_15;
  reg [31:0] _RAND_16;
  reg [31:0] _RAND_17;
  reg [31:0] _RAND_18;
  reg [31:0] _RAND_19;
  reg [31:0] _RAND_20;
  reg [31:0] _RAND_21;
  reg [31:0] _RAND_22;
  reg [31:0] _RAND_23;
  reg [31:0] _RAND_24;
  reg [31:0] _RAND_25;
  reg [31:0] _RAND_26;
  reg [31:0] _RAND_27;
  reg [31:0] _RAND_28;
  reg [31:0] _RAND_29;
  reg [31:0] _RAND_30;
  reg [31:0] _RAND_31;
`endif // RANDOMIZE_REG_INIT
  reg [31:0] rf_0; // @[src/main/scala/riscvsingle/ieu/RegFile.scala 33:8]
  reg [31:0] rf_1; // @[src/main/scala/riscvsingle/ieu/RegFile.scala 33:8]
  reg [31:0] rf_2; // @[src/main/scala/riscvsingle/ieu/RegFile.scala 33:8]
  reg [31:0] rf_3; // @[src/main/scala/riscvsingle/ieu/RegFile.scala 33:8]
  reg [31:0] rf_4; // @[src/main/scala/riscvsingle/ieu/RegFile.scala 33:8]
  reg [31:0] rf_5; // @[src/main/scala/riscvsingle/ieu/RegFile.scala 33:8]
  reg [31:0] rf_6; // @[src/main/scala/riscvsingle/ieu/RegFile.scala 33:8]
  reg [31:0] rf_7; // @[src/main/scala/riscvsingle/ieu/RegFile.scala 33:8]
  reg [31:0] rf_8; // @[src/main/scala/riscvsingle/ieu/RegFile.scala 33:8]
  reg [31:0] rf_9; // @[src/main/scala/riscvsingle/ieu/RegFile.scala 33:8]
  reg [31:0] rf_10; // @[src/main/scala/riscvsingle/ieu/RegFile.scala 33:8]
  reg [31:0] rf_11; // @[src/main/scala/riscvsingle/ieu/RegFile.scala 33:8]
  reg [31:0] rf_12; // @[src/main/scala/riscvsingle/ieu/RegFile.scala 33:8]
  reg [31:0] rf_13; // @[src/main/scala/riscvsingle/ieu/RegFile.scala 33:8]
  reg [31:0] rf_14; // @[src/main/scala/riscvsingle/ieu/RegFile.scala 33:8]
  reg [31:0] rf_15; // @[src/main/scala/riscvsingle/ieu/RegFile.scala 33:8]
  reg [31:0] rf_16; // @[src/main/scala/riscvsingle/ieu/RegFile.scala 33:8]
  reg [31:0] rf_17; // @[src/main/scala/riscvsingle/ieu/RegFile.scala 33:8]
  reg [31:0] rf_18; // @[src/main/scala/riscvsingle/ieu/RegFile.scala 33:8]
  reg [31:0] rf_19; // @[src/main/scala/riscvsingle/ieu/RegFile.scala 33:8]
  reg [31:0] rf_20; // @[src/main/scala/riscvsingle/ieu/RegFile.scala 33:8]
  reg [31:0] rf_21; // @[src/main/scala/riscvsingle/ieu/RegFile.scala 33:8]
  reg [31:0] rf_22; // @[src/main/scala/riscvsingle/ieu/RegFile.scala 33:8]
  reg [31:0] rf_23; // @[src/main/scala/riscvsingle/ieu/RegFile.scala 33:8]
  reg [31:0] rf_24; // @[src/main/scala/riscvsingle/ieu/RegFile.scala 33:8]
  reg [31:0] rf_25; // @[src/main/scala/riscvsingle/ieu/RegFile.scala 33:8]
  reg [31:0] rf_26; // @[src/main/scala/riscvsingle/ieu/RegFile.scala 33:8]
  reg [31:0] rf_27; // @[src/main/scala/riscvsingle/ieu/RegFile.scala 33:8]
  reg [31:0] rf_28; // @[src/main/scala/riscvsingle/ieu/RegFile.scala 33:8]
  reg [31:0] rf_29; // @[src/main/scala/riscvsingle/ieu/RegFile.scala 33:8]
  reg [31:0] rf_30; // @[src/main/scala/riscvsingle/ieu/RegFile.scala 33:8]
  reg [31:0] rf_31; // @[src/main/scala/riscvsingle/ieu/RegFile.scala 33:8]
  wire  WriteEnable = io_WE3 & io_A3 != 5'h0; // @[src/main/scala/riscvsingle/ieu/RegFile.scala 36:25]
  wire [31:0] _GEN_97 = 5'h1 == io_A1 ? rf_1 : rf_0; // @[src/main/scala/riscvsingle/ieu/RegFile.scala 46:{10,10}]
  wire [31:0] _GEN_98 = 5'h2 == io_A1 ? rf_2 : _GEN_97; // @[src/main/scala/riscvsingle/ieu/RegFile.scala 46:{10,10}]
  wire [31:0] _GEN_99 = 5'h3 == io_A1 ? rf_3 : _GEN_98; // @[src/main/scala/riscvsingle/ieu/RegFile.scala 46:{10,10}]
  wire [31:0] _GEN_100 = 5'h4 == io_A1 ? rf_4 : _GEN_99; // @[src/main/scala/riscvsingle/ieu/RegFile.scala 46:{10,10}]
  wire [31:0] _GEN_101 = 5'h5 == io_A1 ? rf_5 : _GEN_100; // @[src/main/scala/riscvsingle/ieu/RegFile.scala 46:{10,10}]
  wire [31:0] _GEN_102 = 5'h6 == io_A1 ? rf_6 : _GEN_101; // @[src/main/scala/riscvsingle/ieu/RegFile.scala 46:{10,10}]
  wire [31:0] _GEN_103 = 5'h7 == io_A1 ? rf_7 : _GEN_102; // @[src/main/scala/riscvsingle/ieu/RegFile.scala 46:{10,10}]
  wire [31:0] _GEN_104 = 5'h8 == io_A1 ? rf_8 : _GEN_103; // @[src/main/scala/riscvsingle/ieu/RegFile.scala 46:{10,10}]
  wire [31:0] _GEN_105 = 5'h9 == io_A1 ? rf_9 : _GEN_104; // @[src/main/scala/riscvsingle/ieu/RegFile.scala 46:{10,10}]
  wire [31:0] _GEN_106 = 5'ha == io_A1 ? rf_10 : _GEN_105; // @[src/main/scala/riscvsingle/ieu/RegFile.scala 46:{10,10}]
  wire [31:0] _GEN_107 = 5'hb == io_A1 ? rf_11 : _GEN_106; // @[src/main/scala/riscvsingle/ieu/RegFile.scala 46:{10,10}]
  wire [31:0] _GEN_108 = 5'hc == io_A1 ? rf_12 : _GEN_107; // @[src/main/scala/riscvsingle/ieu/RegFile.scala 46:{10,10}]
  wire [31:0] _GEN_109 = 5'hd == io_A1 ? rf_13 : _GEN_108; // @[src/main/scala/riscvsingle/ieu/RegFile.scala 46:{10,10}]
  wire [31:0] _GEN_110 = 5'he == io_A1 ? rf_14 : _GEN_109; // @[src/main/scala/riscvsingle/ieu/RegFile.scala 46:{10,10}]
  wire [31:0] _GEN_111 = 5'hf == io_A1 ? rf_15 : _GEN_110; // @[src/main/scala/riscvsingle/ieu/RegFile.scala 46:{10,10}]
  wire [31:0] _GEN_112 = 5'h10 == io_A1 ? rf_16 : _GEN_111; // @[src/main/scala/riscvsingle/ieu/RegFile.scala 46:{10,10}]
  wire [31:0] _GEN_113 = 5'h11 == io_A1 ? rf_17 : _GEN_112; // @[src/main/scala/riscvsingle/ieu/RegFile.scala 46:{10,10}]
  wire [31:0] _GEN_114 = 5'h12 == io_A1 ? rf_18 : _GEN_113; // @[src/main/scala/riscvsingle/ieu/RegFile.scala 46:{10,10}]
  wire [31:0] _GEN_115 = 5'h13 == io_A1 ? rf_19 : _GEN_114; // @[src/main/scala/riscvsingle/ieu/RegFile.scala 46:{10,10}]
  wire [31:0] _GEN_116 = 5'h14 == io_A1 ? rf_20 : _GEN_115; // @[src/main/scala/riscvsingle/ieu/RegFile.scala 46:{10,10}]
  wire [31:0] _GEN_117 = 5'h15 == io_A1 ? rf_21 : _GEN_116; // @[src/main/scala/riscvsingle/ieu/RegFile.scala 46:{10,10}]
  wire [31:0] _GEN_118 = 5'h16 == io_A1 ? rf_22 : _GEN_117; // @[src/main/scala/riscvsingle/ieu/RegFile.scala 46:{10,10}]
  wire [31:0] _GEN_119 = 5'h17 == io_A1 ? rf_23 : _GEN_118; // @[src/main/scala/riscvsingle/ieu/RegFile.scala 46:{10,10}]
  wire [31:0] _GEN_120 = 5'h18 == io_A1 ? rf_24 : _GEN_119; // @[src/main/scala/riscvsingle/ieu/RegFile.scala 46:{10,10}]
  wire [31:0] _GEN_121 = 5'h19 == io_A1 ? rf_25 : _GEN_120; // @[src/main/scala/riscvsingle/ieu/RegFile.scala 46:{10,10}]
  wire [31:0] _GEN_122 = 5'h1a == io_A1 ? rf_26 : _GEN_121; // @[src/main/scala/riscvsingle/ieu/RegFile.scala 46:{10,10}]
  wire [31:0] _GEN_123 = 5'h1b == io_A1 ? rf_27 : _GEN_122; // @[src/main/scala/riscvsingle/ieu/RegFile.scala 46:{10,10}]
  wire [31:0] _GEN_124 = 5'h1c == io_A1 ? rf_28 : _GEN_123; // @[src/main/scala/riscvsingle/ieu/RegFile.scala 46:{10,10}]
  wire [31:0] _GEN_125 = 5'h1d == io_A1 ? rf_29 : _GEN_124; // @[src/main/scala/riscvsingle/ieu/RegFile.scala 46:{10,10}]
  wire [31:0] _GEN_126 = 5'h1e == io_A1 ? rf_30 : _GEN_125; // @[src/main/scala/riscvsingle/ieu/RegFile.scala 46:{10,10}]
  wire [31:0] _GEN_129 = 5'h1 == io_A2 ? rf_1 : rf_0; // @[src/main/scala/riscvsingle/ieu/RegFile.scala 47:{10,10}]
  wire [31:0] _GEN_130 = 5'h2 == io_A2 ? rf_2 : _GEN_129; // @[src/main/scala/riscvsingle/ieu/RegFile.scala 47:{10,10}]
  wire [31:0] _GEN_131 = 5'h3 == io_A2 ? rf_3 : _GEN_130; // @[src/main/scala/riscvsingle/ieu/RegFile.scala 47:{10,10}]
  wire [31:0] _GEN_132 = 5'h4 == io_A2 ? rf_4 : _GEN_131; // @[src/main/scala/riscvsingle/ieu/RegFile.scala 47:{10,10}]
  wire [31:0] _GEN_133 = 5'h5 == io_A2 ? rf_5 : _GEN_132; // @[src/main/scala/riscvsingle/ieu/RegFile.scala 47:{10,10}]
  wire [31:0] _GEN_134 = 5'h6 == io_A2 ? rf_6 : _GEN_133; // @[src/main/scala/riscvsingle/ieu/RegFile.scala 47:{10,10}]
  wire [31:0] _GEN_135 = 5'h7 == io_A2 ? rf_7 : _GEN_134; // @[src/main/scala/riscvsingle/ieu/RegFile.scala 47:{10,10}]
  wire [31:0] _GEN_136 = 5'h8 == io_A2 ? rf_8 : _GEN_135; // @[src/main/scala/riscvsingle/ieu/RegFile.scala 47:{10,10}]
  wire [31:0] _GEN_137 = 5'h9 == io_A2 ? rf_9 : _GEN_136; // @[src/main/scala/riscvsingle/ieu/RegFile.scala 47:{10,10}]
  wire [31:0] _GEN_138 = 5'ha == io_A2 ? rf_10 : _GEN_137; // @[src/main/scala/riscvsingle/ieu/RegFile.scala 47:{10,10}]
  wire [31:0] _GEN_139 = 5'hb == io_A2 ? rf_11 : _GEN_138; // @[src/main/scala/riscvsingle/ieu/RegFile.scala 47:{10,10}]
  wire [31:0] _GEN_140 = 5'hc == io_A2 ? rf_12 : _GEN_139; // @[src/main/scala/riscvsingle/ieu/RegFile.scala 47:{10,10}]
  wire [31:0] _GEN_141 = 5'hd == io_A2 ? rf_13 : _GEN_140; // @[src/main/scala/riscvsingle/ieu/RegFile.scala 47:{10,10}]
  wire [31:0] _GEN_142 = 5'he == io_A2 ? rf_14 : _GEN_141; // @[src/main/scala/riscvsingle/ieu/RegFile.scala 47:{10,10}]
  wire [31:0] _GEN_143 = 5'hf == io_A2 ? rf_15 : _GEN_142; // @[src/main/scala/riscvsingle/ieu/RegFile.scala 47:{10,10}]
  wire [31:0] _GEN_144 = 5'h10 == io_A2 ? rf_16 : _GEN_143; // @[src/main/scala/riscvsingle/ieu/RegFile.scala 47:{10,10}]
  wire [31:0] _GEN_145 = 5'h11 == io_A2 ? rf_17 : _GEN_144; // @[src/main/scala/riscvsingle/ieu/RegFile.scala 47:{10,10}]
  wire [31:0] _GEN_146 = 5'h12 == io_A2 ? rf_18 : _GEN_145; // @[src/main/scala/riscvsingle/ieu/RegFile.scala 47:{10,10}]
  wire [31:0] _GEN_147 = 5'h13 == io_A2 ? rf_19 : _GEN_146; // @[src/main/scala/riscvsingle/ieu/RegFile.scala 47:{10,10}]
  wire [31:0] _GEN_148 = 5'h14 == io_A2 ? rf_20 : _GEN_147; // @[src/main/scala/riscvsingle/ieu/RegFile.scala 47:{10,10}]
  wire [31:0] _GEN_149 = 5'h15 == io_A2 ? rf_21 : _GEN_148; // @[src/main/scala/riscvsingle/ieu/RegFile.scala 47:{10,10}]
  wire [31:0] _GEN_150 = 5'h16 == io_A2 ? rf_22 : _GEN_149; // @[src/main/scala/riscvsingle/ieu/RegFile.scala 47:{10,10}]
  wire [31:0] _GEN_151 = 5'h17 == io_A2 ? rf_23 : _GEN_150; // @[src/main/scala/riscvsingle/ieu/RegFile.scala 47:{10,10}]
  wire [31:0] _GEN_152 = 5'h18 == io_A2 ? rf_24 : _GEN_151; // @[src/main/scala/riscvsingle/ieu/RegFile.scala 47:{10,10}]
  wire [31:0] _GEN_153 = 5'h19 == io_A2 ? rf_25 : _GEN_152; // @[src/main/scala/riscvsingle/ieu/RegFile.scala 47:{10,10}]
  wire [31:0] _GEN_154 = 5'h1a == io_A2 ? rf_26 : _GEN_153; // @[src/main/scala/riscvsingle/ieu/RegFile.scala 47:{10,10}]
  wire [31:0] _GEN_155 = 5'h1b == io_A2 ? rf_27 : _GEN_154; // @[src/main/scala/riscvsingle/ieu/RegFile.scala 47:{10,10}]
  wire [31:0] _GEN_156 = 5'h1c == io_A2 ? rf_28 : _GEN_155; // @[src/main/scala/riscvsingle/ieu/RegFile.scala 47:{10,10}]
  wire [31:0] _GEN_157 = 5'h1d == io_A2 ? rf_29 : _GEN_156; // @[src/main/scala/riscvsingle/ieu/RegFile.scala 47:{10,10}]
  wire [31:0] _GEN_158 = 5'h1e == io_A2 ? rf_30 : _GEN_157; // @[src/main/scala/riscvsingle/ieu/RegFile.scala 47:{10,10}]
  assign io_RD1 = 5'h1f == io_A1 ? rf_31 : _GEN_126; // @[src/main/scala/riscvsingle/ieu/RegFile.scala 46:{10,10}]
  assign io_RD2 = 5'h1f == io_A2 ? rf_31 : _GEN_158; // @[src/main/scala/riscvsingle/ieu/RegFile.scala 47:{10,10}]
  always @(posedge clk) begin
    if (reset) begin // @[src/main/scala/riscvsingle/ieu/RegFile.scala 39:17]
      rf_0 <= 32'h0; // @[src/main/scala/riscvsingle/ieu/RegFile.scala 40:20]
    end else if (WriteEnable) begin // @[src/main/scala/riscvsingle/ieu/RegFile.scala 41:29]
      if (5'h0 == io_A3) begin // @[src/main/scala/riscvsingle/ieu/RegFile.scala 42:24]
        rf_0 <= io_WD3; // @[src/main/scala/riscvsingle/ieu/RegFile.scala 42:24]
      end
    end
    if (!(reset)) begin // @[src/main/scala/riscvsingle/ieu/RegFile.scala 39:17]
      if (WriteEnable) begin // @[src/main/scala/riscvsingle/ieu/RegFile.scala 41:29]
        if (5'h1 == io_A3) begin // @[src/main/scala/riscvsingle/ieu/RegFile.scala 42:24]
          rf_1 <= io_WD3; // @[src/main/scala/riscvsingle/ieu/RegFile.scala 42:24]
        end
      end
    end
    if (!(reset)) begin // @[src/main/scala/riscvsingle/ieu/RegFile.scala 39:17]
      if (WriteEnable) begin // @[src/main/scala/riscvsingle/ieu/RegFile.scala 41:29]
        if (5'h2 == io_A3) begin // @[src/main/scala/riscvsingle/ieu/RegFile.scala 42:24]
          rf_2 <= io_WD3; // @[src/main/scala/riscvsingle/ieu/RegFile.scala 42:24]
        end
      end
    end
    if (!(reset)) begin // @[src/main/scala/riscvsingle/ieu/RegFile.scala 39:17]
      if (WriteEnable) begin // @[src/main/scala/riscvsingle/ieu/RegFile.scala 41:29]
        if (5'h3 == io_A3) begin // @[src/main/scala/riscvsingle/ieu/RegFile.scala 42:24]
          rf_3 <= io_WD3; // @[src/main/scala/riscvsingle/ieu/RegFile.scala 42:24]
        end
      end
    end
    if (!(reset)) begin // @[src/main/scala/riscvsingle/ieu/RegFile.scala 39:17]
      if (WriteEnable) begin // @[src/main/scala/riscvsingle/ieu/RegFile.scala 41:29]
        if (5'h4 == io_A3) begin // @[src/main/scala/riscvsingle/ieu/RegFile.scala 42:24]
          rf_4 <= io_WD3; // @[src/main/scala/riscvsingle/ieu/RegFile.scala 42:24]
        end
      end
    end
    if (!(reset)) begin // @[src/main/scala/riscvsingle/ieu/RegFile.scala 39:17]
      if (WriteEnable) begin // @[src/main/scala/riscvsingle/ieu/RegFile.scala 41:29]
        if (5'h5 == io_A3) begin // @[src/main/scala/riscvsingle/ieu/RegFile.scala 42:24]
          rf_5 <= io_WD3; // @[src/main/scala/riscvsingle/ieu/RegFile.scala 42:24]
        end
      end
    end
    if (!(reset)) begin // @[src/main/scala/riscvsingle/ieu/RegFile.scala 39:17]
      if (WriteEnable) begin // @[src/main/scala/riscvsingle/ieu/RegFile.scala 41:29]
        if (5'h6 == io_A3) begin // @[src/main/scala/riscvsingle/ieu/RegFile.scala 42:24]
          rf_6 <= io_WD3; // @[src/main/scala/riscvsingle/ieu/RegFile.scala 42:24]
        end
      end
    end
    if (!(reset)) begin // @[src/main/scala/riscvsingle/ieu/RegFile.scala 39:17]
      if (WriteEnable) begin // @[src/main/scala/riscvsingle/ieu/RegFile.scala 41:29]
        if (5'h7 == io_A3) begin // @[src/main/scala/riscvsingle/ieu/RegFile.scala 42:24]
          rf_7 <= io_WD3; // @[src/main/scala/riscvsingle/ieu/RegFile.scala 42:24]
        end
      end
    end
    if (!(reset)) begin // @[src/main/scala/riscvsingle/ieu/RegFile.scala 39:17]
      if (WriteEnable) begin // @[src/main/scala/riscvsingle/ieu/RegFile.scala 41:29]
        if (5'h8 == io_A3) begin // @[src/main/scala/riscvsingle/ieu/RegFile.scala 42:24]
          rf_8 <= io_WD3; // @[src/main/scala/riscvsingle/ieu/RegFile.scala 42:24]
        end
      end
    end
    if (!(reset)) begin // @[src/main/scala/riscvsingle/ieu/RegFile.scala 39:17]
      if (WriteEnable) begin // @[src/main/scala/riscvsingle/ieu/RegFile.scala 41:29]
        if (5'h9 == io_A3) begin // @[src/main/scala/riscvsingle/ieu/RegFile.scala 42:24]
          rf_9 <= io_WD3; // @[src/main/scala/riscvsingle/ieu/RegFile.scala 42:24]
        end
      end
    end
    if (!(reset)) begin // @[src/main/scala/riscvsingle/ieu/RegFile.scala 39:17]
      if (WriteEnable) begin // @[src/main/scala/riscvsingle/ieu/RegFile.scala 41:29]
        if (5'ha == io_A3) begin // @[src/main/scala/riscvsingle/ieu/RegFile.scala 42:24]
          rf_10 <= io_WD3; // @[src/main/scala/riscvsingle/ieu/RegFile.scala 42:24]
        end
      end
    end
    if (!(reset)) begin // @[src/main/scala/riscvsingle/ieu/RegFile.scala 39:17]
      if (WriteEnable) begin // @[src/main/scala/riscvsingle/ieu/RegFile.scala 41:29]
        if (5'hb == io_A3) begin // @[src/main/scala/riscvsingle/ieu/RegFile.scala 42:24]
          rf_11 <= io_WD3; // @[src/main/scala/riscvsingle/ieu/RegFile.scala 42:24]
        end
      end
    end
    if (!(reset)) begin // @[src/main/scala/riscvsingle/ieu/RegFile.scala 39:17]
      if (WriteEnable) begin // @[src/main/scala/riscvsingle/ieu/RegFile.scala 41:29]
        if (5'hc == io_A3) begin // @[src/main/scala/riscvsingle/ieu/RegFile.scala 42:24]
          rf_12 <= io_WD3; // @[src/main/scala/riscvsingle/ieu/RegFile.scala 42:24]
        end
      end
    end
    if (!(reset)) begin // @[src/main/scala/riscvsingle/ieu/RegFile.scala 39:17]
      if (WriteEnable) begin // @[src/main/scala/riscvsingle/ieu/RegFile.scala 41:29]
        if (5'hd == io_A3) begin // @[src/main/scala/riscvsingle/ieu/RegFile.scala 42:24]
          rf_13 <= io_WD3; // @[src/main/scala/riscvsingle/ieu/RegFile.scala 42:24]
        end
      end
    end
    if (!(reset)) begin // @[src/main/scala/riscvsingle/ieu/RegFile.scala 39:17]
      if (WriteEnable) begin // @[src/main/scala/riscvsingle/ieu/RegFile.scala 41:29]
        if (5'he == io_A3) begin // @[src/main/scala/riscvsingle/ieu/RegFile.scala 42:24]
          rf_14 <= io_WD3; // @[src/main/scala/riscvsingle/ieu/RegFile.scala 42:24]
        end
      end
    end
    if (!(reset)) begin // @[src/main/scala/riscvsingle/ieu/RegFile.scala 39:17]
      if (WriteEnable) begin // @[src/main/scala/riscvsingle/ieu/RegFile.scala 41:29]
        if (5'hf == io_A3) begin // @[src/main/scala/riscvsingle/ieu/RegFile.scala 42:24]
          rf_15 <= io_WD3; // @[src/main/scala/riscvsingle/ieu/RegFile.scala 42:24]
        end
      end
    end
    if (!(reset)) begin // @[src/main/scala/riscvsingle/ieu/RegFile.scala 39:17]
      if (WriteEnable) begin // @[src/main/scala/riscvsingle/ieu/RegFile.scala 41:29]
        if (5'h10 == io_A3) begin // @[src/main/scala/riscvsingle/ieu/RegFile.scala 42:24]
          rf_16 <= io_WD3; // @[src/main/scala/riscvsingle/ieu/RegFile.scala 42:24]
        end
      end
    end
    if (!(reset)) begin // @[src/main/scala/riscvsingle/ieu/RegFile.scala 39:17]
      if (WriteEnable) begin // @[src/main/scala/riscvsingle/ieu/RegFile.scala 41:29]
        if (5'h11 == io_A3) begin // @[src/main/scala/riscvsingle/ieu/RegFile.scala 42:24]
          rf_17 <= io_WD3; // @[src/main/scala/riscvsingle/ieu/RegFile.scala 42:24]
        end
      end
    end
    if (!(reset)) begin // @[src/main/scala/riscvsingle/ieu/RegFile.scala 39:17]
      if (WriteEnable) begin // @[src/main/scala/riscvsingle/ieu/RegFile.scala 41:29]
        if (5'h12 == io_A3) begin // @[src/main/scala/riscvsingle/ieu/RegFile.scala 42:24]
          rf_18 <= io_WD3; // @[src/main/scala/riscvsingle/ieu/RegFile.scala 42:24]
        end
      end
    end
    if (!(reset)) begin // @[src/main/scala/riscvsingle/ieu/RegFile.scala 39:17]
      if (WriteEnable) begin // @[src/main/scala/riscvsingle/ieu/RegFile.scala 41:29]
        if (5'h13 == io_A3) begin // @[src/main/scala/riscvsingle/ieu/RegFile.scala 42:24]
          rf_19 <= io_WD3; // @[src/main/scala/riscvsingle/ieu/RegFile.scala 42:24]
        end
      end
    end
    if (!(reset)) begin // @[src/main/scala/riscvsingle/ieu/RegFile.scala 39:17]
      if (WriteEnable) begin // @[src/main/scala/riscvsingle/ieu/RegFile.scala 41:29]
        if (5'h14 == io_A3) begin // @[src/main/scala/riscvsingle/ieu/RegFile.scala 42:24]
          rf_20 <= io_WD3; // @[src/main/scala/riscvsingle/ieu/RegFile.scala 42:24]
        end
      end
    end
    if (!(reset)) begin // @[src/main/scala/riscvsingle/ieu/RegFile.scala 39:17]
      if (WriteEnable) begin // @[src/main/scala/riscvsingle/ieu/RegFile.scala 41:29]
        if (5'h15 == io_A3) begin // @[src/main/scala/riscvsingle/ieu/RegFile.scala 42:24]
          rf_21 <= io_WD3; // @[src/main/scala/riscvsingle/ieu/RegFile.scala 42:24]
        end
      end
    end
    if (!(reset)) begin // @[src/main/scala/riscvsingle/ieu/RegFile.scala 39:17]
      if (WriteEnable) begin // @[src/main/scala/riscvsingle/ieu/RegFile.scala 41:29]
        if (5'h16 == io_A3) begin // @[src/main/scala/riscvsingle/ieu/RegFile.scala 42:24]
          rf_22 <= io_WD3; // @[src/main/scala/riscvsingle/ieu/RegFile.scala 42:24]
        end
      end
    end
    if (!(reset)) begin // @[src/main/scala/riscvsingle/ieu/RegFile.scala 39:17]
      if (WriteEnable) begin // @[src/main/scala/riscvsingle/ieu/RegFile.scala 41:29]
        if (5'h17 == io_A3) begin // @[src/main/scala/riscvsingle/ieu/RegFile.scala 42:24]
          rf_23 <= io_WD3; // @[src/main/scala/riscvsingle/ieu/RegFile.scala 42:24]
        end
      end
    end
    if (!(reset)) begin // @[src/main/scala/riscvsingle/ieu/RegFile.scala 39:17]
      if (WriteEnable) begin // @[src/main/scala/riscvsingle/ieu/RegFile.scala 41:29]
        if (5'h18 == io_A3) begin // @[src/main/scala/riscvsingle/ieu/RegFile.scala 42:24]
          rf_24 <= io_WD3; // @[src/main/scala/riscvsingle/ieu/RegFile.scala 42:24]
        end
      end
    end
    if (!(reset)) begin // @[src/main/scala/riscvsingle/ieu/RegFile.scala 39:17]
      if (WriteEnable) begin // @[src/main/scala/riscvsingle/ieu/RegFile.scala 41:29]
        if (5'h19 == io_A3) begin // @[src/main/scala/riscvsingle/ieu/RegFile.scala 42:24]
          rf_25 <= io_WD3; // @[src/main/scala/riscvsingle/ieu/RegFile.scala 42:24]
        end
      end
    end
    if (!(reset)) begin // @[src/main/scala/riscvsingle/ieu/RegFile.scala 39:17]
      if (WriteEnable) begin // @[src/main/scala/riscvsingle/ieu/RegFile.scala 41:29]
        if (5'h1a == io_A3) begin // @[src/main/scala/riscvsingle/ieu/RegFile.scala 42:24]
          rf_26 <= io_WD3; // @[src/main/scala/riscvsingle/ieu/RegFile.scala 42:24]
        end
      end
    end
    if (!(reset)) begin // @[src/main/scala/riscvsingle/ieu/RegFile.scala 39:17]
      if (WriteEnable) begin // @[src/main/scala/riscvsingle/ieu/RegFile.scala 41:29]
        if (5'h1b == io_A3) begin // @[src/main/scala/riscvsingle/ieu/RegFile.scala 42:24]
          rf_27 <= io_WD3; // @[src/main/scala/riscvsingle/ieu/RegFile.scala 42:24]
        end
      end
    end
    if (!(reset)) begin // @[src/main/scala/riscvsingle/ieu/RegFile.scala 39:17]
      if (WriteEnable) begin // @[src/main/scala/riscvsingle/ieu/RegFile.scala 41:29]
        if (5'h1c == io_A3) begin // @[src/main/scala/riscvsingle/ieu/RegFile.scala 42:24]
          rf_28 <= io_WD3; // @[src/main/scala/riscvsingle/ieu/RegFile.scala 42:24]
        end
      end
    end
    if (!(reset)) begin // @[src/main/scala/riscvsingle/ieu/RegFile.scala 39:17]
      if (WriteEnable) begin // @[src/main/scala/riscvsingle/ieu/RegFile.scala 41:29]
        if (5'h1d == io_A3) begin // @[src/main/scala/riscvsingle/ieu/RegFile.scala 42:24]
          rf_29 <= io_WD3; // @[src/main/scala/riscvsingle/ieu/RegFile.scala 42:24]
        end
      end
    end
    if (!(reset)) begin // @[src/main/scala/riscvsingle/ieu/RegFile.scala 39:17]
      if (WriteEnable) begin // @[src/main/scala/riscvsingle/ieu/RegFile.scala 41:29]
        if (5'h1e == io_A3) begin // @[src/main/scala/riscvsingle/ieu/RegFile.scala 42:24]
          rf_30 <= io_WD3; // @[src/main/scala/riscvsingle/ieu/RegFile.scala 42:24]
        end
      end
    end
    if (!(reset)) begin // @[src/main/scala/riscvsingle/ieu/RegFile.scala 39:17]
      if (WriteEnable) begin // @[src/main/scala/riscvsingle/ieu/RegFile.scala 41:29]
        if (5'h1f == io_A3) begin // @[src/main/scala/riscvsingle/ieu/RegFile.scala 42:24]
          rf_31 <= io_WD3; // @[src/main/scala/riscvsingle/ieu/RegFile.scala 42:24]
        end
      end
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
  rf_0 = _RAND_0[31:0];
  _RAND_1 = {1{`RANDOM}};
  rf_1 = _RAND_1[31:0];
  _RAND_2 = {1{`RANDOM}};
  rf_2 = _RAND_2[31:0];
  _RAND_3 = {1{`RANDOM}};
  rf_3 = _RAND_3[31:0];
  _RAND_4 = {1{`RANDOM}};
  rf_4 = _RAND_4[31:0];
  _RAND_5 = {1{`RANDOM}};
  rf_5 = _RAND_5[31:0];
  _RAND_6 = {1{`RANDOM}};
  rf_6 = _RAND_6[31:0];
  _RAND_7 = {1{`RANDOM}};
  rf_7 = _RAND_7[31:0];
  _RAND_8 = {1{`RANDOM}};
  rf_8 = _RAND_8[31:0];
  _RAND_9 = {1{`RANDOM}};
  rf_9 = _RAND_9[31:0];
  _RAND_10 = {1{`RANDOM}};
  rf_10 = _RAND_10[31:0];
  _RAND_11 = {1{`RANDOM}};
  rf_11 = _RAND_11[31:0];
  _RAND_12 = {1{`RANDOM}};
  rf_12 = _RAND_12[31:0];
  _RAND_13 = {1{`RANDOM}};
  rf_13 = _RAND_13[31:0];
  _RAND_14 = {1{`RANDOM}};
  rf_14 = _RAND_14[31:0];
  _RAND_15 = {1{`RANDOM}};
  rf_15 = _RAND_15[31:0];
  _RAND_16 = {1{`RANDOM}};
  rf_16 = _RAND_16[31:0];
  _RAND_17 = {1{`RANDOM}};
  rf_17 = _RAND_17[31:0];
  _RAND_18 = {1{`RANDOM}};
  rf_18 = _RAND_18[31:0];
  _RAND_19 = {1{`RANDOM}};
  rf_19 = _RAND_19[31:0];
  _RAND_20 = {1{`RANDOM}};
  rf_20 = _RAND_20[31:0];
  _RAND_21 = {1{`RANDOM}};
  rf_21 = _RAND_21[31:0];
  _RAND_22 = {1{`RANDOM}};
  rf_22 = _RAND_22[31:0];
  _RAND_23 = {1{`RANDOM}};
  rf_23 = _RAND_23[31:0];
  _RAND_24 = {1{`RANDOM}};
  rf_24 = _RAND_24[31:0];
  _RAND_25 = {1{`RANDOM}};
  rf_25 = _RAND_25[31:0];
  _RAND_26 = {1{`RANDOM}};
  rf_26 = _RAND_26[31:0];
  _RAND_27 = {1{`RANDOM}};
  rf_27 = _RAND_27[31:0];
  _RAND_28 = {1{`RANDOM}};
  rf_28 = _RAND_28[31:0];
  _RAND_29 = {1{`RANDOM}};
  rf_29 = _RAND_29[31:0];
  _RAND_30 = {1{`RANDOM}};
  rf_30 = _RAND_30[31:0];
  _RAND_31 = {1{`RANDOM}};
  rf_31 = _RAND_31[31:0];
`endif // RANDOMIZE_REG_INIT
  `endif // RANDOMIZE
end // initial
`ifdef FIRRTL_AFTER_INITIAL
`FIRRTL_AFTER_INITIAL
`endif
`endif // SYNTHESIS
endmodule
