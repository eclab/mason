
# All the Geometry/GIS java files 
DIRS=sim/field/geo/*.java\
sim/io/geo/*.java\
sim/portrayal/geo/*.java\
sim/util/geo/*.java\
sim/app/geo/campusworld/*.java \
sim/app/geo/nearbyworld/*.java \
sim/app/geo/networkworld/*.java \

all: 
	@ echo This makes the Geometry/GIS code. 
	@ echo Type 'make help' for more options 
	@echo 
	javac $(DIRS) 

# Delete all the classfiles, checkpoints, and other various garbage
clean:
	find . -name "*.class" -exec rm -f {} \; 
	find . -name "jmf.log" -exec rm -f {} \; 
	find . -name ".DS_Store" -exec rm -f {} \; 
	find . -name "*.checkpoint" -exec rm -f {} \;
	find . -name "*.java*~" -exec rm -f {} \; 
	find . -name ".#*" -exec rm -rf {} \;

#Build all the classdocs 
docs: doc

doc: 
	javadoc -classpath ${CLASSPATH}:. -protected -d docs/classdocs sim.util.geo sim.field.geo sim.portrayal.geo sim.io.geo

# Indent to your preferred brace format using emacs.  MASON's default
# format is Whitesmiths at 4 spaces.  Yes, I know.  Idiosyncratic.
# Anyway, beware that this is quite slow.  But it works!
indent: 
	touch ${HOME}/.emacs
	find . -name "*.java" -print -exec emacs --batch --load ~/.emacs --eval='(progn (find-file "{}") (mark-whole-buffer) (setq indent-tabs-mode nil) (untabify (point-min) (point-max)) (indent-region (point-min) (point-max) nil) (save-buffer))' \;

jar:


#Print a help message 
help: 
	@echo MASON Geometry/GIS Module Makefile Options
	@echo 
	@echo "make		Builds the code and examples" 
	@echo "make all		Same thing"
	@echo "make docs	Builds the documentation"
	@echo "make doc 	Same thing"
	@echo "make clean	Cleans various gunk" 
	@echo "make help	This message!" 
	@echo "make indent	Uses emaces to re-indent all the java files"

