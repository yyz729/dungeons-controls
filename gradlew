#!/bin/sh
# Gradle start-up for POSIX. Run from the dungeons-controls directory.

APP_HOME=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)

if [ -n "$JAVA_HOME" ] && [ -x "$JAVA_HOME/bin/java" ]; then
	JAVACMD="$JAVA_HOME/bin/java"
else
	JAVACMD=java
fi

if ! command -v "$JAVACMD" >/dev/null 2>&1 && [ ! -x "$JAVACMD" ]; then
	echo "Java 21 is required. Set JAVA_HOME or install a JDK." >&2
	exit 1
fi

DEFAULT_JVM_OPTS='"-Xmx64m" "-Xms64m"'

eval set -- $DEFAULT_JVM_OPTS ${JAVA_OPTS:-} ${GRADLE_OPTS:-} \
	-Dorg.gradle.appname=gradlew \
	-classpath "\"$APP_HOME/gradle/wrapper/gradle-wrapper.jar\"" \
	org.gradle.wrapper.GradleWrapperMain \
	'"$@"'

exec "$JAVACMD" "$@"
