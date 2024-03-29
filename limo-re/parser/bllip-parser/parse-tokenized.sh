#! /bin/sh
# RERANKDATA=ec50-connll-ic-s5
# RERANKDATA=ec50-f050902-lics5
DIR=/home/bplank/project/limosine/tools/limo/parser/bllip-parser
MODELDIR=$DIR/second-stage/models/ec50spfinal
ESTIMATORNICKNAME=cvlm-l1c10P1
$DIR/first-stage/PARSE/parseIt -K -l399 -N50 $DIR/first-stage/DATA/EN/ $* | $DIR/second-stage/programs/features/best-parses -l $MODELDIR/features.gz $MODELDIR/$ESTIMATORNICKNAME-weights.gz
