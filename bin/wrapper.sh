#!/bin/bash

if [[ $# != 10 ]]
then
   echo "Usage: $0"
   echo "   <[training|test]> TRAINDIR TESTDIR"
   echo "   SVMLIGHTTKDIR SVMPARAMS OUTPUTDIR FEATURES DIRECTED GRANULARITY CONFIGURATION"
   exit 1
fi

configname=$1; shift
traindir=$1; shift
testdir=$1; shift
svmdir=$1; shift
svmparams=$1; shift 
outdir=$1; shift
feats=$1; shift
directed=$1; shift
granularity=$1; shift
config=$1; shift
conf=$(dirname $0)/../$config
features=$(dirname $0)/../$feats
trm_params=$(dirname $0)/../TRM.params.$directed.$granularity
home=$HOME

# Create output directory and configuration files
mkdir -p $outdir
mkdir -p $traindir/process/plain
mkdir -p $traindir/process/parsed
mkdir -p $testdir/process/plain
mkdir -p $testdir/process/parsed

cat $conf | \
   sed -e "s@__TRAINDIR__@$traindir@" | \
   sed -e "s@__TESTDIR__@$testdir@" | \
   sed -e "s@__SVMLIGHT__@$svmdir@" | \
   sed -e "s@__SVMPARAMS__@$svmparams@" | \
   sed -e "s@__OUTDIR__@$outdir@" | \
   sed -e "s@__FEATURES__@$outdir/features.xml@" | \
   sed -e "s@__DICTIONARY__@$outdir/featuresDictionary.dat@" | \
   sed -e "s@__DIRECTED__@$directed@" | \
   sed -e "s@__GRANULARITY__@$granularity@" | \
   sed -e "s@__HOME__@$home@"  \
   > $outdir/conf.xml
cp $features $outdir/features.xml
cp $trm_params $outdir/TRM.params

BASEDIR=`dirname $0`/..
java -Xms8096M -Xmx8096M -cp ${BASEDIR}/build/jar/limo.jar:${BASEDIR}/lib/jdom.jar:${BASEDIR}/lib/stanford-corenlp-1.3.4b.jar limo.exrel.Exrel --exrelXMLConfigFile $outdir/conf.xml \
   --configurationName $configname


