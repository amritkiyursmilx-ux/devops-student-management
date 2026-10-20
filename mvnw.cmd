@REM ----------------------------------------------------------------------------
@REM Maven Wrapper startup batch script for Windows
@REM ----------------------------------------------------------------------------
@IF "%__MVNW_ARG0_NAME__%"=="" (SET "BASE_DIR=%~dp0") ELSE (SET "BASE_DIR=%__MVNW_ARG0_NAME__%")
@IF "%BASE_DIR:~-1%"=="\" SET "BASE_DIR=%BASE_DIR:~0,-1%"
@SET "MAVEN_PROJECTBASEDIR=%BASE_DIR%"
@SET "WRAPPER_JAR=%MAVEN_PROJECTBASEDIR%\.mvn\wrapper\maven-wrapper.jar"
@SET WRAPPER_LAUNCHER=org.apache.maven.wrapper.MavenWrapperMain

@IF NOT EXIST "%WRAPPER_JAR%" (
    @ECHO Maven Wrapper JAR not found. Downloading...
    @POWERSHELL -Command "[Net.ServicePointManager]::SecurityProtocol=[Net.SecurityProtocolType]::Tls12; (New-Object Net.WebClient).DownloadFile('https://repo.maven.apache.org/maven2/org/apache/maven/wrapper/maven-wrapper/3.3.2/maven-wrapper-3.3.2.jar', '%WRAPPER_JAR%')"
)

@IF NOT "%JAVA_HOME%"=="" (
    @SET "JAVACMD=%JAVA_HOME%\bin\java.exe"
) ELSE (
    @FOR %%i IN (java.exe) DO @SET "JAVACMD=%%~$PATH:i"
)

@IF "%JAVACMD%"=="" (
    @IF EXIST "C:\Program Files\Eclipse Adoptium\jdk-21.0.12.101-hotspot\bin\java.exe" (
        @SET "JAVACMD=C:\Program Files\Eclipse Adoptium\jdk-21.0.12.101-hotspot\bin\java.exe"
    )
)

@IF "%JAVACMD%"=="" (
    @ECHO Error: JAVA_HOME is not set and no 'java' command was found in your PATH.
    @ECHO Please set JAVA_HOME to your JDK directory.
    @EXIT /B 1
)

@"%JAVACMD%" -classpath "%WRAPPER_JAR%" "-Dmaven.multiModuleProjectDirectory=%MAVEN_PROJECTBASEDIR%" %WRAPPER_LAUNCHER% %*