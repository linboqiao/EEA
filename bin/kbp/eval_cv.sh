#!/usr/bin/env bash
for i in {0..4}; do
gold_standard=data/mention/kbp/LDC2015E73/eval/lv1_types/goldsplit_$i.tbf
lv1_mention_realis=data/mention/kbp/LDC2015E73/eval/lv1_types/lv1_mention_realissplit_$i.tbf
gold_mention_realis=data/mention/kbp/LDC2015E73/eval/gold_types/gold_mention_realissplit_$i.tbf
../EvmEval/scorer_v1.6.py -g ${gold_standard} -s ${lv1_mention_realis} -t data/mention/kbp/LDC2015E73/tkn \
-d logs/kbp/eval/lv1_types/cv_$i.cmp -o logs/kbp/eval/lv1_types/cv_$i.scores
../EvmEval/scorer_v1.6.py -g ${gold_standard} -s ${gold_mention_realis} -t data/mention/kbp/LDC2015E73/tkn \
-d logs/kbp/eval/gold_types/cv_$i.cmp -o logs/kbp/eval/gold_types/cv_$i.scores
done