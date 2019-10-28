export BERT_MODEL_DIR=/home/linbo/workspace/Datasets/models/BERT/uncased_L-12_H-768_A-12
#export BERT_MODEL_DIR=/home/linbo/workspace/Datasets/models/BERT/chinese_L-12_H-768_A-12
cd /tmp
bert-serving-start -max_seq_len=512 -pooling_strategy=NONE -show_tokens_to_client -model_dir=$BERT_MODEL_DIR -graph_tmp_dir=/tmp -num_worker=1 -port=8701 -port_out=8702
