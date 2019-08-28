#### 2D Rigid Body Physics Makefile
#### By Sean Luke

#### Relevant Stuff:
#### To see all your make options:  type   make help
#### To add flags (like -O) to javac:  change the FLAGS variable below


JAVAC = javac ${FLAGS}

FLAGS = -target 1.4 -source 1.4 -g -nowarn

VERSION = 4


# Main java files, not including the 3D stuff
DIRS = \
sim/app/beadwire/*.java \
sim/app/collisions/*.java \
sim/app/collisionsPJ/*.java \
sim/app/pendulum/*.java \
sim/app/physicsTutorial1/*.java \
sim/app/physicsTutorial2/*.java \
sim/app/physicsTutorial3/*.java \
sim/app/robots/*.java \
sim/physics2D/*.java \
sim/physics2D/collisionDetection/*.java \
sim/physics2D/constraint/*.java \
sim/physics2D/forceGenerator/*.java \
sim/physics2D/integrator/*.java \
sim/physics2D/physicalObject/*.java \
sim/physics2D/shape/*.java \
sim/physics2D/util/*.java \
sim/robot2D/*.java \
sim/util/matrix/*.java \


# Make the main code
all:
	@ echo This makes the 2D Rigid Body Physics code.
	@ echo To learn about other options, type 'make help'
	@ echo 
	${JAVAC} ${FLAGS} ${DIRS}

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
	javadoc -classpath . -protected -d docs/classdocs sim.physics2D sim.physics2D.collisionDetection sim.physics2D.constraint sim.physics2D.forceGenerator sim.physics2D.integrator sim.physics2D.physicalObject sim.physics2D.shape sim.physics2D.util sim.robot2D sim.util.matrix 

docs: doc

# Build a distribution.  Cleans, builds 3d, then builds docs, then
# removes CVS directories
dist: clean all doc
	find . -name "CVS" -exec rm -rf {} \;
	@ echo "If there were CVS directories, expect this to end in an error."
	@ echo "Don't worry about it, things are still fine."

# Build an applet jar file.  Note this collects ALL .class, .png, .jpg, index.html, and simulation.classes
# files.  you'll probably want to strip this down some.
jar: all
	touch /tmp/manifest.add
	rm /tmp/manifest.add
	echo "Main-Class: sim.display.Console" > /tmp/manifest.add
	jar -cvfm jar/physics2d.${VERSION}.jar /tmp/manifest.add `find . -name "*.class"` `find sim -name "*.jpg"` `find sim -name "*.png"` `find sim -name "*.pbm"` `find sim -name "index.html"`

# Indent to your preferred brace format using emacs.  MASON's default
# format is Whitesmiths at 4 spaces.  Yes, I know.  Idiosyncratic.
# Anyway, beware that this is quite slow.  But it works!
indent: 
	touch ${HOME}/.emacs
	find . -name "*.java" -print -exec emacs --batch --load ~/.emacs --eval='(progn (find-file "{}") (mark-whole-buffer) (setq indent-tabs-mode nil) (untabify (point-min) (point-max)) (indent-region (point-min) (point-max) nil) (save-buffer))' \;


# Print a help message
help: 
	@ echo MASON 2D Rigid Body Physics Makefile options
	@ echo 
	@ echo "make          Builds the code"
	@ echo "make all        (Same thing)"
	@ echo "make docs     Builds the class documentation, found in docs/classsdocs"
	@ echo "make doc        (Same thing)"
	@ echo "make clean    Cleans out all classfiles, checkpoints, and various gunk"
	@ echo "make dist     Does a make clean, make docs, and make all, then deletes CVS dirs"

	@ echo "make help     Brings up this message!"
	@ echo "make indent   Uses emacs to re-indent java files as you'd prefer"
	@ echo
	@ echo MASON uses IBM\'s jikes compiler by default.  You can always use javac instead:
	@ echo just change the JAVAC variable in the Makefile.  If you\'d like to try jikes
	@ echo but don\'t have it, you can download it at   http://www.research.ibm.com/jikes/
	@ echo

