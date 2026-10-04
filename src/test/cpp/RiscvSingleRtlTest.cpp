#include "VRiscvSingle.h"
#include "verilated.h"

#include <cstdint>
#include <iostream>
#include <memory>
#include <stdexcept>
#include <string>
#include <vector>

struct ExpectedCycle {
    uint32_t address;
    bool write = false;
    uint32_t data = 0;
};

static void require(bool condition, const std::string& message) {
    if (!condition) throw std::runtime_error(message);
}

int main(int argc, char** argv) {
    try {
        require(argc == 3, "Usage: rtl-test book|expanded seed");
        const std::string scenario(argv[1]);
        require(scenario == "book" || scenario == "expanded", "Unknown scenario");
        const int seed = std::stoi(argv[2]);
        auto context = std::make_unique<VerilatedContext>();
        context->randSeed(seed);
        context->randReset(2); // Randomize unspecified state; reset establishes PC/x0.
        auto dut = std::make_unique<VRiscvSingle>(context.get());
        dut->clk = 0;
        dut->reset = 1;
        dut->eval(); // Also executes the generated ROM's $readmemh.

        auto tick = [&]() {
            dut->clk = 1;
            dut->eval();
            context->timeInc(1);
            dut->clk = 0;
            dut->eval();
            context->timeInc(1);
        };
        tick();
        dut->reset = 0;
        dut->eval();

        const std::vector<ExpectedCycle> book = {
            {5}, {12}, {3}, {8}, {19}, {11}, {0x48}, {5}, {0x28},
            {0xfffffffe}, {12}, {7}, {96, true, 7}, {96}, {18},
            {0x48}, {25}, {100, true, 25}, {0x50}
        };
        const std::vector<ExpectedCycle> expanded = {
            {31}, {511}, {508, true, 31}, {0}, {508}, {7},
            {252, true, 7}, {508}, {100, true, 31}, {0x12c},
            {104, true, 0x128}, {108, true, 31}, {0x134}
        };
        const auto& expected = scenario == "book" ? book : expanded;
        const uint32_t loopAddress = scenario == "book" ? 0x50 : 0x134;
        unsigned writes = 0;
        for (unsigned cycle = 0; cycle < expected.size(); ++cycle) {
            const auto& e = expected[cycle];
            const std::string where = scenario + " cycle " + std::to_string(cycle);
            require(dut->io_IEUAdr == e.address, where + ": IEUAdr mismatch");
            require(bool(dut->io_MemWrite) == e.write, where + ": MemWrite mismatch");
            if (e.write) {
                require(dut->io_WriteData == e.data, where + ": WriteData mismatch");
                ++writes;
            }
            tick();
        }
        auto checkLoop = [&]() {
            require(dut->io_IEUAdr == loopAddress, "Final loop address mismatch");
            require(!dut->io_MemWrite, "Unexpected store in final loop");
            if (scenario == "book") {
                require(dut->io_WriteData == 25, "Final x2 read value mismatch");
            }
        };
        for (unsigned cycle = 0; cycle < 5; ++cycle) {
            checkLoop();
            tick();
        }

        // A reset pulse without a clock edge must leave the execution state alone.
        dut->reset = 1;
        dut->eval();
        checkLoop();
        dut->reset = 0;
        dut->eval();
        checkLoop();

        // A rising clock with reset asserted restarts at the configured vector.
        dut->reset = 1;
        dut->eval();
        tick();
        require(dut->io_IEUAdr == expected.front().address, "Reset edge did not restart CPU");
        require(!dut->io_MemWrite, "Unexpected store at reset vector");
        tick();
        require(dut->io_IEUAdr == expected.front().address, "CPU advanced while reset held");
        dut->reset = 0;
        dut->eval();
        require(dut->io_IEUAdr == expected.front().address, "Reset release changed state without edge");
        dut->final();
        std::cout << "PASS " << scenario << " seed=" << seed << " cycles="
                  << expected.size() << " stores=" << writes
                  << " loop=5 synchronous-reset=PASS\n";
        return 0;
    } catch (const std::exception& error) {
        std::cerr << "FAIL: " << error.what() << '\n';
        return 1;
    }
}
