#ifndef __COMMANDLINE_FLAGS_H__
#define __COMMANDLINE_FLAGS_H__

#include "../utils/utils.h"
#include "../utils/parameters.h"

void parseCommandFlags(int argc, char* argv[])
{
    for (int i = 1; i < argc; ++ i) {
        //cout<<"POS TAG settings"<<argv[i]<<endl;
        if (!strcmp(argv[i], "--min_sup")) {
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
}


#endif
