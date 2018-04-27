export MAVEN_OPTS="-Xmx40g" 
mvn exec:java -Dexec.mainClass="edu.cmu.cs.lti.cds.runners.script.train.MooneyScriptCounterRunner" -Dexec.args='settings.cairo.'$1'.properties '$1
