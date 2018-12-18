#!/bin/sh 
export JWNL=/home/hector/projects/uima-base-tools/caevo/src/main/resources/caevo_resources/jwnl_file_properties.xml
export MAVEN_OPTS="-Xmx10g"
mvn exec:java -pl event-coref -Dexec.mainClass="edu.cmu.cs.lti.after.pipeline.AfterLinkPipeline" -Dexec.args=$1
