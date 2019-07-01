
# coding: utf-8

# In[1]:


import os

CLASSPATH = "$CLASSPATH:"
path_standford = '/home/linbo/workspace/Datasets/Standford-coreNLP/'
path_segmenter = path_standford + 'stanford-segmenter-2018-10-16/stanford-segmenter.jar'
CLASSPATH = CLASSPATH + path_segmenter

path_postagger = path_standford + 'stanford-postagger-full-2018-10-16/stanford-postagger.jar'
CLASSPATH = CLASSPATH + ':' + path_postagger

path_ner = path_standford + 'stanford-ner-2018-10-16/stanford-ner.jar'
CLASSPATH = CLASSPATH + ':' + path_ner

path_parser = path_standford + 'stanford-parser-full-2018-10-17/stanford-parser.jar'
CLASSPATH = CLASSPATH + ':' + path_parser

path_parser_model = path_standford + 'stanford-parser-full-2018-10-17/stanford-parser-3.9.2-models.jar'
CLASSPATH = CLASSPATH + ':' + path_parser_model

path_corenlp = path_standford + 'stanford-corenlp-full-2018-10-05/stanford-corenlp-3.9.2.jar:' 
CLASSPATH = CLASSPATH + ':' + path_corenlp

path_model = path_standford + 'stanford-english-corenlp-2018-10-05-models.jar'
#path_model = path_standford + 'stanford-corenlp-full-2018-10-05/stanford-corenlp-3.9.2-models.jar'
CLASSPATH = CLASSPATH + ':' + path_model

path_api = path_standford + 'stanford-corenlp-full-2018-10-05/slf4j-api.jar'
CLASSPATH = CLASSPATH + ':' + path_api

print(CLASSPATH)

os.environ["CLASSPATH"] = CLASSPATH
os.environ['STANFORD_PARSER'] = path_corenlp
os.environ['STANFORD_MODELS'] = path_model


# In[21]:


from collections import OrderedDict
from nltk.tree import Tree
from nltk.parse.stanford import StanfordParser

class MyParser(StanfordParser):
    def raw_parse_sents(self, sentences, verbose=False):
        """
        Use StanfordParser to parse multiple sentences. Takes multiple sentences as a
        list of strings.
        Each sentence will be automatically tokenized and tagged by the Stanford Parser.
        The output format is `penn`.

        :param sentences: Input sentences to parse
        :type sentences: list(str)
        :rtype: iter(iter(Tree))
        """
        cmd = [
            self._MAIN_CLASS,
            '-model', self.model_path,
            '-outputFormat', 'penn', # conll, conll2007, penn
            '-sentences', 'newline'
        ]
        return self._parse_trees_output(self._execute(cmd, '\n'.join(sentences), True ))
myparser = MyParser(model_path= path_standford + 'stanford-english-corenlp-2018-10-05-models/' 
                    + "edu/stanford/nlp/models/lexparser/englishPCFG.ser.gz")

def load_tags(file_tags):
    tags = OrderedDict()
    with open(file_tags, encoding='utf-8') as ft:
        for line in ft.readlines():
            line = line.strip()
            tags[line] = len(tags)
    # scaled
    for tag in tags:
        tags[tag] = tags[tag]/len(tags)
    return tags

def get_PCFG(sent):
    sent = [sent] # one sentence
    res = list(myparser.raw_parse_sents(sent))
    return res[0]

def get_tag_PCFG(sent):
    sent = [sent] # one sentence
    res = list(myparser.raw_parse_sents(sent))
    for row in res[0]:
        for t in row: 
            x = {s[0]:s.label() for s in t.subtrees(lambda t: t.height() == 2)}
            return x

def get_tagIndexed_PCFG(sent, tags):
    sent = [sent] # one sentence
    res = list(myparser.raw_parse_sents(sent))
    for row in res[0]:
        for t in row: 
            x = {s[0]:tags[s.label()] for s in t.subtrees(lambda t: t.height() == 2)}
            return x

## load tags used in standforad parser
tags = load_tags('tags.csv')

sent = "the quick brown fox jumps over the \" lazy \" dog ."
print(sent)
r = get_PCFG(sent)
for t in r:print(t)
r = get_tag_PCFG(sent)
print(r)
r = get_tagIndexed_PCFG(sent, tags)
print(r)

