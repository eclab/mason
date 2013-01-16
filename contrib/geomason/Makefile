#
# GeoMason make file
#
# Note that you will have to uncomment the GeoTools and/or OGR support
# in the jar file rule if you want support for those importers.
#
# To create jar file of GeoMason component:
# make jar
#
# To make the demos:
# make demos 
#
# See other make options:
# make help

JAVAC = javac 

# GeoMason supports Java 5; note that MASON proper only supports Java 4.
JAVACFLAGS = -target 1.5 -source 1.5 ${FLAGS}
FLAGS = -g -nowarn

VERSION = 1.5

JARFILE = geomason.${VERSION}.jar

SRC_DISTRO  = geomason.src.${VERSION}.tgz
DEMO_DISTRO = geomason.demos.${VERSION}.tgz

all: jar demos


DEMOS = sim/app/geo/campusworld/*.java		\
sim/app/geo/colorworld/*.java			\
sim/app/geo/nearbyworld/*.java			\
sim/app/geo/networkworld/*.java			\
sim/app/geo/touchingworld/*.java		\
sim/app/geo/sleuth/*.java			\
sim/app/geo/schellingspace/*.java		\
sim/app/geo/schellingpolygon/*.java		\
sim/app/geo/gridlock/*.java			\
sim/app/geo/waterworld/*.java			\
sim/app/geo/sillypeds/*.java			\
sim/app/geo/sickStudents/*.java			\
sim/app/geo/turkana/*.java

# Note that we do not restrict demos to Java 1.4 as is this case with MASON
demos : ${JARFILE}
	@echo "Compiling examples..."
	@javac -cp ${CLASSPATH}:${JARFILE} $(DEMOS)


dist : ${SRC_DISTRO} ${DEMO_DISTRO}

${SRC_DISTRO} :
	rm -rf geomason-${VERSION}
	mkdir geomason-${VERSION}
	cp Makefile README LICENSE CHANGES ChangeLog geomason-${VERSION}
	(cd geomason-${VERSION}; ln -is ../sim .)
	tar cvzf $@ geomason-${VERSION}/{README,Makefile,LICENSE,CHANGES,ChangeLog} `find geomason-${VERSION}/sim/field geomason-${VERSION}/sim/io geomason-${VERSION}/sim/portrayal geomason-${VERSION}/sim/util -name "*.java" -or -name "*.jpg" -or -name "*.png" -or -name "index.html" `
	rm -rf geomason-${VERSION}

${DEMO_DISTRO} :
	mkdir geomason-${VERSION}
	(cd geomason-${VERSION}; ln -is ../sim .)
	tar cvzf $@ `find geomason-${VERSION}/sim/app -name "*.java" -or -name "*.jpg" -or -name "*.png" -or -name "index.html" -or -name "README" -or -name "*.shp" -or -name "*.shx" -or -name "*.dbf" -or -name "*.txt" -or -name "*.prj" | grep -v '.svn'`
	rm -rf geomason-${VERSION}


# Delete all the class files, checkpoints, and other various garbage
clean:
	rm -f ${JARFILE}
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
./sim/io/geo/ShapeFileImporter.java			\
./sim/io/geo/ArcInfoASCGridExporter.java		\
./sim/io/geo/ShapeFileExporter.java			\
./sim/portrayal/geo/GeomPortrayal.java			\
./sim/portrayal/geo/GeomVectorFieldPortrayal.java	\
./sim/portrayal/geo/GeomInfo2D.java			\
./sim/util/geo/AttributeValue.java			\
./sim/util/geo/GeometryUtilities.java			\
./sim/util/geo/GeomPlanarGraph.java			\
./sim/util/geo/GeomPlanarGraphDirectedEdge.java		\
./sim/util/geo/GeomPlanarGraphEdge.java			\
./sim/util/geo/MasonGeometry.java			\
./sim/util/geo/PointMoveTo.java				\
./sim/io/geo/ShapeFileExporter.java			


# Optional GDAL based importer
GDAL_FILES = ./sim/io/geo/GDALImporter.java

# Optional GeoTool based importer
GEOTOOL_FILES = sim/io/geo/GeoToolsImporter.java

# Optional GDAL/OGR based importer
OGR_FILES = sim/io/geo/OGRImporter.java	

# Add GDAL_FILES, GEOTOOL_FILES and/or OGR_FILES if you want support for those
${JARFILE} : $(BASE_FILES) # $(GDAL_FILES) $(GEOTOOL_FILES) $(OGR_FILES)
	@echo "Creating ${JARFILE}..."
	@touch /tmp/manifest.add
	@rm /tmp/manifest.add
	@echo "Main-Class: sim.display.Console" > /tmp/manifest.add
	@javac ${JAVACFLAGS} $^
	@jar -cvmf /tmp/manifest.add $@ `find sim/field sim/io sim/portrayal sim/util -name "*.class" -or -name "*.jpg" -or -name "*.png" -or -name "index.html"`
	@rm /tmp/manifest.add


jar : ${JARFILE}

#Print a help message
help:
	@echo MASON Geometry/GIS Module Makefile Options
	@echo
	@echo "make		Builds the code, jar file, and examples"
	@echo "make all	Same thing"
	@echo "make jar	creates ${JARFILE}"
	@echo "make demos	Builds the GeoMason examples"
	@echo "make docs	Builds the documentation"
	@echo "make doc 	Same thing"
	@echo "make dist	Builds distribution tarballs"
	@echo "make clean	Cleans various gunk"
	@echo "make help	This message!"
	@echo "make indent	Uses emaces to re-indent all the java files"

