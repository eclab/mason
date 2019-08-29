#/bin/bash
javac -target 1.8 -source 1.8 `find sim -name *java`
jar cfv $1.jar sim 
