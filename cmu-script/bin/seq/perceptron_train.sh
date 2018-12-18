export MAVEN_OPTS="-Xmx18g" 
mvn exec:java -pl sequence-modeling -Dexec.mainClass="edu.cmu.cs.lti.script.runners.learn.train.PerceptronTrainingRunner" -Dexec.args=$1
