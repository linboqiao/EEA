#！usr/bin/python
# -*- coding: utf-8 -*-
#==========================
import xml.etree.ElementTree as ET
import numpy as np
import os

def event2ann(event):
    '''
    single event (one trigger, multiple args) to ann
    '''
    EVENT_TYPE, CONTEXT, Trigger, spans_Trigger, Args, spans_Args, labels_Args = event
    ann = ''
    annT = ''
    annE = ''
    idxT = 0
    idxE = 0 
    
    idxT = idxT + 1
    idxE = idxE + 1
    label, span, text = EVENT_TYPE, spans_Trigger, Trigger
    annT = annT + 'T'+str(idxT)+'\t'+label+' '+str(span[0])+' '+str(span[1])+'\t'+text+ '\n' 
    annE = annE + 'E'+str(idxE)+'\t'+label+':T'+str(idxT)
    
    for idx_arg in range(len(labels_Args)):
        idxT = idxT + 1
        label, span, text = labels_Args[idx_arg], spans_Args[idx_arg], Args[idx_arg]
        annT = annT + 'T'+str(idxT)+'\t'+label+' '+str(span[0])+' '+str(span[1])+'\t'+text+ '\n' 
        annE = annE + ' '+label+':T'+str(idxT)
    ann = annT + annE
    return ann

def walkData(root_node, level, events, en_verbose): 
    '''
    visit all the nodes, parse event
    '''
    if root_node.tag == 'event':
        TYPE = root_node.attrib['TYPE']
        SUBTYPE = root_node.attrib['SUBTYPE']
        EVENT_TYPE = TYPE+'-'+SUBTYPE  # assign eventType
        idx_be = 0 
        for child in root_node:
            if child.tag == 'event_mention':
                EXTENT, LDC_SCOPE, CONTEXT, Trigger, spans_Trigger, Args, spans_Args, labels_Args = [], [], [], [], [], [], [], []
                for element in child:
                    if element.tag == 'extent':
                        EXTENT =  element[0].text
                        span = [element[0].attrib['START'], element[0].attrib['END']]
                        span = np.array([int(span[0]), int(span[1])+1]) #one char shift
                        
                        CONTEXT = EXTENT + ' ' # assign extent as context
                        CONTEXT = CONTEXT.replace('\n', ' ')
                        CONTEXT = CONTEXT.replace('\r', ' ')
                        idx_be = span[0]
                for element in child:
                    if element.tag == 'ldc_scope':
                        span = [element[0].attrib['START'], element[0].attrib['END']]
                        span = np.array([int(span[0]), int(span[1])+1]) - idx_be #one char shift
                        LDC_SCOPE = CONTEXT[span[0]:span[1]]                        
#                         continue # else use LDC_SCOPE as context
#                         CONTEXT = EXTENT # assign extent as context
#                         CONTEXT = CONTEXT.replace('\n', ' ')
#                         CONTEXT = CONTEXT.replace('\r', ' ')
#                         idx_be = span[0]
                    if element.tag == 'anchor':
                        span = [element[0].attrib['START'], element[0].attrib['END']]
                        span = np.array([int(span[0]), int(span[1])+1]) - idx_be #one char shift
                        spans_Trigger = span
                        Trigger =  CONTEXT[span[0]:span[1]]
                    if element.tag == 'event_mention_argument':
                        span = [element[0][0].attrib['START'], element[0][0].attrib['END']]
                        span = np.array([int(span[0]), int(span[1])+1])- idx_be #one char shift
                        spans_Args.append(span)
                        labels_Args.append(element.attrib['ROLE'])
                        Args.append(CONTEXT[span[0]:span[1]])
                                        
                event = [EVENT_TYPE, CONTEXT, Trigger, spans_Trigger, Args, spans_Args, labels_Args]
                events.append(event)
                
                print(event) if en_verbose else None
                print('trigger:', spans_Trigger, Trigger, CONTEXT[spans_Trigger[0]: spans_Trigger[1] ]) if en_verbose else None
                for idx_arg in range(len(Args)):
                    print('arg:', spans_Args[idx_arg], Args[idx_arg], CONTEXT[spans_Args[idx_arg][0]: spans_Args[idx_arg][1]]) if en_verbose else None
                print('context', CONTEXT) if en_verbose else None  
        return
    
    #遍历每个子节点
    children_node = root_node.getchildren()
    if len(children_node) == 0:
        return
    for child in children_node:
        walkData(child, level + 1, events, en_verbose)
    return
  
# 从文件中读取数据 
def get_events(apfxmlfile, en_verbose):
    level = 1 #节点的深度从1开始
    events = []
    root = ET.parse(apfxmlfile).getroot()
    walkData(root, level, events, en_verbose)
    return events

# 将事件句子(上下文仅有一个event)转化为ann文件
def events2anns(DIR_STORE, UID, classes, events, en_verbose):
    idxE = UID
    clss = classes
    
    for event in events:
        idxE = idxE + 1
        context = event[1]
        file_name = DIR_STORE + str(idxE) + '.txt'
        print(file_name)
        with open(file_name, 'w') as f:
            f.write(context)
            print(context) if en_verbose else None
        ann = event2ann(event)
        file_name = DIR_STORE + str(idxE) + '.ann'
        # print(file_name)
        with open(file_name, 'w') as f:
            f.write(ann)
            print(ann) if en_verbose else None
#         try:    
#             if event[0] not in clss:
#                 clss.add(event[0])
#                 file_name = 'data_test/' + str(len(classes)-1) + '.txt'
#                 print(file_name)
#                 with open(file_name, 'w') as f:
#                     f.write(context)
#                     print(context) if en_verbose else None
#                 ann = event2ann(event)
#                 file_name = 'data_test/' + str(len(classes)-1) + '.ann'
#                 # print(file_name)
#                 with open(file_name, 'w') as f:
#                     f.write(ann)
#                     print(ann) if en_verbose else None
#         except:
#             pass
            
    return clss
        
    

if __name__ == '__main__':
        
    DIR_DATA = '/home/linbo/workspace/Datasets/LDCData/ACE0308/ace_2005/data/'
    langs = ['Chinese', 'English']
    chans = ['bc', 'bn', 'cts', 'nw', 'un', 'wl']
    forms = ['adj', 'fp1', 'fp2', 'timex2norm']
    UID = 0
    classes = set(' ')
    en_verbose = 0
    for lang in langs:
        DIR_STORE = 'data_ACE_' + lang + '/'
        for chan in chans:
            for form in forms:
                DIR_FILE =  DIR_DATA + lang + '/' + chan + '/'+ form + '/'
                if os.path.isdir(DIR_FILE):
                    APF_FILEs = os.listdir(DIR_FILE)
                    for idx in range(len(APF_FILEs)):
                        apfxml= DIR_FILE + APF_FILEs[idx]
                        if apfxml[-8:] == '.apf.xml':
                            print(apfxml)
                            events = get_events(apfxml, en_verbose)
                            classes = events2anns(DIR_STORE, UID, classes, events, en_verbose)
                            UID += len(events)
            





