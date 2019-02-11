#GeoMason make file
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
#LIBS=`function join() { local IFS=$$1; shift; echo "$$*"; }; join ":" ../../library/* `
#GLIBS=`function join() { local IFS=$$1; shift; echo "$$*"; }; join ":" geolib/* `
MASON=../../mason/jar/mason.19.jar
DISTRIBUTED=../distributed/jar/distributed.1.jar
#CP=${LIBS}:${GLIBS}:${MASON}
CP = -cp "$$CLASSPATH:${MASON}:${DISTRIBUTED}" # Override CP if your classpath doesn't agree.
FLAGS = -g ${CP}
JAVAC = mpijavac ${FLAGS}
CC = ${JAVAC}

VERSION = 1.6.1

JARFILE = geomason.${VERSION}.jar

SRC_DISTRO  = geomason.src.${VERSION}.tgz
DEMO_DISTRO = geomason.demos.${VERSION}.tgz

all: demos jar

# All the main GeoMason source files
BASE_FILES = ./sim/field/geo/*.java		\
			 ./sim/io/geo/*.java		\
			 ./sim/portrayal/geo/*.java			\
			 ./sim/util/geo/*.java			\
			 ./sim/io/geo/*.java			
#---- DEMOS AND THEIR ASSOCIATED FILES -----# In alphabetical order anyway
ACEQUIAS = sim/app/geo/acequias/*.java \
		   sim/app/geo/acequias/objects/*.java 

CAMPUSWORLD = sim/app/geo/campusworld/*.java \
			  sim/app/geo/campusworld/data/*.java
CDI = sim/app/geo/CDI/*.java \
	  sim/app/geo/CDI/src/*.java \
	  sim/app/geo/CDI/src/files/*.java \
	  sim/app/geo/CDI/src/files/data/*.java \
	  sim/app/geo/CDI/src/government/*.java \
	  sim/app/geo/CDI/src/migration/*.java \
	  sim/app/geo/CDI/src/migration/util/*.java \
	  sim/app/geo/CDI/src/migration/parameters/*.java \
	  sim/app/geo/CDI/src/movement/*.java \
	  sim/app/geo/CDI/src/movement/data/*.java \
	  sim/app/geo/CDI/src/movement/parameters/*.java \
	  sim/app/geo/CDI/src/optimization/*.java \
	  sim/app/geo/CDI/src/optimization/desirability/*.java \
	  sim/app/geo/CDI/src/optimization/util/*.java \
	  sim/app/geo/CDI/src/phases/*.java \
	  sim/app/geo/CDI/src/phases/util/*.java \
	  sim/app/geo/CDI/src/environment/*.java 

CITYMIGRATION = sim/app/geo/cityMigration/*.java \
				sim/app/geo/cityMigration/tin/*.java

COLORWORLD = sim/app/geo/colorworld/*.java \
			 sim/app/geo/colorworld/data/*.java

CONFLICTDIAMONDS = sim/app/geo/conflictdiamonds/*.java 

DADAAB = sim/app/geo/dadaab/*.java

DCAMPUSWORLD = sim/app/geo/dcampusworld/*.java	\
			   sim/app/geo/dcampusworld/data/*.java

DSCHELLINGSPACE = sim/app/geo/dschellingspace/*.java \
				 sim/app/geo/dschellingspace/data/*.java

EBOLA = sim/app/geo/ebola/*.java

GRIDLOCK = sim/app/geo/gridlock/*.java \
		   sim/app/geo/gridlock/data/*.java

HAITI = sim/app/geo/haiti/*.java

HOTSPOTS = sim/app/geo/hotspots/objects/*.java \
		   sim/app/geo/hotspots/sim/*.java \
		   sim/app/geo/hotspots/visualization/*.java

KIBERA = sim/app/geo/kibera/*.java

MASONCSC = sim/app/geo/masoncsc/*.java \
		   sim/app/geo/masoncsc/gui/*.java \
		   sim/app/geo/masoncsc/properties/*.java \
		   sim/app/geo/masoncsc/submodel/*.java \
		   sim/app/geo/masoncsc/submodel/flockersAndHeatBugs/*.java \
		   sim/app/geo/masoncsc/submodel/flockersAndHeatBugs/heatBugs/*.java \
		   sim/app/geo/masoncsc/submodel/flockersAndHeatBugs/flockers/*.java \
		   sim/app/geo/masoncsc/datawatcher/*.java \
		   sim/app/geo/masoncsc/util/*.java

NEARBYWORLD = sim/app/geo/nearbyworld/*.java

NETWORKWORLD = sim/app/geo/networkworld/*.java

REFUGEE = sim/app/geo/refugee/*.java 

RIFTLAND = sim/app/geo/riftland/*.java \
		   sim/app/geo/riftland/conflict/*.java \
		   sim/app/geo/riftland/dataCollectors/*.java \
		   sim/app/geo/riftland/gui/*.java \
		   sim/app/geo/riftland/household/*.java \
		   sim/app/geo/riftland/parcel/*.java \
		   sim/app/geo/riftland/util/*.java

SCHELLINGPOLYGON = sim/app/geo/schellingpolygon/*.java \
				   sim/app/geo/schellingpolygon/data/*.java

SCHELLINGSPACE = sim/app/geo/schellingspace/*.java \
				 sim/app/geo/schellingspace/data/*.java

SICKSTUDENTS = sim/app/geo/sickStudents/*.java \
			   sim/app/geo/sickStudents/data/*.java

SILLYPEDS = sim/app/geo/sillypeds/*.java 

SLEUTH = sim/app/geo/sleuth/*.java

TOUCHINGWORLD = sim/app/geo/touchingworld/*.java 

TURKANA = sim/app/geo/turkana/*.java

WATERWORLD = sim/app/geo/waterworld/*.java
#----- END DEMOS ------#
DISTRIBUTEDDEMOS = $(DCAMPUSWORLD) $(DSCHELLINGSPACE)
DEMOS = $(CAMPUSWORLD) $(COLORWORLD) $(EBOLA) $(GRIDLOCK) $(HAITI) $(HOTSPOTS) $(MASONCSC) $(NEARBYWORLD) $(NETWORKWORLD) $(REFUGEE) $(SCHELLINGPOLYGON) $(SCHELLINGSPACE) $(SICKSTUDENTS) $(SILLYPEDS) $(SLEUTH) $(TOUCHINGWORLD) $(TOUCHINGWORLD) $(TURKANA) $(WATERWORLD) $(DISTRIBUTEDDEMOS)
# ^ These demos compile
# These demos don't: ACEQUIAS , CDI , CITYMIGRATION , CONFLICTDIAMONDS , DADAAB , KIBERA , RIFTLAND
#

demos : ${JARFILE}
	@echo "Compiling examples..."
	${CC} ${BASE_FILES} ${DEMOS}


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



# Optional GDAL based importer
GDAL_FILES = ./sim/io/geo/GDALImporter.java

# Optional GeoTool based importer
GEOTOOL_FILES = sim/io/geo/GeoToolsImporter.java

# Optional GDAL/OGR based importer
OGR_FILES = sim/io/geo/OGRImporter.java	

# Add GDAL_FILES, GEOTOOL_FILES and/or OGR_FILES if you want support for those
${JARFILE} : $(BASE_FILES) $(GDAL_FILES) $(GEOTOOL_FILES) $(OGR_FILES)
	@echo "Creating ${JARFILE}..."
	@touch /tmp/manifest.add
	@rm /tmp/manifest.add
	@echo "Main-Class: sim.display.Console" > /tmp/manifest.add
	@${CC} ${FLAGS} $^
	@jar -cvmf /tmp/manifest.add $@ `find sim/app sim/field sim/io sim/portrayal sim/util -name "*.class" -or -name "*.jpg" -or -name "*.png" -or -name "index.html"`
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

