#ifndef DICTIONARY_H_
#define DICTIONARY_H_

/**
 * Can be used to read in the dictionary at the specified filename.
*/ 
void readDictionary(char* filename);

/**
 * Use this to retrieve the index for a certain lemma. 
 */
int getIndex(char* lemma);



#endif /*DICTIONARY_H_*/
