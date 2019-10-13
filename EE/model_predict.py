from sklearn.externals import joblib
from bert_serving.client import BertClient
from model_com import event_extract

DIR_MODEL = './save/'
file_model_trig = DIR_MODEL + 'model_trigger.pkl'
file_model_arg = DIR_MODEL + 'model_arg.pkl'

bc = BertClient(ip='127.0.0.1', port=8701, port_out=8702, show_server_config=False) # bert model as service
model_trig, encoder_trig = joblib.load(file_model_trig)
model_arg, encoder_arg = joblib.load(file_model_arg)

text = "分析称印度媒体对中巴联合军演反应过激(图)"
ann = event_extract(text, model_trig, encoder_trig, model_arg, encoder_arg, bc)
print(ann)
