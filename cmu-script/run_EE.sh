

rm -rf sample-output
mkdir sample-output
cd cmu-script
bin/test/coref_plain_text.sh settings/kbp.properties ../sample-input ../sample-output


cd ../../EvmEval
bash kbp_score.sh


