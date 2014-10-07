export MAVEN_OPTS="-Xmx4g" 
mvn exec:java -Dexec.mainClass="edu.cmu.cs.lti.cds.runners.PreparationPipeline" -Dexec.args='/home/hector/projects/gigascript_exp/data/giga_ny/'$1' agiga/'$1' /home/hector/projects/uima-base-tools/fanse-parser/src/main/resources/'
