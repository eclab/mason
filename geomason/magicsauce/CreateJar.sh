#/bin/bash
javac `find sim -name *java`
jar cfv $1.jar sim 

