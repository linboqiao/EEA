export MAVEN_OPTS="-Xmx8g"
mvn exec:java -Dexec.mainClass="edu.cmu.cs.lti.cds.runners.stats.PairPredicateFilter" -Dexec.args='settings.cairo.properties'
