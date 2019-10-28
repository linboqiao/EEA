#model fit
import os
from sklearn.externals import joblib
from bert_serving.client import BertClient
from bratreader.repomodel import RepoModel
from model_com import get_events, fit_on_data, test_on_data, event_extract_kzg, get_events_in_mention

DIR_MODEL = './save/'
file_model_trig = DIR_MODEL + 'model_trigger.pkl'
file_model_arg = DIR_MODEL + 'model_arg.pkl'
bc = BertClient(ip='127.0.0.1', port=8701, port_out=8702, show_server_config=False) # bert model as service

#test
# TEST_DATA = ('data/test/')
#TEST_DATA = ('data_chinese/test/')
TEST_DATA = ('/home/linbo/Downloads/Annotation/military-corpus/')
TEST_FILEs = []
TEST_ALL_FILES = os.listdir(TEST_DATA)
for test_file_name in TEST_ALL_FILES:
    if test_file_name.split('.')[-1] == 'txt':
        TEST_FILEs.append(test_file_name[:-4])
# print(TEST_FILEs)
test_triggers, test_vec_trig, test_label_trig, test_args, test_vec_arg, test_label_arg = [], [], [], [], [], []
test_text= []
test_line= []
test_label_arg_for_each_trig = []
test_corpus = RepoModel(TEST_DATA) # load corpus
for TEST_FILE in TEST_FILEs:
    test_doc = test_corpus.documents[TEST_FILE] # get document with key
    test_ttriggers, test_tvec_trig, test_tlabel_trig, test_targs, test_tvec_arg, test_tlabel_arg,test_tlabel_arg_for_each_trig = get_events_in_mention(test_doc, bc)
    test_triggers.append(test_ttriggers)
    test_vec_trig.append(test_tvec_trig)
    test_label_trig.append(test_tlabel_trig)
    test_args.append(test_targs)
    test_vec_arg.append(test_tvec_arg)
    test_label_arg.append(test_tlabel_arg)
    test_label_arg_for_each_trig.append(test_tlabel_arg_for_each_trig)
    
    test_text.append(test_doc.text)
    
    for sent in test_doc.sentences:
        test_line.append(sent.line)
    
    

model_trig, encoder_trig = joblib.load(file_model_trig)
model_arg, encoder_arg = joblib.load(file_model_arg)
# test_text_1 = "分析称印度媒体对中巴联合军演反应过激(图)"

files_num = len(TEST_FILEs)

match_strict_count_total = 0
match_approx_count_total = 0
for file_i in range(files_num):
    event_match_strict_count, event_match_approx_count= event_extract_kzg(test_text[file_i], model_trig, encoder_trig, 
                                                                          model_arg, encoder_arg, test_vec_trig[file_i],
                                                                          test_vec_arg[file_i], test_label_trig[file_i],
                                                                          test_label_arg_for_each_trig[file_i],bc)
    match_strict_count_total += event_match_strict_count
    match_approx_count_total += event_match_approx_count

print("match_strict_count_total:",match_strict_count_total)
print("match_approx_count_total:",match_approx_count_total)
print("done")
