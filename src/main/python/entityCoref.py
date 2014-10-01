#!/usr/bin/python 
from glob import glob
import csv
from sklearn.cluster import AgglomerativeClustering
import numpy as np
import gensim

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
    entity_vec = np.zeros(embedding_len)
    for headword in headwords:
        try:
            entity_vec += model[headword]
        except KeyError:
            pass
    entity_vec /= len(headwords)
    return entity_vec

embedding_len = 300
entity_dir = '/home/hector/projects/cross-document-script/data/entity_test/*.tsv'

print("loading embeddings")
#embedding = gensim.models.Word2Vec.load_word2vec_format('/home/hector/data/embeddings/mikolov/GoogleNews-vectors-negative300.bin',binary = True)
entities = {}
data_matrix = None

print("building data matrix")
for fname in glob(entity_dir):
    with open(fname) as f:
        tsv = csv.reader(f, delimiter='\t')
        for row in tsv:
            if len(row) > 1:
                eid = row[0]
                headwords, types = parse_entity_info(row[1:])
                entities[eid] = headwords,types
                if data_matrix is None:
                    data_matrix = get_entity_vector(headwords)
                else:
                    data_matrix = np.vstack((data_matrix,get_entity_vector(headwords)))

print("clustering")
clustering = AgglomerativeClustering(linkage='average',n_clusters=10)
cluster_res = clustering.fit_predict(data_matrix)

print("writing results")
out = open('/home/hector/projects/cross-document-script/data/entity_cluster','w')
for eid, cluster_label in zip(entities, cluster_res):
    print ("writing line to "+out.name)
    out.write(eid + str(entities[eid]) +" "+str(cluster_label)+'\n')

print("Done")
