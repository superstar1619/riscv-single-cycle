SBT ?= sbt
VERILATOR ?= verilator
WIDTH ?= 32
TARGET_DIR ?= generated/extend
CMP_TARGET_DIR ?= generated/cmp
SHIFTER_TARGET_DIR ?= generated/shifter
SWBYTEMASK_TARGET_DIR ?= generated/swbytemask
SUBWORDWRITE_TARGET_DIR ?= generated/subwordwrite
SUBWORDREAD_TARGET_DIR ?= generated/subwordread
DTIM_TARGET_DIR ?= generated/dtim
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

.PHONY: generate generate-cmp generate-shifter generate-swbytemask \
  generate-subwordwrite generate-subwordread generate-dtim generate-alu \
  generate-regfile generate-controller generate-datapath generate-ieu \
  generate-irom generate-ifu generate-lsu generate-cpu test test-rtl test-wave \
  test-subwordwrite-wave test-subwordread-wave test-dtim-wave test-lsu-wave clean

generate:
	$(SBT) "runMain riscvsingle.GenerateExtend $(WIDTH) $(TARGET_DIR)"

generate-cmp:
	$(SBT) "runMain riscvsingle.GenerateCmp $(WIDTH) $(CMP_TARGET_DIR)"

generate-shifter:
	$(SBT) "runMain riscvsingle.GenerateShifter $(WIDTH) $(SHIFTER_TARGET_DIR)"

generate-swbytemask:
	$(SBT) "runMain riscvsingle.GenerateSwByteMask $(WIDTH) $(SWBYTEMASK_TARGET_DIR)"

generate-subwordwrite:
	$(SBT) "runMain riscvsingle.GenerateSubwordWrite $(WIDTH) $(SUBWORDWRITE_TARGET_DIR)"

generate-subwordread:
	$(SBT) "runMain riscvsingle.GenerateSubwordRead $(WIDTH) $(SUBWORDREAD_TARGET_DIR)"

generate-dtim:
	$(SBT) "runMain riscvsingle.GenerateDTIM $(WIDTH) $(DMEM_DEPTH) $(DTIM_TARGET_DIR)"

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

test-subwordwrite-wave:
	$(SBT) 'testOnly riscvsingle.lsu.SubwordWriteSpec -- -z "generate a waveform"'
	mkdir -p "$(WAVE_DIR)"
	cp test_run_dir/SubwordWrite_should_demonstrate_fixed_store_copies_and_generate_a_waveform_at_32_bits/SubwordWriteHarness.vcd "$(WAVE_DIR)/subwordwrite32.vcd"
	cp test_run_dir/SubwordWrite_should_demonstrate_fixed_store_copies_and_generate_a_waveform_at_64_bits/SubwordWriteHarness.vcd "$(WAVE_DIR)/subwordwrite64.vcd"

test-subwordread-wave:
	$(SBT) 'testOnly riscvsingle.lsu.SubwordReadSpec -- -z "generate a waveform"'
	mkdir -p "$(WAVE_DIR)"
	cp test_run_dir/SubwordRead_should_demonstrate_fixed_loads_and_generate_a_waveform_at_32_bits/SubwordReadHarness.vcd "$(WAVE_DIR)/subwordread32.vcd"
	cp test_run_dir/SubwordRead_should_demonstrate_fixed_loads_and_generate_a_waveform_at_64_bits/SubwordReadHarness.vcd "$(WAVE_DIR)/subwordread64.vcd"

test-dtim-wave:
	$(SBT) 'testOnly riscvsingle.lsu.DTIMSpec -- -z "generate a waveform"'
	mkdir -p "$(WAVE_DIR)"
	cp test_run_dir/DTIM_should_demonstrate_masked_writes_and_generate_a_waveform_at_32_bits/DTIMHarness.vcd "$(WAVE_DIR)/dtim32.vcd"
	cp test_run_dir/DTIM_should_demonstrate_masked_writes_and_generate_a_waveform_at_64_bits/DTIMHarness.vcd "$(WAVE_DIR)/dtim64.vcd"

test-lsu-wave:
	$(SBT) 'testOnly riscvsingle.lsu.LSUSubwordSpec -- -z "generate a waveform"'
	mkdir -p "$(WAVE_DIR)"
	cp test_run_dir/LSU_subword_access_should_demonstrate_fixed_accesses_and_generate_a_waveform/SubwordLSUHarness.vcd "$(WAVE_DIR)/lsu32.vcd"
