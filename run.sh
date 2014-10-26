#!/bin/sh
export MAVEN_OPTS="-Xmx4g"
mvn exec:java -Dexec.mainClass="edu.cmu.cs.lti.cds.runners.PreparationPipeline" -Dexec.args="/Users/zhengzhongliu/Downloads/agiga_sample/ agiga /Users/zhengzhongliu/Documents/projects/uimafied-tools/fanse-parser/src/main/resources/"
#mvn exec:java -Dexec.mainClass="edu.cmu.cs.lti.cds.runners.StreamingEntityClusteringRunner" 
