#ifndef __DOCUMENTS_H__
#define __DOCUMENTS_H__

#include "../utils/utils.h"

namespace Documents
{
// === global variables ===
    TOTAL_TOKENS_TYPE totalTokens = 0;
    TOTAL_TOKENS_TYPE totalWordTokens = 0;

    TOKEN_ID_TYPE maxTokenID = 0;

    TOKEN_ID_TYPE* wordTokens; // 0 .. totalWordTokens - 1
    bool* wordTokenInfo;

    vector<pair<TOTAL_TOKENS_TYPE, TOTAL_TOKENS_TYPE>> sentences;
    set<string> separatePunc = {",", ".", "\"", ";", "!", ":", "(", ")", "?", "``","$","''"};
// ===
    inline bool isEndOfSentence(int i) {
        return i < 0 || i + 1 >= totalWordTokens || wordTokenInfo[i];
    }

    inline void loadAllTrainingFiles(const string& docFile) {
        if (true) {
            // get total number of tokens and the maximum number of tokens
            FILE* in = tryOpen(docFile, "r");
            totalTokens = 0;
            for (;fscanf(in, "%s", line) == 1; ++ totalTokens) {
                bool flag = true;
                TOKEN_ID_TYPE id = 0;
                for (int i = 0; line[i] && flag; ++ i) {
                    flag &= isdigit(line[i]);
                    id = id * 10 + line[i] - '0';
                }
                if (flag) {
                    maxTokenID = max(maxTokenID, id);
                    ++ totalWordTokens;
                }
            }
            cerr << "# of total tokens = " << totalTokens << endl;
            cerr << "# of total word tokens = " << totalWordTokens << endl;
            cerr << "max word token id = " << maxTokenID << endl;
            fclose(in);
        }

        wordTokens = new TOKEN_ID_TYPE[totalWordTokens];
        wordTokenInfo = new bool[totalWordTokens]();

        FILE* in = tryOpen(docFile, "r");

        INDEX_TYPE docs = 0;
        TOTAL_TOKENS_TYPE ptr = 0;
        int cnt = 0;
        while (getLine(in)) {
            ++ docs;

            stringstream sin(line);

            for (string temp; sin >> temp;) {
                // get token
                bool flag = true;
                TOKEN_ID_TYPE token = 0;
                for (int i = 0; i < temp.size() && flag; ++ i) {
                    flag &= isdigit(temp[i]);
                    token = token * 10 + temp[i] - '0';
                }
                
                if (!flag) {
                    string punc = temp;
                    if (separatePunc.count(punc)) {
                        wordTokenInfo[ptr - 1] = true;
                        cnt ++;
                    }
                }
                if (flag) {
                    wordTokens[ptr] = token;
                    // wordTokenInfo[ptr] = false;
                    ++ ptr;
                }
            }
        }
        cerr << "#ptr = " << ptr << "\t" << cnt << endl;
        fclose(in);

        cerr << "# of documents = " << docs << endl;
    }

    inline void splitIntoSentences() {
        sentences.clear();
        TOTAL_TOKENS_TYPE st = 0;

        for (TOTAL_TOKENS_TYPE i = 0; i < totalWordTokens; ++ i) {
            if (isEndOfSentence(i)) {
                sentences.push_back(make_pair(st, i));
                //postagsent.push_back(make_pair(st, i));
                st = i + 1;
            }
        }

        sentences.shrink_to_fit();
        cerr << "The number of sentences = " << sentences.size() << endl;
        //cout << "The number of sentences = " << sentences.size() << endl;
    }

};

#endif
