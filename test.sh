#!/bin/sh 
export MAVEN_OPTS="-Xmx26g"
mvn exec:java -Dexec.mainClass="edu.cmu.cs.lti.cds.runners.script.mooney.KarlMooneyPredictor" -Dexec.args='settings.properties '$1
