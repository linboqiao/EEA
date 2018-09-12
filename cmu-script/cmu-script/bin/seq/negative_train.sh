export MAVEN_OPTS="-Xmx12g" 
mvn exec:java -pl sequence-modeling -Dexec.mainClass="edu.cmu.cs.lti.cds.runners.learn.train.StochasticGlobalNegativeTrainer" -Dexec.args=$1
