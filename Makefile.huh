All: data
	mvn clean install

reinstall: squeaky_clean
	mvn clean install

clean:
	mvn clean

squeaky_clean: clean
	mvn clean
	rm -rf ~/.m2
	mvn clean

# data: ebola acequias dadaab csv4j psh swiselib toolsui hotspots refugee kibera
data: ebola dadaab csv4j psh swiselib toolsui hotspots refugee kibera


ebola:
	rm -rf geomason/repository/edu/gmu/eclab/ebolaData/
	mvn install:install-file -Dfile=geomason/repository/jars/ebolaData.jar -DgroupId=edu.gmu.eclab -DartifactId=ebolaData -Dversion=1.0 -Dpackaging=jar -DlocalRepositoryPath:geomason/repository/
	mkdir -p geomason/repository/edu/gmu/eclab/ebolaData/
	cp geomason/repository/jars/ebolaData.jar geomason/repository/edu/gmu/eclab/ebolaData/

# acequias:
# 	rm -rf geomason/repository/edu/gmu/eclab/acequiasData/
# 	mvn install:install-file -Dfile=geomason/repository/jars/acequiasData.jar -DgroupId=edu.gmu.eclab -DartifactId=acequiasData -Dversion=1.0 -Dpackaging=jar -DlocalRepositoryPath:geomason/repository/
# 	mkdir -p geomason/repository/edu/gmu/eclab/acequiasData/
# 	cp geomason/repository/jars/acequiasData.jar geomason/repository/edu/gmu/eclab/acequiasData

dadaab:
	rm -rf geomason/repository/edu/gmu/eclab/dadaabData/
	mvn install:install-file -Dfile=geomason/repository/jars/dadaabData.jar -DgroupId=edu.gmu.eclab -DartifactId=dadaabData -Dversion=1.0 -Dpackaging=jar -DlocalRepositoryPath:geomason/repository/
	mkdir -p geomason/repository/edu/gmu/eclab/dadaabData/
	cp geomason/repository/jars/dadaabData.jar geomason/repository/edu/gmu/eclab/dadaabData

csv4j:
	rm -rf geomason/repository/edu/gmu/eclab/csv4j-0.4.0/
	mvn install:install-file -DlocalRepositoryPath:geomason/repository/ -Dfile=geomason/repository/jars/csv4j-0.4.0.jar -DgroupId=edu.gmu.eclab -DartifactId=csv4j-0.4.0 -Dversion=4.0 -Dpackaging=jar
	mkdir -p geomason/repository/edu/gmu/eclab/csv4j-0.4.0/
	cp geomason/repository/jars/csv4j-0.4.0.jar geomason/repository/edu/gmu/eclab/csv4j-0.4.0/

psh:
	rm -rf geomason/repository/edu/gmu/eclab/psh/
	mvn install:install-file -DlocalRepositoryPath:geomason/repository/ -Dfile=geomason/repository/jars/Psh.jar -DgroupId=edu.gmu.eclab -Dversion=4.0 -DartifactId=psh -Dpackaging=jar
	mkdir -p geomason/repository/edu/gmu/eclab/psh/
	cp geomason/repository/jars/Psh.jar geomason/repository/edu/gmu/eclab/psh/

swiselib:
	rm -rf geomason/repository/edu/gmu/eclab/swiselib/
	mvn install:install-file -DlocalRepositoryPath:geomason/repository/ -Dfile=geomason/repository/jars/swiselib.jar -DgroupId=edu.gmu.eclab -Dversion=4.0 -DartifactId=swiselib -Dpackaging=jar
	mkdir -p geomason/repository/edu/gmu/eclab/swiselib/
	cp geomason/repository/jars/swiselib.jar geomason/repository/edu/gmu/eclab/swiselib/

toolsui:
	rm -rf geomason/repository/edu/gmu/eclab/toolsUI-4.6.13/
	mvn install:install-file -DlocalRepositoryPath:geomason/repository/ -Dfile=geomason/repository/jars/toolsUI-4.6.13.jar -DgroupId=edu.gmu.eclab -Dversion=4.0 -DartifactId=toolsUI-4.6.13 -Dpackaging=jar
	mkdir -p geomason/repository/edu/gmu/eclab/toolsUI-4.6.13/
	cp geomason/repository/jars/toolsUI-4.6.13.jar geomason/repository/edu/gmu/eclab/toolsUI-4.6.13/


hotspots:
	rm -rf geomason/repository/edu/gmu/eclab/hotspotsData/
	mvn install:install-file -DlocalRepositoryPath:geomason/repository/ -Dfile=geomason/repository/jars/hotspotsData.jar -DgroupId=edu.gmu.eclab -Dversion=1.0 -DartifactId=hotspotsData -Dpackaging=jar
	mkdir -p geomason/repository/edu/gmu/eclab/hotspotsData/
	cp geomason/repository/jars/hotspotsData.jar geomason/repository/edu/gmu/eclab/hotspotsData/

refugee:
	rm -rf geomason/repository/edu/gmu/eclab/refugeeData/
	mvn install:install-file -DlocalRepositoryPath:geomason/repository/ -Dfile=geomason/repository/jars/refugeeData.jar -DgroupId=edu.gmu.eclab -Dversion=1.0 -DartifactId=refugeeData -Dpackaging=jar
	mkdir -p geomason/repository/edu/gmu/eclab/refugeeData/
	cp geomason/repository/jars/refugeeData.jar geomason/repository/edu/gmu/eclab/refugeeData/

kibera:
	rm -rf geomason/repository/edu/gmu/eclab/kiberaData/
	mvn install:install-file -DlocalRepositoryPath:geomason/repository/ -Dfile=geomason/repository/jars/kiberaData.jar -DgroupId=edu.gmu.eclab -Dversion=1.0 -DartifactId=kiberaData -Dpackaging=jar
	mkdir -p geomason/repository/edu/gmu/eclab/kiberaData/
	cp geomason/repository/jars/kiberaData.jar geomason/repository/edu/gmu/eclab/kiberaData/
