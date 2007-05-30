#### MASON Makefile
#### By Sean Luke

#### Relevant Stuff:
#### To see all your make options:  type   make help
#### To switch from jikes to javac:  change the JAVAC variable below
#### To add flags (like -O) to javac:  change the FLAGS variable below

JAVAC = javac ${JAVACFLAGS}
#JAVAC = jikes ${JIKESFLAGS}

JAVACFLAGS = -target 1.3 -source 1.3 ${FLAGS}
JIKESFLAGS = -target 1.3 +Pno-shadow ${FLAGS}
FLAGS = -g -nowarn

DIRS = \
jungnetwork/*.java\

# Make the main MASON code, not including 3D code
all:
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
	rm -rf docs/classdocs/resources docs/classdocs/ec docs/classdocs/sim docs/classdocs/*.html docs/classdocs/*.css docs/classdocs/package*


# Build the class docs.  They're located in docs/classdocs
doc:
	javadoc -classpath . -protected -d docs/classdocs jungnetwork

docs: doc


# Build a distribution.  Cleans, builds 3d, then builds docs, then
# removes CVS directories
dist: clean all indent doc
	touch TODO
	rm TODO
	touch .cvsignore
	rm .cvsignore
	find . -name "CVS" -exec rm -rf {} \;
	@ echo "If there were CVS directories, expect this to end in an error."
	@ echo "Don't worry about it, things are still fine."


# Indent to your preferred brace format using emacs.  MASON's default
# format is Whitesmiths at 4 spaces.  Yes, I know.  Idiosyncratic.
# Anyway, beware that this is quite slow.  But it works!
indent: 
	touch ${HOME}/.emacs
	find . -name "*.java" -print -exec emacs --batch --load ~/.emacs --eval='(progn (find-file "{}") (mark-whole-buffer) (setq indent-tabs-mode nil) (untabify (point-min) (point-max)) (indent-region (point-min) (point-max) nil) (save-buffer))' \;


# Print a help message
help: 
	@ echo MASON Makefile options
	@ echo 
	@ echo "make          Builds the core code"
	@ echo "make all        (Same thing)"
	@ echo "make docs     Builds the class documentation, found in docs/classsdocs"
	@ echo "make doc        (Same thing)"
	@ echo "make clean    Cleans out all classfiles, checkpoints, and various gunk"
	@ echo "make dist     Does a make clean, make docs, and make all, then deletes CVS dirs"

	@ echo "make help     Brings up this message!"
	@ echo "make indent   Uses emacs to re-indent MASON java files as you'd prefer"

