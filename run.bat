@echo off
rem ============================================================
rem Run the Vehicle Service Management System
rem Auto-detects Java from JAVA_HOME, IntelliJ JDK, or system PATH
rem ============================================================
setlocal
set "JAVA=java"
if defined JAVA_HOME (
  set "JAVA=%JAVA_HOME%\bin\java.exe"
) else if exist "C:\Program Files\JetBrains\IntelliJ IDEA 2026.2.1\jbr\bin\java.exe" (
  set "JAVA=C:\Program Files\JetBrains\IntelliJ IDEA 2026.2.1\jbr\bin\java.exe"
) else (
  for /d %%D in ("C:\Program Files\Java\jdk*") do if exist "%%D\bin\java.exe" set "JAVA=%%D\bin\java.exe"
)

"%JAVA%" -cp "out;lib/*;." vsms.Main
endlocal
pause