export MAVEN_OPTS="-Xmx5g" 
mvn exec:java -Dexec.mainClass="edu.cmu.cs.lti.cds.runners.script.train.MooneyUnigramCounterRunner" -Dexec.args=$1
