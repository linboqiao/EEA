#!/usr/bin/env bash
script_dir="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
cd ${script_dir}/../../../EvmEval
base_dir=../cmu-script/data/mention/kbp/LDC2015E73
log_dir=../cmu-script/logs/kbp/eval/tree_coref
eval_script=scorer_v1.7.py

for i in {0..4}; do
gold_standard=${base_dir}"/eval/tree_coref/gold_split_$i.tbf"

predicted_gold_aver=${base_dir}"/eval/tree_coref/gold_coref_split_$i.tbf"
predicted_gold_final=${base_dir}"/eval/tree_coref/gold_coref_f_split_$i.tbf"

predicted_sys_aver=${base_dir}"/eval/tree_coref/lv1_coref_split_$i.tbf"
predicted_sys_final=${base_dir}"/eval/tree_coref/lv1_coref_f_split_$i.tbf"

${eval_script} -g ${gold_standard} -s ${predicted_gold_aver} -t ${base_dir}/tkn -d ${log_dir}/gold_aver/cv_$i.cmp \
-o ${log_dir}/gold_aver/cv_$i.scores -c ${log_dir}/gold_aver/coref_out_$i

${eval_script} -g ${gold_standard} -s ${predicted_gold_final} -t ${base_dir}/tkn -d ${log_dir}/gold_no_aver/cv_$i.cmp \
-o ${log_dir}/gold_no_aver/cv_$i.scores -c ${log_dir}/gold_no_aver/coref_out_$i

${eval_script} -g ${gold_standard} -s ${predicted_sys_aver} -t ${base_dir}/tkn -d ${log_dir}/lv1_aver/cv_$i.cmp \
-o ${log_dir}/lv1_aver/cv_$i.scores -c ${log_dir}/lv1_aver/coref_out_$i

${eval_script} -g ${gold_standard} -s ${predicted_sys_final} -t ${base_dir}/tkn -d ${log_dir}/lv1_no_aver/cv_$i.cmp \
-o ${log_dir}/lv1_no_aver/cv_$i.scores -c ${log_dir}/lv1_no_aver/coref_out_$i

done
