#!/bin/bash
if [ -z "$LIMODIR" ]
  then
    echo "Variable LIMODIR is not defined"
    exit
fi

#check charniak
./parser/parse-charniak.sh parser/test.charniak > /tmp/out
diff /tmp/out parser/test.charniak.parsed
rm /tmp/out

#check for python3
command -v python3 >/dev/null 2>&1 || { echo >&2 "I require python3 but it's not installed.  Aborting."; exit 1; }

#check svm_learn
file=./svm_semantic/svm_learn

if [[ -x "$file" ]]
then
    echo "Done!"
else
    echo "Error - check the README"
fi
