### DES MASON Makefile
#### By Sean Luke

#### Relevant Stuff:
#### To see all your make options:  type   make help
#### To add flags (like -O) to javac:  change the FLAGS variable below

JAVAC = javac ${JAVACFLAGS}

CLASSROOT = target/classes
JAVACFLAGS = -target 1.8 -source 1.8 ${FLAGS} -nowarn -d ${CLASSROOT}
FLAGS = -g -Xlint:deprecation -cp .:${CLASSPATH}

VERSION = 1

# Main java files, not including the 3D stuff
SRCS = \
sim/app/desexample/*.java \
sim/app/example/mm1/*.java \
sim/app/example/streetfood/*.java \
sim/app/example/ticket/*.java \
sim/des/*.java \
sim/des/portrayal/*.java \


DIRS=$(SRCS)

# Make the main MASON DES code
all:
	@ echo This builds the MASON DES facility
	@ echo To learn about other options, type 'make help'
	@ echo In order to build, first be certain that the MASON jar is in your CLASSPATH
	@ echo to your CLASSPATH
	@ echo
	mkdir -p ${CLASSROOT}
	${JAVAC} ${DIRS}


# Delete all jmf gunk, checkpoints, backup emacs gunk classfiles,
# documentation, and odd MacOS X poops
clean:
	find . -name "*.class" -exec rm -f {} \;
	find . -name "jmf.log" -exec rm -f {} \;
	find . -name ".DS_Store" -exec rm -f {} \; 
	find . -name "*.checkpoint" -exec rm -f {} \;
	find . -name "*.java*~" -exec rm -f {} \;
	find . -name ".#*" -exec rm -rf {} \;
	rm -rf target/*.jar docs/classdocs/resources docs/classdocs/ec docs/classdocs/sim docs/classdocs/*.html docs/classdocs/*.css docs/classdocs/package*
	rm -rf ${CLASSROOT}
	rm -rf target/



# Build the class docs.  They're located in docs/classdocs
# the "-" at the beginning causes Make to ignore errors from javadoc
doc:
	- javadoc -classpath ${CLASSPATH}:${SRCROOT} -protected -d docs/classdocs sim.des sim.des.portrayal

# build a Jar file
jar: all
	jar -cvf target/mason-des.${VERSION}.jar -C ${CLASSROOT} .

docs: doc

# Indent to your preferred brace format using emacs.  MASON's default
# format is Whitesmiths at 4 spaces.  Yes, I know.  Idiosyncratic.
# Anyway, beware that this is quite slow.  But it works!
indent: 
	touch ${HOME}/.emacs
	find ${SRCROOT} -name "*.java" -print -exec emacs --batch --load ~/.emacs --eval='(progn (find-file "{}") (mark-whole-buffer) (setq indent-tabs-mode nil) (untabify (point-min) (point-max)) (indent-region (point-min) (point-max) nil) (save-buffer))' \;

# Print a help message
help:
	@ echo MASON DES Makefile options
	@ echo
	@ echo "make          Builds MASON DES"
	@ echo "make all      (Same thing)"
	@ echo "make clean    Cleans out all classfiles, checkpoints, and various gunk"
	@ echo "make docs     Builds the class documentation, found in docs/classsdocs"
	@ echo "make doc      (Same thing)"
	@ echo "make jar      Builds, then collects ALL class files into a jar file"
	@ echo "              called" target/mason-des.${VERSION}.jar
	@ echo "make help     Brings up this message!"
	@ echo "make indent   Uses emacs to re-indent MASON java files as you'd prefer"
	@ echo


