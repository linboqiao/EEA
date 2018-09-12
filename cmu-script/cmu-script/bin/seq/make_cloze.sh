#!/bin/sh 
# test and dev are all taken from the 03-07 portion
mvn exec:java -pl sequence-modeling -Dexec.mainClass="edu.cmu.cs.lti.script.runners.writers.eval.KmStyleClozeWriter" -Dexec.args=$1' dev' #make dev cloze 
mvn exec:java -pl sequence-modeling -Dexec.mainClass="edu.cmu.cs.lti.script.runners.writers.eval.KmStyleClozeWriter" -Dexec.args=$1' test' #make test cloze
