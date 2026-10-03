SBT ?= sbt
WIDTH ?= 32
TARGET_DIR ?= generated/extend
CMP_TARGET_DIR ?= generated/cmp
ALU_TARGET_DIR ?= generated/alu
REGFILE_TARGET_DIR ?= generated/regfile
REGISTER_COUNT ?= 32
CONTROLLER_TARGET_DIR ?= generated/controller

.PHONY: generate generate-cmp generate-alu generate-regfile generate-controller test clean

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

test:
	$(SBT) test

clean:
	$(SBT) clean
