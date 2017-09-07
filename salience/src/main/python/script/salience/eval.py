"""
Evaluate salience results, assuming the gold standard file and system file use exact ordering of entities.
"""
from script.salience import utils


def evaluate_entity_salience(gold_path, sys_path):
    tp = 0
    fp = 0
    fn = 0

    for gold_data, sys_data in zip(utils.read_salience_file(gold_path), utils.read_salience_file(sys_path)):
        for gold_entity, sys_entity in zip(gold_data['entities'], sys_data['entities']):
            if sys_entity[1] == 1:
                if gold_entity[1] == 1:
                    tp += 1
                else:
                    fp += 1
            else:
                if gold_entity[1] == 1:
                    fn += 1

    precision = 1.0 * tp / (tp + fp)
    recall = 1.0 * tp / (tp + fn)
    f1 = 2 * precision * recall / (precision + recall)

    print("Prec: %.4f, Recall: %.4f, F1: %.4f" % (precision, recall, f1))
