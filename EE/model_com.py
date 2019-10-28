# 配置tensorflow利用显存方式
import tensorflow as tf
from keras.backend.tensorflow_backend import set_session
config = tf.ConfigProto()
#config.gpu_options.per_process_gpu_memory_fraction = 0.3
config.gpu_options.allow_growth=True 
#config.gpu_options.visible_device_list = "0"
set_session(tf.Session(config=config))

import copy
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
from sklearn.externals import joblib


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
    #print([[words[idx], wordslabel[idx], spans[idx]] for idx in np.arange(len(wordslabel))])
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
    en_NULL = False
    events = get_events(doc, bc)
    triggers, embds_triggers, labels_triggers, args, embds_args, labels_args = [], [], [], [], [], []
    
    label_arg_for_each_trig = []
    for index, r in events.iterrows():
        num_trigger_NULL = 0
        triggers.extend(r['triggers'])
        embds_triggers.extend(r['embds_triggers'])
        labels_triggers.extend(r['labels_triggers'])
        if en_NULL:
            triggers.extend(r['args'])
            embds_triggers.extend(r['embds_args'])
            labels_triggers.extend(['NULL']*len(r['labels_args']))
            triggers.extend(['NULL']*len(r['labels_NULL']))
            embds_triggers.extend(r['embds_NULL'])
            labels_triggers.extend(r['labels_NULL'])
        elif(len(r['args'])>0):
            triggers.extend([r['args'][0]])
            embds_triggers.append(r['embds_args'][0])
            labels_triggers.extend(['NULL'])
            
        for idx in range(len(r['triggers'])):
            label_arg_for_each_trig.append([])
            for idxarg in range(len(r['args'])):
                args.extend([r['triggers'][idx] + '->' + r['args'][idxarg]])
                temp_embds_arg = np.append(r['embds_triggers'][idx], r['embds_args'][idxarg], axis=0)
                embds_args.extend([temp_embds_arg])
                labels_args.extend([r['labels_args'][idxarg]])
                label_arg_for_each_trig[idx].extend([r['labels_args'][idxarg]])
            for idxN in range(len(r['labels_NULL'])):
                args.extend([r['triggers'][idx] + '-> NULL'])
                temp_embds_arg = np.append(r['embds_triggers'][idx], r['embds_NULL'][idxN], axis=0)
                embds_args.extend([temp_embds_arg])
                labels_args.extend(['NULL'])
                label_arg_for_each_trig[idx].extend(['NULL'])
                if not en_NULL:
                    break
#label_arg_for_each_trig 是指在处理一个具有多个触发词的句子的时候,将不同trigger情况下的label_arg放在label_arg_for_each_trig的不同行
#labels_triggers debug
    return triggers, embds_triggers, labels_triggers, args, embds_args, labels_args, label_arg_for_each_trig


def create_base_network(input_dim, nb_classes):
    '''Base network to be shared (eq. to feature extraction).
    '''    
    sgd = optimizers.SGD(lr=0.01, clipnorm=1.)
    sgd = optimizers.SGD(lr=0.01, momentum=0.05, decay=0.0, nesterov=True)
    rmsprop = optimizers.RMSprop(lr=0.001, rho=0.9, epsilon=None, decay=0.0)
    adagrad = optimizers.Adagrad(lr=0.01, epsilon=None, decay=0.0)
    adadelta = optimizers.Adadelta(lr=1.0, rho=0.95, epsilon=None, decay=0.0)
    adam = optimizers.Adam(lr=0.001, beta_1=0.9, beta_2=0.999, epsilon=None, decay=0.0, amsgrad=False)
    adamax = optimizers.Adamax(lr=0.002, beta_1=0.9, beta_2=0.999, epsilon=None, decay=0.0)
    nadam = optimizers.Nadam(lr=0.002, beta_1=0.9, beta_2=0.999, epsilon=None, schedule_decay=0.004)
    
    N_nodes = input_dim
    r_droupout = 0.05
    model_base = Sequential()
    model_base.add(Dense(N_nodes, input_shape=(input_dim,)))
    model_base.add(Activation('relu'))
    model_base.add(Dropout(r_droupout))
    #N_nodes = int(np.floor(N_nodes/2))
    model_base.add(Dense(N_nodes))
    model_base.add(Activation('relu'))
    model_base.add(Dropout(r_droupout))
    #N_nodes = int(np.floor(N_nodes/2))
    model_base.add(Dense(N_nodes))
    model_base.add(Activation('relu'))
    model_base.add(Dropout(r_droupout))
    #N_nodes = int(np.floor(N_nodes/2))
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
                       optimizer=sgd,
                       metrics=['accuracy'])
    #model_base.load_weights('model_base.h5')
    return model_base


def fit_on_data(wordsvec='NULL', wordslabel='NULL', model=0, encoder=0, learning_rate = 0.001, N_batch = 4, N_epoch = 16, en_verbose = 0):
    '''
    fit the model on given data
    '''
    print('='*65,'\n>>fit the model on given data, learning_rate:{}, N_batch:{}, N_epoch:{}'.format(learning_rate, N_batch, N_epoch))
    # wordsvec from list to array
    wordsvec = np.asarray(wordsvec)
    print('samples shape:', wordsvec.shape)
    print('labels number:', len(set(wordslabel)), set(wordslabel))
    
    classesnames = set(wordslabel)
    classweight = dict([(i, 1) for i in range(len(classesnames))])
    
    # encode class values as integers
    #encoder = LabelEncoder()
    #encoder.fit(wordslabel)
    Y_encoder = encoder.transform(wordslabel)    
    for idx in range(len(classesnames)):
        classweight[idx] = 10*(1 - float(len(Y_encoder[np.where(Y_encoder==idx)])) / float(len(Y_encoder)))
        
    # convert integers to dummy variables (i.e. one hot encoded)
    Y_encoder = np_utils.to_categorical(Y_encoder)
    
    #X_train, X_test, Y_train, Y_test = train_test_split(wordsvec, Y_encoder, random_state=0)
    X_train, X_test, Y_train, Y_test = wordsvec, wordsvec, Y_encoder, Y_encoder
    #X_train, Y_train  = wordsvec, Y_encoder
    
    # model define
    #input_dim = wordsvec.shape[1]
    #N_classes = len(set(wordslabel))    
    #model = create_base_network(input_dim, N_classes)     
    sgd = optimizers.SGD(lr=learning_rate, clipnorm=1.)
    sgd = optimizers.SGD(lr=learning_rate, momentum=0.05, decay=0.0, nesterov=True)
    #rmsprop = optimizers.RMSprop(lr=0.001, rho=0.9, epsilon=None, decay=0.0)
    adagrad = optimizers.Adagrad(lr=0.01, epsilon=None, decay=0.0)
    adadelta = optimizers.Adadelta(lr=1.0, rho=0.95, epsilon=None, decay=0.0)
    adam = optimizers.Adam(lr=0.001, beta_1=0.9, beta_2=0.999, epsilon=None, decay=0.0, amsgrad=False)
    adamax = optimizers.Adamax(lr=0.002, beta_1=0.9, beta_2=0.999, epsilon=None, decay=0.0)
    nadam = optimizers.Nadam(lr=0.002, beta_1=0.9, beta_2=0.999, epsilon=None, schedule_decay=0.004)
    rmsprop = optimizers.RMSprop(lr=learning_rate, rho=0.9, epsilon=None, decay=0)
    model.compile(loss='categorical_crossentropy',
                  optimizer=sgd,
                  metrics=['accuracy'])
    #model.summary()
    
    # model training
    start   = time.time()
    his = model.fit(X_train, Y_train,
                    batch_size=N_batch, epochs=N_epoch,
                    verbose=en_verbose, validation_data=(X_test, Y_test),
                    class_weight = classweight)
    end     = time.time()
    print('time elapse on training:\t', end - start, 'sec')
    return model, encoder, his


import itertools
import numpy as np
from sklearn.metrics import confusion_matrix
import matplotlib
import matplotlib.pyplot as plt
from matplotlib.font_manager import * 
##查看系统中文字体命令: $> fc-list :lang=zh
#定义自定义字体，文件名从1.b查看系统中文字体中来 
myfont = FontProperties(fname='/usr/share/fonts/opentype/noto/NotoSansCJK-Regular.ttc')  
#解决负号'-'显示为方块的问题  
matplotlib.rcParams['axes.unicode_minus']=False  


def plot_confusion_matrix(cm, classes,
                          normalize=False,
                          title='Confusion matrix',
                          cmap=plt.cm.Blues):
    """
    This function prints and plots the confusion matrix.
    Normalization can be applied by setting `normalize=True`.
    """
    if normalize:
        cm = cm.astype('float') / cm.sum(axis=1)[:, np.newaxis]
        print("Normalized confusion matrix")
    else:
        print('Confusion matrix, without normalization')
    #print(cm)
    
    #plt.figure()
    plt.imshow(cm, interpolation='nearest', cmap=cmap)
    plt.title(title)
    #plt.title(u'中文标题',fontproperties=myfont)
    plt.colorbar()
    tick_marks = np.arange(len(classes))
    plt.xticks(tick_marks, classes, rotation=90, fontproperties=myfont)
    plt.yticks(tick_marks, classes, fontproperties=myfont)

    fmt = '.2f' if normalize else 'd'
    thresh = cm.max() / 2.
    for i, j in itertools.product(range(cm.shape[0]), range(cm.shape[1])):
        plt.text(j, i, format(cm[i, j], fmt),
                 horizontalalignment="center",
                 color="white" if cm[i, j] > thresh else "black")

    plt.ylabel('True label',fontproperties=myfont)
    plt.xlabel('Predicted label',fontproperties=myfont)
    #plt.tight_layout()
    
def print_all_array(x):
    for i in range(len(x)):
        print([y for y in x[i]])

def generate_confusion_matrix(labels_true, labels_pre, label_set):
    cnf_matrix = confusion_matrix(labels_true, labels_pre)
    #print(np.asarray(cnf_matrix))
    print_all_array(np.asarray(cnf_matrix))
    np.set_printoptions(precision=2)
    if True:
        fig =  plt.figure(figsize=(40,40))
        plot_confusion_matrix(cnf_matrix, classes=label_set)
        plt.show(block = False)
        fig.savefig('./save/'+ time.asctime()+'-count.jpg')
        plt.close()
        fig =  plt.figure(figsize=(40,40))
        plot_confusion_matrix(cnf_matrix, classes=label_set, normalize=True, title='Normalized confusion matrix')
        plt.show(block = False)
        fig.savefig('./save/'+ time.asctime()+'-norm.jpg')
        plt.close()
    
    
def test_on_data(model, encoder, wordsvec, wordslabel, en_verbose=0):    
    print('='*65,'\n>>test the model on given data:')
    # wordsvec from list to array
    wordsvec = np.asarray(wordsvec)
    print('samples: {}, {} labels: {}'.format(wordsvec.shape, len(set(wordslabel)), set(wordslabel)))
    
    # encode class values as integers
    Y_encoder = encoder.transform(wordslabel)
    # convert integers to dummy variables (i.e. one hot encoded)
    Y_encoder = np_utils.to_categorical(Y_encoder)
    
    # model test
    print('>>testing')
    probs = model.predict(wordsvec, verbose=en_verbose)
    print(probs.shape)
    
    idxs = np.argmax(probs, axis=1)
    labels_pre = encoder.inverse_transform(idxs)
    labels_true = wordslabel
    labels = []
    for idx in range(len(set(labels_true))):
        labels.append(encoder.inverse_transform(idx))
    generate_confusion_matrix(labels_true, labels_pre, labels)
 
    # model eval
    print('>>evaluating')
    #Returns the loss value & metrics values for the model in test mode.
    [loss, metrics] = model.evaluate(x=wordsvec, y=Y_encoder, verbose=en_verbose)
    print('loss : ', loss)
    print(model.metrics[0], ':', metrics)
    return metrics

#     labels_class = []
#     for label_i in range(len(labels_true)):
#         if labels_true[label_i] not in labels_class:
#             labels_class.append(labels_true[label_i])


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
            if (idx_tokens_trigger[temp_idx+1] == idx_tokens_trigger[temp_idx] + 1):#连续的tokens被标记为相同的label
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
    ann = ''
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


def event_extract_kzg(text, model_trigger, encoder_trigger, model_arg, encoder_arg, trigger_emb, 
                      arg_emb, true_trig_labels, true_arg_labels_list, bc):
#     words, wordsvec = get_embd([text], bc)
    words, wordsvec = get_embd([text], bc)
    # wordsvec from list to array
    wordsvec = np.asarray(wordsvec)
    
    trigger_emb = np.asarray(trigger_emb)
    probs = model_trigger.predict(trigger_emb)
    idxs = np.argmax(probs, axis=1)
    labels_trig = encoder_trigger.inverse_transform(idxs)
    pre_trig_labels = labels_trig
    
    
    event_match_strict_count = 0
    event_match_approx_count = 0
    for word_i in range(len(labels_trig)):

        #稍作修改以便调试
#         if pre_trig_labels[word_i] != 'NULL' and pre_trig_labels[word_i] == true_trig_labels[word_i]:
        if pre_trig_labels[word_i] != 'NULL' and pre_trig_labels[word_i] == true_trig_labels[word_i]:
            
            #当true_trig_labels中不止一个trigger时,数清楚预测到的trigger是true_trig_labels中的第几个
            trigger_count = 0
            for word_idx_j in range(word_i):
                #事实上,这里还要考虑trigger为短语的情况,目前先不考虑,以后再改
                if true_trig_labels[word_idx_j] != 'NULL':
                    trigger_count += 1
            
            embds_args = []
            for idxarg in range(len(labels_trig)):
                if labels_trig[idxarg] == 'NULL':
                    temp_embds_arg = np.append(wordsvec[word_i], wordsvec[idxarg], axis=0)
                    embds_args.extend([temp_embds_arg])
            embds_args = np.asarray(embds_args)
            probs = model_arg.predict(embds_args)
            idxs = np.argmax(probs, axis=1)
            pre_arg_label = encoder_arg.inverse_transform(idxs)
            
            true_arg_label = true_arg_labels_list[trigger_count]
            
            
            arg_count = 0 
            for arg_i in range(len(true_arg_label)):
                if true_arg_label[arg_i] != 'NULL':
                    arg_count +=1
            
            strict_match_mark = True
            for arg_j in range(true_arg_label):
                if true_arg_label[arg_j] != pre_arg_label[arg_j]:
                    event_match_approx_count += 1
                    strict_match_mark = False
                
            if strict_match_mark:
                event_match_strict_count += 1
            
    return  event_match_strict_count, event_match_approx_count



