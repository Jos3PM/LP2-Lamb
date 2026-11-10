@echo off
setlocal
cd /d "%~dp0"
if not defined JAVA_HOME (
  for /d %%J in ("%USERPROFILE%\.jdks\*") do if exist "%%~fJ\bin\javac.exe" set "JAVA_HOME=%%~fJ"
)
set "JAVA_CMD=java"
if defined JAVA_HOME set "JAVA_CMD=%JAVA_HOME%\bin\java.exe"
if exist "target\fia-academico-1.0.0.jar" (
  "%JAVA_CMD%" -jar target\fia-academico-1.0.0.jar --spring.profiles.active=demo
) else if exist "fia-academico-1.0.0.jar" (
  "%JAVA_CMD%" -jar fia-academico-1.0.0.jar --spring.profiles.active=demo
) else (
  call mvn spring-boot:run "-Dspring-boot.run.profiles=demo"
)
pause
