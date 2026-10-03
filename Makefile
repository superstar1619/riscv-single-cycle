SBT ?= sbt
WIDTH ?= 32
TARGET_DIR ?= generated/extend
CMP_TARGET_DIR ?= generated/cmp
ALU_TARGET_DIR ?= generated/alu

.PHONY: generate generate-cmp generate-alu test clean

generate:
	$(SBT) "runMain riscvsingle.GenerateExtend $(WIDTH) $(TARGET_DIR)"

generate-cmp:
	$(SBT) "runMain riscvsingle.GenerateCmp $(WIDTH) $(CMP_TARGET_DIR)"

generate-alu:
	$(SBT) "runMain riscvsingle.GenerateALU $(WIDTH) $(ALU_TARGET_DIR)"

test:
	$(SBT) test

clean:
	$(SBT) clean
