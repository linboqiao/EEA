import time
import numpy as np
from keras import backend as K
from keras.utils import np_utils
from keras.models import Sequential, Model
from keras.layers import Dense, Dropout, Input, Lambda, Activation
from keras.optimizers import SGD, Adam, RMSprop
from sklearn.preprocessing import LabelEncoder
from sklearn.model_selection import train_test_split
import matplotlib.pyplot as plt
from bert_serving.client import BertClient
from bratreader.repomodel import RepoModel

# 配置tensorflow利用显存方式
import tensorflow as tf
from keras.backend.tensorflow_backend import set_session
config = tf.ConfigProto()
#config.gpu_options.per_process_gpu_memory_fraction = 0.3
config.gpu_options.allow_growth=True 
#config.gpu_options.visible_device_list = "0"
set_session(tf.Session(config=config))


def create_base_network(input_dim, nb_classes):
    '''Base network to be shared (eq. to feature extraction).
    '''
    N_nodes = input_dim
    r_droupout = 0.2
    model_base = Sequential()
    model_base.add(Dense(N_nodes, input_shape=(input_dim,)))
    model_base.add(Activation('relu'))
    model_base.add(Dropout(r_droupout))
    model_base.add(Dense(N_nodes))
    model_base.add(Activation('relu'))
    model_base.add(Dropout(r_droupout))
    model_base.add(Dense(N_nodes))
    model_base.add(Activation('relu'))
    model_base.add(Dropout(r_droupout))
    model_base.add(Dense(nb_classes))
    model_base.add(Activation('softmax'))
    model_base.compile(loss='categorical_crossentropy',
                       optimizer=RMSprop(),
                       metrics=['accuracy'])
    #model_base.load_weights('model_base.h5')    
    return model_base

def words_vec_label(doc, bc): 
    '''get: words, embedding, spans, labels
    '''    
    words = []
    wordsvec = []
    spans = []
    wordslabel = []
    
    for str_sent in doc.text.splitlines():
        
        # Embeddings of each sentence/ sequence via BERT.
        vec = bc.encode([str_sent], show_tokens=True)
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
                    wordslabel.append(['NULL'])
                elif( vec[1][idx_sentence][idx_token].find('[SEP]', 0, 5)==0 ):
                    # [SEP]
                    words.append(vec[1][idx_sentence][idx_token])
                    wordsvec.append(vec[0][idx_sentence][idx_token][0:])
                    if len(spans)>0:
                        spans.append([spans[-1][1],spans[-1][1]])
                    else:
                        spans.append([0,0])
                    wordslabel.append(['NULL'])
                elif( vec[1][idx_sentence][idx_token].find('##', 0, 2)<0 ):
                    # Token in BERT table
                    words.append(vec[1][idx_sentence][idx_token])
                    wordsvec.append(vec[0][idx_sentence][idx_token][0:])                      
                    start = doc.text.lower().find(words[-1], spans[-1][0])
                    end = start + len(words[-1])
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

DIR_DATA = "./dataset/tmpbratfiles/"
NAME_FILE = "agm_briefing_unilever_11-05-2005"
def fit_on_data(dir_data=DIR_DATA, name_file=NAME_FILE):
    '''
    fit the model on given file with annotation 
    '''
    corpus = RepoModel(dir_data) # load corpus
    doc = corpus.documents[name_file] # get document with key
    bc = BertClient(ip='127.0.0.1', port=8701, port_out=8702, show_server_config=True) # bert model as service
    
    words, wordsvec, spans, wordslabel = words_vec_label(doc, bc)
    
    # wordsvec from list to array
    wordsvec = np.asarray(wordsvec)
    
    # label encoder
    wordslabel = [label[0] for label in wordslabel]
    # encode class values as integers
    encoder = LabelEncoder()
    encoder.fit(wordslabel)
    Y_encoder = encoder.transform(wordslabel)
    # convert integers to dummy variables (i.e. one hot encoded)
    Y_encoder = np_utils.to_categorical(Y_encoder)
    
    #X_train, X_test, Y_train, Y_test = train_test_split(wordsvec, Y_encoder, random_state=0)
    X_train, X_test, Y_train, Y_test  = wordsvec, wordsvec, Y_encoder, Y_encoder
    
    # model define
    N_batch = 4
    N_epoch = 4
    en_verbose = 1
    input_dim = wordsvec.shape[1]
    N_classes = len(set(wordslabel))
    
    model = create_base_network(X_train[0].shape[0], len(np.unique(wordslabel)))
    model.summary()
    
    # model training
    start   = time.time()
    history = model.fit(X_train, Y_train,
                        batch_size=N_batch, epochs=N_epoch,
                        verbose=en_verbose, validation_data=(X_test, Y_test))
    end     = time.time()
    print('time elapse training:\t', end - start, 'sec')
    return model

def probs_on_data_ann(dir_data=DIR_DATA, name_file=NAME_FILE):
    '''
    test the model on given file with annotation 
    '''
    # model test
    corpus = RepoModel(dir_data) # load corpus
    doc = corpus.documents[name_file] # get document with key
    bc = BertClient(ip='127.0.0.1', port=8701, port_out=8702, show_server_config=True) # bert model as service
    
    words, wordsvec, spans, wordslabel = words_vec_label(doc, bc)
    
    # wordsvec from list to array
    wordsvec = np.asarray(wordsvec)
    
    # label encoder
    wordslabel = [label[0] for label in wordslabel]
    # encode class values as integers
    encoder = LabelEncoder()
    encoder.fit(wordslabel)
    Y_encoder = encoder.transform(wordslabel)
    # convert integers to dummy variables (i.e. one hot encoded)
    Y_encoder = np_utils.to_categorical(Y_encoder)
    
    #X_train, X_test, Y_train, Y_test = train_test_split(wordsvec, Y_encoder, random_state=0)
    X_train, X_test, Y_train, Y_test  = wordsvec, wordsvec, Y_encoder, Y_encoder
    
    # model define
    N_batch = 4
    N_epoch = 4
    en_verbose = 1
    input_dim = wordsvec.shape[1]
    N_classes = len(set(wordslabel))
    
    model = create_base_network(X_train[0].shape[0], len(np.unique(wordslabel)))
    model.summary()
    
    # model training
    start   = time.time()
    probs = model.predict(X_test, verbose=1)
    end     = time.time()
    print('time elapse training:\t', end - start, 'sec')
    return probs



DIR_DATA = "./dataset/tmpbratfiles/"
NAME_FILE = "agm_briefing_unilever_11-05-2005"
fit_on_data(dir_data=DIR_DATA, name_file=NAME_FILE)
probs_on_data_ann(dir_data=DIR_DATA, name_file=NAME_FILE)
