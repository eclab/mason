#### Social Networks Makefile
#### By Sean Luke

#### Relevant Stuff:
#### To see all your make options:  type   make help
#### To switch from jikes to javac:  change the JAVAC variable below
#### To add flags (like -O) to javac:  change the FLAGS variable below


JAVAC = javac ${JAVACFLAGS}
#JAVAC = jikes ${JIKESFLAGS}

JAVACFLAGS = -target 1.4 -source 1.4 ${FLAGS}
JIKESFLAGS = -target 1.4 +Pno-shadow ${FLAGS}
FLAGS = -g -nowarn

VERSION = 4


# Main java files, not including the 3D stuff
DIRS = \
sim/app/inspectednetwork/*.java \
sim/field/network/stats/*.java \
sim/field/network/stats/actorcentrality/*.java \
sim/portrayal/network/stats/*.java \
sim/util/mantissa/*.java \
sim/util/mantissa/linalg/*.java \

# Make the main code
all:
	@ echo This makes the Social Networks code.
	@ echo To learn about other options, type 'make help'
	@ echo 
	${JAVAC} ${FLAGS} ${DIRS}

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
	javadoc -classpath . -protected -d docs/classdocs sim.field.network.stats sim.field.network.stats.actorcentrality sim.portrayal.network.stats sim.util.mantissa sim.util.mantissa.linalg

docs: doc

# Build an applet jar file.  Note this collects ALL .class, .png, .jpg, index.html, and simulation.classes
# files.  you'll probably want to strip this down some.
jar: all
	touch /tmp/manifest.add
	rm /tmp/manifest.add
	echo "Main-Class: sim.display.Console" > /tmp/manifest.add
	jar -cvfm jar/socialnets.${VERSION}.jar /tmp/manifest.add `find . -name "*.class"` `find sim -name "*.jpg"` `find sim -name "*.png"` `find sim -name "*.pbm"` `find sim -name "index.html"`

# Build a distribution.  Cleans, builds 3d, then builds docs, then
# removes SVN directories
dist: clean all doc jar
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
	@ echo MASON Social Nets Module Makefile options
	@ echo 
	@ echo "make          Builds the code"
	@ echo "make all        (Same thing)"
	@ echo "make docs     Builds the class documentation, found in docs/classsdocs"
	@ echo "make doc        (Same thing)"
	@ echo "make clean    Cleans out all classfiles, checkpoints, and various gunk"
	@ echo "make dist     Does a make clean, make docs, and make all, then deletes SVN dirs"

	@ echo "make help     Brings up this message!"
	@ echo "make indent   Uses emacs to re-indent java files as you'd prefer"
	@ echo
	@ echo MASON uses IBM\'s jikes compiler by default.  You can always use javac instead:
	@ echo just change the JAVAC variable in the Makefile.  If you\'d like to try jikes
	@ echo but don\'t have it, you can download it at   http://www.research.ibm.com/jikes/
	@ echo

