#!/bin/sh
export MAVEN_OPTS="-Xmx18g"
mvn exec:java -pl event-mention-detection -Dexec.mainClass="edu.cmu.cs.lti.emd.pipeline.twostep.EventMentionDataPreprocessor" -Dexec.args="event-mention-detection/data/Event-mention-detection-2014/LDC2015E03_DEFT_2014_Event_Nugget_Evaluation_Source_Data/data/ event-mention-detection/data/Event-mention-detection-2014/test"
#mvn exec:java -pl event-mention-detection -Dexec.mainClass="edu.cmu.cs.lti.emd.pipeline.twostep.EventMentionDataPreprocessor"
#mvn exec:java -pl event-mention-detection -Dexec.mainClass="edu.cmu.cs.lti.emd.pipeline.ACEDataPreprocessor"
