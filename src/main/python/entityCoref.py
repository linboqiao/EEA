#!/usr/bin/python 
from glob import glob
import csv
from sklearn.cluster import AgglomerativeClustering
import numpy as np
import gensim

embedding_len = 300
entity_dir = '02_entity_surfaces/*.tsv'

print("loading embeddings")
embedding = gensim.model.Word2Vec.load_word2vec_format('/home/hector/data/embeddings/mikolov/GoogleNews-vectors-negative300.bin',binary = True)

def parse_entity_info(cells):
    headwords = {}
    types = {}
    for cell in cells:
        parts = cell.split(":")
        if len(parts) == 3:
            cell_type = parts[0]
            content = parts[1]
            count = parts[2]
            if cell_type == "H":
                headwords[content] = count
            if cell_type == "T":
                types[content] = count
    return headwords,types
    
def get_entity_vector(headwords):
    entity_vec = zeros(embedding_len)
    for headword in headwords:
        entity_vec += model[headword]
    entity_vec /= len(headwords)

entities = {}
data_matrix = []
for fname in glob(entity_dir):
    with open(fname) as f:
        tsv = csv.reader(f, delimiter='\t')
        for row in tsv:
            if len(row) > 1:
                eid = row[0]
                headwords, types = parse_entity_info(row[1:])
                entities[eid] = headwords,types
                data_matrix.append(get_entity_vector(headwords))

clustering = AgglomerativeClustering(linkage='average',n_clusters=10)
np.array(data_matrix)
clusters = clustering.fit_predict(data_matrix)

for cluster in clusters:
    print cluster
