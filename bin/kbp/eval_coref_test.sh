#!/usr/bin/env bash
script_dir="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
cd ${script_dir}/../../../EvmEval
eval_dir="../cmu-script/data/mention/kbp/LDC2015E95/eval/full_run/"
gold_predicted=${eval_dir}"gold_coref_all.tbf"
sys_predicted=${eval_dir}"lv1_realis_coref_all.tbf"
gold_base="../cmu-script/data/mention/LDC/LDC2015R26/data/"
gold_standard=${gold_base}"tbf/EvalEventHopper20150903.tbf"
token_dir=${gold_base}"tkn"
log_file_base="../cmu-script/logs/kbp/eval/tree_coref/final"
scorer_v1.6.py -g ${gold_standard} -s ${gold_predicted} -t ${token_dir}\
 -d ${log_file_base}/test.cmp -o ${log_file_base}/test_gold_mention.scores -c ${eval_dir}"coref_out"
scorer_v1.6.py -g ${gold_standard} -s ${sys_predicted} -t ${token_dir}\
 -d ${log_file_base}/test.cmp -o ${log_file_base}/test_sys_mention.scores -c ${eval_dir}"sys_coref_out"