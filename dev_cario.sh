#!/bin/sh 
export MAVEN_OPTS="-Xmx10g"
mvn exec:java -Dexec.mainClass="edu.cmu.cs.lti.cds.runners.script.mooney.KarlMooneyPredictor" -Dexec.args='occs_'$1' headcounts_'$1' data/03_cloze_dev 50'
