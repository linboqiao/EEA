#!/usr/bin/env bash
script_dir="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
cd ${script_dir}/../../../EvmEval
eval_script=scorer_v1.7.py

eval_dir="../data/project_data/cmu-script/mention/kbp/LDC2015E95/eval/full_run/"
gold_predicted=${eval_dir}"gold_coref_all.tbf"
sys_predicted=${eval_dir}"lv1_realis_coref_all.tbf"
gold_base="../data/project_data/cmu-script/mention/LDC/LDC2015R26/data/"
gold_standard=${gold_base}"tbf/EvalEventHopper20150903.tbf"
token_dir=${gold_base}"tkn"
log_file_base="../logs/cmu-script/kbp/eng/eval/test/"


#declare -a arr=("gold_type_realis_coref" "lv1_coref" "lv2_coref" "joint_gold_span" "joint" "coref_gold_span" "gold_type_coref" "lv1_coref_pa")
#declare -a arr=("gold_type_realis_coref" "lv1_coref" "gold_type_coref" "lv1_coref_pa")
#declare -a arr=("lv1_coref" "lv1_coref_pa")

declare -a arr=("paMention_hamming" "paMention_noneHamming" "paMention_recallHamming" \
"beamMention_hamming" "beamMention_recallHamming" "beamMention_noneHamming" \
"delayedBeamMention_hamming" "delayedBeamMention_recallHamming" "delayedBeamMention_noneHamming" \
"vanillaBeamMention" "vanillaMention" \
"gold_type_coref" "gold_type_realis_coref" \
"gold_type_delayed_merge" "gold_type_delayed_unmerge" \
"gold_type_early_merge" "gold_type_early_unmerge" \
"beamMention_recallHamming_delayed_merge" "beamMention_recallHamming_delayed_unmerge" \
"beamMention_recallHamming_early_merge" "beamMention_recallHamming_early_unmerge")

for sys_name in "${arr[@]}"
do
    sys_out=${eval_dir}${sys_name}"_all.tbf"
    set -x
    ${eval_script} -g ${gold_standard} -s ${sys_out} -t ${token_dir} -d ${log_file_base}/${sys_name}/test.cmp -o ${log_file_base}/${sys_name}"_result.scores" -c ${log_file_base}/${sys_name}/"coref_out"
    set +x
done