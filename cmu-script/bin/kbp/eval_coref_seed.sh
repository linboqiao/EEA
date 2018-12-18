#!/usr/bin/env bash
script_dir="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
cd ${script_dir}/../../../EvmEval
eval_script=scorer_v1.7.py

eval_dir="../cmu-script/data/mention/kbp/LDC2015E95/eval/full_run"
gold_predicted=${eval_dir}"gold_coref_all.tbf"
sys_predicted=${eval_dir}"lv1_realis_coref_all.tbf"
gold_base="../cmu-script/data/mention/LDC/LDC2015R26/data/"
gold_standard=${gold_base}"tbf/EvalEventHopper20150903.tbf"
token_dir=${gold_base}"tkn"
log_file_base="../cmu-script/logs/kbp/eval/tree_coref/final_seed"


declare -a arr=("gold_type_realis_coref" "lv1_coref" "lv2_coref" "joint_gold_span" "joint" "coref_gold_span" "gold_type_coref")

for sys_name in "${arr[@]}"
do
for i in {1..20};
do
    sys_out=${eval_dir}"_seed_"${i}"/"${sys_name}"_all.tbf"
    ${eval_script} -g ${gold_standard} -s ${sys_out} -t ${token_dir} -d ${log_file_base}/${sys_name}/test.cmp -o ${log_file_base}"/seed_"${i}"/"${sys_name}"_result.scores" -c ${log_file_base}/${sys_name}/"coref_out"
done
done