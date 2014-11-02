#!/bin/sh 

n=$(grep Correct $1 | wc -l)
c=$(grep correct $1 | wc -l)

echo "$n/$c" | bc -l
