SBT ?= sbt
VERILATOR ?= verilator
WIDTH ?= 32
TARGET_DIR ?= generated/extend
CMP_TARGET_DIR ?= generated/cmp
SHIFTER_TARGET_DIR ?= generated/shifter
ALU_TARGET_DIR ?= generated/alu
REGFILE_TARGET_DIR ?= generated/regfile
REGISTER_COUNT ?= 32
CONTROLLER_TARGET_DIR ?= generated/controller
DATAPATH_TARGET_DIR ?= generated/datapath
IEU_TARGET_DIR ?= generated/ieu
IROM_TARGET_DIR ?= generated/irom
IMEM_DEPTH ?= 64
INSTRUCTION_INIT_FILE ?= programs/riscvtest.memfile
IFU_TARGET_DIR ?= generated/ifu
RESET_VECTOR ?= 0
LSU_TARGET_DIR ?= generated/lsu
DMEM_DEPTH ?= 64
CPU_TARGET_DIR ?= generated/riscv-single
WAVE_DIR ?= target/waveforms

.PHONY: generate generate-cmp generate-shifter generate-alu generate-regfile generate-controller generate-datapath generate-ieu generate-irom generate-ifu generate-lsu generate-cpu test test-rtl test-wave clean

generate:
	$(SBT) "runMain riscvsingle.GenerateExtend $(WIDTH) $(TARGET_DIR)"

generate-cmp:
	$(SBT) "runMain riscvsingle.GenerateCmp $(WIDTH) $(CMP_TARGET_DIR)"

generate-shifter:
	$(SBT) "runMain riscvsingle.GenerateShifter $(WIDTH) $(SHIFTER_TARGET_DIR)"

generate-alu:
	$(SBT) "runMain riscvsingle.GenerateALU $(WIDTH) $(ALU_TARGET_DIR)"

generate-regfile:
	$(SBT) "runMain riscvsingle.GenerateRegFile $(WIDTH) $(REGFILE_TARGET_DIR) $(REGISTER_COUNT)"

generate-controller:
	$(SBT) "runMain riscvsingle.GenerateController $(CONTROLLER_TARGET_DIR)"

generate-datapath:
	$(SBT) "runMain riscvsingle.GenerateDatapath $(DATAPATH_TARGET_DIR)"

generate-ieu:
	$(SBT) "runMain riscvsingle.GenerateIEU $(IEU_TARGET_DIR)"

generate-irom:
	$(SBT) "runMain riscvsingle.GenerateIROM $(IMEM_DEPTH) $(IROM_TARGET_DIR) $(INSTRUCTION_INIT_FILE)"

generate-ifu:
	$(SBT) "runMain riscvsingle.GenerateIFU $(IMEM_DEPTH) $(IFU_TARGET_DIR) $(INSTRUCTION_INIT_FILE) $(RESET_VECTOR)"

generate-lsu:
	$(SBT) "runMain riscvsingle.GenerateLSU $(DMEM_DEPTH) $(LSU_TARGET_DIR)"

generate-cpu:
	$(SBT) "runMain riscvsingle.GenerateRiscvSingle $(IMEM_DEPTH) $(DMEM_DEPTH) $(CPU_TARGET_DIR) $(INSTRUCTION_INIT_FILE) $(RESET_VECTOR)"

test:
	$(SBT) test

test-rtl:
	VERILATOR="$(VERILATOR)" ./scripts/test-rtl.sh

test-wave:
	$(SBT) 'testOnly riscvsingle.RiscvSingleSpec -- -z "Code Example 2.16"'
	mkdir -p "$(WAVE_DIR)"
	cp test_run_dir/RiscvSingle_should_execute_Code_Example_216_and_generate_its_waveform/RiscvSingleHarness.vcd "$(WAVE_DIR)/code-example-2.16.vcd"

clean:
	$(SBT) clean
