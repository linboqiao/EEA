#!/bin/sh
java -cp build/jar/limo.jar:lib/stanford-corenlp-1.3.4b.jar:lib/jdom.jar limo.io.convert.Ace2RothYih -f ACE2005 $1 ignoreRelationsACE2005.txt