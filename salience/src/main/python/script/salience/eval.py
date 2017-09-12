"""
Evaluate salience results, assuming the gold standard file and system file use exact ordering of entities.
"""
from script.salience import utils


def get_salience(entities):
    salient_entities = set()
    for entity in entities:
        if entity[1] == 1:
            salient_entities.add(entity[6])
    return salient_entities


def evaluate_entity_salience(gold_path, sys_path):
    tp = 0
    fp = 0
    fn = 0

    for gold_data, sys_data in zip(utils.read_salience_file(gold_path), utils.read_salience_file(sys_path)):
        gold_saliences = get_salience(gold_data['entities'])
        sys_saliences = get_salience(sys_data['entities'])

        for e in sys_saliences:
            if e in gold_saliences:
                tp += 1
            else:
                fp += 1

        for e in gold_saliences:
            if e not in sys_saliences:
                fn += 1

    precision = 1.0 * tp / (tp + fp)
    recall = 1.0 * tp / (tp + fn)
    f1 = 2 * precision * recall / (precision + recall)

    print("Prec: %.4f, Recall: %.4f, F1: %.4f" % (precision, recall, f1))
