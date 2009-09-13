#### MASON Makefile
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


# Main java files, not including the 3D stuff
DIRS = \
sim/app/heatbugs/*.java\
sim/app/hexabugs/*.java \
sim/app/antsforage/*.java \
sim/app/virus/*.java \
sim/app/cto/*.java \
sim/app/woims/*.java \
sim/app/mav/*.java \
sim/app/mousetraps/*.java \
sim/app/networktest/*.java \
sim/app/keepaway/*.java \
sim/app/lsystem/*.java \
sim/app/lightcycles/*.java \
sim/app/swarmgame/*.java \
sim/app/flockers/*.java \
sim/app/tutorial1and2/*.java \
sim/app/tutorial3/*.java \
sim/app/tutorial4/*.java \
sim/app/tutorial5/*.java \
sim/app/schelling/*.java \
sim/app/pso/*.java \
sim/display/*.java \
sim/engine/*.java \
sim/util/*.java \
sim/util/media/*.java \
sim/util/media/chart/*.java \
sim/util/gui/*.java \
sim/field/*.java \
sim/field/grid/*.java \
sim/field/continuous/*.java \
sim/field/network/*.java \
sim/portrayal/*.java \
sim/portrayal/grid/*.java \
sim/portrayal/continuous/*.java \
sim/portrayal/network/*.java \
sim/portrayal/simple/*.java \
sim/portrayal/inspector/*.java \
ec/util/*.java \


# The additional 3D java files
3DDIRS = \
sim/app/heatbugs3d/*.java \
sim/app/woims3d/*.java \
sim/app/mousetraps3d/*.java \
sim/app/crowd3d/*.java \
sim/app/balls3d/*.java \
sim/app/particles3d/*.java \
sim/app/pso3d/*.java \
sim/app/tutorial6/*.java \
sim/app/tutorial7/*.java \
sim/portrayal3d/*.java \
sim/portrayal3d/simple/*.java \
sim/portrayal3d/grid/*.java \
sim/portrayal3d/grid/quad/*.java \
sim/portrayal3d/continuous/*.java \
sim/portrayal3d/network/*.java \
sim/portrayal3d/inspector/*.java \
sim/display3d/*.java \


# Make the main MASON code, not including 3D code
all:
	@ echo This makes the 2D MASON code.
	@ echo To learn about other options, type 'make help'
	@ echo 
	${JAVAC} ${DIRS}

# Make the main MASON code AND the 3D code
3d:
	${JAVAC} ${DIRS} ${3DDIRS}


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
	javadoc -classpath . -protected -d docs/classdocs sim.display sim.engine sim.util sim.util.gui sim.util.media sim.util.media.chart sim.field sim.field.grid sim.field.continuous sim.field.network sim.portrayal sim.portrayal.grid sim.portrayal.continuous sim.portrayal.network sim.portrayal.simple ec.util sim.portrayal3d sim.portrayal3d.grid sim.portrayal3d.continuous sim.portrayal3d.simple sim.portrayal3d.grid.quad sim.display3d

docs: doc

# Build an applet jar file.  Note this collects ALL .class, .png, .jpg, index.html, and simulation.classes
# files.  you'll probably want to strip this down some.
jar: 3d
	touch /tmp/manifest.add
	rm /tmp/manifest.add
	echo "Main-Class: sim.display.Console" > /tmp/manifest.add
	jar -cvfm mason.jar /tmp/manifest.add `find . -name "*.class"` `find sim -name "*.jpg"` `find sim -name "*.png"` `find sim -name "index.html"` sim/display/simulation.classes sim/portrayal/inspector/propertyinspector.classes

# Build a distribution.  Cleans, builds 3d, then builds docs, then
# removes CVS directories
dist: clean 3d indent doc
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
	@ echo "make          Builds the model core, utilities, and 2D code/apps only"
	@ echo "make all        (Same thing)"
	@ echo "make 3d       Builds the model core, utilities, and both 2D and 3D code/apps"
	@ echo "make docs     Builds the class documentation, found in docs/classsdocs"
	@ echo "make doc        (Same thing)"
	@ echo "make clean    Cleans out all classfiles, checkpoints, and various gunk"
	@ echo "make dist     Does a make clean, make docs, and make 3d, then deletes CVS dirs"
	@ echo "make jar      Makes 3d, then collects ALL class files into a jar file"
	@ echo "              called mason.jar.  Heavyweight -- all class files included."

	@ echo "make help     Brings up this message!"
	@ echo "make indent   Uses emacs to re-indent MASON java files as you'd prefer"

