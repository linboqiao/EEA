# 配置tensorflow利用显存方式
import tensorflow as tf
from keras.backend.tensorflow_backend import set_session
config = tf.ConfigProto()
#config.gpu_options.per_process_gpu_memory_fraction = 0.3
config.gpu_options.allow_growth=True 
#config.gpu_options.visible_device_list = "0"
set_session(tf.Session(config=config))


import time
import pickle
import numpy as np
import pandas as pd
from keras import backend as K
from keras import optimizers
from keras.utils import np_utils
from keras.models import Sequential, Model
from keras.layers import Dense, Dropout, Input, Lambda, Activation
from keras.optimizers import SGD, Adam, RMSprop
from sklearn.preprocessing import LabelEncoder
from sklearn.model_selection import train_test_split
import matplotlib.pyplot as plt


def get_embdinspan(embds, spans, wordslabel, span):
    for idxs in range(len(spans)):
        s = spans[idxs]
        if s[0]<=span[0] and span[0]<s[1]:
            break
    for idxe in range(len(spans)):
        s = spans[idxe]
        if s[0]<=span[1] and span[1]<=s[1]:
            break
    idxe = idxe + 1
    embdsin = []
    labelsin = []
    for idx in range(idxs,idxe):
        embdsin.append(embds[idx])
        labelsin.append(wordslabel[idx])
    return embdsin, labelsin

def get_embdoutspan(embds, spans, wordslabel, span):
    for idxs in range(len(spans)):
        s = spans[idxs]
        if s[0]<=span[0] and span[0]<=s[1]:
            break
    for idxe in range(len(spans)):
        s = spans[idxe]
        if s[0]<=span[1] and span[1]<=s[1]:
            break
    embdsout = []
    labelsout = []
    for idx in range(0,idxs):
        embdsout.append(embds[idx])
        labelsout.append(wordslabel[idx])
    for idx in range(idxe,len(wordslabel)):
        embdsout.append(embds[idx])
        labelsout.append(wordslabel[idx])
    return embdsout, labelsout

def get_embdintype(embds, spans, wordslabel, labelType='NULL'):
    embds_NULL = []
    labels_NULL = []
    for idx in range(len(spans)):
        if wordslabel[idx]==labelType:
            embds_NULL.append(embds[idx])
            labels_NULL.append(wordslabel[idx])
    return embds_NULL, labels_NULL

def get_words(doc, bc): 
    '''get: words, embedding, spans, labels
    '''    
    words = []
    wordsvec = []
    spans = []
    wordslabel = []
    
    for sent in doc.sentences:
        str_sent = sent.line        
        # Embeddings of each sentence/ sequence via BERT.
        vec = bc.encode([str_sent], show_tokens=True)
        #print(type(vec))
        for idx_sentence in range(len(vec[1])):
            #print('\n',vec[1][idx_sentence])
            for idx_token in range(len(vec[1][idx_sentence])):
                #print(vec[1][idx_sentence][idx_token],'\t', vec[0][idx_sentence][idx_token][0:5])                
                if( vec[1][idx_sentence][idx_token].find('[CLS]', 0, 5)==0 ):
                    # [CLS]
                    words.append(vec[1][idx_sentence][idx_token])
                    wordsvec.append(vec[0][idx_sentence][idx_token][0:])
                    if len(spans)>0:
                        spans.append([spans[-1][1],spans[-1][1]])
                    else:
                        spans.append([0,0])
                    wordslabel.append(['[CLS]'])
                elif( vec[1][idx_sentence][idx_token].find('[SEP]', 0, 5)==0 ):
                    # [SEP]
                    words.append(vec[1][idx_sentence][idx_token])
                    wordsvec.append(vec[0][idx_sentence][idx_token][0:])
                    if len(spans)>0:
                        spans.append([spans[-1][1],spans[-1][1]])
                    else:
                        spans.append([0,0])
                    wordslabel.append(['[SEP]'])
                elif( vec[1][idx_sentence][idx_token].find('##', 0, 2)<0 ):
                    # Token in BERT table
                    words.append(vec[1][idx_sentence][idx_token])
                    wordsvec.append(vec[0][idx_sentence][idx_token][0:])                      
                    start = doc.text.lower().find(words[-1], spans[-1][0]) + idx_sentence
                    end = start + len(words[-1]) + idx_sentence
                    spans.append([start, end])
                    label = list(set(doc.getlabelinspan(start, end)))
                    if len(str(label))>2:
                        wordslabel.append(label)
                    else:
                        wordslabel.append(['NULL'])                    
                else:
                    # Token started with '##' in BERT
                    words[-1] = words[-1] + vec[1][idx_sentence][idx_token][2:]
                    wordsvec[-1] = wordsvec[-1] + vec[0][idx_sentence][idx_token][0:]
                    spans[-1] = ([spans[-1][0], spans[-1][0]+len(words[-1])])
                    label = list(set(doc.getlabelinspan(spans[-1][0], spans[-1][1])))
                    if len(str(label))>2:
                        wordslabel[-1] = label
                    else:
                        wordslabel[-1] = ['NULL']
                #print(spans[-1], wordslabel[-1], words[-1], wordsvec[-1])
    return words, wordsvec, spans, wordslabel


def get_embd(sents, bc):
    '''get embedding of sents
    ''' 
    words = []
    wordsvec = []
    
    for str_sent in sents:      
        # Embeddings of each sentence/ sequence via BERT.
        print(str_sent)
        vec = bc.encode([str_sent], show_tokens=True)
        #print(type(vec))
        for idx_sentence in range(len(vec[1])):
            #print('\n',vec[1][idx_sentence])
            for idx_token in range(len(vec[1][idx_sentence])):
                #print(vec[1][idx_sentence][idx_token],'\t', vec[0][idx_sentence][idx_token][0:5])                
                if( vec[1][idx_sentence][idx_token].find('[CLS]', 0, 5)==0 ):
                    # [CLS]
                    continue
                    words.append(vec[1][idx_sentence][idx_token])
                    wordsvec.append(vec[0][idx_sentence][idx_token][0:])
                elif( vec[1][idx_sentence][idx_token].find('[SEP]', 0, 5)==0 ):
                    # [SEP]
                    continue
                    words.append(vec[1][idx_sentence][idx_token])
                    wordsvec.append(vec[0][idx_sentence][idx_token][0:])
                elif( vec[1][idx_sentence][idx_token].find('##', 0, 2)<0 ):
                    # Token in BERT table
                    words.append(vec[1][idx_sentence][idx_token])
                    wordsvec.append(vec[0][idx_sentence][idx_token][0:])  
                else:
                    # Token started with '##' in BERT
                    words[-1] = words[-1] + vec[1][idx_sentence][idx_token][2:]
                    wordsvec[-1] = wordsvec[-1] + vec[0][idx_sentence][idx_token][0:]
                #print(spans[-1], wordslabel[-1], words[-1], wordsvec[-1])
    return words, wordsvec


def get_events(doc, bc):
    '''get: triggers, embds_triggers, labels_triggers, args, embds_args, labels_args
            embeddings of arg is the concatation of trigger and arg
    ''' 
    words, wordsvec, spans, wordslabel = get_words(doc, bc)
    wordslabel = [label[0] for label in wordslabel]
    #print('labels:', len(set(wordslabel)), set(wordslabel))
    print([[words[idx], wordslabel[idx], spans[idx]] for idx in np.arange(len(wordslabel))])
    names=['triggers', 'embds_triggers', 'labels_triggers', 'args', 'embds_args', 'labels_args', 'espan'] 
    events = pd.DataFrame(columns=names)
    idx = 0
    
    for event in doc.events:
        triggers, embds_triggers, labels_triggers, args, embds_args, labels_args, espan= [], [], [], [], [], [], []
        embdsin, labelsin = get_embdinspan(wordsvec, spans, wordslabel, event.trigger_spans)
        espan = event.trigger_spans # assume that only one trigger for one event mention
        for ebd in embdsin:
            triggers.append(event.trigger)
            embds_triggers.append(ebd)
            labels_triggers.append(event.trigger_label)
        for idx in range(len(event.args)):
            arg = event.args[idx]
            span = event.args_spans[idx]
            espan[0] = np.min([espan[0], span[0]])
            espan[1] = np.max([espan[1], span[1]])
            label = event.args_labels[idx]
            embdsin, labelsin = get_embdinspan(wordsvec, spans, wordslabel, span)
            for ebd in embdsin:
                args.append(arg)
                embds_args.append(ebd)
                labels_args.append(label)
        embds_NULL, labels_NULL = get_embdintype(wordsvec, spans, wordslabel, labelType='NULL')
        #print(type(embds_triggers), type(labels_triggers), type(embds_args), type(labels_args), type(embds_NULL), type(labels_NULL))
        events = events.append({'triggers':triggers, 'embds_triggers':embds_triggers, 'labels_triggers':labels_triggers,
                                'args':args, 'embds_args':embds_args, 'labels_args':labels_args,
                                'embds_NULL':embds_NULL, 'labels_NULL':labels_NULL, 'espan':espan}, ignore_index=True)
    return events


def get_events_in_mention(doc, bc):
    '''get: triggers, embds_triggers, labels_triggers, args, embds_args, labels_args
    '''
    events = get_events(doc, bc)
    for (index, row) in events.iterrows():
        print(row['triggers'])
    triggers, embds_triggers, labels_triggers, args, embds_args, labels_args = [], [], [], [], [], []
    
    for index, r in events.iterrows():
        triggers.extend(r['triggers'])
        embds_triggers.extend(r['embds_triggers'])
        labels_triggers.extend(r['labels_triggers'])
        triggers.extend(r['args'])
        embds_triggers.extend(r['embds_args'])
        labels_triggers.extend(['NULL']*len(r['labels_args']))
        triggers.extend(['NULL']*len(r['labels_NULL']))
        embds_triggers.extend(r['embds_NULL'])
        labels_triggers.extend(r['labels_NULL'])
        for idx in range(len(r['triggers'])):
            for idxarg in range(len(r['args'])):
                args.extend([r['triggers'][idx] + '->' + r['args'][idxarg]])
                temp_embds_arg = np.append(r['embds_triggers'][idx], r['embds_args'][idxarg], axis=0)
                embds_args.extend([temp_embds_arg])
                labels_args.extend([r['labels_args'][idxarg]])
            for idxN in range(len(r['labels_NULL'])):
                args.extend([r['triggers'][idx] + '-> NULL'])
                temp_embds_arg = np.append(r['embds_triggers'][idx], r['embds_NULL'][idxN], axis=0)
                embds_args.extend([temp_embds_arg])
                labels_args.extend(['NULL'])
    return triggers, embds_triggers, labels_triggers, args, embds_args, labels_args


def create_base_network(input_dim, nb_classes):
    '''Base network to be shared (eq. to feature extraction).
    '''    
    sgd = optimizers.SGD(lr=0.01, clipnorm=1.)
    sgd = optimizers.SGD(lr=0.01, momentum=0.0, decay=0.0, nesterov=False)
    rmsprop = optimizers.RMSprop(lr=0.001, rho=0.9, epsilon=None, decay=0.0)
    adagrad = optimizers.Adagrad(lr=0.01, epsilon=None, decay=0.0)
    adadelta = optimizers.Adadelta(lr=1.0, rho=0.95, epsilon=None, decay=0.0)
    adam = optimizers.Adam(lr=0.001, beta_1=0.9, beta_2=0.999, epsilon=None, decay=0.0, amsgrad=False)
    adamax = optimizers.Adamax(lr=0.002, beta_1=0.9, beta_2=0.999, epsilon=None, decay=0.0)
    nadam = optimizers.Nadam(lr=0.002, beta_1=0.9, beta_2=0.999, epsilon=None, schedule_decay=0.004)
    
    N_nodes = input_dim
    r_droupout = 0.2
    model_base = Sequential()
    model_base.add(Dense(N_nodes, input_shape=(input_dim,)))
    model_base.add(Activation('relu'))
    model_base.add(Dropout(r_droupout))
    N_nodes = int(np.floor(N_nodes/2))
    model_base.add(Dense(N_nodes))
    model_base.add(Activation('relu'))
    model_base.add(Dropout(r_droupout))
    N_nodes = int(np.floor(N_nodes/2))
    model_base.add(Dense(N_nodes))
    model_base.add(Activation('relu'))
    model_base.add(Dropout(r_droupout))
    N_nodes = int(np.floor(N_nodes/2))
    model_base.add(Dense(N_nodes))
    model_base.add(Activation('relu'))
    model_base.add(Dropout(r_droupout))
    N_nodes = int(np.floor(N_nodes/2))
    model_base.add(Dense(N_nodes))
    model_base.add(Activation('relu'))
    model_base.add(Dropout(r_droupout))
    N_nodes = int(np.floor(N_nodes/2))
    model_base.add(Dense(N_nodes))
    model_base.add(Activation('relu'))
    model_base.add(Dense(nb_classes))
    model_base.add(Activation('softmax'))    
    model_base.compile(loss='categorical_crossentropy',
                       optimizer=rmsprop,
                       metrics=['accuracy'])
    #model_base.load_weights('model_base.h5')
    return model_base


def fit_on_data(wordsvec='NULL', wordslabel='NULL', N_batch = 4, N_epoch = 16, en_verbose = 0):
    '''
    fit the model on given data
    '''
    print('fit the model on given data:')
    # wordsvec from list to array
    wordsvec = np.asarray(wordsvec)
    print('samples shape:', wordsvec.shape)
    print('labels number:', len(set(wordslabel)), set(wordslabel))
    
    # encode class values as integers
    encoder = LabelEncoder()
    encoder.fit(wordslabel)
    Y_encoder = encoder.transform(wordslabel)
    # convert integers to dummy variables (i.e. one hot encoded)
    Y_encoder = np_utils.to_categorical(Y_encoder)
    
    #X_train, X_test, Y_train, Y_test = train_test_split(wordsvec, Y_encoder, random_state=0)
    X_train, X_test, Y_train, Y_test  = wordsvec, wordsvec, Y_encoder, Y_encoder
    
    # model define
    input_dim = wordsvec.shape[1]
    N_classes = len(set(wordslabel))
    
    model = create_base_network(input_dim, N_classes)
    model.summary()
    
    # model training
    start   = time.time()
    model.fit(X_train, Y_train,
                        batch_size=N_batch, epochs=N_epoch,
                        verbose=en_verbose, validation_data=(X_test, Y_test))
    end     = time.time()
    print('time elapse on training:\t', end - start, 'sec')
    return model, encoder


def test_on_data(model, encoder, wordsvec, wordslabel):   
    print('test the model on given data:')
    # wordsvec from list to array
    wordsvec = np.asarray(wordsvec)
    print('samples shape:', wordsvec.shape)
    print('labels number:', len(set(wordslabel)), set(wordslabel))
    
    # encode class values as integers
    Y_encoder = encoder.transform(wordslabel)
    # convert integers to dummy variables (i.e. one hot encoded)
    Y_encoder = np_utils.to_categorical(Y_encoder)
    
    # model test
    print('='*65,'\n>>testing')
    probs = model.predict(wordsvec, verbose=1)
    print(probs.shape)
    
    # model eval
    print('='*65,'\n>>evaluating')
    #Returns the loss value & metrics values for the model in test mode.
    [loss, metrics] = model.evaluate(x=wordsvec, y=Y_encoder, verbose=1)
    print('loss : ', loss)
    print(model.metrics[0], ':', metrics)


def label2ann(words, labels_trig, labels_arg, idxT, idxE):
    '''
    single event labels to ann
    '''
    ann = ''
    annT = ''
    annE = ''
    set_trigger = set(labels_trig)
    set_args = set(labels_arg)
    
    blank_tokens = [' ']
    for label in set_trigger:
        if label == 'NULL':
            continue
        spans = [0, 0]
        idx_tokens_trigger = np.where(labels_trig==label)[0]
        spans[0] = np.min(idx_tokens_trigger)
        for temp_idx in range(len(idx_tokens_trigger)-1):
            if (idx_tokens_trigger[temp_idx+1]==idx_tokens_trigger[temp_idx] + 1):#连续的tokens被标记为相同的label
                spans[1] = idx_tokens_trigger[temp_idx+1]
        spans[1] = spans[1] + 1
        idxT = idxT + 1
        idxE = idxE + 1
        annT = annT + '\n' + 'T'+str(idxT)+'    '+label+' '+str(spans[0])+' '+str(spans[1])+'    '+''.join(words[spans[0]:spans[1]])
        annE = annE + '\n' + 'E'+str(idxE)+'    '+label+':T'+str(idxT)
    for label in set_args:
        if label == 'NULL':
            continue
        spans = [0, 0]
        idx_tokens_trigger = np.where(labels_arg==label)[0]
        spans[0] = np.min(idx_tokens_trigger)
        for temp_idx in range(len(idx_tokens_trigger)-1):
            if (idx_tokens_trigger[temp_idx+1]==idx_tokens_trigger[temp_idx] + 1):#连续的tokens被标记为相同的label
                spans[1] = idx_tokens_trigger[temp_idx+1]
        spans[1] = spans[1] + 1
        idxT = idxT + 1
        annT = annT + '\n' + 'T'+str(idxT)+'    '+label+' '+str(spans[0])+' '+str(spans[1])+'    '+''.join(words[spans[0]:spans[1]])
        #annE = annE + ' +Agent-A-Arg:T1'
        annE = annE + ' '+label+':T'+str(idxT)
    ann = annT + annE
    return ann, idxT, idxE

def event_extract(text, model_trigger, encoder_trigger, model_arg, encoder_arg, bc):
    
    words, wordsvec = get_embd([text], bc)
    # wordsvec from list to array
    wordsvec = np.asarray(wordsvec)
    
    probs = model_trigger.predict(wordsvec)
    idxs = np.argmax(probs, axis=1)
    labels_trig = encoder_trigger.inverse_transform(idxs)
    print(probs.shape, len(labels_trig))
    print([[words[idx], labels_trig[idx]] for idx in np.arange(len(labels_trig))])
    
    labels_arg = []
    idxT = 0
    idxE = 0
    
    for idx in range(len(labels_trig)):
        embds_args = []
        if labels_trig[idx] == 'NULL':
            continue
        for idxarg in range(len(labels_trig)):
            if labels_trig[idxarg] == 'NULL':
                temp_embds_arg = np.append(wordsvec[idx], wordsvec[idxarg], axis=0)
                embds_args.extend([temp_embds_arg])
        embds_args = np.asarray(embds_args)
        probs = model_arg.predict(embds_args)
        idxs = np.argmax(probs, axis=1)
        labels_arg = encoder_arg.inverse_transform(idxs)
        print(probs.shape, len(labels_arg))
        print([[words[idx]+'->'+ words[idx_arg], labels_arg[idx_arg]] for idx_arg in np.arange(len(labels_arg))])
        ann, idxT, idxE = label2ann(words, labels_trig, labels_arg, idxT, idxE)
    return ann

