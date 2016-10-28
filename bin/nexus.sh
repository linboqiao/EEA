#!/usr/bin/env bash
artifacts=(
'argument'
'chain-script'
'cds'
'event-coref'
'learning'
'core'
'preprocessor'
'seq'
)
subdirs=(
'argument-modeling'
'chain-script'
'cross-document-script'
'event-coref'
'learning'
'script-core'
'script-preprocessor'
'sequence-modeling'
)

version='0.0.2'

mvn deploy:deploy-file -DgroupId=edu.cmu.cs.lti.script -Dversion=${version} -DartifactId=cmu-script -Dfile=pom.xml -Dpackaging=pom -Durl=http://deftpack.bbn.com:8081/nexus/content/repositories/DEFTLibraryDependencies -DrepositoryId=DEFTLibraryDependencies

for index in ${!artifacts[*]}; do
  artifact=${artifacts[${index}]}
  subdir=${subdirs[${index}]}
  echo "Pushing artifact ${artifact} in ${subdir} of version ${version}"
  mvn deploy:deploy-file -DgroupId=edu.cmu.cs.lti.script -Dversion=${version} -DartifactId=${artifact} -Dfile=${subdir}/target/${artifact}-${version}.jar -DpomFile=${subdir}/pom.xml -Dpackaging=jar -DgeneratePom=false -Durl=http://deftpack.bbn.com:8081/nexus/content/repositories/DEFTLibraryDependencies -DrepositoryId=DEFTLibraryDependencies
done


