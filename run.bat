@echo off
rem Run the Vehicle Service Management System from the project root.
rem db.properties is read from the current working directory.
java -cp "out;lib/*;." vsms.Main
pause