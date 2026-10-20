@echo off
set "JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-21.0.12.101-hotspot"
set "PATH=%JAVA_HOME%\bin;C:\Program Files\Git\cmd;C:\Program Files\Docker\Docker\resources\bin;%PATH%"
echo Environment configured:
echo   JAVA_HOME=%JAVA_HOME%
java -version
git --version
