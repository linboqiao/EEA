#!/bin/sh 
export MAVEN_OPTS="-Xmx15g"
mvn exec:java -pl sequence-modeling -Dexec.mainClass="edu.cmu.cs.lti.script.runners.learn.test.MultiArgumentClozeTestRunner" -Dexec.args="$1"
