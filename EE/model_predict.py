import os
from sklearn.externals import joblib
from bert_serving.client import BertClient
from bratreader.repomodel import RepoModel
from model_com import get_events, fit_on_data, test_on_data, event_extract, event_extract_kzg, get_events_in_mention

DIR_MODEL = './save/'
file_model_trig = DIR_MODEL + 'model_trigger.pkl'
file_model_arg = DIR_MODEL + 'model_arg.pkl'

bc = BertClient(ip='127.0.0.1', port=8701, port_out=8702, show_server_config=False) # bert model as service
model_trig, encoder_trig = joblib.load(file_model_trig)
model_arg, encoder_arg = joblib.load(file_model_arg)




### sents test
test_sent = "分析称印度媒体对中巴联合军演反应过激(图)"
test_sent = "Henry Charles \"Hank\" Stackpole III (born May 7, 1935) is a retired lieutenant general in the United States Marine Corps."
test_sent = "Kyle Jerome White (born 1987) is a former United States Army soldier, and is the seventh living recipient of the Medal of Honor from the War in Afghanistan."
ann = event_extract(test_sent, model_trig, encoder_trig, model_arg, encoder_arg, bc)
print(ann)





### corpus test
#DIR_DATA = "/home/linbo/workspace/GitHubs/Delta/brat/data/test_files/"#DIR_DATA = ('data/train')
DIR_DATA = ('/home/linbo/Downloads/Annotation/military-corpus/')
DIR_DATA = 'data_ACE_Chinese'
TASK_NAME = DIR_DATA

# obtain all the files list
ANN_FILEs = []
DIR_ALL_FILES = os.listdir(DIR_DATA)
for file_name in DIR_ALL_FILES:
    if file_name.split('.')[-1] == 'txt':
        ANN_FILEs.append(file_name[:-4])

DIR_MODEL = './save/'
file_model_trig = DIR_MODEL + TASK_NAME +'_model_trigger.pkl'
file_model_arg = DIR_MODEL + TASK_NAME + '_model_arg.pkl'

bc = BertClient(ip='127.0.0.1', port=8701, port_out=8702, show_server_config=False) # bert model as service
triggers, vec_trig, label_trig, args, vec_arg, label_arg = [], [], [], [], [], []
corpus = RepoModel(DIR_DATA) # load corpus
for ANN_FILE in ANN_FILEs:
    doc = corpus.documents[ANN_FILE] # get document with key
    ttriggers, tvec_trig, tlabel_trig, targs, tvec_arg, tlabel_arg,tlabel_arg_for_each_trig = get_events_in_mention(doc, bc)
    triggers.extend(ttriggers)
    vec_trig.extend(tvec_trig)
    label_trig.extend(tlabel_trig)
    args.extend(targs)
    vec_arg.extend(tvec_arg)
    label_arg.extend(tlabel_arg)

print('trigs:', len(vec_trig), 'args:', len(vec_arg))
words, wordsvec, wordslabel = args, vec_arg, label_arg
import numpy as np
wordsvec = np.asarray(wordsvec)

print('trigger model training:')
words, wordsvec, wordslabel = triggers, vec_trig, label_trig
test_on_data(model_trig, encoder_trig, wordsvec, wordslabel, en_verbose=0)

print('argument model training:')
words, wordsvec, wordslabel = args, vec_arg, label_arg
test_on_data(model_arg, encoder_arg, wordsvec, wordslabel,en_verbose=0)




