#!/bin/sh 
export MAVEN_OPTS="-Xmx6g"
mvn exec:java -pl sequence-modeling -Dexec.mainClass="edu.cmu.cs.lti.script.runners.learn.test.CompactLogLinearTestRunner" -Dexec.args="$1"
