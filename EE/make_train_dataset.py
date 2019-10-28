import os
import shutil
 
 
data_dir = "BioNLP_data/output/"
# data_dir_new = "/mnt/CHIP/bioann/"
 
 
# file_reader = os.listdir(data_dir)
#  
# train_data_set = []
# dev_data_set = []
# test_data_set = []
#  
#  
# txt_files = []
# a1_appear = False
# for data_file in file_reader:
#     # data_file_name = data_file.split('.')[0]
#     data_file_suffix = data_file.split('.')[-1]
#     if data_file_suffix == 'txt':
#         txt_files.append(data_file[:-4])
#     if data_file_suffix == 'a1':
#         a1_appear = True
#          
#  
#  
# file_count = 0
# a1_file_contect_lines = []
# for file_j in range(len(txt_files)):
#     txt_file_name = data_dir + txt_files[file_j] + '.txt'
#     a2_file_name = data_dir + txt_files[file_j] + '.a2'
#     with open(txt_file_name,'r+') as txt_f:
#         txt_file_contect = txt_f.read()
#     txt_file_contect = txt_file_contect.strip()
#     txt_file_contect_lines = txt_file_contect.split('\n')
#     # print(a1_file_name)
#     if a1_appear:
#         a1_file_name = data_dir + txt_files[file_j] + '.a1'
#         with open(a1_file_name,'r+') as a1_f:
#             a1_file_contect = a1_f.read()
#     # print(a1_file_contect)
#         a1_file_contect = a1_file_contect.strip()
#         a1_file_contect_lines = a1_file_contect.split('\n')
#     with open(a2_file_name,'r+') as a2_f:
#         a2_file_contect = a2_f.read()
#     a2_file_contect = a2_file_contect.strip()
#     a2_file_contect_lines = a2_file_contect.split('\n')
#  
#     ann_contect_lines = a1_file_contect_lines + a2_file_contect_lines
#  
#  
#  
#     if file_count < 7:
#         ann_file_path = data_dir + 'train/' + str(file_j) + '.ann'
#         txt_file_path = data_dir + 'train/' + str(file_j) + '.txt'
#     elif file_count < 9:
#         ann_file_path = data_dir + 'dev/' + str(file_j) + '.ann'
#         txt_file_path = data_dir + 'dev/' + str(file_j) + '.txt'
#     elif file_count == 9:
#         ann_file_path = data_dir + 'test/' + str(file_j) + '.ann'
#         txt_file_path = data_dir + 'test/' + str(file_j) + '.txt'
#     file_count += 1
#     if file_count == 10:
#         file_count = 0
#  
#  
#     if os.path.exists(ann_file_path):
#         os.remove(ann_file_path)
#     if os.path.exists(txt_file_path):
#         os.remove(txt_file_path)
#  
#     with open(ann_file_path,'a+') as ann_f1:
#         for ann_line in ann_contect_lines:
#             ann_f1.write(ann_line + '\n')
#  
#  
#     with open(txt_file_path,'a+') as txt_f1:
#         for txt_line in txt_file_contect_lines:
#             txt_f1.write(txt_line + '\n')


# files_name = []
# file_reader = os.listdir(data_dir)
# for file_i in file_reader:
#     file_nu = file_i.split('.')[0]
#     file_suf = file_i.split('.')[1]
#     if 
#     





tar_dir = 'data/'
num = 1

for i in range(num*7):
    shutil.copyfile(data_dir + str(i) + '.ann', tar_dir + 'train/' + str(i) + '.ann')
    shutil.copyfile(data_dir + str(i) + '.txt', tar_dir + 'train/' + str(i) + '.txt')


for j in range(num*7,num*9):
    shutil.copyfile(data_dir + str(j) + '.ann', tar_dir + 'dev/' + str(j) + '.ann')
    shutil.copyfile(data_dir + str(j) + '.txt', tar_dir + 'dev/' + str(j) + '.txt')
    
for k in range(num*9,num*10):
    shutil.copyfile(data_dir + str(k) + '.ann', tar_dir + 'test/' + str(k) + '.ann')
    shutil.copyfile(data_dir + str(k) + '.txt', tar_dir + 'test/' + str(k) + '.txt')  


# import os 
# import shutil
# 
# DATA_DIR = 'data/'
# 
# 
# files_name = os.listdir(DATA_DIR)
# 
# datas_name = []
# for file_name in files_name:
#     if file_name[-4:] == '.txt':
#         datas_name.append(file_name[:-4])



     
 



