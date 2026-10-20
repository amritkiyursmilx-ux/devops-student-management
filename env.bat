@echo off
REM ---------------------------------------------------------------------------
REM Load environment variables for the Student Management project.
REM
REM JDK 21 is located in this order:
REM   1. An installed Adoptium/Temurin "jdk-21*" directory.
REM   2. A Windows registry key under HKLM\SOFTWARE\JavaSoft\JDK whose version
REM      segment is 21 (for example ...\JavaSoft\JDK\21).
REM Every candidate is a JDK 21 by construction, so a JDK 8/11/17 install can
REM never be selected and break the build with "invalid target release: 21".
REM env.ps1 applies exactly the same rules.
REM ---------------------------------------------------------------------------

setlocal EnableDelayedExpansion

set "JAVA_HOME="
set "FOUND="
set "CANDIDATE="
set "KEYNAME="

REM 1. Installed Adoptium/Temurin JDK 21.
for /d %%d in ("C:\Program Files\Eclipse Adoptium\jdk-21*") do (
    if not defined FOUND if exist "%%~d\bin\java.exe" (
        set "JAVA_HOME=%%~d"
        set "FOUND=1"
    )
)

REM 2. Registered JDK 21 - the registry key name carries the version.
if not defined FOUND (
    for /f "tokens=*" %%k in ('reg query "HKLM\SOFTWARE\JavaSoft\JDK" 2^>nul ^| findstr /i "HKEY_LOCAL_MACHINE"') do (
        if not defined FOUND (
            for %%t in ("%%k") do set "KEYNAME=%%~nt"
            echo !KEYNAME! | findstr /b /c:"21" >nul && call :use-registry-key "%%k"
        )
    )
)

if not defined JAVA_HOME (
    echo ERROR: Could not locate a JDK 21. Set JAVA_HOME manually.
    endlocal & exit /b 1
)

set "PATH=%JAVA_HOME%\bin;C:\Program Files\Git\cmd;C:\Program Files\Docker\Docker\resources\bin;%PATH%"

echo Environment configured:
echo   JAVA_HOME=%JAVA_HOME%
java -version
git --version
docker --version

endlocal & set "JAVA_HOME=%JAVA_HOME%" & set "PATH=%PATH%"
exit /b 0

REM ---------------------------------------------------------------------------
REM :use-registry-key <full registry key path>
REM Uses the JavaHome value of the given key when it points at a real JDK.
REM ---------------------------------------------------------------------------
:use-registry-key
if defined FOUND exit /b 0
set "CANDIDATE="
for /f "usebackq tokens=2*" %%i in (`reg query "%~1" /v JavaHome 2^>nul ^| findstr /i "JavaHome"`) do (
    set "CANDIDATE=%%j"
)
if not defined CANDIDATE exit /b 0
if not exist "%CANDIDATE%bin\java.exe" exit /b 0
set "JAVA_HOME=%CANDIDATE%"
set "FOUND=1"
exit /b 0
