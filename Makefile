SBT ?= sbt
WIDTH ?= 32
TARGET_DIR ?= generated/extend

.PHONY: generate test clean

generate:
	$(SBT) "runMain riscvsingle.GenerateExtend $(WIDTH) $(TARGET_DIR)"

test:
	$(SBT) test

clean:
	$(SBT) clean
