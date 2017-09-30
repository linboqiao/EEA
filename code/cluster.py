from collections import defaultdict

from scipy.spatial.distance import cosine
from spherecluster import SphericalKMeans
import numpy as np
import sys

class Clusterer:

    def __init__(self, data, n_cluster):
        self.data = data
        self.n_cluster = n_cluster
        self.clus = SphericalKMeans(n_cluster)
        self.clusters = defaultdict(list)  # cluster id -> members
        self.membership = None  # a list contain the membership of the data points
        self.center_ids = None  # a list contain the ids of the cluster centers
        self.inertia_scores = None

    def fit(self):
        self.clus.fit(self.data)
        labels = self.clus.labels_
        for idx, label in enumerate(labels):
            self.clusters[label].append(idx)
        self.membership = labels
        self.center_ids = self.gen_center_idx()
        self.inertia_scores = self.clus.inertia_
        print('Clustering concentration score:', self.inertia_scores)

    # find the idx of each cluster center
    def gen_center_idx(self):
        ret = []
        for cluster_id in xrange(self.n_cluster):
            center_idx = self.find_center_idx_for_one_cluster(cluster_id)
            ret.append((cluster_id, center_idx))
        return ret

    def find_center_idx_for_one_cluster(self, cluster_id):
        query_vec = self.clus.cluster_centers_[cluster_id]
        members = self.clusters[cluster_id]
        best_similarity, ret = -1, -1
        for member_idx in members:
            member_vec = self.data[member_idx]
            cosine_sim = self.calc_cosine(query_vec, member_vec)
            if cosine_sim > best_similarity:
                best_similarity = cosine_sim
                ret = member_idx
        return ret

    def calc_cosine(self, vec_a, vec_b):
        return 1 - cosine(vec_a, vec_b)

def load_emb(emb_path, trigger):
    with open(emb_path, 'r') as IN, open(trigger, 'r') as TRI:
        triggers = dict()
        inv_triggers = dict()
        for num,line in enumerate(TRI):
            triggers[line.strip().replace(' ','_')] = 0
        IN.readline()
        emb = []
        for line in IN:
            line=line.strip().split(' ')
            if line[0] in triggers:
                if triggers[line[0]] == 0:
                    triggers[line[0]] = len(emb)
                    inv_triggers[len(emb)] = line[0]
                #print line
                #print line[0], ' '.join(line[:1]).decode('ascii')
                emb.append(map(float, line[1:]))
        return np.array(emb), inv_triggers

def nameCluster(ref, clusters, out):
    refs = set()
    with open(ref) as IN:
        for line in IN:
            refs.add(line.strip())
    with open(out, 'w') as OUT:
        for k,v in clusters.iteritems():
            pivots = []
            discovers = []
            for word in v:
                if word in refs:
                    pivots.append(word)
                else:
                    discovers.append(word)
            OUT.write('Cluster '+str(k)+':\t' + ','.join(pivots) + '\t' + ','.join(discovers) + '\n')



if __name__ == '__main__':
    train_data, triggers = load_emb(sys.argv[1], sys.argv[2])
    print train_data.shape
    tmp = Clusterer(train_data, 30)
    tmp.fit()
    for k,v in tmp.clusters.iteritems():
        tmp.clusters[k] = map(lambda x: triggers[x], v)
    #print tmp.clusters
    nameCluster(sys.argv[3], tmp.clusters, sys.argv[4])

