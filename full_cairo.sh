#!/bin/sh
export MAVEN_OPTS="-Xmx7g"
mvn exec:java -Dexec.mainClass="edu.cmu.cs.lti.cds.runners.FullSystemRunner" -Dexec.args='data//'$1' data event_tuples/'$1' data/duplicate.count.tail'
