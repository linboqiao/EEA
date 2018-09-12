#! /bin/sh
# RERANKDATA=ec50-connll-ic-s5
# RERANKDATA=ec50-f050902-lics5
MODELDIR=second-stage/models/ec50spfinal
ESTIMATORNICKNAME=cvlm-l1c10P1
DIR=`dirname $0`
$DIR/first-stage/PARSE/parseIt -l399 -N50 $DIR/first-stage/DATA/EN/ $* | $DIR/second-stage/programs/features/best-parses -l $DIR/$MODELDIR/features.gz $DIR/$MODELDIR/$ESTIMATORNICKNAME-weights.gz
