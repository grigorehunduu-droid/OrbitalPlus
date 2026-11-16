@echo off
rem Builds build\libs\OrbitalPlus-<version>.jar using the JDK and Gradle in .tools (no installs needed).
cd /d "%~dp0"
set "JAVA_HOME=%~dp0.tools\jdk-25.0.5+8"
call "%~dp0.tools\gradle-9.9.1\bin\gradle.bat" build --no-daemon -q
if errorlevel 1 (echo Build failed. & pause & exit /b 1)
echo.
echo Built: build\libs\
dir /b build\libs
