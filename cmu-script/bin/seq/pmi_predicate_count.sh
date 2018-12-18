export MAVEN_OPTS="-Xmx15g" 
mvn exec:java -Dexec.mainClass="edu.cmu.cs.lti.cds.runners.script.FastEventMentionHeadCounterRunner" -Dexec.args='settings.cairo.properties '$1
