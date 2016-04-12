#!/usr/bin/env bash
export MAVEN_OPTS="-Xmx14g"
set -x
mvn exec:java -pl event-coref -Dexec.mainClass="edu.cmu.cs.lti.event_coref.pipeline.PlainTextProcessor" -Dexec.args="$*"
