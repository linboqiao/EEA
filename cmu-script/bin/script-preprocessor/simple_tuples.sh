#!/usr/bin/env bash
mvn exec:java -pl script-preprocessor -Dexec.mainClass="edu.cmu.cs.lti.script.annotators.SimpleTupleContextPrinter" \
-Dexec.args=$1" "$2" "$3
