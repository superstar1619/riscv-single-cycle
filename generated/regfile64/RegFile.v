module RegFile(
  input         clk, // @[src/main/scala/riscvsingle/ieu/RegFile.scala 28:15]
  input         reset, // @[src/main/scala/riscvsingle/ieu/RegFile.scala 29:17]
  input         io_WE3, // @[src/main/scala/riscvsingle/ieu/RegFile.scala 30:14]
  input  [4:0]  io_A1, // @[src/main/scala/riscvsingle/ieu/RegFile.scala 30:14]
  input  [4:0]  io_A2, // @[src/main/scala/riscvsingle/ieu/RegFile.scala 30:14]
  input  [4:0]  io_A3, // @[src/main/scala/riscvsingle/ieu/RegFile.scala 30:14]
  input  [63:0] io_WD3, // @[src/main/scala/riscvsingle/ieu/RegFile.scala 30:14]
  output [63:0] io_RD1, // @[src/main/scala/riscvsingle/ieu/RegFile.scala 30:14]
  output [63:0] io_RD2 // @[src/main/scala/riscvsingle/ieu/RegFile.scala 30:14]
);
`ifdef RANDOMIZE_REG_INIT
  reg [63:0] _RAND_0;
  reg [63:0] _RAND_1;
  reg [63:0] _RAND_2;
  reg [63:0] _RAND_3;
  reg [63:0] _RAND_4;
  reg [63:0] _RAND_5;
  reg [63:0] _RAND_6;
  reg [63:0] _RAND_7;
  reg [63:0] _RAND_8;
  reg [63:0] _RAND_9;
  reg [63:0] _RAND_10;
  reg [63:0] _RAND_11;
  reg [63:0] _RAND_12;
  reg [63:0] _RAND_13;
  reg [63:0] _RAND_14;
  reg [63:0] _RAND_15;
  reg [63:0] _RAND_16;
  reg [63:0] _RAND_17;
  reg [63:0] _RAND_18;
  reg [63:0] _RAND_19;
  reg [63:0] _RAND_20;
  reg [63:0] _RAND_21;
  reg [63:0] _RAND_22;
  reg [63:0] _RAND_23;
  reg [63:0] _RAND_24;
  reg [63:0] _RAND_25;
  reg [63:0] _RAND_26;
  reg [63:0] _RAND_27;
  reg [63:0] _RAND_28;
  reg [63:0] _RAND_29;
  reg [63:0] _RAND_30;
  reg [63:0] _RAND_31;
`endif // RANDOMIZE_REG_INIT
  reg [63:0] rf_0; // @[src/main/scala/riscvsingle/ieu/RegFile.scala 33:32]
  reg [63:0] rf_1; // @[src/main/scala/riscvsingle/ieu/RegFile.scala 33:32]
  reg [63:0] rf_2; // @[src/main/scala/riscvsingle/ieu/RegFile.scala 33:32]
  reg [63:0] rf_3; // @[src/main/scala/riscvsingle/ieu/RegFile.scala 33:32]
  reg [63:0] rf_4; // @[src/main/scala/riscvsingle/ieu/RegFile.scala 33:32]
  reg [63:0] rf_5; // @[src/main/scala/riscvsingle/ieu/RegFile.scala 33:32]
  reg [63:0] rf_6; // @[src/main/scala/riscvsingle/ieu/RegFile.scala 33:32]
  reg [63:0] rf_7; // @[src/main/scala/riscvsingle/ieu/RegFile.scala 33:32]
  reg [63:0] rf_8; // @[src/main/scala/riscvsingle/ieu/RegFile.scala 33:32]
  reg [63:0] rf_9; // @[src/main/scala/riscvsingle/ieu/RegFile.scala 33:32]
  reg [63:0] rf_10; // @[src/main/scala/riscvsingle/ieu/RegFile.scala 33:32]
  reg [63:0] rf_11; // @[src/main/scala/riscvsingle/ieu/RegFile.scala 33:32]
  reg [63:0] rf_12; // @[src/main/scala/riscvsingle/ieu/RegFile.scala 33:32]
  reg [63:0] rf_13; // @[src/main/scala/riscvsingle/ieu/RegFile.scala 33:32]
  reg [63:0] rf_14; // @[src/main/scala/riscvsingle/ieu/RegFile.scala 33:32]
  reg [63:0] rf_15; // @[src/main/scala/riscvsingle/ieu/RegFile.scala 33:32]
  reg [63:0] rf_16; // @[src/main/scala/riscvsingle/ieu/RegFile.scala 33:32]
  reg [63:0] rf_17; // @[src/main/scala/riscvsingle/ieu/RegFile.scala 33:32]
  reg [63:0] rf_18; // @[src/main/scala/riscvsingle/ieu/RegFile.scala 33:32]
  reg [63:0] rf_19; // @[src/main/scala/riscvsingle/ieu/RegFile.scala 33:32]
  reg [63:0] rf_20; // @[src/main/scala/riscvsingle/ieu/RegFile.scala 33:32]
  reg [63:0] rf_21; // @[src/main/scala/riscvsingle/ieu/RegFile.scala 33:32]
  reg [63:0] rf_22; // @[src/main/scala/riscvsingle/ieu/RegFile.scala 33:32]
  reg [63:0] rf_23; // @[src/main/scala/riscvsingle/ieu/RegFile.scala 33:32]
  reg [63:0] rf_24; // @[src/main/scala/riscvsingle/ieu/RegFile.scala 33:32]
  reg [63:0] rf_25; // @[src/main/scala/riscvsingle/ieu/RegFile.scala 33:32]
  reg [63:0] rf_26; // @[src/main/scala/riscvsingle/ieu/RegFile.scala 33:32]
  reg [63:0] rf_27; // @[src/main/scala/riscvsingle/ieu/RegFile.scala 33:32]
  reg [63:0] rf_28; // @[src/main/scala/riscvsingle/ieu/RegFile.scala 33:32]
  reg [63:0] rf_29; // @[src/main/scala/riscvsingle/ieu/RegFile.scala 33:32]
  reg [63:0] rf_30; // @[src/main/scala/riscvsingle/ieu/RegFile.scala 33:32]
  reg [63:0] rf_31; // @[src/main/scala/riscvsingle/ieu/RegFile.scala 33:32]
  wire  WriteEnable = io_WE3 & io_A3 != 5'h0; // @[src/main/scala/riscvsingle/ieu/RegFile.scala 35:25]
  wire [63:0] _GEN_97 = 5'h1 == io_A1 ? rf_1 : rf_0; // @[src/main/scala/riscvsingle/ieu/RegFile.scala 45:{10,10}]
  wire [63:0] _GEN_98 = 5'h2 == io_A1 ? rf_2 : _GEN_97; // @[src/main/scala/riscvsingle/ieu/RegFile.scala 45:{10,10}]
  wire [63:0] _GEN_99 = 5'h3 == io_A1 ? rf_3 : _GEN_98; // @[src/main/scala/riscvsingle/ieu/RegFile.scala 45:{10,10}]
  wire [63:0] _GEN_100 = 5'h4 == io_A1 ? rf_4 : _GEN_99; // @[src/main/scala/riscvsingle/ieu/RegFile.scala 45:{10,10}]
  wire [63:0] _GEN_101 = 5'h5 == io_A1 ? rf_5 : _GEN_100; // @[src/main/scala/riscvsingle/ieu/RegFile.scala 45:{10,10}]
  wire [63:0] _GEN_102 = 5'h6 == io_A1 ? rf_6 : _GEN_101; // @[src/main/scala/riscvsingle/ieu/RegFile.scala 45:{10,10}]
  wire [63:0] _GEN_103 = 5'h7 == io_A1 ? rf_7 : _GEN_102; // @[src/main/scala/riscvsingle/ieu/RegFile.scala 45:{10,10}]
  wire [63:0] _GEN_104 = 5'h8 == io_A1 ? rf_8 : _GEN_103; // @[src/main/scala/riscvsingle/ieu/RegFile.scala 45:{10,10}]
  wire [63:0] _GEN_105 = 5'h9 == io_A1 ? rf_9 : _GEN_104; // @[src/main/scala/riscvsingle/ieu/RegFile.scala 45:{10,10}]
  wire [63:0] _GEN_106 = 5'ha == io_A1 ? rf_10 : _GEN_105; // @[src/main/scala/riscvsingle/ieu/RegFile.scala 45:{10,10}]
  wire [63:0] _GEN_107 = 5'hb == io_A1 ? rf_11 : _GEN_106; // @[src/main/scala/riscvsingle/ieu/RegFile.scala 45:{10,10}]
  wire [63:0] _GEN_108 = 5'hc == io_A1 ? rf_12 : _GEN_107; // @[src/main/scala/riscvsingle/ieu/RegFile.scala 45:{10,10}]
  wire [63:0] _GEN_109 = 5'hd == io_A1 ? rf_13 : _GEN_108; // @[src/main/scala/riscvsingle/ieu/RegFile.scala 45:{10,10}]
  wire [63:0] _GEN_110 = 5'he == io_A1 ? rf_14 : _GEN_109; // @[src/main/scala/riscvsingle/ieu/RegFile.scala 45:{10,10}]
  wire [63:0] _GEN_111 = 5'hf == io_A1 ? rf_15 : _GEN_110; // @[src/main/scala/riscvsingle/ieu/RegFile.scala 45:{10,10}]
  wire [63:0] _GEN_112 = 5'h10 == io_A1 ? rf_16 : _GEN_111; // @[src/main/scala/riscvsingle/ieu/RegFile.scala 45:{10,10}]
  wire [63:0] _GEN_113 = 5'h11 == io_A1 ? rf_17 : _GEN_112; // @[src/main/scala/riscvsingle/ieu/RegFile.scala 45:{10,10}]
  wire [63:0] _GEN_114 = 5'h12 == io_A1 ? rf_18 : _GEN_113; // @[src/main/scala/riscvsingle/ieu/RegFile.scala 45:{10,10}]
  wire [63:0] _GEN_115 = 5'h13 == io_A1 ? rf_19 : _GEN_114; // @[src/main/scala/riscvsingle/ieu/RegFile.scala 45:{10,10}]
  wire [63:0] _GEN_116 = 5'h14 == io_A1 ? rf_20 : _GEN_115; // @[src/main/scala/riscvsingle/ieu/RegFile.scala 45:{10,10}]
  wire [63:0] _GEN_117 = 5'h15 == io_A1 ? rf_21 : _GEN_116; // @[src/main/scala/riscvsingle/ieu/RegFile.scala 45:{10,10}]
  wire [63:0] _GEN_118 = 5'h16 == io_A1 ? rf_22 : _GEN_117; // @[src/main/scala/riscvsingle/ieu/RegFile.scala 45:{10,10}]
  wire [63:0] _GEN_119 = 5'h17 == io_A1 ? rf_23 : _GEN_118; // @[src/main/scala/riscvsingle/ieu/RegFile.scala 45:{10,10}]
  wire [63:0] _GEN_120 = 5'h18 == io_A1 ? rf_24 : _GEN_119; // @[src/main/scala/riscvsingle/ieu/RegFile.scala 45:{10,10}]
  wire [63:0] _GEN_121 = 5'h19 == io_A1 ? rf_25 : _GEN_120; // @[src/main/scala/riscvsingle/ieu/RegFile.scala 45:{10,10}]
  wire [63:0] _GEN_122 = 5'h1a == io_A1 ? rf_26 : _GEN_121; // @[src/main/scala/riscvsingle/ieu/RegFile.scala 45:{10,10}]
  wire [63:0] _GEN_123 = 5'h1b == io_A1 ? rf_27 : _GEN_122; // @[src/main/scala/riscvsingle/ieu/RegFile.scala 45:{10,10}]
  wire [63:0] _GEN_124 = 5'h1c == io_A1 ? rf_28 : _GEN_123; // @[src/main/scala/riscvsingle/ieu/RegFile.scala 45:{10,10}]
  wire [63:0] _GEN_125 = 5'h1d == io_A1 ? rf_29 : _GEN_124; // @[src/main/scala/riscvsingle/ieu/RegFile.scala 45:{10,10}]
  wire [63:0] _GEN_126 = 5'h1e == io_A1 ? rf_30 : _GEN_125; // @[src/main/scala/riscvsingle/ieu/RegFile.scala 45:{10,10}]
  wire [63:0] _GEN_129 = 5'h1 == io_A2 ? rf_1 : rf_0; // @[src/main/scala/riscvsingle/ieu/RegFile.scala 46:{10,10}]
  wire [63:0] _GEN_130 = 5'h2 == io_A2 ? rf_2 : _GEN_129; // @[src/main/scala/riscvsingle/ieu/RegFile.scala 46:{10,10}]
  wire [63:0] _GEN_131 = 5'h3 == io_A2 ? rf_3 : _GEN_130; // @[src/main/scala/riscvsingle/ieu/RegFile.scala 46:{10,10}]
  wire [63:0] _GEN_132 = 5'h4 == io_A2 ? rf_4 : _GEN_131; // @[src/main/scala/riscvsingle/ieu/RegFile.scala 46:{10,10}]
  wire [63:0] _GEN_133 = 5'h5 == io_A2 ? rf_5 : _GEN_132; // @[src/main/scala/riscvsingle/ieu/RegFile.scala 46:{10,10}]
  wire [63:0] _GEN_134 = 5'h6 == io_A2 ? rf_6 : _GEN_133; // @[src/main/scala/riscvsingle/ieu/RegFile.scala 46:{10,10}]
  wire [63:0] _GEN_135 = 5'h7 == io_A2 ? rf_7 : _GEN_134; // @[src/main/scala/riscvsingle/ieu/RegFile.scala 46:{10,10}]
  wire [63:0] _GEN_136 = 5'h8 == io_A2 ? rf_8 : _GEN_135; // @[src/main/scala/riscvsingle/ieu/RegFile.scala 46:{10,10}]
  wire [63:0] _GEN_137 = 5'h9 == io_A2 ? rf_9 : _GEN_136; // @[src/main/scala/riscvsingle/ieu/RegFile.scala 46:{10,10}]
  wire [63:0] _GEN_138 = 5'ha == io_A2 ? rf_10 : _GEN_137; // @[src/main/scala/riscvsingle/ieu/RegFile.scala 46:{10,10}]
  wire [63:0] _GEN_139 = 5'hb == io_A2 ? rf_11 : _GEN_138; // @[src/main/scala/riscvsingle/ieu/RegFile.scala 46:{10,10}]
  wire [63:0] _GEN_140 = 5'hc == io_A2 ? rf_12 : _GEN_139; // @[src/main/scala/riscvsingle/ieu/RegFile.scala 46:{10,10}]
  wire [63:0] _GEN_141 = 5'hd == io_A2 ? rf_13 : _GEN_140; // @[src/main/scala/riscvsingle/ieu/RegFile.scala 46:{10,10}]
  wire [63:0] _GEN_142 = 5'he == io_A2 ? rf_14 : _GEN_141; // @[src/main/scala/riscvsingle/ieu/RegFile.scala 46:{10,10}]
  wire [63:0] _GEN_143 = 5'hf == io_A2 ? rf_15 : _GEN_142; // @[src/main/scala/riscvsingle/ieu/RegFile.scala 46:{10,10}]
  wire [63:0] _GEN_144 = 5'h10 == io_A2 ? rf_16 : _GEN_143; // @[src/main/scala/riscvsingle/ieu/RegFile.scala 46:{10,10}]
  wire [63:0] _GEN_145 = 5'h11 == io_A2 ? rf_17 : _GEN_144; // @[src/main/scala/riscvsingle/ieu/RegFile.scala 46:{10,10}]
  wire [63:0] _GEN_146 = 5'h12 == io_A2 ? rf_18 : _GEN_145; // @[src/main/scala/riscvsingle/ieu/RegFile.scala 46:{10,10}]
  wire [63:0] _GEN_147 = 5'h13 == io_A2 ? rf_19 : _GEN_146; // @[src/main/scala/riscvsingle/ieu/RegFile.scala 46:{10,10}]
  wire [63:0] _GEN_148 = 5'h14 == io_A2 ? rf_20 : _GEN_147; // @[src/main/scala/riscvsingle/ieu/RegFile.scala 46:{10,10}]
  wire [63:0] _GEN_149 = 5'h15 == io_A2 ? rf_21 : _GEN_148; // @[src/main/scala/riscvsingle/ieu/RegFile.scala 46:{10,10}]
  wire [63:0] _GEN_150 = 5'h16 == io_A2 ? rf_22 : _GEN_149; // @[src/main/scala/riscvsingle/ieu/RegFile.scala 46:{10,10}]
  wire [63:0] _GEN_151 = 5'h17 == io_A2 ? rf_23 : _GEN_150; // @[src/main/scala/riscvsingle/ieu/RegFile.scala 46:{10,10}]
  wire [63:0] _GEN_152 = 5'h18 == io_A2 ? rf_24 : _GEN_151; // @[src/main/scala/riscvsingle/ieu/RegFile.scala 46:{10,10}]
  wire [63:0] _GEN_153 = 5'h19 == io_A2 ? rf_25 : _GEN_152; // @[src/main/scala/riscvsingle/ieu/RegFile.scala 46:{10,10}]
  wire [63:0] _GEN_154 = 5'h1a == io_A2 ? rf_26 : _GEN_153; // @[src/main/scala/riscvsingle/ieu/RegFile.scala 46:{10,10}]
  wire [63:0] _GEN_155 = 5'h1b == io_A2 ? rf_27 : _GEN_154; // @[src/main/scala/riscvsingle/ieu/RegFile.scala 46:{10,10}]
  wire [63:0] _GEN_156 = 5'h1c == io_A2 ? rf_28 : _GEN_155; // @[src/main/scala/riscvsingle/ieu/RegFile.scala 46:{10,10}]
  wire [63:0] _GEN_157 = 5'h1d == io_A2 ? rf_29 : _GEN_156; // @[src/main/scala/riscvsingle/ieu/RegFile.scala 46:{10,10}]
  wire [63:0] _GEN_158 = 5'h1e == io_A2 ? rf_30 : _GEN_157; // @[src/main/scala/riscvsingle/ieu/RegFile.scala 46:{10,10}]
  assign io_RD1 = 5'h1f == io_A1 ? rf_31 : _GEN_126; // @[src/main/scala/riscvsingle/ieu/RegFile.scala 45:{10,10}]
  assign io_RD2 = 5'h1f == io_A2 ? rf_31 : _GEN_158; // @[src/main/scala/riscvsingle/ieu/RegFile.scala 46:{10,10}]
  always @(posedge clk) begin
    if (reset) begin // @[src/main/scala/riscvsingle/ieu/RegFile.scala 38:17]
      rf_0 <= 64'h0; // @[src/main/scala/riscvsingle/ieu/RegFile.scala 39:13]
    end else if (WriteEnable) begin // @[src/main/scala/riscvsingle/ieu/RegFile.scala 40:29]
      if (5'h0 == io_A3) begin // @[src/main/scala/riscvsingle/ieu/RegFile.scala 41:17]
        rf_0 <= io_WD3; // @[src/main/scala/riscvsingle/ieu/RegFile.scala 41:17]
      end
    end
    if (!(reset)) begin // @[src/main/scala/riscvsingle/ieu/RegFile.scala 38:17]
      if (WriteEnable) begin // @[src/main/scala/riscvsingle/ieu/RegFile.scala 40:29]
        if (5'h1 == io_A3) begin // @[src/main/scala/riscvsingle/ieu/RegFile.scala 41:17]
          rf_1 <= io_WD3; // @[src/main/scala/riscvsingle/ieu/RegFile.scala 41:17]
        end
      end
    end
    if (!(reset)) begin // @[src/main/scala/riscvsingle/ieu/RegFile.scala 38:17]
      if (WriteEnable) begin // @[src/main/scala/riscvsingle/ieu/RegFile.scala 40:29]
        if (5'h2 == io_A3) begin // @[src/main/scala/riscvsingle/ieu/RegFile.scala 41:17]
          rf_2 <= io_WD3; // @[src/main/scala/riscvsingle/ieu/RegFile.scala 41:17]
        end
      end
    end
    if (!(reset)) begin // @[src/main/scala/riscvsingle/ieu/RegFile.scala 38:17]
      if (WriteEnable) begin // @[src/main/scala/riscvsingle/ieu/RegFile.scala 40:29]
        if (5'h3 == io_A3) begin // @[src/main/scala/riscvsingle/ieu/RegFile.scala 41:17]
          rf_3 <= io_WD3; // @[src/main/scala/riscvsingle/ieu/RegFile.scala 41:17]
        end
      end
    end
    if (!(reset)) begin // @[src/main/scala/riscvsingle/ieu/RegFile.scala 38:17]
      if (WriteEnable) begin // @[src/main/scala/riscvsingle/ieu/RegFile.scala 40:29]
        if (5'h4 == io_A3) begin // @[src/main/scala/riscvsingle/ieu/RegFile.scala 41:17]
          rf_4 <= io_WD3; // @[src/main/scala/riscvsingle/ieu/RegFile.scala 41:17]
        end
      end
    end
    if (!(reset)) begin // @[src/main/scala/riscvsingle/ieu/RegFile.scala 38:17]
      if (WriteEnable) begin // @[src/main/scala/riscvsingle/ieu/RegFile.scala 40:29]
        if (5'h5 == io_A3) begin // @[src/main/scala/riscvsingle/ieu/RegFile.scala 41:17]
          rf_5 <= io_WD3; // @[src/main/scala/riscvsingle/ieu/RegFile.scala 41:17]
        end
      end
    end
    if (!(reset)) begin // @[src/main/scala/riscvsingle/ieu/RegFile.scala 38:17]
      if (WriteEnable) begin // @[src/main/scala/riscvsingle/ieu/RegFile.scala 40:29]
        if (5'h6 == io_A3) begin // @[src/main/scala/riscvsingle/ieu/RegFile.scala 41:17]
          rf_6 <= io_WD3; // @[src/main/scala/riscvsingle/ieu/RegFile.scala 41:17]
        end
      end
    end
    if (!(reset)) begin // @[src/main/scala/riscvsingle/ieu/RegFile.scala 38:17]
      if (WriteEnable) begin // @[src/main/scala/riscvsingle/ieu/RegFile.scala 40:29]
        if (5'h7 == io_A3) begin // @[src/main/scala/riscvsingle/ieu/RegFile.scala 41:17]
          rf_7 <= io_WD3; // @[src/main/scala/riscvsingle/ieu/RegFile.scala 41:17]
        end
      end
    end
    if (!(reset)) begin // @[src/main/scala/riscvsingle/ieu/RegFile.scala 38:17]
      if (WriteEnable) begin // @[src/main/scala/riscvsingle/ieu/RegFile.scala 40:29]
        if (5'h8 == io_A3) begin // @[src/main/scala/riscvsingle/ieu/RegFile.scala 41:17]
          rf_8 <= io_WD3; // @[src/main/scala/riscvsingle/ieu/RegFile.scala 41:17]
        end
      end
    end
    if (!(reset)) begin // @[src/main/scala/riscvsingle/ieu/RegFile.scala 38:17]
      if (WriteEnable) begin // @[src/main/scala/riscvsingle/ieu/RegFile.scala 40:29]
        if (5'h9 == io_A3) begin // @[src/main/scala/riscvsingle/ieu/RegFile.scala 41:17]
          rf_9 <= io_WD3; // @[src/main/scala/riscvsingle/ieu/RegFile.scala 41:17]
        end
      end
    end
    if (!(reset)) begin // @[src/main/scala/riscvsingle/ieu/RegFile.scala 38:17]
      if (WriteEnable) begin // @[src/main/scala/riscvsingle/ieu/RegFile.scala 40:29]
        if (5'ha == io_A3) begin // @[src/main/scala/riscvsingle/ieu/RegFile.scala 41:17]
          rf_10 <= io_WD3; // @[src/main/scala/riscvsingle/ieu/RegFile.scala 41:17]
        end
      end
    end
    if (!(reset)) begin // @[src/main/scala/riscvsingle/ieu/RegFile.scala 38:17]
      if (WriteEnable) begin // @[src/main/scala/riscvsingle/ieu/RegFile.scala 40:29]
        if (5'hb == io_A3) begin // @[src/main/scala/riscvsingle/ieu/RegFile.scala 41:17]
          rf_11 <= io_WD3; // @[src/main/scala/riscvsingle/ieu/RegFile.scala 41:17]
        end
      end
    end
    if (!(reset)) begin // @[src/main/scala/riscvsingle/ieu/RegFile.scala 38:17]
      if (WriteEnable) begin // @[src/main/scala/riscvsingle/ieu/RegFile.scala 40:29]
        if (5'hc == io_A3) begin // @[src/main/scala/riscvsingle/ieu/RegFile.scala 41:17]
          rf_12 <= io_WD3; // @[src/main/scala/riscvsingle/ieu/RegFile.scala 41:17]
        end
      end
    end
    if (!(reset)) begin // @[src/main/scala/riscvsingle/ieu/RegFile.scala 38:17]
      if (WriteEnable) begin // @[src/main/scala/riscvsingle/ieu/RegFile.scala 40:29]
        if (5'hd == io_A3) begin // @[src/main/scala/riscvsingle/ieu/RegFile.scala 41:17]
          rf_13 <= io_WD3; // @[src/main/scala/riscvsingle/ieu/RegFile.scala 41:17]
        end
      end
    end
    if (!(reset)) begin // @[src/main/scala/riscvsingle/ieu/RegFile.scala 38:17]
      if (WriteEnable) begin // @[src/main/scala/riscvsingle/ieu/RegFile.scala 40:29]
        if (5'he == io_A3) begin // @[src/main/scala/riscvsingle/ieu/RegFile.scala 41:17]
          rf_14 <= io_WD3; // @[src/main/scala/riscvsingle/ieu/RegFile.scala 41:17]
        end
      end
    end
    if (!(reset)) begin // @[src/main/scala/riscvsingle/ieu/RegFile.scala 38:17]
      if (WriteEnable) begin // @[src/main/scala/riscvsingle/ieu/RegFile.scala 40:29]
        if (5'hf == io_A3) begin // @[src/main/scala/riscvsingle/ieu/RegFile.scala 41:17]
          rf_15 <= io_WD3; // @[src/main/scala/riscvsingle/ieu/RegFile.scala 41:17]
        end
      end
    end
    if (!(reset)) begin // @[src/main/scala/riscvsingle/ieu/RegFile.scala 38:17]
      if (WriteEnable) begin // @[src/main/scala/riscvsingle/ieu/RegFile.scala 40:29]
        if (5'h10 == io_A3) begin // @[src/main/scala/riscvsingle/ieu/RegFile.scala 41:17]
          rf_16 <= io_WD3; // @[src/main/scala/riscvsingle/ieu/RegFile.scala 41:17]
        end
      end
    end
    if (!(reset)) begin // @[src/main/scala/riscvsingle/ieu/RegFile.scala 38:17]
      if (WriteEnable) begin // @[src/main/scala/riscvsingle/ieu/RegFile.scala 40:29]
        if (5'h11 == io_A3) begin // @[src/main/scala/riscvsingle/ieu/RegFile.scala 41:17]
          rf_17 <= io_WD3; // @[src/main/scala/riscvsingle/ieu/RegFile.scala 41:17]
        end
      end
    end
    if (!(reset)) begin // @[src/main/scala/riscvsingle/ieu/RegFile.scala 38:17]
      if (WriteEnable) begin // @[src/main/scala/riscvsingle/ieu/RegFile.scala 40:29]
        if (5'h12 == io_A3) begin // @[src/main/scala/riscvsingle/ieu/RegFile.scala 41:17]
          rf_18 <= io_WD3; // @[src/main/scala/riscvsingle/ieu/RegFile.scala 41:17]
        end
      end
    end
    if (!(reset)) begin // @[src/main/scala/riscvsingle/ieu/RegFile.scala 38:17]
      if (WriteEnable) begin // @[src/main/scala/riscvsingle/ieu/RegFile.scala 40:29]
        if (5'h13 == io_A3) begin // @[src/main/scala/riscvsingle/ieu/RegFile.scala 41:17]
          rf_19 <= io_WD3; // @[src/main/scala/riscvsingle/ieu/RegFile.scala 41:17]
        end
      end
    end
    if (!(reset)) begin // @[src/main/scala/riscvsingle/ieu/RegFile.scala 38:17]
      if (WriteEnable) begin // @[src/main/scala/riscvsingle/ieu/RegFile.scala 40:29]
        if (5'h14 == io_A3) begin // @[src/main/scala/riscvsingle/ieu/RegFile.scala 41:17]
          rf_20 <= io_WD3; // @[src/main/scala/riscvsingle/ieu/RegFile.scala 41:17]
        end
      end
    end
    if (!(reset)) begin // @[src/main/scala/riscvsingle/ieu/RegFile.scala 38:17]
      if (WriteEnable) begin // @[src/main/scala/riscvsingle/ieu/RegFile.scala 40:29]
        if (5'h15 == io_A3) begin // @[src/main/scala/riscvsingle/ieu/RegFile.scala 41:17]
          rf_21 <= io_WD3; // @[src/main/scala/riscvsingle/ieu/RegFile.scala 41:17]
        end
      end
    end
    if (!(reset)) begin // @[src/main/scala/riscvsingle/ieu/RegFile.scala 38:17]
      if (WriteEnable) begin // @[src/main/scala/riscvsingle/ieu/RegFile.scala 40:29]
        if (5'h16 == io_A3) begin // @[src/main/scala/riscvsingle/ieu/RegFile.scala 41:17]
          rf_22 <= io_WD3; // @[src/main/scala/riscvsingle/ieu/RegFile.scala 41:17]
        end
      end
    end
    if (!(reset)) begin // @[src/main/scala/riscvsingle/ieu/RegFile.scala 38:17]
      if (WriteEnable) begin // @[src/main/scala/riscvsingle/ieu/RegFile.scala 40:29]
        if (5'h17 == io_A3) begin // @[src/main/scala/riscvsingle/ieu/RegFile.scala 41:17]
          rf_23 <= io_WD3; // @[src/main/scala/riscvsingle/ieu/RegFile.scala 41:17]
        end
      end
    end
    if (!(reset)) begin // @[src/main/scala/riscvsingle/ieu/RegFile.scala 38:17]
      if (WriteEnable) begin // @[src/main/scala/riscvsingle/ieu/RegFile.scala 40:29]
        if (5'h18 == io_A3) begin // @[src/main/scala/riscvsingle/ieu/RegFile.scala 41:17]
          rf_24 <= io_WD3; // @[src/main/scala/riscvsingle/ieu/RegFile.scala 41:17]
        end
      end
    end
    if (!(reset)) begin // @[src/main/scala/riscvsingle/ieu/RegFile.scala 38:17]
      if (WriteEnable) begin // @[src/main/scala/riscvsingle/ieu/RegFile.scala 40:29]
        if (5'h19 == io_A3) begin // @[src/main/scala/riscvsingle/ieu/RegFile.scala 41:17]
          rf_25 <= io_WD3; // @[src/main/scala/riscvsingle/ieu/RegFile.scala 41:17]
        end
      end
    end
    if (!(reset)) begin // @[src/main/scala/riscvsingle/ieu/RegFile.scala 38:17]
      if (WriteEnable) begin // @[src/main/scala/riscvsingle/ieu/RegFile.scala 40:29]
        if (5'h1a == io_A3) begin // @[src/main/scala/riscvsingle/ieu/RegFile.scala 41:17]
          rf_26 <= io_WD3; // @[src/main/scala/riscvsingle/ieu/RegFile.scala 41:17]
        end
      end
    end
    if (!(reset)) begin // @[src/main/scala/riscvsingle/ieu/RegFile.scala 38:17]
      if (WriteEnable) begin // @[src/main/scala/riscvsingle/ieu/RegFile.scala 40:29]
        if (5'h1b == io_A3) begin // @[src/main/scala/riscvsingle/ieu/RegFile.scala 41:17]
          rf_27 <= io_WD3; // @[src/main/scala/riscvsingle/ieu/RegFile.scala 41:17]
        end
      end
    end
    if (!(reset)) begin // @[src/main/scala/riscvsingle/ieu/RegFile.scala 38:17]
      if (WriteEnable) begin // @[src/main/scala/riscvsingle/ieu/RegFile.scala 40:29]
        if (5'h1c == io_A3) begin // @[src/main/scala/riscvsingle/ieu/RegFile.scala 41:17]
          rf_28 <= io_WD3; // @[src/main/scala/riscvsingle/ieu/RegFile.scala 41:17]
        end
      end
    end
    if (!(reset)) begin // @[src/main/scala/riscvsingle/ieu/RegFile.scala 38:17]
      if (WriteEnable) begin // @[src/main/scala/riscvsingle/ieu/RegFile.scala 40:29]
        if (5'h1d == io_A3) begin // @[src/main/scala/riscvsingle/ieu/RegFile.scala 41:17]
          rf_29 <= io_WD3; // @[src/main/scala/riscvsingle/ieu/RegFile.scala 41:17]
        end
      end
    end
    if (!(reset)) begin // @[src/main/scala/riscvsingle/ieu/RegFile.scala 38:17]
      if (WriteEnable) begin // @[src/main/scala/riscvsingle/ieu/RegFile.scala 40:29]
        if (5'h1e == io_A3) begin // @[src/main/scala/riscvsingle/ieu/RegFile.scala 41:17]
          rf_30 <= io_WD3; // @[src/main/scala/riscvsingle/ieu/RegFile.scala 41:17]
        end
      end
    end
    if (!(reset)) begin // @[src/main/scala/riscvsingle/ieu/RegFile.scala 38:17]
      if (WriteEnable) begin // @[src/main/scala/riscvsingle/ieu/RegFile.scala 40:29]
        if (5'h1f == io_A3) begin // @[src/main/scala/riscvsingle/ieu/RegFile.scala 41:17]
          rf_31 <= io_WD3; // @[src/main/scala/riscvsingle/ieu/RegFile.scala 41:17]
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
  _RAND_0 = {2{`RANDOM}};
  rf_0 = _RAND_0[63:0];
  _RAND_1 = {2{`RANDOM}};
  rf_1 = _RAND_1[63:0];
  _RAND_2 = {2{`RANDOM}};
  rf_2 = _RAND_2[63:0];
  _RAND_3 = {2{`RANDOM}};
  rf_3 = _RAND_3[63:0];
  _RAND_4 = {2{`RANDOM}};
  rf_4 = _RAND_4[63:0];
  _RAND_5 = {2{`RANDOM}};
  rf_5 = _RAND_5[63:0];
  _RAND_6 = {2{`RANDOM}};
  rf_6 = _RAND_6[63:0];
  _RAND_7 = {2{`RANDOM}};
  rf_7 = _RAND_7[63:0];
  _RAND_8 = {2{`RANDOM}};
  rf_8 = _RAND_8[63:0];
  _RAND_9 = {2{`RANDOM}};
  rf_9 = _RAND_9[63:0];
  _RAND_10 = {2{`RANDOM}};
  rf_10 = _RAND_10[63:0];
  _RAND_11 = {2{`RANDOM}};
  rf_11 = _RAND_11[63:0];
  _RAND_12 = {2{`RANDOM}};
  rf_12 = _RAND_12[63:0];
  _RAND_13 = {2{`RANDOM}};
  rf_13 = _RAND_13[63:0];
  _RAND_14 = {2{`RANDOM}};
  rf_14 = _RAND_14[63:0];
  _RAND_15 = {2{`RANDOM}};
  rf_15 = _RAND_15[63:0];
  _RAND_16 = {2{`RANDOM}};
  rf_16 = _RAND_16[63:0];
  _RAND_17 = {2{`RANDOM}};
  rf_17 = _RAND_17[63:0];
  _RAND_18 = {2{`RANDOM}};
  rf_18 = _RAND_18[63:0];
  _RAND_19 = {2{`RANDOM}};
  rf_19 = _RAND_19[63:0];
  _RAND_20 = {2{`RANDOM}};
  rf_20 = _RAND_20[63:0];
  _RAND_21 = {2{`RANDOM}};
  rf_21 = _RAND_21[63:0];
  _RAND_22 = {2{`RANDOM}};
  rf_22 = _RAND_22[63:0];
  _RAND_23 = {2{`RANDOM}};
  rf_23 = _RAND_23[63:0];
  _RAND_24 = {2{`RANDOM}};
  rf_24 = _RAND_24[63:0];
  _RAND_25 = {2{`RANDOM}};
  rf_25 = _RAND_25[63:0];
  _RAND_26 = {2{`RANDOM}};
  rf_26 = _RAND_26[63:0];
  _RAND_27 = {2{`RANDOM}};
  rf_27 = _RAND_27[63:0];
  _RAND_28 = {2{`RANDOM}};
  rf_28 = _RAND_28[63:0];
  _RAND_29 = {2{`RANDOM}};
  rf_29 = _RAND_29[63:0];
  _RAND_30 = {2{`RANDOM}};
  rf_30 = _RAND_30[63:0];
  _RAND_31 = {2{`RANDOM}};
  rf_31 = _RAND_31[63:0];
`endif // RANDOMIZE_REG_INIT
  `endif // RANDOMIZE
end // initial
`ifdef FIRRTL_AFTER_INITIAL
`FIRRTL_AFTER_INITIAL
`endif
`endif // SYNTHESIS
endmodule
