export MAVEN_OPTS="-Xmx5g" 
mvn exec:java -Dexec.mainClass="edu.cmu.cs.lti.cds.runners.FullSystemRunner" -Dexec.args='data/01_discourse_parsed/'$1' data event_tuples/'$1' data/duplicate.count.tail '$1
