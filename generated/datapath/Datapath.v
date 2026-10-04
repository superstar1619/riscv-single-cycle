module RegFile(
  input         clk, // @[src/main/scala/riscvsingle/ieu/RegFile.scala 28:15]
  input         reset, // @[src/main/scala/riscvsingle/ieu/RegFile.scala 29:17]
  input         io_WE3, // @[src/main/scala/riscvsingle/ieu/RegFile.scala 30:14]
  input  [4:0]  io_A1, // @[src/main/scala/riscvsingle/ieu/RegFile.scala 30:14]
  input  [4:0]  io_A2, // @[src/main/scala/riscvsingle/ieu/RegFile.scala 30:14]
  input  [4:0]  io_A3, // @[src/main/scala/riscvsingle/ieu/RegFile.scala 30:14]
  input  [31:0] io_WD3, // @[src/main/scala/riscvsingle/ieu/RegFile.scala 30:14]
  output [31:0] io_RD1, // @[src/main/scala/riscvsingle/ieu/RegFile.scala 30:14]
  output [31:0] io_RD2 // @[src/main/scala/riscvsingle/ieu/RegFile.scala 30:14]
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
  reg [31:0] rf_0; // @[src/main/scala/riscvsingle/ieu/RegFile.scala 33:32]
  reg [31:0] rf_1; // @[src/main/scala/riscvsingle/ieu/RegFile.scala 33:32]
  reg [31:0] rf_2; // @[src/main/scala/riscvsingle/ieu/RegFile.scala 33:32]
  reg [31:0] rf_3; // @[src/main/scala/riscvsingle/ieu/RegFile.scala 33:32]
  reg [31:0] rf_4; // @[src/main/scala/riscvsingle/ieu/RegFile.scala 33:32]
  reg [31:0] rf_5; // @[src/main/scala/riscvsingle/ieu/RegFile.scala 33:32]
  reg [31:0] rf_6; // @[src/main/scala/riscvsingle/ieu/RegFile.scala 33:32]
  reg [31:0] rf_7; // @[src/main/scala/riscvsingle/ieu/RegFile.scala 33:32]
  reg [31:0] rf_8; // @[src/main/scala/riscvsingle/ieu/RegFile.scala 33:32]
  reg [31:0] rf_9; // @[src/main/scala/riscvsingle/ieu/RegFile.scala 33:32]
  reg [31:0] rf_10; // @[src/main/scala/riscvsingle/ieu/RegFile.scala 33:32]
  reg [31:0] rf_11; // @[src/main/scala/riscvsingle/ieu/RegFile.scala 33:32]
  reg [31:0] rf_12; // @[src/main/scala/riscvsingle/ieu/RegFile.scala 33:32]
  reg [31:0] rf_13; // @[src/main/scala/riscvsingle/ieu/RegFile.scala 33:32]
  reg [31:0] rf_14; // @[src/main/scala/riscvsingle/ieu/RegFile.scala 33:32]
  reg [31:0] rf_15; // @[src/main/scala/riscvsingle/ieu/RegFile.scala 33:32]
  reg [31:0] rf_16; // @[src/main/scala/riscvsingle/ieu/RegFile.scala 33:32]
  reg [31:0] rf_17; // @[src/main/scala/riscvsingle/ieu/RegFile.scala 33:32]
  reg [31:0] rf_18; // @[src/main/scala/riscvsingle/ieu/RegFile.scala 33:32]
  reg [31:0] rf_19; // @[src/main/scala/riscvsingle/ieu/RegFile.scala 33:32]
  reg [31:0] rf_20; // @[src/main/scala/riscvsingle/ieu/RegFile.scala 33:32]
  reg [31:0] rf_21; // @[src/main/scala/riscvsingle/ieu/RegFile.scala 33:32]
  reg [31:0] rf_22; // @[src/main/scala/riscvsingle/ieu/RegFile.scala 33:32]
  reg [31:0] rf_23; // @[src/main/scala/riscvsingle/ieu/RegFile.scala 33:32]
  reg [31:0] rf_24; // @[src/main/scala/riscvsingle/ieu/RegFile.scala 33:32]
  reg [31:0] rf_25; // @[src/main/scala/riscvsingle/ieu/RegFile.scala 33:32]
  reg [31:0] rf_26; // @[src/main/scala/riscvsingle/ieu/RegFile.scala 33:32]
  reg [31:0] rf_27; // @[src/main/scala/riscvsingle/ieu/RegFile.scala 33:32]
  reg [31:0] rf_28; // @[src/main/scala/riscvsingle/ieu/RegFile.scala 33:32]
  reg [31:0] rf_29; // @[src/main/scala/riscvsingle/ieu/RegFile.scala 33:32]
  reg [31:0] rf_30; // @[src/main/scala/riscvsingle/ieu/RegFile.scala 33:32]
  reg [31:0] rf_31; // @[src/main/scala/riscvsingle/ieu/RegFile.scala 33:32]
  wire  WriteEnable = io_WE3 & io_A3 != 5'h0; // @[src/main/scala/riscvsingle/ieu/RegFile.scala 35:25]
  wire [31:0] _GEN_97 = 5'h1 == io_A1 ? rf_1 : rf_0; // @[src/main/scala/riscvsingle/ieu/RegFile.scala 45:{10,10}]
  wire [31:0] _GEN_98 = 5'h2 == io_A1 ? rf_2 : _GEN_97; // @[src/main/scala/riscvsingle/ieu/RegFile.scala 45:{10,10}]
  wire [31:0] _GEN_99 = 5'h3 == io_A1 ? rf_3 : _GEN_98; // @[src/main/scala/riscvsingle/ieu/RegFile.scala 45:{10,10}]
  wire [31:0] _GEN_100 = 5'h4 == io_A1 ? rf_4 : _GEN_99; // @[src/main/scala/riscvsingle/ieu/RegFile.scala 45:{10,10}]
  wire [31:0] _GEN_101 = 5'h5 == io_A1 ? rf_5 : _GEN_100; // @[src/main/scala/riscvsingle/ieu/RegFile.scala 45:{10,10}]
  wire [31:0] _GEN_102 = 5'h6 == io_A1 ? rf_6 : _GEN_101; // @[src/main/scala/riscvsingle/ieu/RegFile.scala 45:{10,10}]
  wire [31:0] _GEN_103 = 5'h7 == io_A1 ? rf_7 : _GEN_102; // @[src/main/scala/riscvsingle/ieu/RegFile.scala 45:{10,10}]
  wire [31:0] _GEN_104 = 5'h8 == io_A1 ? rf_8 : _GEN_103; // @[src/main/scala/riscvsingle/ieu/RegFile.scala 45:{10,10}]
  wire [31:0] _GEN_105 = 5'h9 == io_A1 ? rf_9 : _GEN_104; // @[src/main/scala/riscvsingle/ieu/RegFile.scala 45:{10,10}]
  wire [31:0] _GEN_106 = 5'ha == io_A1 ? rf_10 : _GEN_105; // @[src/main/scala/riscvsingle/ieu/RegFile.scala 45:{10,10}]
  wire [31:0] _GEN_107 = 5'hb == io_A1 ? rf_11 : _GEN_106; // @[src/main/scala/riscvsingle/ieu/RegFile.scala 45:{10,10}]
  wire [31:0] _GEN_108 = 5'hc == io_A1 ? rf_12 : _GEN_107; // @[src/main/scala/riscvsingle/ieu/RegFile.scala 45:{10,10}]
  wire [31:0] _GEN_109 = 5'hd == io_A1 ? rf_13 : _GEN_108; // @[src/main/scala/riscvsingle/ieu/RegFile.scala 45:{10,10}]
  wire [31:0] _GEN_110 = 5'he == io_A1 ? rf_14 : _GEN_109; // @[src/main/scala/riscvsingle/ieu/RegFile.scala 45:{10,10}]
  wire [31:0] _GEN_111 = 5'hf == io_A1 ? rf_15 : _GEN_110; // @[src/main/scala/riscvsingle/ieu/RegFile.scala 45:{10,10}]
  wire [31:0] _GEN_112 = 5'h10 == io_A1 ? rf_16 : _GEN_111; // @[src/main/scala/riscvsingle/ieu/RegFile.scala 45:{10,10}]
  wire [31:0] _GEN_113 = 5'h11 == io_A1 ? rf_17 : _GEN_112; // @[src/main/scala/riscvsingle/ieu/RegFile.scala 45:{10,10}]
  wire [31:0] _GEN_114 = 5'h12 == io_A1 ? rf_18 : _GEN_113; // @[src/main/scala/riscvsingle/ieu/RegFile.scala 45:{10,10}]
  wire [31:0] _GEN_115 = 5'h13 == io_A1 ? rf_19 : _GEN_114; // @[src/main/scala/riscvsingle/ieu/RegFile.scala 45:{10,10}]
  wire [31:0] _GEN_116 = 5'h14 == io_A1 ? rf_20 : _GEN_115; // @[src/main/scala/riscvsingle/ieu/RegFile.scala 45:{10,10}]
  wire [31:0] _GEN_117 = 5'h15 == io_A1 ? rf_21 : _GEN_116; // @[src/main/scala/riscvsingle/ieu/RegFile.scala 45:{10,10}]
  wire [31:0] _GEN_118 = 5'h16 == io_A1 ? rf_22 : _GEN_117; // @[src/main/scala/riscvsingle/ieu/RegFile.scala 45:{10,10}]
  wire [31:0] _GEN_119 = 5'h17 == io_A1 ? rf_23 : _GEN_118; // @[src/main/scala/riscvsingle/ieu/RegFile.scala 45:{10,10}]
  wire [31:0] _GEN_120 = 5'h18 == io_A1 ? rf_24 : _GEN_119; // @[src/main/scala/riscvsingle/ieu/RegFile.scala 45:{10,10}]
  wire [31:0] _GEN_121 = 5'h19 == io_A1 ? rf_25 : _GEN_120; // @[src/main/scala/riscvsingle/ieu/RegFile.scala 45:{10,10}]
  wire [31:0] _GEN_122 = 5'h1a == io_A1 ? rf_26 : _GEN_121; // @[src/main/scala/riscvsingle/ieu/RegFile.scala 45:{10,10}]
  wire [31:0] _GEN_123 = 5'h1b == io_A1 ? rf_27 : _GEN_122; // @[src/main/scala/riscvsingle/ieu/RegFile.scala 45:{10,10}]
  wire [31:0] _GEN_124 = 5'h1c == io_A1 ? rf_28 : _GEN_123; // @[src/main/scala/riscvsingle/ieu/RegFile.scala 45:{10,10}]
  wire [31:0] _GEN_125 = 5'h1d == io_A1 ? rf_29 : _GEN_124; // @[src/main/scala/riscvsingle/ieu/RegFile.scala 45:{10,10}]
  wire [31:0] _GEN_126 = 5'h1e == io_A1 ? rf_30 : _GEN_125; // @[src/main/scala/riscvsingle/ieu/RegFile.scala 45:{10,10}]
  wire [31:0] _GEN_129 = 5'h1 == io_A2 ? rf_1 : rf_0; // @[src/main/scala/riscvsingle/ieu/RegFile.scala 46:{10,10}]
  wire [31:0] _GEN_130 = 5'h2 == io_A2 ? rf_2 : _GEN_129; // @[src/main/scala/riscvsingle/ieu/RegFile.scala 46:{10,10}]
  wire [31:0] _GEN_131 = 5'h3 == io_A2 ? rf_3 : _GEN_130; // @[src/main/scala/riscvsingle/ieu/RegFile.scala 46:{10,10}]
  wire [31:0] _GEN_132 = 5'h4 == io_A2 ? rf_4 : _GEN_131; // @[src/main/scala/riscvsingle/ieu/RegFile.scala 46:{10,10}]
  wire [31:0] _GEN_133 = 5'h5 == io_A2 ? rf_5 : _GEN_132; // @[src/main/scala/riscvsingle/ieu/RegFile.scala 46:{10,10}]
  wire [31:0] _GEN_134 = 5'h6 == io_A2 ? rf_6 : _GEN_133; // @[src/main/scala/riscvsingle/ieu/RegFile.scala 46:{10,10}]
  wire [31:0] _GEN_135 = 5'h7 == io_A2 ? rf_7 : _GEN_134; // @[src/main/scala/riscvsingle/ieu/RegFile.scala 46:{10,10}]
  wire [31:0] _GEN_136 = 5'h8 == io_A2 ? rf_8 : _GEN_135; // @[src/main/scala/riscvsingle/ieu/RegFile.scala 46:{10,10}]
  wire [31:0] _GEN_137 = 5'h9 == io_A2 ? rf_9 : _GEN_136; // @[src/main/scala/riscvsingle/ieu/RegFile.scala 46:{10,10}]
  wire [31:0] _GEN_138 = 5'ha == io_A2 ? rf_10 : _GEN_137; // @[src/main/scala/riscvsingle/ieu/RegFile.scala 46:{10,10}]
  wire [31:0] _GEN_139 = 5'hb == io_A2 ? rf_11 : _GEN_138; // @[src/main/scala/riscvsingle/ieu/RegFile.scala 46:{10,10}]
  wire [31:0] _GEN_140 = 5'hc == io_A2 ? rf_12 : _GEN_139; // @[src/main/scala/riscvsingle/ieu/RegFile.scala 46:{10,10}]
  wire [31:0] _GEN_141 = 5'hd == io_A2 ? rf_13 : _GEN_140; // @[src/main/scala/riscvsingle/ieu/RegFile.scala 46:{10,10}]
  wire [31:0] _GEN_142 = 5'he == io_A2 ? rf_14 : _GEN_141; // @[src/main/scala/riscvsingle/ieu/RegFile.scala 46:{10,10}]
  wire [31:0] _GEN_143 = 5'hf == io_A2 ? rf_15 : _GEN_142; // @[src/main/scala/riscvsingle/ieu/RegFile.scala 46:{10,10}]
  wire [31:0] _GEN_144 = 5'h10 == io_A2 ? rf_16 : _GEN_143; // @[src/main/scala/riscvsingle/ieu/RegFile.scala 46:{10,10}]
  wire [31:0] _GEN_145 = 5'h11 == io_A2 ? rf_17 : _GEN_144; // @[src/main/scala/riscvsingle/ieu/RegFile.scala 46:{10,10}]
  wire [31:0] _GEN_146 = 5'h12 == io_A2 ? rf_18 : _GEN_145; // @[src/main/scala/riscvsingle/ieu/RegFile.scala 46:{10,10}]
  wire [31:0] _GEN_147 = 5'h13 == io_A2 ? rf_19 : _GEN_146; // @[src/main/scala/riscvsingle/ieu/RegFile.scala 46:{10,10}]
  wire [31:0] _GEN_148 = 5'h14 == io_A2 ? rf_20 : _GEN_147; // @[src/main/scala/riscvsingle/ieu/RegFile.scala 46:{10,10}]
  wire [31:0] _GEN_149 = 5'h15 == io_A2 ? rf_21 : _GEN_148; // @[src/main/scala/riscvsingle/ieu/RegFile.scala 46:{10,10}]
  wire [31:0] _GEN_150 = 5'h16 == io_A2 ? rf_22 : _GEN_149; // @[src/main/scala/riscvsingle/ieu/RegFile.scala 46:{10,10}]
  wire [31:0] _GEN_151 = 5'h17 == io_A2 ? rf_23 : _GEN_150; // @[src/main/scala/riscvsingle/ieu/RegFile.scala 46:{10,10}]
  wire [31:0] _GEN_152 = 5'h18 == io_A2 ? rf_24 : _GEN_151; // @[src/main/scala/riscvsingle/ieu/RegFile.scala 46:{10,10}]
  wire [31:0] _GEN_153 = 5'h19 == io_A2 ? rf_25 : _GEN_152; // @[src/main/scala/riscvsingle/ieu/RegFile.scala 46:{10,10}]
  wire [31:0] _GEN_154 = 5'h1a == io_A2 ? rf_26 : _GEN_153; // @[src/main/scala/riscvsingle/ieu/RegFile.scala 46:{10,10}]
  wire [31:0] _GEN_155 = 5'h1b == io_A2 ? rf_27 : _GEN_154; // @[src/main/scala/riscvsingle/ieu/RegFile.scala 46:{10,10}]
  wire [31:0] _GEN_156 = 5'h1c == io_A2 ? rf_28 : _GEN_155; // @[src/main/scala/riscvsingle/ieu/RegFile.scala 46:{10,10}]
  wire [31:0] _GEN_157 = 5'h1d == io_A2 ? rf_29 : _GEN_156; // @[src/main/scala/riscvsingle/ieu/RegFile.scala 46:{10,10}]
  wire [31:0] _GEN_158 = 5'h1e == io_A2 ? rf_30 : _GEN_157; // @[src/main/scala/riscvsingle/ieu/RegFile.scala 46:{10,10}]
  assign io_RD1 = 5'h1f == io_A1 ? rf_31 : _GEN_126; // @[src/main/scala/riscvsingle/ieu/RegFile.scala 45:{10,10}]
  assign io_RD2 = 5'h1f == io_A2 ? rf_31 : _GEN_158; // @[src/main/scala/riscvsingle/ieu/RegFile.scala 46:{10,10}]
  always @(posedge clk) begin
    if (reset) begin // @[src/main/scala/riscvsingle/ieu/RegFile.scala 38:17]
      rf_0 <= 32'h0; // @[src/main/scala/riscvsingle/ieu/RegFile.scala 39:13]
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
module Extend(
  input  [24:0] io_Instr, // @[src/main/scala/riscvsingle/ieu/Extend.scala 19:14]
  input  [2:0]  io_ImmSrc, // @[src/main/scala/riscvsingle/ieu/Extend.scala 19:14]
  output [31:0] io_ImmExt // @[src/main/scala/riscvsingle/ieu/Extend.scala 19:14]
);
  wire [31:0] InstrFull = {io_Instr,7'h0}; // @[src/main/scala/riscvsingle/ieu/Extend.scala 23:19]
  wire [19:0] _ImmI_T_1 = InstrFull[31] ? 20'hfffff : 20'h0; // @[src/main/scala/riscvsingle/ieu/Extend.scala 31:19]
  wire [31:0] ImmI = {_ImmI_T_1,InstrFull[31:20]}; // @[src/main/scala/riscvsingle/ieu/Extend.scala 31:14]
  wire [31:0] ImmS = {_ImmI_T_1,InstrFull[31:25],InstrFull[11:7]}; // @[src/main/scala/riscvsingle/ieu/Extend.scala 32:14]
  wire [31:0] ImmB = {_ImmI_T_1,InstrFull[7],InstrFull[30:25],InstrFull[11:8],1'h0}; // @[src/main/scala/riscvsingle/ieu/Extend.scala 33:14]
  wire [11:0] _ImmJ_T_1 = InstrFull[31] ? 12'hfff : 12'h0; // @[src/main/scala/riscvsingle/ieu/Extend.scala 35:19]
  wire [31:0] ImmJ = {_ImmJ_T_1,InstrFull[19:12],InstrFull[20],InstrFull[30:21],1'h0}; // @[src/main/scala/riscvsingle/ieu/Extend.scala 35:14]
  wire [31:0] ImmU = {InstrFull[31],InstrFull[30:12],12'h0}; // @[src/main/scala/riscvsingle/ieu/Extend.scala 37:14]
  wire [31:0] _io_ImmExt_T_1 = 3'h0 == io_ImmSrc ? ImmI : 32'h0; // @[src/main/scala/riscvsingle/ieu/Extend.scala 40:56]
  wire [31:0] _io_ImmExt_T_3 = 3'h1 == io_ImmSrc ? ImmS : _io_ImmExt_T_1; // @[src/main/scala/riscvsingle/ieu/Extend.scala 40:56]
  wire [31:0] _io_ImmExt_T_5 = 3'h2 == io_ImmSrc ? ImmB : _io_ImmExt_T_3; // @[src/main/scala/riscvsingle/ieu/Extend.scala 40:56]
  wire [31:0] _io_ImmExt_T_7 = 3'h3 == io_ImmSrc ? ImmJ : _io_ImmExt_T_5; // @[src/main/scala/riscvsingle/ieu/Extend.scala 40:56]
  assign io_ImmExt = 3'h4 == io_ImmSrc ? ImmU : _io_ImmExt_T_7; // @[src/main/scala/riscvsingle/ieu/Extend.scala 40:56]
endmodule
module Cmp(
  input  [31:0] io_R1, // @[src/main/scala/riscvsingle/ieu/Cmp.scala 22:14]
  input  [31:0] io_R2, // @[src/main/scala/riscvsingle/ieu/Cmp.scala 22:14]
  output        io_Eq, // @[src/main/scala/riscvsingle/ieu/Cmp.scala 22:14]
  output        io_LT, // @[src/main/scala/riscvsingle/ieu/Cmp.scala 22:14]
  output        io_LTU // @[src/main/scala/riscvsingle/ieu/Cmp.scala 22:14]
);
  wire [31:0] af = io_R1 ^ 32'h80000000; // @[src/main/scala/riscvsingle/ieu/Cmp.scala 29:15]
  wire [31:0] bf = io_R2 ^ 32'h80000000; // @[src/main/scala/riscvsingle/ieu/Cmp.scala 30:15]
  assign io_Eq = io_R1 == io_R2; // @[src/main/scala/riscvsingle/ieu/Cmp.scala 35:21]
  assign io_LT = af < bf; // @[src/main/scala/riscvsingle/ieu/Cmp.scala 36:18]
  assign io_LTU = io_R1 < io_R2; // @[src/main/scala/riscvsingle/ieu/Cmp.scala 37:23]
endmodule
module Shifter(
  input  [31:0] io_A, // @[src/main/scala/riscvsingle/ieu/Shifter.scala 23:14]
  input  [4:0]  io_Amt, // @[src/main/scala/riscvsingle/ieu/Shifter.scala 23:14]
  input         io_Right, // @[src/main/scala/riscvsingle/ieu/Shifter.scala 23:14]
  input         io_SubArith, // @[src/main/scala/riscvsingle/ieu/Shifter.scala 23:14]
  output [31:0] io_Y // @[src/main/scala/riscvsingle/ieu/Shifter.scala 23:14]
);
  wire  Sign = io_A[31] & io_SubArith; // @[src/main/scala/riscvsingle/ieu/Shifter.scala 30:31]
  wire [30:0] _Z_T = Sign ? 31'h7fffffff : 31'h0; // @[src/main/scala/riscvsingle/ieu/Shifter.scala 31:30]
  wire [62:0] _Z_T_1 = {_Z_T,io_A}; // @[src/main/scala/riscvsingle/ieu/Shifter.scala 31:25]
  wire [62:0] _Z_T_2 = {io_A,31'h0}; // @[src/main/scala/riscvsingle/ieu/Shifter.scala 32:8]
  wire [62:0] Z = io_Right ? _Z_T_1 : _Z_T_2; // @[src/main/scala/riscvsingle/ieu/Shifter.scala 31:11]
  wire [4:0] _Offset_T = ~io_Amt; // @[src/main/scala/riscvsingle/ieu/Shifter.scala 34:35]
  wire [4:0] Offset = io_Right ? io_Amt : _Offset_T; // @[src/main/scala/riscvsingle/ieu/Shifter.scala 34:16]
  wire [62:0] ZShift = Z >> Offset; // @[src/main/scala/riscvsingle/ieu/Shifter.scala 35:15]
  assign io_Y = ZShift[31:0]; // @[src/main/scala/riscvsingle/ieu/Shifter.scala 36:17]
endmodule
module ALU(
  input  [31:0] io_SrcA, // @[src/main/scala/riscvsingle/ieu/ALU.scala 23:14]
  input  [31:0] io_SrcB, // @[src/main/scala/riscvsingle/ieu/ALU.scala 23:14]
  input  [1:0]  io_ALUControl, // @[src/main/scala/riscvsingle/ieu/ALU.scala 23:14]
  input  [2:0]  io_Funct3, // @[src/main/scala/riscvsingle/ieu/ALU.scala 23:14]
  output [31:0] io_ALUResult, // @[src/main/scala/riscvsingle/ieu/ALU.scala 23:14]
  output [31:0] io_IEUAdr // @[src/main/scala/riscvsingle/ieu/ALU.scala 23:14]
);
  wire [31:0] shifter_io_A; // @[src/main/scala/riscvsingle/ieu/ALU.scala 61:23]
  wire [4:0] shifter_io_Amt; // @[src/main/scala/riscvsingle/ieu/ALU.scala 61:23]
  wire  shifter_io_Right; // @[src/main/scala/riscvsingle/ieu/ALU.scala 61:23]
  wire  shifter_io_SubArith; // @[src/main/scala/riscvsingle/ieu/ALU.scala 61:23]
  wire [31:0] shifter_io_Y; // @[src/main/scala/riscvsingle/ieu/ALU.scala 61:23]
  wire  ALUOp = io_ALUControl[0]; // @[src/main/scala/riscvsingle/ieu/ALU.scala 40:25]
  wire  SubArith = io_ALUControl[1]; // @[src/main/scala/riscvsingle/ieu/ALU.scala 41:28]
  wire [31:0] _CondInvb_T = ~io_SrcB; // @[src/main/scala/riscvsingle/ieu/ALU.scala 42:29]
  wire [31:0] CondInvb = SubArith ? _CondInvb_T : io_SrcB; // @[src/main/scala/riscvsingle/ieu/ALU.scala 42:18]
  wire [32:0] _SumExt_T = io_SrcA + CondInvb; // @[src/main/scala/riscvsingle/ieu/ALU.scala 43:22]
  wire [32:0] _GEN_0 = {{32'd0}, SubArith}; // @[src/main/scala/riscvsingle/ieu/ALU.scala 43:35]
  wire [32:0] SumExt = _SumExt_T + _GEN_0; // @[src/main/scala/riscvsingle/ieu/ALU.scala 43:35]
  wire  Carry = SumExt[32]; // @[src/main/scala/riscvsingle/ieu/ALU.scala 44:18]
  wire [31:0] Sum = SumExt[31:0]; // @[src/main/scala/riscvsingle/ieu/ALU.scala 45:16]
  wire  _Overflow_T_5 = io_SrcA[31] ^ Sum[31]; // @[src/main/scala/riscvsingle/ieu/ALU.scala 51:29]
  wire  Overflow = (io_SrcA[31] ^ io_SrcB[31]) & _Overflow_T_5; // @[src/main/scala/riscvsingle/ieu/ALU.scala 50:65]
  wire  LT = Sum[31] ^ Overflow; // @[src/main/scala/riscvsingle/ieu/ALU.scala 53:13]
  wire  LTU = ~Carry; // @[src/main/scala/riscvsingle/ieu/ALU.scala 54:10]
  wire [31:0] SLT = LT ? 32'h1 : 32'h0; // @[src/main/scala/riscvsingle/ieu/ALU.scala 55:13]
  wire [31:0] SLTU = LTU ? 32'h1 : 32'h0; // @[src/main/scala/riscvsingle/ieu/ALU.scala 56:14]
  wire [31:0] ShiftResult = shifter_io_Y; // @[src/main/scala/riscvsingle/ieu/ALU.scala 68:30]
  wire [2:0] _ALUSelect_T = ALUOp ? 3'h7 : 3'h0; // @[src/main/scala/riscvsingle/ieu/ALU.scala 72:32]
  wire [2:0] ALUSelect = io_Funct3 & _ALUSelect_T; // @[src/main/scala/riscvsingle/ieu/ALU.scala 72:26]
  wire [31:0] _io_ALUResult_T = io_SrcA ^ io_SrcB; // @[src/main/scala/riscvsingle/ieu/ALU.scala 78:21]
  wire [31:0] _io_ALUResult_T_1 = io_SrcA | io_SrcB; // @[src/main/scala/riscvsingle/ieu/ALU.scala 80:21]
  wire [31:0] _io_ALUResult_T_2 = io_SrcA & io_SrcB; // @[src/main/scala/riscvsingle/ieu/ALU.scala 81:21]
  wire [31:0] _io_ALUResult_T_4 = 3'h1 == ALUSelect ? ShiftResult : Sum; // @[src/main/scala/riscvsingle/ieu/ALU.scala 73:57]
  wire [31:0] _io_ALUResult_T_6 = 3'h2 == ALUSelect ? SLT : _io_ALUResult_T_4; // @[src/main/scala/riscvsingle/ieu/ALU.scala 73:57]
  wire [31:0] _io_ALUResult_T_8 = 3'h3 == ALUSelect ? SLTU : _io_ALUResult_T_6; // @[src/main/scala/riscvsingle/ieu/ALU.scala 73:57]
  wire [31:0] _io_ALUResult_T_10 = 3'h4 == ALUSelect ? _io_ALUResult_T : _io_ALUResult_T_8; // @[src/main/scala/riscvsingle/ieu/ALU.scala 73:57]
  wire [31:0] _io_ALUResult_T_12 = 3'h5 == ALUSelect ? ShiftResult : _io_ALUResult_T_10; // @[src/main/scala/riscvsingle/ieu/ALU.scala 73:57]
  wire [31:0] _io_ALUResult_T_14 = 3'h6 == ALUSelect ? _io_ALUResult_T_1 : _io_ALUResult_T_12; // @[src/main/scala/riscvsingle/ieu/ALU.scala 73:57]
  Shifter shifter ( // @[src/main/scala/riscvsingle/ieu/ALU.scala 61:23]
    .io_A(shifter_io_A),
    .io_Amt(shifter_io_Amt),
    .io_Right(shifter_io_Right),
    .io_SubArith(shifter_io_SubArith),
    .io_Y(shifter_io_Y)
  );
  assign io_ALUResult = 3'h7 == ALUSelect ? _io_ALUResult_T_2 : _io_ALUResult_T_14; // @[src/main/scala/riscvsingle/ieu/ALU.scala 73:57]
  assign io_IEUAdr = SumExt[31:0]; // @[src/main/scala/riscvsingle/ieu/ALU.scala 45:16]
  assign shifter_io_A = io_SrcA; // @[src/main/scala/riscvsingle/ieu/ALU.scala 62:16]
  assign shifter_io_Amt = io_SrcB[4:0]; // @[src/main/scala/riscvsingle/ieu/ALU.scala 65:17]
  assign shifter_io_Right = io_Funct3[2]; // @[src/main/scala/riscvsingle/ieu/ALU.scala 66:32]
  assign shifter_io_SubArith = io_ALUControl[1]; // @[src/main/scala/riscvsingle/ieu/ALU.scala 41:28]
endmodule
module Datapath(
  input         clk, // @[src/main/scala/riscvsingle/ieu/Datapath.scala 31:15]
  input         reset, // @[src/main/scala/riscvsingle/ieu/Datapath.scala 32:17]
  input  [2:0]  io_Funct3, // @[src/main/scala/riscvsingle/ieu/Datapath.scala 33:14]
  input         io_ALUResultSrc, // @[src/main/scala/riscvsingle/ieu/Datapath.scala 33:14]
  input         io_ResultSrc, // @[src/main/scala/riscvsingle/ieu/Datapath.scala 33:14]
  input  [1:0]  io_ALUSrc, // @[src/main/scala/riscvsingle/ieu/Datapath.scala 33:14]
  input         io_RegWrite, // @[src/main/scala/riscvsingle/ieu/Datapath.scala 33:14]
  input  [2:0]  io_ImmSrc, // @[src/main/scala/riscvsingle/ieu/Datapath.scala 33:14]
  input  [1:0]  io_ALUControl, // @[src/main/scala/riscvsingle/ieu/Datapath.scala 33:14]
  output        io_Eq, // @[src/main/scala/riscvsingle/ieu/Datapath.scala 33:14]
  output        io_LT, // @[src/main/scala/riscvsingle/ieu/Datapath.scala 33:14]
  output        io_LTU, // @[src/main/scala/riscvsingle/ieu/Datapath.scala 33:14]
  input  [31:0] io_PC, // @[src/main/scala/riscvsingle/ieu/Datapath.scala 33:14]
  input  [31:0] io_PCPlus4, // @[src/main/scala/riscvsingle/ieu/Datapath.scala 33:14]
  input  [31:0] io_Instr, // @[src/main/scala/riscvsingle/ieu/Datapath.scala 33:14]
  output [31:0] io_IEUAdr, // @[src/main/scala/riscvsingle/ieu/Datapath.scala 33:14]
  output [31:0] io_WriteData, // @[src/main/scala/riscvsingle/ieu/Datapath.scala 33:14]
  input  [31:0] io_ReadData // @[src/main/scala/riscvsingle/ieu/Datapath.scala 33:14]
);
  wire  rf_clk; // @[src/main/scala/riscvsingle/ieu/Datapath.scala 44:18]
  wire  rf_reset; // @[src/main/scala/riscvsingle/ieu/Datapath.scala 44:18]
  wire  rf_io_WE3; // @[src/main/scala/riscvsingle/ieu/Datapath.scala 44:18]
  wire [4:0] rf_io_A1; // @[src/main/scala/riscvsingle/ieu/Datapath.scala 44:18]
  wire [4:0] rf_io_A2; // @[src/main/scala/riscvsingle/ieu/Datapath.scala 44:18]
  wire [4:0] rf_io_A3; // @[src/main/scala/riscvsingle/ieu/Datapath.scala 44:18]
  wire [31:0] rf_io_WD3; // @[src/main/scala/riscvsingle/ieu/Datapath.scala 44:18]
  wire [31:0] rf_io_RD1; // @[src/main/scala/riscvsingle/ieu/Datapath.scala 44:18]
  wire [31:0] rf_io_RD2; // @[src/main/scala/riscvsingle/ieu/Datapath.scala 44:18]
  wire [24:0] ext_io_Instr; // @[src/main/scala/riscvsingle/ieu/Datapath.scala 45:19]
  wire [2:0] ext_io_ImmSrc; // @[src/main/scala/riscvsingle/ieu/Datapath.scala 45:19]
  wire [31:0] ext_io_ImmExt; // @[src/main/scala/riscvsingle/ieu/Datapath.scala 45:19]
  wire [31:0] cmp_io_R1; // @[src/main/scala/riscvsingle/ieu/Datapath.scala 46:19]
  wire [31:0] cmp_io_R2; // @[src/main/scala/riscvsingle/ieu/Datapath.scala 46:19]
  wire  cmp_io_Eq; // @[src/main/scala/riscvsingle/ieu/Datapath.scala 46:19]
  wire  cmp_io_LT; // @[src/main/scala/riscvsingle/ieu/Datapath.scala 46:19]
  wire  cmp_io_LTU; // @[src/main/scala/riscvsingle/ieu/Datapath.scala 46:19]
  wire [31:0] alu_io_SrcA; // @[src/main/scala/riscvsingle/ieu/Datapath.scala 47:19]
  wire [31:0] alu_io_SrcB; // @[src/main/scala/riscvsingle/ieu/Datapath.scala 47:19]
  wire [1:0] alu_io_ALUControl; // @[src/main/scala/riscvsingle/ieu/Datapath.scala 47:19]
  wire [2:0] alu_io_Funct3; // @[src/main/scala/riscvsingle/ieu/Datapath.scala 47:19]
  wire [31:0] alu_io_ALUResult; // @[src/main/scala/riscvsingle/ieu/Datapath.scala 47:19]
  wire [31:0] alu_io_IEUAdr; // @[src/main/scala/riscvsingle/ieu/Datapath.scala 47:19]
  wire [31:0] R1 = rf_io_RD1; // @[src/main/scala/riscvsingle/ieu/Datapath.scala 36:16 56:6]
  wire [31:0] ImmExt = ext_io_ImmExt; // @[src/main/scala/riscvsingle/ieu/Datapath.scala 35:20 61:10]
  wire [31:0] R2 = rf_io_RD2; // @[src/main/scala/riscvsingle/ieu/Datapath.scala 37:16 57:6]
  wire [31:0] ALUResult = alu_io_ALUResult; // @[src/main/scala/riscvsingle/ieu/Datapath.scala 40:23 75:13]
  wire [31:0] IEUResult = io_ALUResultSrc ? io_PCPlus4 : ALUResult; // @[src/main/scala/riscvsingle/ieu/Datapath.scala 78:19]
  RegFile rf ( // @[src/main/scala/riscvsingle/ieu/Datapath.scala 44:18]
    .clk(rf_clk),
    .reset(rf_reset),
    .io_WE3(rf_io_WE3),
    .io_A1(rf_io_A1),
    .io_A2(rf_io_A2),
    .io_A3(rf_io_A3),
    .io_WD3(rf_io_WD3),
    .io_RD1(rf_io_RD1),
    .io_RD2(rf_io_RD2)
  );
  Extend ext ( // @[src/main/scala/riscvsingle/ieu/Datapath.scala 45:19]
    .io_Instr(ext_io_Instr),
    .io_ImmSrc(ext_io_ImmSrc),
    .io_ImmExt(ext_io_ImmExt)
  );
  Cmp cmp ( // @[src/main/scala/riscvsingle/ieu/Datapath.scala 46:19]
    .io_R1(cmp_io_R1),
    .io_R2(cmp_io_R2),
    .io_Eq(cmp_io_Eq),
    .io_LT(cmp_io_LT),
    .io_LTU(cmp_io_LTU)
  );
  ALU alu ( // @[src/main/scala/riscvsingle/ieu/Datapath.scala 47:19]
    .io_SrcA(alu_io_SrcA),
    .io_SrcB(alu_io_SrcB),
    .io_ALUControl(alu_io_ALUControl),
    .io_Funct3(alu_io_Funct3),
    .io_ALUResult(alu_io_ALUResult),
    .io_IEUAdr(alu_io_IEUAdr)
  );
  assign io_Eq = cmp_io_Eq; // @[src/main/scala/riscvsingle/ieu/Datapath.scala 65:9]
  assign io_LT = cmp_io_LT; // @[src/main/scala/riscvsingle/ieu/Datapath.scala 66:9]
  assign io_LTU = cmp_io_LTU; // @[src/main/scala/riscvsingle/ieu/Datapath.scala 67:10]
  assign io_IEUAdr = alu_io_IEUAdr; // @[src/main/scala/riscvsingle/ieu/Datapath.scala 76:13]
  assign io_WriteData = rf_io_RD2; // @[src/main/scala/riscvsingle/ieu/Datapath.scala 37:16 57:6]
  assign rf_clk = clk; // @[src/main/scala/riscvsingle/ieu/Datapath.scala 49:10]
  assign rf_reset = reset; // @[src/main/scala/riscvsingle/ieu/Datapath.scala 50:12]
  assign rf_io_WE3 = io_RegWrite; // @[src/main/scala/riscvsingle/ieu/Datapath.scala 51:13]
  assign rf_io_A1 = io_Instr[19:15]; // @[src/main/scala/riscvsingle/ieu/Datapath.scala 52:23]
  assign rf_io_A2 = io_Instr[24:20]; // @[src/main/scala/riscvsingle/ieu/Datapath.scala 53:23]
  assign rf_io_A3 = io_Instr[11:7]; // @[src/main/scala/riscvsingle/ieu/Datapath.scala 54:23]
  assign rf_io_WD3 = io_ResultSrc ? io_ReadData : IEUResult; // @[src/main/scala/riscvsingle/ieu/Datapath.scala 79:16]
  assign ext_io_Instr = io_Instr[31:7]; // @[src/main/scala/riscvsingle/ieu/Datapath.scala 59:27]
  assign ext_io_ImmSrc = io_ImmSrc; // @[src/main/scala/riscvsingle/ieu/Datapath.scala 60:17]
  assign cmp_io_R1 = rf_io_RD1; // @[src/main/scala/riscvsingle/ieu/Datapath.scala 36:16 56:6]
  assign cmp_io_R2 = rf_io_RD2; // @[src/main/scala/riscvsingle/ieu/Datapath.scala 37:16 57:6]
  assign alu_io_SrcA = io_ALUSrc[1] ? io_PC : R1; // @[src/main/scala/riscvsingle/ieu/Datapath.scala 69:14]
  assign alu_io_SrcB = io_ALUSrc[0] ? ImmExt : R2; // @[src/main/scala/riscvsingle/ieu/Datapath.scala 70:14]
  assign alu_io_ALUControl = io_ALUControl; // @[src/main/scala/riscvsingle/ieu/Datapath.scala 73:21]
  assign alu_io_Funct3 = io_Funct3; // @[src/main/scala/riscvsingle/ieu/Datapath.scala 74:17]
endmodule
