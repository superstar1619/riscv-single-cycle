SBT ?= sbt
WIDTH ?= 32
TARGET_DIR ?= generated/extend
CMP_TARGET_DIR ?= generated/cmp
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

.PHONY: generate generate-cmp generate-alu generate-regfile generate-controller generate-datapath generate-ieu generate-irom generate-ifu generate-lsu generate-cpu test clean

generate:
	$(SBT) "runMain riscvsingle.GenerateExtend $(WIDTH) $(TARGET_DIR)"

generate-cmp:
	$(SBT) "runMain riscvsingle.GenerateCmp $(WIDTH) $(CMP_TARGET_DIR)"

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

clean:
	$(SBT) clean
