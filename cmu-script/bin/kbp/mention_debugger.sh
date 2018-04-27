#!/bin/sh 
export MAVEN_OPTS="-Xmx18g"
mvn exec:java -pl event-coref -Dexec.mainClass="edu.cmu.cs.lti.emd.annotators.SingleInstanceDebugger" -Dexec.args=$1" "$2" "$3
