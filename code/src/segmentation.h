#ifndef __SEGMENTATION_H__
#define __SEGMENTATION_H__

#include <cassert>

#include "utils/utils.h"
#include "frequent_pattern_mining.h"
#include "documents.h"

using FrequentPatternMining::Pattern;
// === global variables ===
using FrequentPatternMining::patterns;
using FrequentPatternMining::pattern2id;
using FrequentPatternMining::id2ends;
using FrequentPatternMining::unigrams;

mutex POSTagMutex[SUFFIX_MASK + 1];

struct TrieNode {
    unordered_map<TOTAL_TOKENS_TYPE, size_t> children;

    PATTERN_ID_TYPE id;

    TrieNode() {
        id = -1;
        children.clear();
    }
};
vector<TrieNode> trie;

// ===

void constructTrie() {
    trie.clear();
    trie.push_back(TrieNode());
    for (PATTERN_ID_TYPE i = 0; i < patterns.size(); ++ i) {
        const vector<TOTAL_TOKENS_TYPE>& tokens = patterns[i].tokens;
        if (tokens.size() == 0 || tokens.size() > 1 && patterns[i].currentFreq == 0) {
            continue;
        }
        size_t u = 0;
        for (const TOTAL_TOKENS_TYPE& token : tokens) {
            if (!trie[u].children.count(token)) {
                trie[u].children[token] = trie.size();
                trie.push_back(TrieNode());
            }
            u = trie[u].children[token];
        }
        trie[u].id = i;
    }
    if (INTERMEDIATE) {
        cerr << "# of trie nodes = " << trie.size() << endl;
    }
}

class Segmentation
{
private:
    static const double INF;
    static vector<vector<TOTAL_TOKENS_TYPE>> total;

public:
    static bool ENABLE_POS_TAGGING;
    static double penalty;
    static vector<vector<double>> connect, disconnect;

private:
    // generated
    int maxLen;
    double *prob;

    void normalize() {
        vector<double> sum(maxLen + 1, 0);
        for (PATTERN_ID_TYPE i = 0; i < patterns.size(); ++ i) {
            sum[patterns[i].size()] += prob[i];
        }
        for (PATTERN_ID_TYPE i = 0; i < patterns.size(); ++ i) {
            prob[i] /= sum[patterns[i].size()];
        }
    }

    void initialize() {
        // compute maximum tokens
        maxLen = 0;
        for (PATTERN_ID_TYPE i = 0; i < patterns.size(); ++ i) {
            maxLen = max(maxLen, patterns[i].size());
        }

        prob = new double[patterns.size()];
        for (PATTERN_ID_TYPE i = 0; i < patterns.size(); ++ i) {
            if (patterns[i].currentFreq > 0 && Documents::totalTriggers.count(patterns[i].encode_trigger()))
                prob[i] = 1;
            else 
                prob[i] = 0;
        }
        // normalize();
    }

public:

    double getProb(int id) const {
        return exp(prob[id]);
    }

    ~Segmentation() {
        delete [] prob;
    }

    Segmentation() {
        initialize();
        for (PATTERN_ID_TYPE i = 0; i < patterns.size(); ++ i) {
            prob[i] = log(prob[i] + EPS);
            patterns[i].probability = prob[i];
        }
    }

    inline double viterbi(const vector<TOKEN_ID_TYPE> &tokens, vector<double> &f, vector<int> &pre) {
        f.clear();
        f.resize(tokens.size() + 1, -INF);
        pre.clear();
        pre.resize(tokens.size() + 1, -1);
        f[0] = 0;
        pre[0] = 0;
        for (size_t i = 0 ; i < tokens.size(); ++ i) {
            if (f[i] < -1e80) {
                continue;
            }
            bool impossible = true;
            for (size_t j = i, u = 0; j < tokens.size(); ++ j) {
                if (!trie[u].children.count(tokens[j])) {
                    break;
                }
                u = trie[u].children[tokens[j]];
                if (trie[u].id != -1) {
                    impossible = false;
                    PATTERN_ID_TYPE id = trie[u].id;
                    double p = prob[id];
                    if (f[i] + p > f[j + 1]) {
                        f[j + 1] = f[i] + p;
                        pre[j + 1] = i;
                    }
                }
            }
            if (impossible) {
                if (f[i] > f[i + 1]) {
                    f[i + 1] = f[i];
                    pre[i + 1] = i;
                }
            }
        }
        return f[tokens.size()];
    }

};

const double Segmentation::INF = 1e100;
vector<vector<TOTAL_TOKENS_TYPE>> Segmentation::total;
#endif