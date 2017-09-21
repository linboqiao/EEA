#ifndef __COMMANDLINE_FLAGS_H__
#define __COMMANDLINE_FLAGS_H__

#include "../utils/utils.h"
#include "../utils/parameters.h"

void parseCommandFlags(int argc, char* argv[])
{
    for (int i = 1; i < argc; ++ i) {
        //cout<<"POS TAG settings"<<argv[i]<<endl;
        if (!strcmp(argv[i], "--iter")) {
            fromString(argv[i + 1], ITERATIONS);
            ++ i;
        } else if (!strcmp(argv[i], "--min_sup")) {
            fromString(argv[++ i], MIN_SUP);
            ++ i;
        } else if (!strcmp(argv[i], "--max_len")) {
            fromString(argv[++ i], MAX_LEN);
            ++ i;
        } else if (!strcmp(argv[i], "--thread")) {
            fromString(argv[++ i], NTHREADS);
        } else {
            fprintf(stderr, "[Warning] Unknown Parameter: %s\n", argv[i]);
        }
    }
    if (INTERMEDIATE) {
        fprintf(stderr, "=== Current Settings ===\n");
        fprintf(stderr, "Iterations = %d\n", ITERATIONS);
        fprintf(stderr, "Minimum Support Threshold = %d\n", MIN_SUP);
        fprintf(stderr, "Maximum Length Threshold = %d\n", MAX_LEN);
        if (ENABLE_POS_TAGGING) {
            fprintf(stderr, "Constraints Mode Enabled\n");
        } else {
            fprintf(stderr, "Constraints Mode Disabled\n");
            fprintf(stderr, "Discard Ratio = %.6f\n", DISCARD);
        }
        fprintf(stderr, "Number of threads = %d\n", NTHREADS);
        if (LABEL_FILE != "") {
            fprintf(stderr, "Load labels from %s\n", LABEL_FILE.c_str());
        } else {
            fprintf(stderr, "Auto labels from knowledge bases\n");
            fprintf(stderr, "\tLabeling Method = %s\n", LABEL_METHOD.c_str());
            fprintf(stderr, "\tMax Positive Samples = %d\n", MAX_POSITIVE);
            fprintf(stderr, "\tNegative Sampling Ratio = %d\n", NEGATIVE_RATIO);
        }
        fprintf(stderr, "=======\n");
    }
}


#endif
