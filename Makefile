All: data
	mvn clean install

clean:
	mvn clean

squeaky_clean: clean
	mvn clean
	rm -rf ~/.m2
	mvn clean

data: ebola acequias dadaab csv4j psh swiselib toolsui


ebola:
	rm -rf geomason/repository/edu/gmu/eclab/ebolaData/*
	mvn install:install-file -Dfile=geomason/repository/jars/ebolaData.jar -DgroupId=edu.gmu.eclab -DartifactId=ebolaData -Dversion=1.0 -Dpackaging=jar -DlocalRepositoryPath:geomason/repository/
	cp geomason/repository/jars/ebolaData.jar geomason/repository/edu/gmu/eclab/ebolaData/

acequias:
	rm -rf geomason/repository/edu/gmu/eclab/acequiasData/*
	mvn install:install-file -Dfile=geomason/repository/jars/acequiasData.jar -DgroupId=edu.gmu.eclab -DartifactId=acequiasData -Dversion=1.0 -Dpackaging=jar -DlocalRepositoryPath:geomason/repository/
	cp geomason/repository/jars/acequiasData.jar geomason/repository/edu/gmu/eclab/acequiasData

dadaab:
	rm -rf geomason/repository/edu/gmu/eclab/dadaabData/*
	mvn install:install-file -Dfile=geomason/repository/jars/dadaabData.jar -DgroupId=edu.gmu.eclab -DartifactId=dadaabData -Dversion=1.0 -Dpackaging=jar -DlocalRepositoryPath:geomason/repository/
	cp geomason/repository/jars/dadaabData.jar geomason/repository/edu/gmu/eclab/dadaabData

csv4j:
	rm -rf geomason/repository/edu/gmu/eclab/csv4j-0.4.0/*
	mvn install:install-file -DlocalRepositoryPath:geomason/repository/ -Dfile=geomason/repository/jars/csv4j-0.4.0.jar -DgroupId=edu.gmu.eclab -DartifactId=csv4j-0.4.0 -Dversion=4.0 -Dpackaging=jar
	cp geomason/repository/jars/csv4j-0.4.0.jar geomason/repository/edu/gmu/eclab/csv4j-0.4.0/

psh:
	rm -rf geomason/repository/edu/gmu/eclab/psh/*
	mvn install:install-file -DlocalRepositoryPath:geomason/repository/ -Dfile=geomason/repository/jars/Psh.jar -DgroupId=edu.gmu.eclab -Dversion=4.0 -DartifactId=psh -Dpackaging=jar
	cp geomason/repository/jars/Psh.jar geomason/repository/edu/gmu/eclab/psh/

swiselib:
	rm -rf geomason/repository/edu/gmu/eclab/swiselib/*
	mvn install:install-file -DlocalRepositoryPath:geomason/repository/ -Dfile=geomason/repository/jars/swiselib.jar -DgroupId=edu.gmu.eclab -Dversion=4.0 -DartifactId=swiselib -Dpackaging=jar
	cp geomason/repository/jars/swiselib.jar geomason/repository/edu/gmu/eclab/swiselib/

toolsui:
	rm -rf geomason/repository/edu/gmu/eclab/toolsUI-4.6.13/*
	mvn install:install-file -DlocalRepositoryPath:geomason/repository/ -Dfile=geomason/repository/jars/toolsUI-4.6.13.jar -DgroupId=edu.gmu.eclab -Dversion=4.0 -DartifactId=toolsUI-4.6.13 -Dpackaging=jar
	cp geomason/repository/jars/toolsUI-4.6.13.jar geomason/repository/edu/gmu/eclab/toolsUI-4.6.13/
