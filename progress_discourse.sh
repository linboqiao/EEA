echo -n  "Number of file parsed: "
cat discourse.nohup.*.out | grep nyt_eng | wc -l
