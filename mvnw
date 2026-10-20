#!/bin/sh
# ----------------------------------------------------------------------------
# Maven Wrapper startup script for Linux / macOS (POSIX shell).
#
# The Windows counterpart is mvnw.cmd. This script exists so the Maven Wrapper
# also works on the Linux Jenkins agent, where the pipeline runs ./mvnw.
# ----------------------------------------------------------------------------

set -e

# Resolve the directory containing this script, following symlinks.
PRG="$0"
while [ -h "$PRG" ]; do
  ls_out=$(ls -ld "$PRG")
  link=$(expr "$ls_out" : '.*-> \(.*\)$')
  case "$link" in
    /*) PRG="$link" ;;
    *)  PRG="$(dirname "$PRG")/$link" ;;
  esac
done
BASE_DIR="$(cd "$(dirname "$PRG")" && pwd)"

WRAPPER_JAR="$BASE_DIR/.mvn/wrapper/maven-wrapper.jar"
WRAPPER_LAUNCHER=org.apache.maven.wrapper.MavenWrapperMain
WRAPPER_URL="https://repo.maven.apache.org/maven2/org/apache/maven/wrapper/maven-wrapper/3.3.2/maven-wrapper-3.3.2.jar"

# Download the wrapper jar on first use (it is gitignored via the *.jar rule).
# The download goes to a temporary file that is moved into place only after it
# completes, so an interrupted transfer can never leave a truncated jar behind:
# a partial jar would be treated as valid by the check above and would break
# every later build in a persistent CI workspace.
if [ ! -f "$WRAPPER_JAR" ]; then
  echo "Maven Wrapper JAR not found. Downloading from Maven Central..."
  TMP_JAR="$WRAPPER_JAR.download"
  rm -f "$TMP_JAR"
  if command -v wget >/dev/null 2>&1; then
    wget -q -O "$TMP_JAR" "$WRAPPER_URL" || { rm -f "$TMP_JAR"; echo "Error: failed to download the Maven Wrapper JAR." >&2; exit 1; }
  elif command -v curl >/dev/null 2>&1; then
    curl -fsSL -o "$TMP_JAR" "$WRAPPER_URL" || { rm -f "$TMP_JAR"; echo "Error: failed to download the Maven Wrapper JAR." >&2; exit 1; }
  else
    echo "Error: neither wget nor curl is available to download the Maven Wrapper." >&2
    exit 1
  fi
  mv "$TMP_JAR" "$WRAPPER_JAR"
fi

# Locate a Java runtime.
if [ -n "$JAVA_HOME" ] && [ -x "$JAVA_HOME/bin/java" ]; then
  JAVACMD="$JAVA_HOME/bin/java"
elif command -v java >/dev/null 2>&1; then
  JAVACMD="java"
else
  echo "Error: JAVA_HOME is not set and no 'java' command was found in PATH." >&2
  echo "Please set JAVA_HOME to your JDK directory." >&2
  exit 1
fi

exec "$JAVACMD" \
  -classpath "$WRAPPER_JAR" \
  "-Dmaven.multiModuleProjectDirectory=$BASE_DIR" \
  $WRAPPER_LAUNCHER \
  "$@"
