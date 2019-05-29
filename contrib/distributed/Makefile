### MASON Makefile
#### By Sean Luke

#### Relevant Stuff:
#### To see all your make options:  type   make help
#### To add flags (like -O) to javac:  change the FLAGS variable below
LIBS=$$CLASSPATH
FLAGS = -target 1.8 -source 1.8 -g -nowarn -cp ${LIBS}
CC = mpijavac ${FLAGS}
VERSION = 1
TARGET = distributed.${VERSION}.jar


# Main java files, not including the 3D stuff

FIELD= sim/field/*.java \
		sim/field/grid/*.java \
		sim/field/storage/*.java \
		sim/field/continuous/*.java
UTIL= sim/util/*.java
APP = sim/app/dflockers/*.java \
	  sim/app/dheatbugs/*.java
ENGINE=sim/engine/*.java

DIRS = ${FIELD} ${UTIL} ${APP} ${ENGINE}

# Make the main MASON code, not including 3D code
all:	jar

compile:
	@ echo This makes the Distributed MASON code.
	@ echo To learn about other options, type 'make help'
	@ echo 
	 ${CC} ${DIRS}

# Delete all jmf gunk, checkpoints, backup emacs gunk classfiles,
# documentation, and odd MacOS X poops
clean:
	find . -name "*.class" -exec rm -f {} \;
	find . -name "jmf.log" -exec rm -f {} \;
	find . -name ".DS_Store" -exec rm -f {} \; 
	find . -name "*.checkpoint" -exec rm -f {} \;
	find . -name "*.java*~" -exec rm -f {} \;
	find . -name ".#*" -exec rm -rf {} \;
	rm -rf jar/*.jar docs/classdocs/resources docs/classdocs/ec docs/classdocs/sim docs/classdocs/*.html docs/classdocs/*.css docs/classdocs/package*


# Build the class docs.  They're located in docs/classdocs
doc:
	javadoc -classpath . -protected -d docs/classdocs sim.display sim.engine sim.util sim.util.gui sim.util.media sim.util.media.chart sim.field sim.field.grid sim.field.continuous sim.field.network sim.portrayal sim.portrayal.grid sim.portrayal.continuous sim.portrayal.network sim.portrayal.simple ec.util sim.portrayal3d sim.portrayal3d.grid sim.portrayal3d.continuous sim.portrayal3d.simple sim.portrayal3d.grid.quad sim.display3d sim.util.distribution

docs: doc

# Build an applet jar file.  Note this collects ALL .class, .png, .jpg, index.html, and simulation.classes
# files.  you'll probably want to strip this down some.
jar: compile
	@touch /tmp/manifest.add
	@rm /tmp/manifest.add
	@echo "Main-Class: sim.display.Console" > /tmp/manifest.add
	jar cvfm jar/${TARGET} /tmp/manifest.add  sim/ docs/ scripts/
	rm /tmp/manifest.add
	echo "check jar/${TARGET}"


# Build a distribution.  Cleans, builds 3d, then builds docs, then
# removes SVN directories
dist: clean indent doc jar
	@ echo If the version is being updated, change it here:
	@ echo "1. sim.engine.SimState.version()"
	@ echo "2. manual.tex frontmatter (including copyright year)"
	@ echo "3. Makefile VERSION variable"
	touch TODO
	rm TODO
	find . -name ".svn" -exec rm -rf {} \;
	@ echo "If there were SVN directories, expect this to end in an error."
	@ echo "Don't worry about it, things are still fine."


# Indent to your preferred brace format using emacs.  MASON's default
# format is Whitesmiths at 4 spaces.  Yes, I know.  Idiosyncratic.
# Anyway, beware that this is quite slow.  But it works!
indent: 
	touch ${HOME}/.emacs
	find . -name "*.java" -print -exec emacs --batch --load ~/.emacs --eval='(progn (find-file "{}") (mark-whole-buffer) (setq indent-tabs-mode nil) (untabify (point-min) (point-max)) (indent-region (point-min) (point-max) nil) (save-buffer))' \;


# Print a help message
help: 
	@ echo Distributed MASON Makefile options
	@ echo 
	@ echo "make          Builds the model core, utilities, apps"
	@ echo "make all        (Same thing)"
	@ echo "make docs     Builds the class documentation, found in docs/classsdocs"
	@ echo "make doc        (Same thing)"
	@ echo "make clean    Cleans out all classfiles, checkpoints, and various gunk"
	@ echo "make jar      Makes 3d, then collects ALL class files into a jar file"
	@ echo "make help     Brings up this message!"
	@ echo "make indent   Uses emacs to re-indent MASON java files as you'd prefer"

