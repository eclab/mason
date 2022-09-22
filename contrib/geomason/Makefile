### Geomason Makefile
#### By Sean Luke

#### Relevant Stuff:
#### To see all your make options:  type   make help
#### To add flags (like -O) to javac:  change the FLAGS variable below

JAVAC = javac ${JAVACFLAGS}

CLASSROOT = target/classes
JAVACFLAGS = -target 1.8 -source 1.8 ${FLAGS} -nowarn -d ${CLASSROOT}
FLAGS = -g -Xlint:deprecation -cp src/main/java/:${CLASSPATH}

VERSION = 20

# Main java files, not including the 3D stuff
SRCS = \
sim/app/geo/acequias/*.java \
sim/app/geo/campusworld/*.java \
sim/app/geo/cityMigration/*.java \
sim/app/geo/colorworld/*.java \
sim/app/geo/conflictdiamonds/*.java \
sim/app/geo/dadaab/*.java \
sim/app/geo/ebola/*.java \
sim/app/geo/gridlock/*.java \
sim/app/geo/haiti/*.java \
sim/app/geo/hotspots/*/*.java \
sim/app/geo/kibera/*.java \
sim/app/geo/masoncsc/*.java \
sim/app/geo/nearbyworld/*.java \
sim/app/geo/networkworld/*.java \
sim/app/geo/omolandCA/*.java \
sim/app/geo/refugee/*.java \
sim/app/geo/riftland/*.java \
sim/app/geo/schellingpolygon/*.java \
sim/app/geo/schellingspace/*.java \
sim/app/geo/sickStudents/*.java \
sim/app/geo/sillypeds/*.java \
sim/app/geo/sleuth/*.java \
sim/app/geo/touchingworld/*.java \
sim/app/geo/turkana/*.java \
sim/app/geo/walkThisWay/*.java \
sim/app/geo/waterworld/*.java \
sim/field/geo/*.java \
sim/io/geo/*.java \


RSRCROOT=src/main/resources/
SRCROOT=src/main/java/
DIRS=$(addprefix $(SRCROOT)/, $(SRCS))
3DDIRS=$(addprefix $(SRCROOT)/, $(3DSRCS))

# Make the main MASON code, not including 3D code
all:
	@ echo This makes the Geomason code.
	@ echo To learn about other options, type 'make help'
	@ echo
	mkdir -p ${CLASSROOT}
	${JAVAC} ${DIRS}
	tar -C ${RSRCROOT} -c sim ec | tar -C ${CLASSROOT} -x # Hack to combine resources with class files



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


# Build an applet jar file.  Note this collects ALL .class, .png, .jpg, index.html, and simulation.classes
# files.  you'll probably want to strip this down some.
jar: all
	touch /tmp/manifest.add
	rm /tmp/manifest.add
	echo "Main-Class: sim.display.Console" > /tmp/manifest.add
	jar -cvfm target/geomason.${VERSION}.jar /tmp/manifest.add -C ${CLASSROOT} .

# Build a distribution.  Cleans, builds 3d, then builds docs, then
# removes SVN directories
dist: clean indent jar
	@ echo If the version is being updated, change it here:
	@ echo "1. sim.engine.SimState.version()"
	@ echo "2. manual.tex frontmatter (including copyright year)"
	@ echo "3. Makefile VERSION variable"
	- touch TODO
	- rm TODO
	- find . -name ".svn" -exec rm -rf {} \;
	- rm repository
	@ echo "If there were SVN directories, expect this to end in an error."
	@ echo "Don't worry about it, things are still fine."


# Indent to your preferred brace format using emacs.  MASON's default
# format is Whitesmiths at 4 spaces.  Yes, I know.  Idiosyncratic.
# Anyway, beware that this is quite slow.  But it works!
indent: 
	touch ${HOME}/.emacs
	find ${SRCROOT} -name "*.java" -print -exec emacs --batch --load ~/.emacs --eval='(progn (find-file "{}") (mark-whole-buffer) (setq indent-tabs-mode nil) (untabify (point-min) (point-max)) (indent-region (point-min) (point-max) nil) (save-buffer))' \;


# Print a help message
help: 
	@ echo MASON Makefile options
	@ echo 
	@ echo "make          Builds the model core, utilities, and 2D code/apps only"
	@ echo "make all        (Same thing)"
	@ echo "make clean    Cleans out all classfiles, checkpoints, and various gunk"
	@ echo "make dist     Does a make clean, make docs, and make 3d, then deletes SVN dirs"
	@ echo "make jar      Makes 3d, then collects ALL class files into a jar file"
	@ echo "              called geomason.jar.  Heavyweight -- all class files included."

	@ echo "make help     Brings up this message!"
	@ echo "make indent   Uses emacs to re-indent MASON java files as you'd prefer"

