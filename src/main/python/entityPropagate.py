#!/usr/bin/python 
from glob import glob
import csv
from sklearn.cluster import AgglomerativeClustering
import numpy as np
import gensim
import math
from itertools import izip
from heapq import heappush,heappop


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


def get_type_compatibility(types1, types2):
    if len(types1) == 0 or len(types2) == 0:
        return True
    
    for t, v in types1.iteritems():
        if types2.has_key(t):
            return True

    return False


def dot_product(vec1, vec2):
    return sum(map(lambda x: x[0] * x[1], izip(vec1, vec2)))


def cosine_sim(v1,v2):
    len1 = math.sqrt(dot_product(v1, v1))
    len2 = math.sqrt(dot_product(v2, v2))
    if len1 == 0 or len2 == 0:
        return 0

    prod = dot_product(v1, v2)
    return prod/(len1*len2)

embedding_len = 300
entity_dir = '/home/hector/projects/cross-document-script/data/entity_test/*.tsv'

print("loading embeddings")
#embedding = gensim.models.Word2Vec.load_word2vec_format('/home/hector/data/embeddings/mikolov/GoogleNews-vectors-negative300.bin',binary = True)
entity_chains = []

print("Single pass clustering")
for fname in glob(entity_dir):
    with open(fname) as f:
        tsv = csv.reader(f, delimiter='\t')
        
        potential_corefs = [[] for x in xrange(len(entity_chains))]
        
        new_entities = {}

        for row in tsv:
            if len(row) > 1:
                eid = row[0]
                headwords, types = parse_entity_info(row[1:])
                
                vec = get_entity_vector(headwords)

                new_entities[eid] = (vec, types)
                
                if len(entity_chains) ==  0:
                    entity_chains.append((vec,types,[eid]))
                else:
                    for index, (cluster_vec, cluster_types, eids) in enumerate(entity_chains):
                        if get_type_compatibility(types, cluster_types):
                            sim = cosine_sim(vec,cluster_vec)
                            if sim > 0.8:
                                heappush(potential_corefs[index], (1 - sim, index, eid, types))

        mapped_chain = [0]* len(entity_chains)
        mapped_new_entities = set()

        for potential_coref in potential_corefs:
            if len(potential_coref) == 0 :
                pass
            else:
                dist, index, eid, types = heappop(potential_coref)
                
                if mapped_chain[index] != 0:
                    continue

                mapped_chain[index] = 1
                mapped_new_entity.add(eid)

                cluster_vec, cluster_types, eids = entity_chains[index]

                old_size = len(eids)
                eids.append(eid)
                new_cluster_vec = (cluster_vec * old_size + cluster_vec) / (old_size + 1)
                for t in types:
                    try:
                        cluster_types[t] += 1
                    except KeyError:
                        cluster_types[t] = 1

    
        for eid, (vec,types) in new_entities.iteritems():
            if not eid in mapped_new_entities:
                entity_chains.append((vec,types,[eid]))


print("writing results")
out = open('/home/hector/projects/cross-document-script/data/entity_cluster','w')
for vec, types, eids in entity_chains:
    for eid in eids:
        out.write(eid+" ")
    for t in types:
        out.write(t+" ")
    out.write("\n")

print("Done")
