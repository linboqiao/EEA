#!/bin/sh
echo -n  "Number of file parsed: "
cat nohup.*.out | grep nyt_eng | wc -l
