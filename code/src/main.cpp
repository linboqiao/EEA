#include "utils/parameters.h"
#include "utils/commandline_flags.h"
#include "utils/utils.h"
#include "frequent_pattern_mining.h"
#include "documents.h"
#include "segmentation.h"
// #include "data/dump.h"
#include <fstream>

using FrequentPatternMining::Pattern;
using FrequentPatternMining::patterns;

vector<double> f;
vector<int> pre;

void process(const vector<TOTAL_TOKENS_TYPE>& tokens, Segmentation& segmenter, FILE* out)
{
    segmenter.viterbi(tokens, f, pre);

    int i = (int)tokens.size();
    assert(f[i] > -1e80);
    // assert(tokens.size() == deps.size());
    vector<string> ret;
    while (i > 0) {
        int j = pre[i];
        size_t u = 0;
        bool quality = true;
        for (int k = j; k < i; ++ k) {
            if (!trie[u].children.count(tokens[k])) {
                quality = false;
                break;
            }
            u = trie[u].children[tokens[k]];
        }
        quality &= trie[u].id >= 0 && patterns[trie[u].id].probability >= 0;

        if (quality) {
            ret.push_back("</trigger>");
        }

        if (true) {
            for (int k = i - 1; k >= j; -- k) {
                ostringstream sout;
                sout << tokens[k];
                ret.push_back(sout.str());
            }
        }
        
        if (quality) {
			ret.push_back("<trigger>");
        }

        i = j;
    }

    reverse(ret.begin(), ret.end());
    for (int i = 0; i < ret.size(); ++ i) {
        fprintf(out, "%s%c", ret[i].c_str(), i + 1 == ret.size() ? '\n' : ' ');
    }
}

int main(int argc, char* argv[])
{
    parseCommandFlags(argc, argv);

    sscanf(argv[1], "%d", &NTHREADS);
    omp_set_num_threads(NTHREADS);

    Documents::loadAllTrainingFiles(TRAIN_FILE);
    Documents::loadTriggers(TRIGGER_FILE);
    Documents::splitIntoSentences();

    cerr << "Mining frequent phrases..." << endl;
    FrequentPatternMining::mine(MIN_SUP, MAX_LEN);

    cerr << "Marking triggers ..." << endl;
    constructTrie();
    FILE* out = tryOpen("tmp/tokenized_segmented_sentences.txt", "w");

    Segmentation segmenter;
    vector<TOTAL_TOKENS_TYPE> tokens;
    for (const auto& sentence : Documents::sentences) {
    	for (int st = sentence.first; st <= sentence.second; ++st) {
    		tokens.push_back(Documents::wordTokens[st]);
    	}
    	if (tokens.size() > 0) {
    		process(tokens, segmenter, out);
    		tokens.clear();
    	}
    }

    fclose(out);

    return 0;
}