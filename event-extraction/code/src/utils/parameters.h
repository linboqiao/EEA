#ifndef __PARAMETERS_H__
#define __PARAMETERS_H__

#include "../utils/utils.h"

typedef int TOTAL_TOKENS_TYPE;
typedef int PATTERN_ID_TYPE;
typedef int TOKEN_ID_TYPE;
typedef unsigned long long ULL;
typedef int INDEX_TYPE; // sentence id

int NTHREADS = 4;
int MIN_SUP = 30;
int MAX_LEN = 4;
bool INTERMEDIATE = true;

const string TRAIN_FILE = "tmp/tokenized_test.txt";
const string TRIGGER_FILE = "tmp/tokenized_triggers.txt";
#endif