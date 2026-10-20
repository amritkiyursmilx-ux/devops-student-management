@echo off
REM ---------------------------------------------------------------------------
REM Load environment variables for the Student Management project.
REM JDK 21 is discovered from the Windows registry instead of being hardcoded
REM to one patched version, so this keeps working after a JDK update.
REM ---------------------------------------------------------------------------

set "JAVA_HOME="
for /f "usebackq tokens=2*" %%i in (`reg query "HKLM\SOFTWARE\JavaSoft\JDK" /s /v JavaHome 2^>nul ^| findstr /i "JavaHome"`) do (
    set "CANDIDATE=%%j"
)
if defined CANDIDATE set "JAVA_HOME=%CANDIDATE%"

if not defined JAVA_HOME (
    if exist "C:\Program Files\Eclipse Adoptium" (
        for /f "delims=" %%d in ('dir /b /ad /o-n "C:\Program Files\Eclipse Adoptium\jdk-21*" 2^>nul') do (
            if not defined JAVA_HOME set "JAVA_HOME=C:\Program Files\Eclipse Adoptium\%%d"
        )
    )
)

if not defined JAVA_HOME (
    echo ERROR: Could not locate a JDK. Set JAVA_HOME manually.
    exit /b 1
)

set "PATH=%JAVA_HOME%\bin;C:\Program Files\Git\cmd;C:\Program Files\Docker\Docker\resources\bin;%PATH%"

echo Environment configured:
echo   JAVA_HOME=%JAVA_HOME%
java -version
git --version
docker --version
