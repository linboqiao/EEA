#ifndef SEMKERNEL_H_
#define SEMKERNEL_H_

/*
 * Call this only once together with similarity file to intitialize
*/
int initializeSimilarities(char* similaritymatrixfile);


/* Use this to ask for similarity of two items. General contract is as follows:
 * - if index1 equals index2 result will always be 1
 * - else if matrix contains an entry at cell_{index1,index2} this value will be returned
 * - else the result will be 0
*/
float getSimilarity(int index1, int index2);

#endif /*SEMKERNEL_H_*/
