#!/bin/sh
export MAVEN_OPTS="-Xmx18g"
mvn exec:java -Dexec.mainClass="edu.cmu.cs.lti.emd.pipeline.Preprocessor"
