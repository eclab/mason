This is a directory for GIS helper scripts in debugging/etc.

Use rewrite.py for broken ASCII files for clean up. It seems to work 99% correctly Precondition: mkdir clean/ in the same directory as the running location Usage: python2 rewrite.py at-least-one-ascii-file+ Postcondition: Dumps clean files into the clean directory

Whenever it's unsure about whether a particular token is correct or incorrect, it will prompt you for correct clean up/replacement.

Use CreateJar.sh when you have a sim/ directory to create/recompile a data jar. Precondition: your data jar's root directory is called sim and CreateJar.sh is in the same directory as it. Usage: ./CreateJar.sh name-of-jar Postcondition: Will compile all java files in sim/ and then package everything into a jar called name-of-jar.jar

