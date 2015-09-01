export MAVEN_OPTS="-Xmx18g" 
mvn exec:java -pl event-mention-detection -Dexec.mainClass="edu.cmu.cs.lti.emd.pipeline.twostep.EventMentionTester" -Dexec.args=$1" "$2" "$3
