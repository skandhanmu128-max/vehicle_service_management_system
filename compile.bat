@echo off
rem Compile the Vehicle Service Management System
rem Requires JDK 8 or newer and the MySQL driver jar in lib\
if not exist out mkdir out
javac -encoding UTF-8 -cp "lib/*" -d out ^
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
  echo Build FAILED. See errors above.
)