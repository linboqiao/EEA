#!/bin/sh
set -x
export MAVEN_OPTS="-Xmx10g"
mvn exec:java -pl event-coref -Dexec.mainClass="edu.cmu.cs.lti.event_coref.pipeline.SubmissionPipeline" -Dexec.args="\"""$@""\""
