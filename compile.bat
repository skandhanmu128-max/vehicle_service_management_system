@echo off
rem ============================================================
rem Compile the Vehicle Service Management System
rem Requires JDK 8+ (JAVA_HOME is used when set, otherwise javac
rem must be on the PATH) and the MySQL driver jar in lib\
rem ============================================================
setlocal
set "JAVAC=javac"
if defined JAVA_HOME set "JAVAC=%JAVA_HOME%\bin\javac.exe"

if not exist out mkdir out
%JAVAC% -encoding UTF-8 -cp "lib/*" -d out ^
  src\vsms\*.java ^
  src\vsms\db\*.java ^
  src\vsms\dao\*.java ^
  src\vsms\model\*.java ^
  src\vsms\service\*.java ^
  src\vsms\pdf\*.java ^
  src\vsms\ui\*.java
if %errorlevel%==0 (
  echo.
  echo Build OK. Run with run.bat
) else (
  echo.
  echo Build FAILED. See errors above. Check java/javac or JAVA_HOME.
)
endlocal