#
# Note that you will have to uncomment the GeoTools and/or OGR support
# in the jar file rule if you want support for those importers.
#

JAVAC = javac ${JAVACFLAGS}
JAVACFLAGS = -target 1.4 -source 1.4 ${FLAGS}
FLAGS = -g -nowarn

all: jar demos


DEMOS = sim/app/geo/campusworld/*.java		\
sim/app/geo/colorworld/*.java			\
sim/app/geo/nearbyworld/*.java			\
sim/app/geo/networkworld/*.java			\
sim/app/geo/touchingworld/*.java

demos : geomason.jar
	@echo "Compiling examples..."
	@$(JAVAC) -cp ${CLASSPATH}:geomason.jar $(FLAGS) $(DEMOS)


# Delete all the class files, checkpoints, and other various garbage
clean:
	rm -f geomason.jar
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


# All the main GeoMason source files
BASE_FILES = ./sim/field/geo/GeomField.java		\
./sim/field/geo/GeomGridField.java			\
./sim/field/geo/GeomVectorField.java			\
./sim/io/geo/ArcInfoASCGridImporter.java		\
./sim/io/geo/GeomExporter.java				\
./sim/io/geo/GeomImporter.java				\
./sim/io/geo/ShapeFileExporter.java			\
./sim/io/geo/ShapeFileImporter.java			\
./sim/portrayal/geo/GeomPortrayal.java			\
./sim/portrayal/geo/GeomVectorFieldPortrayal.java	\
./sim/portrayal/geo/GeomInfo2D.java			\
./sim/util/geo/AttributeField.java			\
./sim/util/geo/GeometryProperties.java			\
./sim/util/geo/GeometryUtilities.java			\
./sim/util/geo/GeomPlanarGraph.java			\
./sim/util/geo/GeomPlanarGraphDirectedEdge.java		\
./sim/util/geo/GeomPlanarGraphEdge.java			\
./sim/util/geo/MasonGeometry.java			\
./sim/util/geo/PointMoveTo.java

# Optional GDAL based importer
GDAL_FILES = ./sim/io/geo/GDALImporter.java

# Optional GeoTool based importer
GEOTOOL_FILES = sim/io/geo/GeoToolsImporter.java

# Optional GDAL/OGR based importer
OGR_FILES = sim/io/geo/OGRImporter.java	

# Add GDAL_FILES, GEOTOOL_FILES and/or OGR_FILES if you want support for those
geomason.jar : $(BASE_FILES) # $(GDAL_FILES) $(GEOTOOL_FILES) $(OGR_FILES)
	@echo "Creating geomason.jar..."
	@javac $(FLAGS) $^
	@jar -cvf $@ `find . -name "*.class"` `find sim -name "*.jpg"` `find sim -name "*.png"` `find sim -name "index.html"`


jar : geomason.jar

#Print a help message
help:
	@echo MASON Geometry/GIS Module Makefile Options
	@echo
	@echo "make		Builds the code, jar file, and examples"
	@echo "make all		Same thing"
	@echo "make jar	    creates geomason.jar"
	@echo "make demos	Builds the GeoMason examples"
	@echo "make docs	Builds the documentation"
	@echo "make doc 	Same thing"
	@echo "make clean	Cleans various gunk"
	@echo "make help	This message!"
	@echo "make indent	Uses emaces to re-indent all the java files"

