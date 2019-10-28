#model fit
import os
import numpy as np
from sklearn.externals import joblib
from bert_serving.client import BertClient
from bratreader.repomodel import RepoModel
from sklearn.preprocessing import LabelEncoder
from model_com import create_base_network, get_events, fit_on_data, test_on_data, event_extract_kzg, get_events_in_mention


import tensorflow as tf
from keras import backend as K
en_GPU = 1
if en_GPU:
    num_GPU, num_CPU = 1, 8
else:
    num_GPU, num_CPU = 0, 8

config = tf.ConfigProto(intra_op_parallelism_threads=num_CPU,
                        inter_op_parallelism_threads=num_CPU, 
                        allow_soft_placement=True,
                        device_count = {'CPU' : num_CPU,
                                        'GPU' : num_GPU}
                       )
session = tf.Session(config=config)
K.set_session(session)




def training(DIR_DATA): 
    print('\ndata importing:')
    TASK_NAME = DIR_DATA
    NAME_DATA_FILE = TASK_NAME+'_data_import'+'.save'
    
    # obtain all the files list
    ANN_FILEs = []
    DIR_ALL_FILES = os.listdir(DIR_DATA)
    for file_name in DIR_ALL_FILES:
        if file_name.split('.')[-1] == 'txt':
            ANN_FILEs.append(file_name[:-4])
    
    DIR_MODEL = './save_Eng/'
    file_model_trig = DIR_MODEL + TASK_NAME +'_model_trigger.pkl'
    file_model_arg = DIR_MODEL + TASK_NAME + '_model_arg.pkl'
    triggers, vec_trig, label_trig, args, vec_arg, label_arg = [], [], [], [], [], []
    try:
        triggers, vec_trig, label_trig, args, vec_arg, label_arg = joblib.load(NAME_DATA_FILE)
        args, vec_arg, label_arg = None, None, None
    except:
        corpus = RepoModel(DIR_DATA) # load corpus
        for ANN_FILE in ANN_FILEs:
            bc = BertClient(ip='127.0.0.1', port=8701, port_out=8702, show_server_config=False) # bert model as service
            doc = corpus.documents[ANN_FILE] # get document with key
            ttriggers, tvec_trig, tlabel_trig, targs, tvec_arg, tlabel_arg, tlabel_arg_for_each_trig = get_events_in_mention(doc, bc)
            triggers.extend(ttriggers)
            vec_trig.extend(tvec_trig)
            label_trig.extend(tlabel_trig)
            args.extend(targs)
            vec_arg.extend(tvec_arg)
            label_arg.extend(tlabel_arg)
        
        print('trigs:', len(vec_trig), 'args:', len(vec_arg))
        joblib.dump([triggers, vec_trig, label_trig, args, vec_arg, label_arg], NAME_DATA_FILE)
        args, vec_arg, label_arg = None, None, None
    
    print('='*65,'\n>>trigger model training:')
    try:
        model_trig, encoder_trig = joblib.load(file_model_trig)
        acc_pre = test_on_data(model_trig, encoder_trig, vec_trig, label_trig, en_verbose = 0)
    except:
        # model define
        input_dim = np.asarray(vec_trig).shape[1]
        N_classes = len(set(label_trig))
        model_trig = create_base_network(input_dim, N_classes)
        encoder_trig = LabelEncoder()
        encoder_trig.fit(label_trig)
        acc_pre = 0
        
    N_batchs =[len(label_trig), 8192, 4096, 2048, 1024, 512, 32, 16, 8, 4, 2, 1]
    lrs = [0.001, 0.00001]
    for N_batch in N_batchs:
        for lr in lrs:
            Times_training, N_batch, N_epoch, en_verbose = 3, N_batch, max(16, int(np.floor(np.sqrt(10*N_batch)))), 1
            for times in range(1, Times_training):
                the_lr = lr/times
                model_trig, encoder_trig, his = fit_on_data(vec_trig, label_trig, model_trig, encoder_trig, 
                                                            the_lr, N_batch = N_batch, N_epoch = N_epoch, en_verbose = en_verbose)
                print('acc:{}'.format(his.history['acc'][-1]))
                val_acc = test_on_data(model_trig, encoder_trig, vec_trig, label_trig, en_verbose = en_verbose)
                joblib.dump([model_trig,encoder_trig], '{}_{:.5f}_{:.5f}_{:.5f}_{:.5f}.pkl'.format(
                    file_model_trig[0:-4], his.history['acc'][-1], val_acc, the_lr, N_batch)) # save the model to disk
                if val_acc > acc_pre:
                    acc_pre = val_acc
                    joblib.dump([model_trig,encoder_trig], '{}.pkl'.format(file_model_trig[0:-4])) # save the model to disk
                else:
                    break
    return

    print('='*65,'\n>>argument model training:')
    try:
        triggers, vec_trig, label_trig = None, None, None
        triggers, vec_trig, label_trig, args, vec_arg, label_arg = joblib.load(NAME_DATA_FILE)
        triggers, vec_trig, label_trig = None, None, None
        model_arg, encoder_arg = joblib.load(file_model_arg)
        acc_pre = test_on_data(model_arg, encoder_arg, vec_arg, label_arg, en_verbose = 0)
    except:
        encoder_arg = LabelEncoder()
        encoder_arg.fit(label_arg)
        # model define
        input_dim = np.asarray(vec_arg).shape[1]
        N_classes = len(set(label_arg))
        model_arg = create_base_network(input_dim, N_classes)
        acc_pre = 0
    
    for lr in lrs:
        for N_batch in N_batchs:
            Times_training, N_batch, N_epoch, en_verbose = 3, N_batch, max(16, int(np.floor(np.sqrt(10*N_batch)))), 1
            for times in range(1, Times_training):
                the_lr = lr/times
                model_arg, encoder_arg, his = fit_on_data(vec_arg, label_arg, model_arg, encoder_arg, 
                                                          the_lr, N_batch = N_batch, N_epoch = N_epoch, en_verbose = en_verbose)
                print('acc:{}'.format(his.history['acc'][-1]))
                val_acc = test_on_data(model_arg, encoder_arg, vec_arg, label_arg, en_verbose = en_verbose)
                joblib.dump([model_arg,encoder_arg], '{}_{:.5f}_{:.5f}_{:.5f}_{:.5f}.pkl'.format(
                    file_model_arg[0:-4], his.history['acc'][-1], val_acc, the_lr, N_batch)) # save the model to disk
                if val_acc > acc_pre:
                    acc_pre = val_acc
                    joblib.dump([model_arg,encoder_arg], '{}.pkl'.format(file_model_arg[0:-4])) # save the model to disk
                else:
                    break
    
    

DIR_DATAs = ['data_ACE_English']
for DIR_DATA in DIR_DATAs:
    training(DIR_DATA)
    
