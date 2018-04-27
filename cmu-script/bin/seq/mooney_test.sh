#!/bin/sh 
export MAVEN_OPTS="-Xmx15g"
mvn exec:java -Dexec.mainClass="edu.cmu.cs.lti.cds.runners.script.test.KarlMooneyPredictor" -Dexec.args=$1
