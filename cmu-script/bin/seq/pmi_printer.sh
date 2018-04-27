export MAVEN_OPTS="-Xmx4g"
mvn exec:java -Dexec.mainClass="edu.cmu.cs.lti.cds.runners.writers.PredicatePmiCalculatorRunner" -Dexec.args='settings.cairo.properties'
