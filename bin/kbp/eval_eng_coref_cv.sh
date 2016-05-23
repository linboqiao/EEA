#!/usr/bin/env bash
script_dir="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
cd ${script_dir}/../../../EvmEval
base_dir=../data/project_data/cmu-script/mention/kbp/LDC2015E73
log_dir=../logs/cmu-script/kbp/eng/eval/coref/cv
eval_script=scorer_v1.7.py

for i in {0..4}; do
gold_standard=${base_dir}"/eval/coref_cv/gold_split_$i.tbf"

#declare -a arr=("gold_type_coref" "gold_type_realis_coref" "lv1_coref" "lv1_coref_merged" "lv2_coref" "lv2_coref_merged" "joint" "coref_gold_span" "coref_gold_span_merged" "joint_gold_span")
#declare -a arr=("gold_type_realis_coref" "lv1_coref" "lv2_coref" "joint_gold_span" "joint" "coref_gold_span" "gold_type_coref")
#declare -a arr=("gold_type_realis_coref" "lv1_coref" "gold_type_coref" "joint_span_coref")

#declare -a arr=("paMention_hamming" "paMention_noneHamming" "paMention_recallHamming" \
#"beamMention_hamming" "beamMention_recallHamming" "beamMention_noneHamming" \
#"delayedBeamMention_hamming" "delayedBeamMention_recallHamming" "delayedBeamMention_noneHamming" \
#"vanillaBeamMention" "vanillaMention" \
#"gold_type_coref" "gold_type_realis_coref" \
#"gold_type_delayed_merge" "gold_type_delayed_unmerge" \
#"gold_type_early_merge" "gold_type_early_unmerge" \
#"beamMention_recallHamming_delayed_merge" "beamMention_recallHamming_delayed_unmerge" \
#"beamMention_recallHamming_early_merge" "beamMention_recallHamming_early_unmerge")

#declare -a arr=("gold_type_coref" "gold_type_realis_coref" "gold_type_delayed_merge" "gold_type_delayed_unmerge" \
#"gold_type_early_merge" "gold_type_early_unmerge" \
#"beamMention_recallHamming_delayed_merge" "beamMention_recallHamming_delayed_unmerge" \
#"beamMention_recallHamming_early_merge" "beamMention_recallHamming_early_unmerge")

declare -a arr=("gold_type_realis_vanilla_coref")

for sys_name in "${arr[@]}"
do
    sys_out=${base_dir}"/eval/coref_cv/"${sys_name}"_split_$i.tbf"
    set -x
    ${eval_script} -g ${gold_standard} -s ${sys_out} -t ${base_dir}/tkn -d ${log_dir}/${sys_name}/split_$i.cmp \
-o ${log_dir}/${sys_name}/split_$i.scores -c ${log_dir}/${sys_name}/split_$i.coref_out
    set +x
done

done
