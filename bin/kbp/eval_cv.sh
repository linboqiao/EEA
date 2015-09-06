#!/usr/bin/env bash
for i in {0..4}; do
../EvmEval/scorer_v1.6.py -g data/mention/kbp/LDC2015E73/eval/lv1_types/goldsplit_$i.tbf -s \
data/mention/kbp/LDC2015E73/eval/lv1_types/predictedsplit_$i.tbf -t data/mention/kbp/LDC2015E73/tkn \
-d logs/kbp/eval/cv_$i.cmp -o logs/kbp/eval/cv_$i.scores
done