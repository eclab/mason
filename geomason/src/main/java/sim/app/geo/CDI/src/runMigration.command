#!/bin/sh

# Run with the GUI in a sub-region (the Mandera region)

cd `dirname $0`

java -Xms1g -Xmx8g -jar migration-phase1.jar

