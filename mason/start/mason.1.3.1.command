#! /bin/tcsh

# Set MASON_HOME to the 'mason' directory 
setenv MASON_HOME       ${0:h}/..

# Add MASON_HOME to the original classpath, even if the original is empty
setenv ORIGINAL_CLASSPATH `printenv CLASSPATH`
setenv CLASSPATH .:${MASON_HOME}:${ORIGINAL_CLASSPATH}

# Tack on jar files in the 'mason' directory.  
# Turn off matching first so foreach doesn't freak on us.
# That will require testing for the existence of ${MASON_HOME}/\*\.jar, which
# will tell us that there were no jar files to be had.
set nonomatch=true
set jars=(${MASON_HOME}/*.jar)
if ("$jars" != "${MASON_HOME}/\*\.jar") then
	foreach i ($jars)
		setenv CLASSPATH ${i}:${CLASSPATH}
	end
endif

# Since we want to use Java 1.3.1, we force it here
alias java /System/Library/Frameworks/JavaVM.framework/Versions/1.3.1/Home/bin/java

# and away we go!
#java -Xdock:name="MASON (Java 1.3.1)" sim.display.Console >& /dev/null&
java sim.display.Console >& /dev/null&
