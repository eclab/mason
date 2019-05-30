@ECHO OFF


rem MASON needs this batch file because we cannot repeatedly append things to a
rem variable in a loop without turning on delayed expansion/evaluation.  Thus 
rem all occurances of CLASSPATH in the batch file are replaced when the script
rem is started.  Using a separate script for each append forces a fresh copy 
rem of CLASSPATH to read every time.

IF NOT "%*"==""  set classpath=%classpath%;%*
rem The reason for the IF statement is that nothing should happen if the user 
rem clicks on this script.
