include Makefile.git

# 设置包含所有7个jar包的CLASSPATH
export CLASSPATH=/usr/local/lib/antlr-*-complete.jar:/usr/local/lib/*.jar

DOMAINNAME = oj.compilers.cpl.icu
ANTLR = java -jar /usr/local/lib/antlr-*-complete.jar -listener -visitor -long-messages
JAVAC = javac -g
JAVA = java


PFILE = $(shell find . -name "SysYParser.g4")
LFILE = $(shell find . -name "SysYLexer.g4")
JAVAFILE = $(shell find . -name "*.java")
ANTLRPATH = $(shell find /usr/local/lib -name "antlr-*-complete.jar")
JARS = $(shell find /usr/local/lib -name "*.jar" | tr '\n' ':')

compile: antlr
	$(call git_commit,"make")
	mkdir -p classes
	$(JAVAC) -cp "$(JARS):./classes" -d classes src/*.java


run: compile
	$(call git_commit,"run")
	$(JAVA) -cp "./classes:$(JARS)" Main $(INPUT) $(OUTPUT)


antlr: $(LFILE) $(PFILE) 
	$(ANTLR) $(PFILE) $(LFILE)


test: compile
	$(call git_commit, "test")
	nohup java -classpath ./classes:$(JARS) Main ./tests/test.sysy ./tests/test.ll &


clean:
	rm -f src/*.tokens
	rm -f src/*.interp
	rm -f src/SysYLexer.java src/SysYParser.java src/SysYParserBaseListener.java src/SysYParserBaseVisitor.java src/SysYParserListener.java src/SysYParserVisitor.java
	rm -rf classes
	rm -rf out
	rm -rf src/.antlr


submit: clean
	git gc
	bash submit.sh


.PHONY: compile antlr test run clean submit


