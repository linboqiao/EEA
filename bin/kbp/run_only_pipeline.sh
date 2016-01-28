#!/bin/sh 
export MAVEN_OPTS="-Xmx14g"
mvn exec:java -pl event-coref -Dexec.mainClass="edu.cmu.cs.lti.event_coref.pipeline.RunOnlyPipeline" -Dexec.args=$1" "$2" "$3
