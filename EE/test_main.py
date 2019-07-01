
# coding: utf-8

# In[1]:


import matplotlib.pyplot as plt
from bert_serving.client import BertClient
bc = BertClient(ip='127.0.0.1', port=8701, port_out=8702, show_server_config=True)


# In[27]:


vec = bc.encode(
    ['First do it',  # [CLS] First do it [SEP] [word embedding for padding symbol]
     'then do it right', 
     'then do it better',
     'In the middle of nowhere, you will find that you are nobody, nobody in the middle of nowhere.'],
    show_tokens=True)

print(vec[0].shape, vec[1])
for idx_sentence in range(len(vec[1])):
    print('\n',vec[1][idx_sentence])
    for idx_token in range(len(vec[1][idx_sentence])):
        print(vec[1][idx_sentence][idx_token],'\t', vec[0][idx_sentence][idx_token][0:5])

vec = vec[0]

plt.subplot(2, 1, 1)
plt.plot(vec[0][0:5].T)
plt.ylabel('Token Embedding')
plt.show()

plt.subplot(2, 1, 2)
plt.plot(vec[0][5:].T)
plt.ylabel('Token Embedding')
plt.show()




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



sent = "Kalla, it\'s a dog!"

from nltk.tokenize.stanford import StanfordTokenizer

tokenizer = StanfordTokenizer()
print(tokenizer.tokenize(sent))



from nltk.parse.stanford import StanfordParser

class MyParser(StanfordParser):
    def raw_parse_sents(self, sentences, verbose=False):
        """
        Use StanfordParser to parse multiple sentences. Takes multiple sentences as a
        list of strings.
        Each sentence will be automatically tokenized and tagged by the Stanford Parser.
        The output format is `wordsAndTags`.

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

en_GUI = 1
sent = "the quick brown fox jumps over the \" lazy \" dog ."
print(sent)
res = list(myparser.raw_parse_sents([sent, sent]))
for row in res:
    for t in row:
        print(t)
        if  en_GUI:
            t.draw()
