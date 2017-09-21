#include "utils/parameters.h"
#include "utils/commandline_flags.h"
#include "utils/utils.h"
#include "frequent_pattern_mining.h"
#include "documents.h"
// #include "model_training/segmentation.h"
// #include "data/dump.h"
#include <fstream>

using FrequentPatternMining::Pattern;
using FrequentPatternMining::patterns;

int main(int argc, char* argv[])
{
    parseCommandFlags(argc, argv);

    sscanf(argv[1], "%d", &NTHREADS);
    omp_set_num_threads(NTHREADS);

    Documents::loadAllTrainingFiles(TRAIN_FILE);
    Documents::splitIntoSentences();

    cerr << "Mining frequent phrases..." << endl;
    FrequentPatternMining::mine(MIN_SUP, MAX_LEN);

    return 0;
}