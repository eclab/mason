

FLAGS = -target 1.6 -source 1.6 -g -nowarn -Xlint:deprecation
JAVAC = javac ${FLAGS}
SRCS=src/*/*.java src/*/*/*.java
OBJS=src/*/*.class src/*/*/*.class
LIB=libs
LIBS=${LIB}/commons-math3-3.0.jar:${LIB}/csv4j-0.4.0.jar:${LIB}/iText-2.1.5.jar:${LIB}/jcommon-1.0.17.jar:${LIB}/jfreechart-1.0.14.jar:${LIB}/jmf.jar:${LIB}/mason-02.24.2014.jar:${LIB}/toolsUI-4.3.jar:${LIB}/ecj.jar:${LIB}/PowerLaws.jar:${LIB}/pshecj.jar:${LIB}/jzlib.jar
VERSION=-phase1
TARGET=migration${VERSION}.jar
DIST=dist
DATA=src/files src/migration/index.html



all:
	${JAVAC} -cp ${LIBS} ${SRCS}
	@#rm -rf src
	@#mkdir src
	@#cd src; ln -s ../files files
	@#jar cvfm ${TARGET} manifest.txt ${OBJS} ${DATA}
	@#rm -rf ${DIST}
	@#mkdir ${DIST}
	@#mkdir ${DIST}/data
	@#mkdir ${DIST}/data/tempData
	@#mkdir ${DIST}/data/lib
	@#mkdir ${DIST}/src
	@#mv ${TARGET} ${DIST}/.
	@#cp -r files ${DIST}/src/.
	@#cp -r ../needed\ libraries ${DIST}/lib
	@#cp runMigration.command ${DIST}
	@#cp runMigration.sh ${DIST}
	@#cp optimization/desirability/doexperiments ${DIST}
	@#cp optimization/desirability/es.params ${DIST}
	@#ln ../data/tempData/tas* ${DIST}/data/tempData/.

clean:
	rm -f ${OBJS} *~ */*~ */*/*~ */*/*/*~

