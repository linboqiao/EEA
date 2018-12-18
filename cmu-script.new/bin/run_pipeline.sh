#!/usr/bin/env bash
args=${@:3}

export MAVEN_OPTS="-Xmx15g"

if (( $# > 0 )); then
    mvn exec:java -pl $1 -Dexec.mainClass="$2" -Dexec.args="$args"
fi
