#include "VRiscvSingle.h"
#include "verilated.h"

#include <array>
#include <cstdint>
#include <fstream>
#include <iostream>
#include <memory>
#include <sstream>
#include <stdexcept>
#include <string>

static void require(bool condition, const std::string& message) {
    if (!condition) throw std::runtime_error(message);
}

int main(int argc, char** argv) {
    try {
        require(argc == 3, "Usage: acceptance-rtl trace-file seed");
        std::ifstream trace(argv[1]);
        require(trace.is_open(), "Cannot read trace file");
        const int seed = std::stoi(argv[2]);
        auto context = std::make_unique<VerilatedContext>();
        context->randSeed(seed);
        context->randReset(2); // 所有未规定初值随机化；程序自行建立所需 RF/RAM 值。
        auto dut = std::make_unique<VRiscvSingle>(context.get());
        dut->clk = 0;
        dut->reset = 1;
        dut->eval();
        auto tick = [&]() {
            dut->clk = 1;
            dut->eval();
            context->timeInc(1);
            dut->clk = 0;
            dut->eval();
            context->timeInc(1);
        };
        require(!dut->io_MemRW && !dut->io_MemWrite && !dut->io_ByteMask,
                "Reset must suppress memory requests");
        tick();
        tick();
        dut->reset = 0;
        dut->eval();
        unsigned cycles = 0, stores = 0;
        std::string line;
        while (std::getline(trace, line)) {
            if (line.empty() || line[0] == '#') continue;
            std::istringstream input(line);
            std::array<uint32_t, 8> row{};
            for (auto& value : row) require(bool(input >> std::hex >> value), "Bad trace row");
            const auto where = std::string(argv[1]) + " cycle=" + std::to_string(cycles);
            if (row[3]) require(dut->io_IEUAdr == row[2], where + ": address mismatch");
            require(dut->io_MemRW == row[4], where + ": request mismatch");
            require(dut->io_Funct3 == row[5], where + ": function mismatch");
            require(dut->io_ByteMask == row[6], where + ": byte mask mismatch");
            require(bool(dut->io_MemWrite) == bool(row[6]), where + ": write enable mismatch");
            if (row[6]) {
                require(dut->io_WriteData == row[7], where + ": store data mismatch");
                ++stores;
            }
            tick();
            ++cycles;
        }
        require(cycles == 110, "Incomplete acceptance trace");
        // 结束后复位保持三拍，访存观察口必须保持关闭。
        dut->reset = 1;
        dut->eval();
        for (unsigned cycle = 0; cycle < 3; ++cycle) {
            require(!dut->io_MemRW && !dut->io_MemWrite && !dut->io_ByteMask,
                    "Memory request during final reset");
            tick();
        }
        dut->final();
        std::cout << "PASS " << argv[1] << " seed=" << seed << " cycles=" << cycles
                  << " stores=" << stores << " RV32-37=PASS subword=PASS\n";
    } catch (const std::exception& error) {
        std::cerr << "FAIL: " << error.what() << '\n';
        return 1;
    }
}
