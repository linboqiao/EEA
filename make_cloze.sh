#!/bin/sh 
# test and dev are all taken from the 03-07 portion
mvn exec:java -Dexec.mainClass="edu.cmu.cs.lti.cds.runners.writers.eval.KmStyleClozeWriter" -Dexec.args='settings.properties '$1
