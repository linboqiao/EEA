RAW_TRAIN=$1
TOKENIZED_TRAIN=tmp/tokenized_train.txt
TOKENIZED_TEST=tmp/tokenized_test.txt
TOKEN_MAPPING=tmp/tokenized_mapping.txt
TOKEN_TRIGGER=tmp/triggers.txt
TOKENIZED_TIGGER=tmp/tokenized_triggers.txt
THREAD=10
TOKENIZER="-cp .:tokenizer/lib/*:tokenizer/resources/:tokenizer/build/ Tokenizer"
javac -cp ".:tokenizer/lib/*" tokenizer/src/Tokenizer.java -d tokenizer/build/
java $TOKENIZER -m train -i $RAW_TRAIN -o $TOKENIZED_TRAIN -t $TOKEN_MAPPING -c N -thread $THREAD
java $TOKENIZER -m direct_test -i $TOKEN_TRIGGER -o $TOKENIZED_TIGGER -t $TOKEN_MAPPING -c N -thread $THREAD
java $TOKENIZER -m direct_test -i $RAW_TRAIN -o $TOKENIZED_TEST -t $TOKEN_MAPPING -c N -thread $THREAD
./code/bin/mine_triggers --thread $THREAD
java $TOKENIZER -m segmentation -i $RAW_TRAIN -segmented tmp/tokenized_segmented_sentences.txt -o tmp/segmentation.txt -tokenized_raw tmp/raw_tokenized_test.txt -tokenized_id $TOKENIZED_TEST -c N
