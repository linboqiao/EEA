#!/bin/sh 
find "data/03_cloze_dev/" -size 0 -type f -print0 | xargs -0 rm
find "data/03_cloze_test/" -size 0 -type f -print0 | xargs -0 rm
