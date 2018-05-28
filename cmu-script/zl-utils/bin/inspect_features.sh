#!/bin/sh 
export MAVEN_OPTS="-Xmx20g"

if [ "$#" -ne 2 ]; then
    echo "Usage : inspect_feature.sh [model directory] [output directory]"
    exit 1
fi

mvn exec:java -Dexec.mainClass="edu.cmu.cs.lti.learning.debug.HashedFeatureInspector" -Dexec.args="$1 $2"
