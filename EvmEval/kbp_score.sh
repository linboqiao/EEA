#!/bin/sh 
scorer=./bin/scorer_v1.8.py

gold_dir=/home/linbo/workspace/Datasets/LDCData/TAC/LDC2016E36_TAC_KBP_English_Event_Nugget_Detection_and_Coreference_Comprehensive_Training_and_Evaluation_Data_2014-2015/conversion

echo "Evaluating system A: cmu-script\n Stored score report at output/score.txt\n"
$scorer -g $gold_dir/gold_nugget -s /home/linbo/workspace/GitHubs/TA/cmu-script/sample-output/eval/full_run/lv1_coref.tbf -t $gold_dir/tkn/ -d ./output/A_out.tmp -o ./output/score_cmu-script.txt
more output/score_cmu-script.txt






