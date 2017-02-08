#!/bin/sh 
export MAVEN_OPTS="-Xmx10g"
mvn exec:java -pl event-coref -Dexec.mainClass="edu.cmu.cs.lti.after.pipeline.AfterLinkPipeline" -Dexec.args=$1
