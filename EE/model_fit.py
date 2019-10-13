#model fit
import os
from sklearn.externals import joblib
from bert_serving.client import BertClient
from bratreader.repomodel import RepoModel
from model_com import get_events, fit_on_data, test_on_data, event_extract, get_events_in_mention

#DIR_DATA = "/home/linbo/workspace/GitHubs/Delta/brat/data/test_files/"
# DIR_DATA = ('data/train')
DIR_DATA = ('data_all')
# obtain all the files list
ANN_FILEs = []
DIR_ALL_FILES = os.listdir(DIR_DATA)
for file_name in DIR_ALL_FILES:
    if file_name.split('.')[-1] == 'txt':
        ANN_FILEs.append(file_name[:-4])
        
print(ANN_FILEs)

DIR_MODEL = './save/'
file_model_trig = DIR_MODEL + 'model_trigger.pkl'
file_model_arg = DIR_MODEL + 'model_arg.pkl'
bc = BertClient(ip='127.0.0.1', port=8701, port_out=8702, show_server_config=False) # bert model as service
triggers, vec_trig, label_trig, args, vec_arg, label_arg = [], [], [], [], [], []
corpus = RepoModel(DIR_DATA) # load corpus
for ANN_FILE in ANN_FILEs:
    doc = corpus.documents[ANN_FILE] # get document with key
    ttriggers, tvec_trig, tlabel_trig, targs, tvec_arg, tlabel_arg = get_events_in_mention(doc, bc)
    triggers.extend(ttriggers)
    vec_trig.extend(tvec_trig)
    label_trig.extend(tlabel_trig)
    args.extend(targs)
    vec_arg.extend(tvec_arg)
    label_arg.extend(tlabel_arg)
    



words, wordsvec, wordslabel = args, vec_arg, label_arg
import numpy as np
wordsvec = np.asarray(wordsvec)
print(np.cov(wordsvec))

words, wordsvec, wordslabel = triggers, vec_trig, label_trig
model_trig, encoder_trig = fit_on_data(wordsvec, wordslabel)
test_on_data(model_trig, encoder_trig, wordsvec, wordslabel)
joblib.dump([model_trig,encoder_trig], file_model_trig)# save the model to disk

words, wordsvec, wordslabel = args, vec_arg, label_arg
model_arg, encoder_arg = fit_on_data(wordsvec, wordslabel, N_batch = 4, N_epoch = 16, en_verbose = 0)
test_on_data(model_arg, encoder_arg, wordsvec, wordslabel)
joblib.dump([model_arg, encoder_arg], file_model_arg)


#
model_trig, encoder_trig = joblib.load(file_model_trig)
model_arg, encoder_arg = joblib.load(file_model_arg)
text = "分析称印度媒体对中巴联合军演反应过激(图)"
ann = event_extract(text, model_trig, encoder_trig, model_arg, encoder_arg, bc)
print(ann)