#!/usr/bin/env bash
script_dir="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
cd ${script_dir}/../../../EvmEval
base_dir=../cmu-script/data/mention/kbp/LDC2015E73
log_dir=../cmu-script/logs/kbp/eval/tree_coref
eval_script=scorer_v1.7.py

for i in {0..4}; do
gold_standard=${base_dir}"/eval/tree_coref/gold_split_$i.tbf"

#declare -a arr=("gold_type_coref" "gold_type_realis_coref" "lv1_coref" "lv1_coref_merged" "lv2_coref" "lv2_coref_merged" "joint" "coref_gold_span" "coref_gold_span_merged" "joint_gold_span")
declare -a arr=("gold_type_realis_coref" "lv1_coref" "lv2_coref" "joint_gold_span" "joint" "coref_gold_span" "gold_type_coref")

for sys_name in "${arr[@]}"
do
    sys_out=${base_dir}"/eval/tree_coref/"${sys_name}"_split_$i.tbf"
    ${eval_script} -g ${gold_standard} -s ${sys_out} -t ${base_dir}/tkn -d ${log_dir}/${sys_name}/cv_$i.cmp \
-o ${log_dir}/${sys_name}/cv_$i.scores -c ${log_dir}/${sys_name}/coref_out_$i
done

done

#gold_type_coref="gold_type_coref"
#gold_type_relias_coref="gold_type_realis_coref"
#lv1_coref="lv1_coref"
#lv2_coref="lv2_coref"
#joint="joint"
#gold_span_coref="coref_gold_span"
#gold_span_joint="joint_gold_span"
##gold_span_joint_trained="joint_traind_gold_span"
##joint_trained="joint_trained"
##joint_trained_no_dd="joint_trained_no_dd"
##joint_trained_no_dd_using_doc="joint_trained_no_dd_using_doc"
##coref_adapted="coref_adapted"
#
#out_gold_type_coref=${base_dir}"/eval/tree_coref/"${gold_type_coref}"_split_$i.tbf"
#out_gold_type_realis_coref=${base_dir}"/eval/tree_coref/"${gold_type_relias_coref}"_split_$i.tbf"
#out_lv1_coref=${base_dir}"/eval/tree_coref/"${lv1_coref}"_split_$i.tbf"
#out_lv2_coref=${base_dir}"/eval/tree_coref/"${lv2_coref}"_split_$i.tbf"
#out_sys_joint=${base_dir}"/eval/tree_coref/"${joint}"_split_$i.tbf"
#out_coref_gold_span=${base_dir}"/eval/tree_coref/"${gold_span_coref}"_split_$i.tbf"
#out_joint_gold_span=${base_dir}"/eval/tree_coref/"${gold_span_joint}"_split_$i.tbf"
#out_joint_train_gold_span=${base_dir}"eval/tree_coref/"${gold_span_joint_trained}"_split_$i.tbf"
#out_joint_trained=${base_dir}"/eval/tree_coref/"${joint_trained}"_split_$i.tbf"
#out_joint_trained_no_dd=${base_dir}"/eval/tree_coref/"${joint_trained_no_dd}"_split_$i.tbf"
#out_joint_trained_no_dd_using_doc=${base_dir}"/eval/tree_coref/"${joint_trained_no_dd_using_doc}"_split_$i.tbf"
#out_coref_adapted=${base_dir}"/eval/tree_coref/"${coref_adapted}"_split_$i.tbf"
#
##${eval_script} -g ${gold_standard} -s ${out_gold_type_coref} -t ${base_dir}/tkn -d ${log_dir}/${gold_type_coref}/cv_$i.cmp \
##-o ${log_dir}/${gold_type_coref}/cv_$i.scores -c ${log_dir}/${gold_type_coref}/coref_out_$i
##
##${eval_script} -g ${gold_standard} -s ${out_gold_type_realis_coref} -t ${base_dir}/tkn -d ${log_dir}/${gold_type_relias_coref}/cv_$i.cmp \
##-o ${log_dir}/${gold_type_relias_coref}/cv_$i.scores -c ${log_dir}/${gold_type_relias_coref}/coref_out_$i
#
#${eval_script} -g ${gold_standard} -s ${out_lv1_coref} -t ${base_dir}/tkn -d ${log_dir}/${lv1_coref}/cv_$i.cmp \
#-o ${log_dir}/${lv1_coref}/cv_$i.scores -c ${log_dir}/${lv1_coref}/coref_out_$i
#
#${eval_script} -g ${gold_standard} -s ${out_lv2_coref} -t ${base_dir}/tkn -d ${log_dir}/${lv2_coref}/cv_$i.cmp \
#-o ${log_dir}/${lv2_coref}/cv_$i.scores -c ${log_dir}/${lv2_coref}/coref_out_$i

#${eval_script} -g ${gold_standard} -s ${out_sys_joint} -t ${base_dir}/tkn -d ${log_dir}/${joint}/cv_$i.cmp \
#-o ${log_dir}/${joint}/cv_$i.scores -c ${log_dir}/${joint}/coref_out_$i
#
#${eval_script} -g ${gold_standard} -s ${out_coref_gold_span} -t ${base_dir}/tkn -d ${log_dir}/${gold_span_coref}/cv_$i.cmp \
#-o ${log_dir}/${gold_span_coref}/cv_$i.scores -c ${log_dir}/${gold_span_coref}/coref__$i
#
#${eval_script} -g ${gold_standard} -s ${out_joint_gold_span} -t ${base_dir}/tkn -d ${log_dir}/${gold_span_joint}/cv_$i.cmp \
#-o ${log_dir}/${gold_span_joint}/cv_$i.scores -c ${log_dir}/${gold_span_joint}/coref_out_$i

#${eval_script} -g ${gold_standard} -s ${out_joint_trained} -t ${base_dir}/tkn -d ${log_dir}/${joint_trained}/cv_$i.cmp \
#-o ${log_dir}/${joint_trained}/cv_$i.scores -c ${log_dir}/${joint_trained}/coref_out_$i
#
#${eval_script} -g ${gold_standard} -s ${out_joint_train_gold_span} -t ${base_dir}/tkn -d ${log_dir}/${gold_span_joint_trained}/cv_$i.cmp \
#-o ${log_dir}/${gold_span_joint_trained}/cv_$i.scores -c ${log_dir}/${gold_span_joint_trained}/coref_out_$i

#${eval_script} -g ${gold_standard} -s ${out_joint_trained_no_dd} -t ${base_dir}/tkn -d ${log_dir}/${joint_trained_no_dd}/cv_$i.cmp \
#-o ${log_dir}/${joint_trained_no_dd}/cv_$i.scores -c ${log_dir}/${joint_trained_no_dd}/coref_out_$i
#
#${eval_script} -g ${gold_standard} -s ${out_joint_trained_no_dd_using_doc} -t ${base_dir}/tkn -d ${log_dir}/${joint_trained_no_dd_using_doc}/cv_$i.cmp \
#-o ${log_dir}/${joint_trained_no_dd_using_doc}/cv_$i.scores -c ${log_dir}/${joint_trained_no_dd_using_doc}/coref_out_$i
#
#${eval_script} -g ${gold_standard} -s ${out_coref_adapted} -t ${base_dir}/tkn -d ${log_dir}/${coref_adapted}/cv_$i.cmp \
#-o ${log_dir}/${coref_adapted}/cv_$i.scores -c ${log_dir}/${coref_adapted}/coref_out_$i


