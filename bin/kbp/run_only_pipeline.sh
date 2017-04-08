#!/usr/bin/env bash
export MAVEN_OPTS="-Xmx14g"
args=$@
mvn exec:java -pl event-coref -Dexec.mainClass="edu.cmu.cs.lti.event_coref.pipeline.RunOnlyPipeline" -Dexec.args="$args"
