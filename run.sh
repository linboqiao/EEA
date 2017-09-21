RAW_TRAIN=$1
TOKENIZED_TRAIN=tokenized_train.txt
TOKEN_MAPPING=tokenized_mapping.txt
THREAD=10
TOKENIZER="-cp .:tokenizer/lib/*:tokenizer/resources/:tokenizer/build/ Tokenizer"
java $TOKENIZER -m train -i $RAW_TRAIN -o $TOKENIZED_TRAIN -t $TOKEN_MAPPING -c N -thread $THREAD