@echo off
rem ============================================================
rem Compile the Vehicle Service Management System
rem Auto-detects Javac from JAVA_HOME, IntelliJ JDK, or system PATH
rem ============================================================
setlocal
set "JAVAC=javac"
if defined JAVA_HOME (
  set "JAVAC=%JAVA_HOME%\bin\javac.exe"
) else if exist "C:\Program Files\JetBrains\IntelliJ IDEA 2026.2.1\jbr\bin\javac.exe" (
  set "JAVAC=C:\Program Files\JetBrains\IntelliJ IDEA 2026.2.1\jbr\bin\javac.exe"
) else (
  for /d %%D in ("C:\Program Files\Java\jdk*") do if exist "%%D\bin\javac.exe" set "JAVAC=%%D\bin\javac.exe"
)

if not exist out mkdir out
"%JAVAC%" -encoding UTF-8 -cp "lib/*" -d out ^
  src\vsms\*.java ^
  src\vsms\db\*.java ^
  src\vsms\dao\*.java ^
  src\vsms\model\*.java ^
  src\vsms\service\*.java ^
  src\vsms\pdf\*.java ^
  src\vsms\ui\*.java ^
  src\vsms\web\*.java
if %errorlevel%==0 (
  echo.
  echo Build OK. Run with run.bat
) else (
  echo.
  echo Build FAILED. See errors above. Check java/javac or JAVA_HOME.
)
endlocal