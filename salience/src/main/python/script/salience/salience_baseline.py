"""
Contains several basic salience baselines.
"""
import os

import errno
import operator
import sys

from script.salience import utils


def most_frequent(entities):
    kb_count = {}
    for entity in entities:
        try:
            kb_count[entity[6]] += 1
        except KeyError:
            kb_count[entity[6]] = 1

    sorted_kb_counts = sorted(kb_count.items(), key=operator.itemgetter(1), reverse=True)

    frequent_kbs = {}

    max_count = -1
    for kb, count in sorted_kb_counts:
        if max_count == -1:
            max_count = count
            frequent_kbs[kb] = count
        else:
            if not count == max_count:
                break
            else:
                frequent_kbs[kb] = count

    return frequent_kbs, kb_count


def kb_frequency_baseline(test_gold, out_path):
    tp = 0
    fp = 0
    fn = 0

    if not os.path.exists(os.path.dirname(out_path)):
        try:
            os.makedirs(os.path.dirname(out_path))
        except OSError as exc:
            if exc.errno != errno.EEXIST:
                raise

    with open(out_path, 'w') as out_file:
        for data in utils.read_salience_file(test_gold):
            entities = data['entities']
            frequent_kbs, all_counts = most_frequent(data['entities'])

            out_file.write(data['docid'] + " " + data['title'] + "\n")

            for entity in entities:
                prediction = list(entity[:])
                # Appending gold standard at the end for debugging.
                prediction.append(entity[1])

                salience = entity[1]
                kbid = entity[6]
                if kbid in frequent_kbs:
                    prediction[1] = 1
                    prediction[2] = frequent_kbs[kbid]
                    if salience == 1:
                        tp += 1
                    else:
                        fp += 1
                else:
                    prediction[1] = 0
                    prediction[2] = all_counts[kbid]
                    if salience == 1:
                        fn += 1

                out_file.write("\t".join([str(x) for x in prediction]) + "\n")
            out_file.write("\n")

    precision = 1.0 * tp / (tp + fp)
    recall = 1.0 * tp / (tp + fn)
    f1 = 2 * precision * recall / (precision + recall)

    print("Prec: %.4f, Recall: %.4f, F1: %.4f" % (precision, recall, f1))


def run(argv):
    baseline_type = argv[1]
    gold = argv[2]
    output = argv[3]

    if baseline_type == "frequency":
        kb_frequency_baseline(gold, output)
