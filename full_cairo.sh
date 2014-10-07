export MAVEN_OPTS="-Xmx7g" 
mvn exec:java -Dexec.mainClass="edu.cmu.cs.lti.cds.runners.FullSystemRunner" -Dexec.args='/home/hector/projects/gigascript_exp/data/giga_ny/00-02/ test_out /home/hector/projects/uima-base-tools/fanse-parser/src/main/resources/'
