export MAVEN_OPTS="-Xmx6g"
mvn exec:java -pl sequence-modeling -Dexec.mainClass="edu.cmu.cs.lti.script.runners.stats.EventMentionHeadCounterRunner" -Dexec.args='settings.cairo.properties'
