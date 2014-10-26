#!/bin/sh
export MAVEN_OPTS="-Xmx4g"
mvn exec:java -Dexec.mainClass="edu.cmu.cs.lti.cds.runners.DiscourseParserRunner" -Dexec.args='data/00_agiga/'$1' 0 discourse_parsed/'$1
