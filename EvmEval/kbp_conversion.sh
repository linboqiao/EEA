#!/usr/bin/env bash
# This file demonstrate how to run the end-to-end conversion procedures. There are two possible type of annotated data:
# 1. Event Nugget only
# 2. Nugget and hopper
# 3. This file shows both conversion, user might just pick one of them for their specific use case.

data_home=/home/linbo/workspace/Datasets/LDCData/TAC/LDC2016E36_TAC_KBP_English_Event_Nugget_Detection_and_Coreference_Comprehensive_Training_and_Evaluation_Data_2014-2015/

#change these lines to the LDC annotation data folder
ldc_text_dir=$data_home/data/2015/eval/source
ldc_nugget_dir=$data_home/data/2015/eval/nugget
ldc_hopper_dir=$data_home/data/2015/eval/hopper

#change the following lines to your desired output folder
brat_output_dir=$data_home/conversion/ann
token_table_dir=$data_home/conversion/tkn
nugget_output_tbf_filename=gold_nugget
nugget_output_tbf_basename=$data_home/conversion/$nugget_output_tbf_filename
hopper_output_tbf_filename=gold_hopper
hopper_output_tbf_basename=$data_home/conversion/$hopper_output_tbf_filename
tbf_output_dir=$data_home/conversion

# The following are for nugget conversion.
echo "Running XML to Brat Converter for nuggets..."
java -jar bin/rich_ere_to_brat_converter.jar -t "$ldc_text_dir" -te "txt" -a "$ldc_nugget_dir" -ae "event_nuggets.xml" -o "$brat_output_dir" -i event-nugget
echo "Running tokenizer..."
java -jar bin/token-file-maker-1.0.4-jar-with-dependencies.jar -a "$brat_output_dir" -t "$brat_output_dir" -e "txt" -o "$token_table_dir"
echo "Converting to TBF format"
java -jar bin/rich_ere_to_tbf_converter.jar -t "$brat_output_dir" -te "txt" -a "$ldc_nugget_dir" -ae "event_nuggets.xml" -o "$nugget_output_tbf_basename" -i event-nugget
