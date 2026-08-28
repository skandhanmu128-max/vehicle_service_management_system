@echo off
rem ============================================================
rem Run the Vehicle Service Management System from the project
rem root. Uses JAVA_HOME when set, otherwise java must be on PATH.
rem db.properties is read from the current working directory.
rem ============================================================
setlocal
set "JAVA=java"
if defined JAVA_HOME set "JAVA=%JAVA_HOME%\bin\java.exe"

%JAVA% -cp "out;lib/*;." vsms.Main
endlocal
pause