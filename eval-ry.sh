#!/bin/sh
if test $# -lt 2 
then
    echo "./eval-ry.sh GOLD PRED"
    exit
fi
if [ -f $1 ];
then
    java -cp build/jar/limo.jar:lib/stanford-corenlp-1.3.4b.jar:lib/jdom.jar limo.eval.ScorerRothYih $1 $2
else
    echo "no predicted gold file"
fi
