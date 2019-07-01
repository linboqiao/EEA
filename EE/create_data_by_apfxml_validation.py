import os
import re
import xml.dom.minidom
import copy
from stanfordcorenlp import StanfordCoreNLP

validation_num = 10


nlp = StanfordCoreNLP(r'/mnt/stanford_nlp/stanford-corenlp-full-2018-02-27')
props = {'annotators': 'pos,ner', 'pipelineLanguage': 'en', 'outputFormat': 'text'}

xmldir = "/media/kzg/5bd8734f-987e-4749-aa04-b716d02eeec6/to kzg/EE-data/LDCData/ACE0308/ace_2005/data/English/"
file_need = "timex2norm"
files_source = ["bc", "bn", "cts", "nw", "un", "wl"]
# files_source = ["bc","bn","cts","nw","wl"]

train_file_set = []
dev_file_set = []
test_file_set = []

argument_train_file_set = []
argument_dev_file_set = []
argument_test_file_set = []

output_path = "/mnt/BERT/BERT-master/bert/glue_data/EE/argument_data_debug"
debug_path = "../b_1/argument_debug/"

if validation_num == 1:
    train_file = output_path + "/single_data/train.txt"
    dev_file = output_path + "/single_data/dev.txt"
    test_file = output_path + "/single_data/test.txt"


    if os.path.exists(train_file):
        os.remove(train_file)
    if os.path.exists(dev_file):
        os.remove(dev_file)
    if os.path.exists(test_file):
        os.remove(test_file)

    with open(train_file, 'a+')as f1:
        f1.write(
            'sentence_id' + '\t' + '/' + '\t' +  'token' + '\t' + 'token_mark' + '\t' + 'pos' + '\t' + 'event_type' + '\n')
        f1.write('-' * 10 + '\n')
    with open(dev_file, 'a+')as f1:
        f1.write(
            'sentence_id' + '\t' + '/' + '\t' +  'token' + '\t' + 'token_mark' + '\t' + 'pos' + '\t' + 'event_type' + '\n')
        f1.write('-' * 10 + '\n')
    with open(test_file, 'a+')as f1:
        f1.write(
            'sentence_id' + '\t' + '/' + '\t' + 'token' + '\t' + 'token_mark' + '\t' + 'pos' + '\t' + 'event_type' + '\n')
        f1.write('-' * 10 + '\n')


    argument_train_file = output_path + "/single_data/argument_train.txt"
    argument_dev_file = output_path + "/single_data/argument_dev.txt"
    argument_test_file = output_path + "/single_data/argument_test.txt"

    if os.path.exists(argument_train_file):
        os.remove(argument_train_file)
    if os.path.exists(argument_dev_file):
        os.remove(argument_dev_file)
    if os.path.exists(argument_test_file):
        os.remove(argument_test_file)

    with open(argument_train_file, 'a+')as f1:
        f1.write(
            'sentence_id' + '\t' + '/' + '\t' +  'token' + '\t' + 'token_mark' + '\t' + 'pos' + '\t'
            + '1_mark' + '\t' + 'argument_role_1' + '\t' + 'trigger_for_this_sentence_1' + '\t' + 'event_type_1'
            + '2_mark' + '\t' + 'argument_role_2' + '\t' + 'trigger_for_this_sentence_2' + '\t' + 'event_type_2'
            + '3_mark' + '\t' + 'argument_role_3' + '\t' + 'trigger_for_this_sentence_3' + '\t' + 'event_type_3'
            + '\t' + 'trigger' + '\t' + 'argument_role' '\n')
        f1.write('-' * 10 + '\n')
    with open(argument_dev_file, 'a+')as f1:
        f1.write(
            'sentence_id' + '\t' + '/' + '\t' + 'token' + '\t' + 'token_mark' + '\t' + 'pos' + '\t'
            + '1_mark' + '\t' + 'argument_role_1' + '\t' + 'trigger_for_this_sentence_1' + '\t' + 'event_type_1'
            + '2_mark' + '\t' + 'argument_role_2' + '\t' + 'trigger_for_this_sentence_2' + '\t' + 'event_type_2'
            + '3_mark' + '\t' + 'argument_role_3' + '\t' + 'trigger_for_this_sentence_3' + '\t' + 'event_type_3'
            + '\t' + 'trigger' + '\t' + 'argument_role' '\n')
        f1.write('-' * 10 + '\n')
    with open(argument_test_file, 'a+')as f1:
        f1.write(
            'sentence_id' + '\t' + '/' + '\t' + 'token' + '\t' + 'token_mark' + '\t' + 'pos' + '\t'
            + '1_mark' + '\t' + 'argument_role_1' + '\t' + 'trigger_for_this_sentence_1' + '\t' + 'event_type_1'
            + '2_mark' + '\t' + 'argument_role_2' + '\t' + 'trigger_for_this_sentence_2' + '\t' + 'event_type_2'
            + '3_mark' + '\t' + 'argument_role_3' + '\t' + 'trigger_for_this_sentence_3' + '\t' + 'event_type_3'
            + '\t' + 'trigger' + '\t' + 'argument_role' '\n')
        f1.write('-' * 10 + '\n')

else:
    for i1 in range(validation_num):

        train_file = output_path + '/data_' + str(i1) + '/'+ "train.txt"
        dev_file = output_path + '/data_' + str(i1) + '/' + "dev.txt"
        test_file = output_path + '/data_' + str(i1) + '/' + "test.txt"

        train_file_set.append(train_file)
        dev_file_set.append(dev_file)
        test_file_set.append(test_file)



        if os.path.exists(train_file):
            os.remove(train_file)
        if os.path.exists(dev_file):
            os.remove(dev_file)
        if os.path.exists(test_file):
            os.remove(test_file)
        if os.path.exists(output_path + '/data_' + str(i1) + '/'+"event_type_num_statistics.txt"):
            os.remove(output_path + '/data_' + str(i1) + '/'+"event_type_num_statistics.txt")
        if os.path.exists(debug_path + "trigger_write_error.txt"):
            os.remove(debug_path + "trigger_write_error.txt")
        if os.path.exists(debug_path + "argument_has_2_role_s.txt"):
            os.remove(debug_path + "argument_has_2_role_s.txt")

        with open(train_file, 'a+')as f1:
            f1.write(
                'sentence_id' + '\t' + '/' + '\t' +  'token' + '\t' + 'token_mark' + '\t' + 'pos' + '\t' + 'event_type' + '\n')
            f1.write('-' * 10 + '\n')
        with open(dev_file, 'a+')as f1:
            f1.write(
                'sentence_id' + '\t' + '/' + '\t' +  'token' + '\t' + 'token_mark' + '\t' + 'pos' + '\t' + 'event_type' + '\n')
            f1.write('-' * 10 + '\n')
        with open(test_file, 'a+')as f1:
            f1.write(
                'sentence_id' + '\t' + '/' + '\t' + 'token' + '\t' + 'token_mark' + '\t' + 'pos' + '\t' + 'event_type' + '\n')
            f1.write('-' * 10 + '\n')


        argument_train_file = output_path + '/data_' + str(i1) + '/'+ "argument_train.txt"
        argument_dev_file = output_path + '/data_' + str(i1) + '/' + "argument_dev.txt"
        argument_test_file = output_path + '/data_' + str(i1) + '/' + "argument_test.txt"

        argument_train_file_set.append(argument_train_file)
        argument_dev_file_set.append(argument_dev_file)
        argument_test_file_set.append(argument_test_file)

        debug_path = "../b_1/argument_debug/"

        if os.path.exists(argument_train_file):
            os.remove(argument_train_file)
        if os.path.exists(argument_dev_file):
            os.remove(argument_dev_file)
        if os.path.exists(argument_test_file):
            os.remove(argument_test_file)
        if os.path.exists(output_path + '/data_' + str(i1) + '/'+"argument_role_num_statistics.txt"):
            os.remove(output_path + '/data_' + str(i1) + '/'+"argument_role_num_statistics.txt")
        if os.path.exists(debug_path + "argument_write_error.txt"):
            os.remove(debug_path + "argument_write_error.txt")
        if os.path.exists(debug_path + "argument_has_2_role_s.txt"):
            os.remove(debug_path + "argument_has_2_role_s.txt")

        with open(argument_train_file, 'a+')as f1:
            f1.write(
                'sentence_id' + '\t' + '/' + '\t' +  'token' + '\t' + 'token_mark' + '\t' + 'pos' + '\t'
                + '1_mark' + '\t' + 'argument_role_1' + '\t' + 'trigger_for_this_sentence_1' + '\t' + 'event_type_1'
                + '2_mark' + '\t' + 'argument_role_2' + '\t' + 'trigger_for_this_sentence_2' + '\t' + 'event_type_2'
                + '3_mark' + '\t' + 'argument_role_3' + '\t' + 'trigger_for_this_sentence_3' + '\t' + 'event_type_3'
                + '\t' + 'trigger' + '\t' + 'argument_role' '\n')
            f1.write('-' * 10 + '\n')
        with open(argument_dev_file, 'a+')as f1:
            f1.write(
                'sentence_id' + '\t' + '/' + '\t' + 'token' + '\t' + 'token_mark' + '\t' + 'pos' + '\t'
                + '1_mark' + '\t' + 'argument_role_1' + '\t' + 'trigger_for_this_sentence_1' + '\t' + 'event_type_1'
                + '2_mark' + '\t' + 'argument_role_2' + '\t' + 'trigger_for_this_sentence_2' + '\t' + 'event_type_2'
                + '3_mark' + '\t' + 'argument_role_3' + '\t' + 'trigger_for_this_sentence_3' + '\t' + 'event_type_3'
                + '\t' + 'trigger' + '\t' + 'argument_role' '\n')
            f1.write('-' * 10 + '\n')
        with open(argument_test_file, 'a+')as f1:
            f1.write(
                'sentence_id' + '\t' + '/' + '\t' + 'token' + '\t' + 'token_mark' + '\t' + 'pos' + '\t'
                + '1_mark' + '\t' + 'argument_role_1' + '\t' + 'trigger_for_this_sentence_1' + '\t' + 'event_type_1'
                + '2_mark' + '\t' + 'argument_role_2' + '\t' + 'trigger_for_this_sentence_2' + '\t' + 'event_type_2'
                + '3_mark' + '\t' + 'argument_role_3' + '\t' + 'trigger_for_this_sentence_3' + '\t' + 'event_type_3'
                + '\t' + 'trigger' + '\t' + 'argument_role' '\n')
            f1.write('-' * 10 + '\n')


def get_event_parments(xmldir, file_source, file_need, file_name):
    # 限定每个trigger的argument个数为三个
    argumennt_num_max = 15

    file_name = file_name + ".apf.xml"
    # file_name = xmldir + file_source +'/' + file_name + ".apf.xml"
    dom = xml.dom.minidom.parse(os.path.join(xmldir + file_source + '/' + file_need, file_name))
    root = dom.documentElement
    # extent = root.getElementsByTagName('extent')
    # print("extent:",extent)
    # extent0 = root.getElementsByTagName('extent')[0]
    #
    # extent1 = root.getElementsByTagName('extent')[1]
    # print("extent0:",extent0)
    # print("extent1:",extent1)

    # key = str(root.getElementsByTagName('extent')[i].firstChild.data)
    event_parameters_list = []
    objectlist = root.getElementsByTagName('event')
    for object in objectlist:
        TYPE = object.getAttribute("TYPE")
        SUBTYPE = object.getAttribute("SUBTYPE")
        event_argument_list = object.getElementsByTagName("event_argument")
        event_mention_list = object.getElementsByTagName("event_mention")
        # print("event_mention_list:",event_mention_list)
        for event_mention in event_mention_list:
            # print("event_mention:", event_mention)
            event_parameters = []
            for i4 in range(10):
                event_parameters.append([])
            event_extent_list = event_mention.getElementsByTagName("extent")
            ldc_scope_list = event_mention.getElementsByTagName("ldc_scope")
            anchor_list = event_mention.getElementsByTagName("anchor")
            event_mention_argument_list = event_mention.getElementsByTagName("event_mention_argument")
            event_final = []
            sentence_final = []
            trigger_final = []
            event_SUBTYPE_final = []
            argument_final = []
            argument_role_final = []
            argument_start_position_final = []
            argument_end_position_final = []
            sentence_start_final = []
            trigger_start_final = []
            # argument_start_final = []
            event_extent = event_extent_list[0]
            event_SUBTYPE_final.append(SUBTYPE)
            event = event_extent.getElementsByTagName("charseq")[0].firstChild.data
            event = str(event.replace('\n', ' '))
            event = event.replace('\t_', '  ')
            event = event.replace('_\t', '  ')
            event = event.replace(' _', '  ')
            event = event.replace('_ ', '  ')
            event_final.append(event)
            for ldc_scope in ldc_scope_list:
                sentence = ldc_scope.getElementsByTagName("charseq")[0].firstChild.data
                sentence = str(sentence.replace('\n', ' '))
                sentence = sentence.replace('\t', ' ')
                sentence = sentence.replace('\t_', '  ')
                sentence = sentence.replace('_\t', '  ')
                sentence = sentence.replace(' _', '  ')
                sentence = sentence.replace('_ ', '  ')
                sentence_final.append(sentence)
                sentence_start = ldc_scope.getElementsByTagName("charseq")[0].getAttribute("START")
                sentence_start_final.append(sentence_start)
            # print("sentence_final:",sentence_final)
            for anchor in anchor_list:
                trigger = anchor.getElementsByTagName("charseq")[0].firstChild.data
                trigger = str(trigger.replace('\n', ' '))
                trigger = trigger.replace('\t', ' ')
                trigger = trigger.replace('\t_', '  ')
                trigger = trigger.replace('_\t', '  ')
                trigger = trigger.replace(' _', '  ')
                trigger = trigger.replace('_ ', '  ')
                trigger_final.append(trigger)
                trigger_start = anchor.getElementsByTagName("charseq")[0].getAttribute("START")
                trigger_start_final.append(trigger_start)
            # 限定argument的值为argumennt_num_max个
            len_event_mention_argument_list = len(event_mention_argument_list)
            if len_event_mention_argument_list > argumennt_num_max:
                event_mention_argument_list = event_mention_argument_list[:argumennt_num_max]

            for event_mention_argument in event_mention_argument_list:
                argument = event_mention_argument.getElementsByTagName("extent")[0].getElementsByTagName("charseq")[
                    0].firstChild.data
                argument = str(argument.replace('\n', ' '))
                argument = argument.replace('\t', ' ')
                argument = argument.replace('_\t', '  ')
                argument = argument.replace('\t_', '  ')
                argument = argument.replace(' _', '  ')
                argument = argument.replace('_ ', '  ')
                argument_final.append(argument)
                # argument_start = event_mention_argument.getElementsByTagName("extent")[0].getElementsByTagName("charseq")[0].getAttribute("START")
                # argument_start_final.append(argument_start)

                argument_role = event_mention_argument.getAttribute("ROLE")
                argument_role_final.append(argument_role)
                argument_start_position = \
                    event_mention_argument.getElementsByTagName("extent")[0].getElementsByTagName("charseq")[
                        0].getAttribute("START")
                argument_end_position = \
                    event_mention_argument.getElementsByTagName("extent")[0].getElementsByTagName("charseq")[
                        0].getAttribute("END")
                argument_start_position_final.append(argument_start_position)
                argument_end_position_final.append(argument_end_position)

            len_argument_start_position_final = len(argument_start_position_final)
            for j in range(len_argument_start_position_final):
                argument_start_position_final[j] = str(eval(argument_start_position_final[j]) - eval(sentence_start))
                argument_end_position_final[j] = str(eval(argument_end_position_final[j]) - eval(sentence_start))

            # 强行将argument变成argumennt_num_max个

            len_event_mention_argument_list = len(event_mention_argument_list)
            for num_i in range(argumennt_num_max - len_event_mention_argument_list):
                argument_final.append("None")
                argument_role_final.append("None")
                argument_start_position_final.append("None")
                argument_end_position_final.append("None")

            len_event_parameters_list = len(event_parameters_list)
            find_sentence_mark = False
            sentence_id = 0
            for i1 in range(len_event_parameters_list):
                # print("sentence_final[0]:", sentence_final[0])
                # print("event_parameters_list[i1][0]:", event_parameters_list[i1][0])

                if sentence_final[0] in event_parameters_list[i1][0][0]:
                    # print("sentence_final[0]:", sentence_final[0])
                    sentence_id = i1
                    find_sentence_mark = True
                    break
            if find_sentence_mark:
                event_parameters_list[sentence_id][1].append(event_final)
                event_parameters_list[sentence_id][2].append(trigger_final)
                event_parameters_list[sentence_id][3].append(event_SUBTYPE_final)
                event_parameters_list[sentence_id][4].append(argument_final)
                event_parameters_list[sentence_id][5].append(argument_role_final)
                event_parameters_list[sentence_id][6].append(sentence_start_final)
                event_parameters_list[sentence_id][7].append(trigger_start_final)
                event_parameters_list[sentence_id][8].append(argument_start_position_final)
                event_parameters_list[sentence_id][9].append(argument_end_position_final)
            else:
                event_parameters[0].append(sentence_final)
                event_parameters[1].append(event_final)
                event_parameters[2].append(trigger_final)
                event_parameters[3].append(event_SUBTYPE_final)
                event_parameters[4].append(argument_final)
                event_parameters[5].append(argument_role_final)
                event_parameters[6].append(sentence_start_final)
                event_parameters[7].append(trigger_start_final)
                event_parameters[8].append(argument_start_position_final)
                event_parameters[9].append(argument_end_position_final)

                # print("event_parameters:",event_parameters)

                event_parameters_list.append(event_parameters)

    # print("event_parameters_list:",event_parameters_list)
    return event_parameters_list


def is_triggerToken_in_sentenceToken(trigger_token, sentence_token):
    len_trigger_token = len(trigger_token)
    len_sentence_token = len(sentence_token)
    if len_sentence_token == len_trigger_token:
        if trigger_token == sentence_token:
            return True
        else:
            return False
    elif len_trigger_token < len_sentence_token:
        for sentence_token_i in range(len_sentence_token - len_trigger_token + 1):
            if trigger_token == sentence_token[sentence_token_i: sentence_token_i + len_trigger_token]:
                return True
        return False
    else:
        return False

def is_triggerToken_in_sentenceToken_dont_equal(trigger_token, sentence_token):
    len_trigger_token = len(trigger_token)
    len_sentence_token = len(sentence_token)
    if len_sentence_token == len_trigger_token:
            return False
    elif len_trigger_token < len_sentence_token:
        for sentence_token_i in range(len_sentence_token - len_trigger_token + 1):
            if trigger_token == sentence_token[sentence_token_i: sentence_token_i + len_trigger_token]:
                return True
        return False
    else:
        return False



def add_trigger(event_sentence_split, trigger_list_list, event_subtype_list_list,sentence_start_position,trigger_start_position_list_list, token_parament_to_write_list, file_source,
                file_name, event_sentence):
    res_parament_to_write_list_trigger = copy.deepcopy(token_parament_to_write_list)
    # print("trigger_id(res_parament_to_write_list):",id(res_parament_to_write_list_trigger))
    # print("trigger_id(token_parament_to_write_list):",id(token_parament_to_write_list))
    for trigger_list_id in range(len(trigger_list_list)):

        len_trigger_list = len(trigger_list_list[trigger_list_id])
        for trigger_i1 in range(len_trigger_list):

            # if trigger_list[trigger_i1] == "september 11th" or trigger_list[trigger_i1] == "this" or trigger_list[trigger_i1] == "it":
            #     print(file_source,"-->",file_name,"-->trigger:",trigger_list[trigger_i1])
            trigger = trigger_list_list[trigger_list_id][trigger_i1]
            trigger_tokens = trigger.split()
            len_trigger_tokens = len(trigger_tokens)
            len_event_sentence_split = len(event_sentence_split)

            triggr_like_count = 0
            find_trigger_position = False
            trigger_start = 0
            for token_i1 in range(len_event_sentence_split - len_trigger_tokens + 1):
                if trigger_tokens == event_sentence_split[token_i1: token_i1 + len_trigger_tokens]:
                    find_trigger_position = True
                    trigger_start = token_i1
                    triggr_like_count += 1
                    # break

            if not find_trigger_position:
                for token_i2 in range(len_event_sentence_split - len_trigger_tokens + 1):
                    find_mark = True
                    for trigger_token_j1 in range(len_trigger_tokens):
                        if not is_triggerToken_in_sentenceToken(trigger_tokens[trigger_token_j1],
                                                                event_sentence_split[token_i2 + trigger_token_j1]):
                            find_mark = False
                            break
                    if find_mark:
                        find_trigger_position = True
                        trigger_start = token_i2
                        triggr_like_count += 1



            if triggr_like_count > 1:

                all_trigger_like_count_by_char = 0

                trigger_start_position = trigger_start_position_list_list[trigger_list_id][trigger_i1]
                # print("trigger_start_position:",trigger_start_position)
                sentence_start_position = sentence_start_position
                len_trigger = len(trigger)
                trigger_like_token_position_list = []
                trigger_like_token_find = False
                for i5 in range(len(event_sentence) - len_trigger + 1):
                    # trigger_like_token_find = False
                    # for i6 in range(len(trigger)):
                    if trigger == event_sentence[i5 : i5 + len_trigger]:
                        trigger_like_token_position_list.append(i5)
                        trigger_like_token_find = True
                if not trigger_like_token_find or len(trigger_like_token_position_list) <2:
                    print("can't find trigger from trigger-like token:        ",trigger,"-->",event_sentence)

                get_trigger_mark = False
                trigger_to_find_num = -1

                for trigger_like_token_position_id in range(len(trigger_like_token_position_list)):
                    douhao_count = 0
                    for i6 in range(trigger_like_token_position_list[trigger_like_token_position_id]):
                        if event_sentence[i6] == ',':
                            douhao_count += 1


                    # print("liangzhezhicha:",eval(trigger_start_position) - eval(sentence_start_position))
                    # print("liangzhezhicha+douhao_count:",eval(trigger_start_position) - eval(sentence_start_position)+ douhao_count)
                    # print("juzizhong de weizhi:",trigger_like_token_position_list[trigger_like_token_position_id])

                    if trigger_like_token_position_list[trigger_like_token_position_id] == eval(trigger_start_position) - eval(sentence_start_position) + douhao_count:
                        get_trigger_mark = True
                        trigger_to_find_num = trigger_like_token_position_id


                if trigger_to_find_num == -1:
                    # print("*****************trigger_to_find_num == -1!!!")
                    print("*****************trigger_to_find_num == -1!!!:  ", file_source, "-->", file_name, "-->", trigger, "-->",
                          event_sentence)
                    print("trigger_tokens:",trigger_tokens)
                    # print("event_sentence_split[token_i3: token_i3 + len_trigger_tokens]:",event_sentence_split[token_i3: token_i3 + len_trigger_tokens])
                    break


                # print("trigger_to_find_num-->:",trigger,"-->",trigger_to_find_num)
                if get_trigger_mark:
                    trigger_to_find_count = 0
                    equal_mark = False
                    for token_i3 in range(len_event_sentence_split - len_trigger_tokens + 1):
                        if trigger_tokens == event_sentence_split[token_i3: token_i3 + len_trigger_tokens]:
                            if trigger_to_find_count == trigger_to_find_num:
                                trigger_start = token_i3
                                equal_mark = True
                                # print("trigger_to_find_count-->:",trigger,"-->",trigger_to_find_count)
                                # print("trigger_tokens:",trigger_tokens)
                                # print("event_sentence_split[token_i3: token_i3 + len_trigger_tokens]:",event_sentence_split[token_i3: token_i3 + len_trigger_tokens])
                                # print("messages:  ", file_source, "-->", file_name, "-->", trigger, "-->",
                                #       event_sentence)
                                break
                            else:
                                trigger_to_find_count += 1
                        #else 主要针对trigger为it,而有些词为it's的情况
                        else:
                            # for token_i4 in range(len_event_sentence_split - len_trigger_tokens + 1):
                            find_mark = True
                            for trigger_token_j3 in range(len_trigger_tokens):
                                if not is_triggerToken_in_sentenceToken_dont_equal(trigger_tokens[trigger_token_j3],
                                                                        event_sentence_split[token_i3 + trigger_token_j3]):
                                    find_mark = False
                                    break
                            if find_mark:
                                # print("!!!!!!!!!!!!!!!!!!!!!!!!!!")
                                if trigger_to_find_count == trigger_to_find_num:

                                    trigger_start = token_i3
                                    equal_mark = True

                                    # print("not_trigger_to_find_count-->:",trigger,"-->", trigger_to_find_count)
                                    # print("trigger_tokens:", trigger_tokens)
                                    # print("event_sentence_split[token_i3: token_i3 + len_trigger_tokens]:",
                                    #       event_sentence_split[token_i3: token_i3 + len_trigger_tokens])
                                    # print("trigger_start:", trigger_start)
                                    break

                                else:
                                    trigger_to_find_count += 1
                    # print("trigger_to_find_count:",trigger_to_find_count)

                    if not equal_mark:
                        print("!!!!!!!!!equal_mark false!!!",trigger,"-->",event_sentence)

                else:
                    print("can't find trigger by trigger-like token from sentence:        ",trigger,"-->",event_sentence)



            if find_trigger_position:
                for trigger_token_j2 in range(len_trigger_tokens):
                    if res_parament_to_write_list_trigger[trigger_start + trigger_token_j2][3] != "None":
                        # print("find same trigger-----trigger_start:", trigger_start)
                        if res_parament_to_write_list_trigger[trigger_start + trigger_token_j2][3] != event_subtype_list_list[trigger_list_id][trigger_i1]:
                            print("*****find same trigger:  ",file_source,"-->",file_name,"-->",trigger,"-->",event_sentence)
                    res_parament_to_write_list_trigger[trigger_start + trigger_token_j2][3] = event_subtype_list_list[trigger_list_id][trigger_i1]
            else:
                print("find error!")
                with open(debug_path + "trigger_write_error.txt", 'a+')as f1:
                    f1.write("error doc:" + file_source + "-->" + file_name + "**" * 20 + "\n")
                    f1.write("trigger：" + trigger + "\n" + "-" * 20 + "\n")
                    f1.write("sentence：\n" + event_sentence + "\n" + "-" * 20 + "\n")
                    f1.write("sentence_token：" + "\n")
                    for sentence_token_i2 in range(len(event_sentence_split)):
                        f1.write(event_sentence_split[sentence_token_i2] + '^')
                    f1.write("\n\n\n")
    # print("res_parament_to_write_list：",res_parament_to_write_list)
    return res_parament_to_write_list_trigger


def add_argument(event_sentence_split, argument_list, argument_role_list,sentence_start_position,argument_start_position_list ,trigger_1,trigger_1_event_type,token_parament_to_write_list, file_source,
                file_name, event_sentence,same_token_count,same_argument_count,all_argument_count,fu1_count):


    res_parament_to_write_list_argument = copy.deepcopy(token_parament_to_write_list)
    # for res_parament_to_write in res_parament_to_write_list_argument:
    #     res_parament_to_write[5] = trigger_1

    for token_parament_i in res_parament_to_write_list_argument:
        token_parament_i[5] = trigger_1
        token_parament_i[6] = trigger_1_event_type


    len_argument_list = len(argument_list)
    for argument_i1 in range(len_argument_list):
        same_mark = False
        argument = argument_list[argument_i1]

        if argument == "None":
            continue
        # if argument_tokens1 == "American shores":
        #     print("find!!!!!!!!!!!!!!1")
        all_argument_count += 1
        argument = argument.replace(',', ' ,')
        argument = argument.replace('\t', ' ')
        argument_tokens = argument.split()

        len_argument_tokens = len(argument_tokens)
        len_event_sentence_split = len(event_sentence_split)

        argument_like_count = 0
        find_argument_position = False
        argument_start = 0
        for token_i1 in range(len_event_sentence_split - len_argument_tokens + 1):
            if argument_tokens == event_sentence_split[token_i1: token_i1 + len_argument_tokens]:
                find_argument_position = True
                argument_start = token_i1
                argument_like_count += 1
                # break

        if not find_argument_position:
            for token_i2 in range(len_event_sentence_split - len_argument_tokens + 1):
                find_mark = True
                for argument_token_j1 in range(len_argument_tokens):
                    if not is_triggerToken_in_sentenceToken(argument_tokens[argument_token_j1],
                                                            event_sentence_split[token_i2 + argument_token_j1]):
                        find_mark = False
                        break
                if find_mark:
                    find_argument_position = True
                    argument_start = token_i2
                    argument_like_count += 1
                    # break


        if argument_like_count > 1:

            all_argument_like_count_by_char = 0

            argument_start_position = argument_start_position_list[argument_i1]
            # print("argument_start_position:",argument_start_position)
            sentence_start_position = sentence_start_position
            len_argument = len(argument)
            argument_like_token_position_list = []
            argument_like_token_find = False
            for i5 in range(len(event_sentence) - len_argument + 1):
                # argument_like_token_find = False
                # for i6 in range(len(argument)):
                if argument == event_sentence[i5: i5 + len_argument]:
                    argument_like_token_position_list.append(i5)
                    argument_like_token_find = True
            if not argument_like_token_find or len(argument_like_token_position_list) < 2:
                print("can't find argument from argument-like token:        ", argument, "-->", event_sentence)


            get_argument_mark = False
            argument_to_find_num = -1

            for argument_like_token_position_id in range(len(argument_like_token_position_list)):
                douhao_count = 0
                for i6 in range(argument_like_token_position_list[argument_like_token_position_id]):
                    if event_sentence[i6] == ',':
                        douhao_count += 1
                # print("argument_start_position:",argument_start_position)
                # print("sentence_start_position:",sentence_start_position)
                # print("liangzhezhicha  :", eval(argument_start_position) - eval(sentence_start_position))
                # print("zhenshijuzizhong de weizhi   ：",argument_like_token_position_list[argument_like_token_position_id])
                if argument_like_token_position_list[argument_like_token_position_id] == eval(argument_start_position)+ douhao_count:
                    get_argument_mark = True
                    argument_to_find_num = argument_like_token_position_id
                elif argument_like_token_position_list[argument_like_token_position_id] >= eval(argument_start_position) + douhao_count - 6 and \
                        argument_like_token_position_list[argument_like_token_position_id] <= eval(argument_start_position) + douhao_count + 6:
                    get_argument_mark = True
                    argument_to_find_num = argument_like_token_position_id
            if argument_to_find_num == -1:
                fu1_count += 1
                # print("liangzhezhicha  :",eval(argument_start_position) - eval(sentence_start_position))
                print("*****************argument_to_find_num == -1!!!:  ", file_source, "-->", file_name, "-->", argument,"-->",event_sentence)
                print("argument_tokens:", argument_tokens)
                # print("event_sentence_split[token_i3: token_i3 + len_argument_tokens]:",event_sentence_split[token_i3: token_i3 + len_argument_tokens])
                break

            # print("argument_to_find_num-->:",argument,"-->",argument_to_find_num)
            if get_argument_mark:
                argument_to_find_count = 0
                equal_mark = False
                for token_i3 in range(len_event_sentence_split - len_argument_tokens + 1):
                    if argument_tokens == event_sentence_split[token_i3: token_i3 + len_argument_tokens]:
                        if argument_to_find_count == argument_to_find_num:
                            argument_start = token_i3
                            equal_mark = True
                            # print("argument_to_find_count-->:",argument,"-->",argument_to_find_count)
                            # print("argument_tokens:",argument_tokens)
                            # print("event_sentence_split[token_i3: token_i3 + len_argument_tokens]:",event_sentence_split[token_i3: token_i3 + len_argument_tokens])
                            # print("messages:  ", file_source, "-->", file_name, "-->", argument, "-->",
                            #       event_sentence)
                            break
                        else:
                            argument_to_find_count += 1
                    # else 主要针对argument为it,而有些词为it's的情况
                    else:
                        # for token_i4 in range(len_event_sentence_split - len_argument_tokens + 1):
                        find_mark = True
                        for argument_token_j3 in range(len_argument_tokens):
                            if not is_triggerToken_in_sentenceToken(argument_tokens[argument_token_j3],
                                                                               event_sentence_split[
                                                                                   token_i3 + argument_token_j3]):
                                find_mark = False
                                break
                        if find_mark:
                            # print("!!!!!!!!!!!!!!!!!!!!!!!!!!")
                            if argument_to_find_count == argument_to_find_num:

                                argument_start = token_i3
                                equal_mark = True

                                # print("not_argument_to_find_count-->:",argument,"-->", argument_to_find_count)
                                # print("argument_tokens:", argument_tokens)
                                # print("event_sentence_split[token_i3: token_i3 + len_argument_tokens]:",
                                #       event_sentence_split[token_i3: token_i3 + len_argument_tokens])
                                # print("argument_start:", argument_start)
                                break

                            else:
                                argument_to_find_count += 1
                # print("argument_to_find_count:",argument_to_find_count)

                if not equal_mark:
                    print("!!!!!!!!!equal_mark false!!!", argument, "-->", event_sentence)

            else:
                print("can't find argument by argument-like token from sentence:        ", argument, "-->", event_sentence)


        if find_argument_position:
            argument_role_count = 0
            for argument_token_j4 in range(len_argument_tokens):
                if res_parament_to_write_list_argument[argument_start + argument_token_j4][3] != '0':
                    argument_role_count = 1
                    break
            for argument_token_j5 in range(len_argument_tokens):
                if res_parament_to_write_list_argument[argument_start + argument_token_j5][7] != '0':
                    argument_role_count = 2
                    break


            for argument_token_j2 in range(len_argument_tokens):

                if argument_role_count == 0:
                    # argument_num = eval(res_parament_to_write_list[argument_start + argument_token_j2][4]) + 1
                    # res_parament_to_write_list[argument_start + argument_token_j2][4] = str(argument_num)
                    res_parament_to_write_list_argument[argument_start + argument_token_j2][4] = argument_role_list[argument_i1]
                    res_parament_to_write_list_argument[argument_start + argument_token_j2][5] = trigger_1
                    res_parament_to_write_list_argument[argument_start + argument_token_j2][6] = trigger_1_event_type
                    res_parament_to_write_list_argument[argument_start + argument_token_j2][3] = "argument_role_count1"
                elif argument_role_count == 1:
                    res_parament_to_write_list_argument[argument_start + argument_token_j2][8] = argument_role_list[argument_i1]
                    res_parament_to_write_list_argument[argument_start + argument_token_j2][9] = trigger_1
                    res_parament_to_write_list_argument[argument_start + argument_token_j2][10] = trigger_1_event_type
                    res_parament_to_write_list_argument[argument_start + argument_token_j2][7] = "argument_role_count2"
                elif argument_role_count == 2:
                    res_parament_to_write_list_argument[argument_start + argument_token_j2][12] = argument_role_list[argument_i1]
                    res_parament_to_write_list_argument[argument_start + argument_token_j2][13] = trigger_1
                    res_parament_to_write_list_argument[argument_start + argument_token_j2][14] = trigger_1_event_type
                    res_parament_to_write_list_argument[argument_start + argument_token_j2][11] = "argument_role_count3"
                    # print("*****count3!!!:  ", file_source, "-->", file_name, "-->",
                    #       argument, "-->", event_sentence)
                else:
                    # print("res_parament_to_write_list[argument_start + argument_token_j2][4]:",res_parament_to_write_list[argument_start + argument_token_j2][4])
                    len_event_sentence_splitl = True
                    same_mark = True
                    same_token_count += 1
                    # print("!!!!same argument!:",file_source, "-->", file_name, "-->", argument,"-->",event_sentence)
                    # print("res_parament_to_write_list[argument_start + argument_token_j2][0]:",res_parament_to_write_list[argument_start + argument_token_j2][0])

        else:
            print("argument find error! can't find argument")
            with open(debug_path + "argument_write_error.txt", 'a+')as f1:
                f1.write("error doc:" + file_source + "-->" + file_name + "**" * 20 + "\n")
                f1.write("argument：" + argument_list[argument_i1] + "\n" + "-" * 20 + "\n")
                f1.write("sentence：\n" + event_sentence + "\n" + "-" * 20 + "\n")
                f1.write("sentence_token：" + "\n")
                for sentence_token_i2 in range(len(event_sentence_split)):
                    f1.write(event_sentence_split[sentence_token_i2] + '^')
                f1.write("\n\n\n")

        if same_mark:
            same_argument_count += 1
            # print("!!!!same argument!:",file_source, "-->", file_name, "-->", argument,"-->",event_sentence)
            with open(debug_path + "argument_has_2_role_s.txt", 'a+')as f1:
                for token_parament_to_write in res_parament_to_write_list_argument:
                    for parament_i in token_parament_to_write:
                        f1.write(parament_i + '\t')
                    f1.write('\n')
                f1.write('-' * 10 + '\n')
                f1.write("argument:  " + argument + '\n')
                f1.write("argument_role:  "+ argument_role_list[argument_i1])
                f1.write("\n" + '-' * 10 + '\n' * 3)

    return res_parament_to_write_list_argument,same_token_count,same_argument_count,all_argument_count,fu1_count


def get_pos(token, nlp_res):
    contect_split = nlp_res.split("Text=" + token)
    if len(contect_split) == 1:
        # print("can't find token:  " + token)
        # print (nlp_res)
        res_pos = "None"
    else:
        contect_split_1 = contect_split[1]
        contect_split_2 = contect_split_1.split("PartOfSpeech=")[1]
        res_pos = contect_split_2.split(']')[0]
    return res_pos


def get_parament_to_write(file_source, file_name,event_parameters_list,same_token_count, same_argument_count,all_argument_count,fu1_count):
    doc_level_token_parament_to_write_list_for_trigger = []
    doc_level_token_parament_to_write_list_for_argument = []
    # print("event_parameters_list:",event_parameters_list)
    for event_i in event_parameters_list:

            # print("event_i:",event_i)
            event_sentence = event_i[0][0][0]
            trigger_list_list = event_i[2]
            event_subtype_list_list = event_i[3]
            argument_list_list = event_i[4]
            argument_role_list_list = event_i[5]
            sentence_start_position = event_i[6][0][0]
            trigger_start_position_list_list = event_i[7]
            argument_start_position_list_list = event_i[8]

            # all_argument_count += len(argument_list)


            argument_role_all.extend(event_i[5])
            argument_token_parament_to_write_list = []
            trigger_token_parament_to_write_list = []

            event_sentence = event_sentence.replace(',', ' ,')
            event_sentence = event_sentence.replace('\t', ' ')
            event_sentence_split = re.split(r'[ ]', event_sentence)
            label_i3 = 0
            while label_i3 < len(event_sentence_split):
                if len(event_sentence_split[label_i3]) == 0:
                    event_sentence_split.pop(label_i3)
                else:
                    label_i3 += 1

            len_event_sentence_split = len(event_sentence_split)



            for token_i2 in range(len_event_sentence_split):
                argument_token_parament_to_write_list.append([])
                argument_token_parament_to_write_list[token_i2].append(event_sentence_split[token_i2])
                argument_token_parament_to_write_list[token_i2].append("token")

                for i2 in range(13):
                    argument_token_parament_to_write_list[token_i2].append("None")
                argument_token_parament_to_write_list[token_i2][3] = "0"
                argument_token_parament_to_write_list[token_i2][7] = "0"
                argument_token_parament_to_write_list[token_i2][11] = "0"

            for token_i3 in range(len_event_sentence_split):
                trigger_token_parament_to_write_list.append([])
                trigger_token_parament_to_write_list[token_i3].append(event_sentence_split[token_i3])
                trigger_token_parament_to_write_list[token_i3].append("token")

                for i4 in range(2):
                    trigger_token_parament_to_write_list[token_i3].append("None")



                # use stanford corenlp to get pos
                props_token = {'annotators': 'pos', 'pipelineLanguage': 'en', 'outputFormat': 'text'}
                nlp_res_token = nlp.annotate(event_sentence_split[token_i3], properties=props_token)
                pos_1 = nlp_res_token.split("PartOfSpeech=")[1]
                pos_1 = pos_1.split(']')[0]
                trigger_token_parament_to_write_list[token_i3][2] = pos_1
                argument_token_parament_to_write_list[token_i3][2] = pos_1

                props_sentence = {'annotators': 'pos', 'pipelineLanguage': 'en', 'outputFormat': 'text'}
                nlp_res_sentence = nlp.annotate(event_sentence, properties=props_sentence)

                pos = get_pos(event_sentence_split[token_i3], nlp_res_sentence)
                if pos != "None":
                    trigger_token_parament_to_write_list[token_i3][2] = pos
                    argument_token_parament_to_write_list[token_i3][2] = pos
                # 一定要记得关闭！！！！

            token_parament_to_write_list_for_trigger = add_trigger(event_sentence_split, trigger_list_list, event_subtype_list_list,
                                                                   sentence_start_position,trigger_start_position_list_list,trigger_token_parament_to_write_list,
                                                                   file_source, file_name, event_sentence)
            doc_level_token_parament_to_write_list_for_trigger.append(token_parament_to_write_list_for_trigger)

            for trigger_list_id in range(len(trigger_list_list)):
                if len(trigger_list_list[trigger_list_id]) > 1:
                    print("!!!!!!!!!!!!!!!!!!!!!!!!!!trigger_list_list[trigger_list_id]>1!!!!!!!!!!!!!!!")
                for trigger_1_id in range(len(trigger_list_list[trigger_list_id])):
                    trigger_1 = trigger_list_list[trigger_list_id][trigger_1_id]
                    trigger_1_event_type = event_subtype_list_list[trigger_list_id][trigger_1_id]
                    token_parament_to_write_list_for_argument, same_token_count, same_argument_count,all_argument_count,fu1_count =add_argument(event_sentence_split,
                                            argument_list_list[trigger_list_id], argument_role_list_list[trigger_list_id],sentence_start_position,argument_start_position_list_list[trigger_list_id], trigger_1,trigger_1_event_type,argument_token_parament_to_write_list, file_source,
                                            file_name, event_sentence, same_token_count, same_argument_count,all_argument_count,fu1_count)


                    doc_level_token_parament_to_write_list_for_argument.append(token_parament_to_write_list_for_argument)


    return  doc_level_token_parament_to_write_list_for_trigger,doc_level_token_parament_to_write_list_for_argument,same_token_count, same_argument_count,all_argument_count,fu1_count

def write_parament_into_file(doc_level_token_parament_to_write_list,train_file,sentence_count):
    # sentence_count = 0
    for token_parament_to_write_list in doc_level_token_parament_to_write_list:
        with open(train_file, 'a+')as f1:
            f1.write(str(sentence_count) + '\n')
            for token_parament_to_write in token_parament_to_write_list:
                for parament_i in token_parament_to_write:
                    f1.write(parament_i + '\t')
                f1.write('\n')
            f1.write('-' * 10 + '\n')
        sentence_count += 1

    return sentence_count


id_j_set = []
# id_k_set = []
id_m_set = []

file_num = 0
argument_role_all = []

#tongji argument zhong yi ge token you liang ge label de ge shu
same_token_count, same_argument_count,all_argument_count,fu1_count  = 0,0,0,0


# 用来统计事件类型的数量
event_type_all_train_set = []
event_type_count_train_set = []
# event_type_all_dev_set = []
# event_type_count_dev_set = []
event_type_all_test_set = []
event_type_count_test_set = []
for i3 in range(validation_num):
    event_type_all_train_set.append([])
    event_type_count_train_set.append([])
    # event_type_all_dev_set.append([])
    # event_type_count_dev_set.append([])
    event_type_all_test_set.append([])
    event_type_count_test_set.append([])

    id_j_set.append(0)
    # id_k_set.append(0)
    id_m_set.append(0)

sentence_count = 0
argument_sentence_count = 0



all_argument_role = []
the_most_trggers_in_one_sentence = 0
longest_trigger_len = 0

for file_source in files_source:
    filelist_file = xmldir + file_source + '/' + "FileList"
    with open(filelist_file, 'r+')as f1:
        filelist_contect = f1.read()
    filelist_contect = filelist_contect.strip()
    filelist_lines = filelist_contect.split('\n')
    files_names = []
    files_docs_names = []
    for line in filelist_lines:
        filelist_parments = line.split('\t')
        if filelist_parments[0] == "# DOCID" or filelist_parments[0].split(":")[0] == "Total" or filelist_parments[
            0] == '':
            continue
        file_positions = filelist_parments[1].split(',')
        if file_need in file_positions:
            files_names.append(filelist_parments[0])
            files_docs_names.append(filelist_parments[0])

    file_num = len(files_names)
    train_file_count = 0
    for file_name in files_names:

        event_parameters_list = get_event_parments(xmldir, file_source, file_need, file_name)


        for i8 in range(len(event_parameters_list)):
            event_i2_argument_role = event_parameters_list[i8][5]
            event_i2_trigger = event_parameters_list[i8][2]
            for argument_role_list in event_i2_argument_role:
                all_argument_role.extend(argument_role_list)
            for trigger_list_1 in event_i2_trigger:
                if len(event_i2_trigger) > the_most_trggers_in_one_sentence:
                    the_most_trggers_in_one_sentence = len(event_i2_trigger)
                #统计长度最长的trigger有多长

                trigger_list_1_split = trigger_list_1[0].split()
                if len(trigger_list_1_split) > longest_trigger_len:
                    longest_trigger_len = len(trigger_list_1_split)





        # print("event_parameters_list:",event_parameters_list)
        doc_level_token_parament_to_write_list_for_trigger,doc_level_token_parament_to_write_list_for_argument,\
        same_token_count, same_argument_count,all_argument_count,fu1_count = \
            get_parament_to_write(file_source, file_name, event_parameters_list,same_token_count,
                                  same_argument_count,all_argument_count,fu1_count)



        validation_count = 0


        if validation_num == 1:


            train_file = output_path + "/single_data/train.txt"
            dev_file = output_path + "/single_data/dev.txt"
            test_file = output_path + "/single_data/test.txt"

            argument_train_file = output_path + "/single_data/argument_train.txt"
            argument_dev_file = output_path + "/single_data/argument_dev.txt"
            argument_test_file = output_path + "/single_data/argument_test.txt"


            train_file_condition = False
            dev_file_condition = False
            test_file_condition = False

            if train_file_count < file_num * 0.7:
                train_file_condition = True
            elif train_file_count >= file_num * 0.7 and train_file_count < file_num * 0.9:
                dev_file_condition = True
            else:
                test_file_condition = True



            if train_file_condition:
                sentence_count = write_parament_into_file(doc_level_token_parament_to_write_list_for_trigger, train_file,sentence_count)
                argument_sentence_count = write_parament_into_file(doc_level_token_parament_to_write_list_for_argument, argument_train_file,argument_sentence_count)
                print("current part has " + str(file_num) + "papers.     " + str(train_file_count) + "have done!   in train" )


            elif dev_file_condition:
                sentence_count = write_parament_into_file(doc_level_token_parament_to_write_list_for_trigger, dev_file,sentence_count)
                argument_sentence_count = write_parament_into_file(doc_level_token_parament_to_write_list_for_argument,
                                                                   argument_dev_file, argument_sentence_count)
                print("current part has " + str(file_num) + "papers.     " + str(
                    train_file_count) + "have done!   in dev" )



            elif test_file_condition:
                sentence_count = write_parament_into_file(doc_level_token_parament_to_write_list_for_trigger, test_file,sentence_count)
                argument_sentence_count = write_parament_into_file(doc_level_token_parament_to_write_list_for_argument,
                                                                   argument_test_file, argument_sentence_count)
                print("current part has " + str(file_num) + "papers.     " + str(
                    train_file_count) + "have done!   in test")
        else:
            for i2 in range(validation_num):
                train_file = train_file_set[i2]
                # dev_file = dev_file_set[i2]
                test_file = test_file_set[i2]
                argument_train_file = argument_train_file_set[i2]
                # argument_dev_file = argument_dev_file_set[i2]
                argument_test_file = argument_test_file_set[i2]


                # doc_parments_list_by_sentence = get_final_doc_parment_list(event_parameters_list,doc_parments_list_by_sentence)
                train_file_condition = False
                # dev_file_condition = False
                test_file_condition = False

                if i2 == 0:
                    if train_file_count >= file_num * 0.9:
                        test_file_condition = True
                    else:
                        train_file_condition = True
                else:
                    if train_file_count >= file_num * 0.1 *(i2-1) and train_file_count < file_num * 0.1 * i2:
                        test_file_condition = True
                    else:
                        train_file_condition = True


                if train_file_condition:
                    sentence_count = write_parament_into_file(doc_level_token_parament_to_write_list_for_trigger, train_file,sentence_count)
                    # print("doc_level_token_parament_to_write_list_for_argument:",doc_level_token_parament_to_write_list_for_argument)
                    argument_sentence_count = write_parament_into_file(doc_level_token_parament_to_write_list_for_argument, argument_train_file,argument_sentence_count)
                    print("current part has " + str(file_num) + "papers.     " + str(train_file_count) + "have done!   " + "by" + str(i2) + "turns             in train")



                elif test_file_condition:
                    # doc_level_token_parament_to_write_list = get_parament_to_write(file_source,file_name,event_parameters_list)
                    sentence_count = write_parament_into_file(doc_level_token_parament_to_write_list_for_trigger, test_file,sentence_count)
                    argument_sentence_count = write_parament_into_file(doc_level_token_parament_to_write_list_for_argument,
                                                                       argument_test_file, argument_sentence_count)
                    print("current part has " + str(file_num) + "papers.     " + str(train_file_count) + "have done!   "+ "by" + str(i2) + "turns             in test")

        train_file_count += 1


print("same_argument_count:",same_argument_count)
print("same_token_count:",same_token_count)
print("all_argument_count:",all_argument_count)
print("fu1_count:",fu1_count)
argument_s = list(set(all_argument_role))
print("len(argument_s):",len(argument_s))
print("argument_s:",argument_s)


print("the_most_trggers_in_one_sentence:",the_most_trggers_in_one_sentence)
print("longest_trigger_len:",longest_trigger_len)





nlp.close()