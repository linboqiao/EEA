#!/usr/bin/env bash
script_dir="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
cd ${script_dir}/../../../EvmEval
predicted="../cmu-script/data/mention/kbp/LDC2015E95/eval/tree_coref/coref_final15.tbf"
gold_base="../cmu-script/data/mention/LDC/LDC2015R26/data/"
gold_standard=${gold_base}"tbf/EvalEventHopper20150903.tbf"
token_dir=${gold_base}"tkn"
log_file_base=../cmu-script/logs/kbp/eval/tree_coref/final
scorer_v1.6.py -g ${gold_standard} -s ${predicted} -t ${token_dir}\
 -d ${log_file_base}/test.cmp -o ${log_file_base}/test.scores -c coref_out
