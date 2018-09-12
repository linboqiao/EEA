#! /bin/sh
# insert empty line after parse
options=-K
scriptdir=`dirname $0`
$scriptdir/bllip-parser/parse.sh $1 $options| awk '{print $0; print ""}'
#~/project/limosine/tools/limo/parser/bllip-parser/parse.sh $1 $options| awk '{print $0; print ""}'

