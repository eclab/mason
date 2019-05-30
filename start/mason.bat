@ECHO OFF

rem Saving the classpath so we can restore it at the end.
SET OLDCLASSPATH=%CLASSPATH%


rem Assuming you kept this batch file in MASON_DIR\start
rem MASON_DIR is whatever .. expands to.
set STARTING_POINT=%CD%
cd ..
SET MASON_DIR=%CD%
cd %STARTING_POINT%
rem Otherwise feel free to set MASON_DIR to the right thing.


rem You need MASON_DIR in classpath to run _java ... Console_
rem so we'll add it. Btw, ignoreme.bat adds its argument to the classpath
call ignoreme.bat %MASON_DIR%


rem If you don't keep your jars in the MASON_DIR, change this:
SET JAR_DIR=%MASON_DIR%


rem adding all jars in the jar directory to the classpath.
(for /F %%f IN ('dir /b /a-d "%JAR_DIR%\*.jar"') do call ignoreme.bat %JAR_DIR%\%%f%) 2>nul



java -Xmx200M sim.display.Console >nul 2>nul 

rem Restoring the classpath.
SET CLASSPATH=%OLDCLASSPATH%
