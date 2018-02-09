COMPILER ?= javac
SRC_PATH ?= src
BIN_PATH ?= bin
JGRAPHT_CLASS_PATH ?= jgrapht/jgrapht-core/src/main/java
EXTERNAL_JARS_PATHS ?= lib/gson-2.8.2.jar
FLAGS ?= -Xlint -cp "$(SRC_PATH):$(JGRAPHT_CLASS_PATH):$(EXTERNAL_JARS_PATHS)" -d $(BIN_PATH)
JAR_NAME ?= RegexStaticAnalysis.jar


SRCS= $(wildcard src/*.java) \
	$(wildcard src/driver/*.java) \
	$(wildcard src/analysis/*.java) \
	$(wildcard src/analysis/driver/*.java) \
	$(wildcard src/regexcompiler/*.java) \
	$(wildcard src/util/*.java) \
	$(wildcard src/preprocessor/*.java) \
	$(wildcard src/nfa/*.java) \
	$(wildcard src/matcher/*.java) \
	$(wildcard src/matcher/driver/*.java)
CLASSES=$(SRCS:src/%.java=bin/%.class)

all: directories $(CLASSES) pumper

directories:
	@mkdir -p bin

bin/%.class: $(SRC_PATH)/%.java
	$(COMPILER) $(FLAGS) $<

pumper: utils/pumper/PumperJava.java
	$(COMPILER) -Xlint -cp 'utils/pumper' -d utils/pumper/ $<

new: clean all

exejar: all
	printf "Main-Class: driver.Main\n" > Manifest.txt
	jar cfm $(JAR_NAME) Manifest.txt ./src* -C ./bin .
	rm -f Manifest.txt
	chmod u+x $(JAR_NAME)

clean:
	rm -f ./$(JAR_NAME)
	find ./ -name "*.class" -type f -delete

# vim: tabstop=4
