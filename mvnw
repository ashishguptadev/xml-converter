#!/bin/sh
set -e

BASE_DIR="$(cd "$(dirname "$0")"; pwd)"
WRAPPER_DIR="$BASE_DIR/.mvn/wrapper"
WRAPPER_JAR="$WRAPPER_DIR/maven-wrapper.jar"
WRAPPER_PROPS="$WRAPPER_DIR/maven-wrapper.properties"

if [ ! -f "$WRAPPER_JAR" ]; then
  if [ ! -f "$WRAPPER_PROPS" ]; then
    echo "Missing wrapper properties: $WRAPPER_PROPS" >&2
    exit 1
  fi

  WRAPPER_URL=$(grep '^wrapperUrl=' "$WRAPPER_PROPS" | sed 's/wrapperUrl=//')
  if [ -z "$WRAPPER_URL" ]; then
    echo "wrapperUrl is missing in $WRAPPER_PROPS" >&2
    exit 1
  fi

  mkdir -p "$WRAPPER_DIR"
  if command -v curl >/dev/null 2>&1; then
    curl -fsSL "$WRAPPER_URL" -o "$WRAPPER_JAR"
  elif command -v wget >/dev/null 2>&1; then
    wget -q "$WRAPPER_URL" -O "$WRAPPER_JAR"
  else
    echo "Neither curl nor wget is available to download the Maven wrapper jar." >&2
    exit 1
  fi
fi

if [ -n "$JAVA_HOME" ]; then
  JAVA_EXE="$JAVA_HOME/bin/java"
else
  JAVA_EXE="java"
fi

exec "$JAVA_EXE" -Dmaven.multiModuleProjectDirectory="$BASE_DIR" -jar "$WRAPPER_JAR" "$@"
