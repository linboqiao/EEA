export MAVEN_OPTS="-Xmx20g" 
mvn exec:java -Dexec.mainClass="edu.cmu.cs.lti.cds.runners.script.mooney.MooneyScriptCounterRunner" -Dexec.args='data/02_event_tuples/'$1' data/duplicate.count.tail '$1
