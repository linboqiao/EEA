import json
from pycorenlp import StanfordCoreNLP

nlp = StanfordCoreNLP('http://localhost:{0}'.format(9000))


def get_stanford_annotations(text, annotators='tokenize,ssplit,pos,lemma,depparse,parse'):
    output = nlp.annotate(text, properties={
        "timeout": "10000",
        'tokenize.whitespace': 'true',
        'ssplit.isOneSentence': 'true',
        # "tokenize.options": "ptb3Escaping=false",
        'annotators': annotators,
    })
    return output


def get_dep_parse(text):
    annotations = get_stanford_annotations(text, annotators='tokenize,ssplit,depparse')
    annotations = json.loads(annotations, encoding="utf-8", strict=False)
    dep = annotations['sentences'][0]['basicDependencies']
    return dep


if __name__ == '__main__':
    with open('/shared/data/czhang82/projects/event-extraction/data/la/tweets_2.txt', encoding='utf-8') as in_file, \
            open('la_tweets_depparse.txt', 'w', encoding='utf-8') as out_file:
        for line_num, line in enumerate(in_file):
            if line_num % 10000 == 0:
                print(line_num)
            tokens = line.strip().split(' ')
            dep = get_dep_parse(line.strip())
            out_file.write(json.dumps(dep) + '\n')
