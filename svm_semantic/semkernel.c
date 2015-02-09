//#include "semkernel.h"
//#include "dictionary.h"
#include <stdlib.h>
#include <stdio.h>
#include <sys/stat.h>
#include <ext/hash_map>
#include <fstream>
#include <iostream>
#include <map>
#include <string>


using namespace std;

// first call to kernel evaluation will initialize the similarity matrix
// char initialized=0;

// a matrix cell, i.e. a pair of indices
typedef struct cell {
	unsigned int row;
    unsigned int col;
}CELL;

map<string,int> dict;

struct CellEquality {
	bool operator()(CELL c1, CELL c2) const {
		return (c1.row==c2.row && c1.col==c2.col);
	}
};

struct CellHash {
	std::size_t operator()(CELL c) const {
		return c.row+(c.col<<16);	//TODO suited for 32bit systems, max key should be 65xxx
	}
};
typedef   __gnu_cxx::hash_map<CELL, float, CellHash, CellEquality> myMap;
typedef   __gnu_cxx::hash_map<CELL, float, CellHash, CellEquality>::const_iterator myMapIter;


myMap similarities;


// heuristics for avoiding unnecessary queries to the hashmap
unsigned int maxindex = 0;
char * indexshortcut;


////the custom kernel
//extern "C" double custom_kernel(KERNEL_PARM *kernel_parm, SVECTOR *a, SVECTOR *b)
//{
//	if(!initialized){
//		initializeSimilarities(kernel_parm->custom);
//	}
//
//	// actual evaluation part of the kernel
//	double sum=0;
//	WORD *ai,*bj;
//	ai=a->words;
//	while (ai->wnum){
//		bj=b->words;
//		while (bj->wnum){
//			float simweight=getSimilarity(ai->wnum,bj->wnum);
//			if (simweight!=0) sum = sum + (simweight*ai->weight*bj->weight);
//			bj++;
//		}
//		ai++;
//	}
//	return sum;
//}

extern "C"{
int initializeSimilarities(char* similaritymatrixfile){
	  
  	printf("\nInitializing custom kernel module for semantic kernel...");
  	printf("\n(c) Stephan Bloehdorn, 2006");
  	printf("\nLoading similarity matrix from file ");
  	printf(similaritymatrixfile);
  	printf("...");
  	fflush(stdout);
  	ifstream infile (similaritymatrixfile, ios::in|ios::binary);

  	struct stat filestats;
    int fileok = stat(similaritymatrixfile, &filestats);
    if (fileok == -1){
  		printf("\nError: unable to open matrix file ");
  		printf(similaritymatrixfile);
  		printf("\nexiting...");
  		exit(1);
  	}
  	else if (fileok==0&&(filestats.st_size==0||(filestats.st_size%12)>0)){
  		printf("\nError: matrix file ");
  		printf(similaritymatrixfile);
  		printf(" appears to be empty or in wrong format.\nexiting...");
  		exit(1);
  	}
  	else{
  		int counter=0;
  		CELL curr_cell;
  		float simvalue;
  		unsigned int tmp;
  		while (!infile.eof() ){
	  		counter++;
	  		if (counter%100000==0) {
	  			printf("%d..",counter);
	  			fflush(stdout);
	  		}
	  		infile.read(reinterpret_cast<char *>(&curr_cell),sizeof(CELL));
			infile.read(reinterpret_cast<char *>(&simvalue),sizeof(float));
			if (curr_cell.row>curr_cell.col){ //swap this, as we store everything in upper triangular part of matrix
				tmp=curr_cell.row;
				curr_cell.row=curr_cell.col;
				curr_cell.col=tmp;
			}
			//TODO: duplicate cells are overwritten by newest value
			similarities[curr_cell]=simvalue;

			if (maxindex<curr_cell.row) maxindex = curr_cell.row;
			if (maxindex<curr_cell.col) maxindex = curr_cell.col;
		}
		infile.close();
		printf("OK.");
		fflush(stdout);
		printf("\nsetting up heuristics for faster matrix access...");
		fflush(stdout);

		// heuristics for avoiding unnecessary queries to the hashmap
		// requires second pass through data though
		indexshortcut = (char*) malloc(maxindex*sizeof(char));
		if (indexshortcut==NULL){
	  		printf("\nError: cannot allocate memory for matrix heuristics array. (maxindex is %d)",maxindex);
  			printf("\nexiting...");
	  		exit(1);
		}
		for (int i=0;i<maxindex;i++) indexshortcut[i]=0;
		myMapIter it;
		for (it = similarities.begin(); it != similarities.end(); ++it){
			indexshortcut[it->first.row-1]=1;
			indexshortcut[it->first.col-1]=1;
		}

		int fill=0;
		for (int i=0;i<maxindex;i++) if(indexshortcut[i]) fill++;

		printf("(maxindex %d, fill-score %d) OK.",maxindex,fill);
  		printf("\nCustom kernel ready.");
  		printf("\n\n");
  		fflush(stdout);
  	}
}
  	
  	}
  	
  	
extern "C" {  	
 float getSimilarity(int index1, int index2){
 	cell c;
	myMapIter it;
  	float simweight;

  	if ((index1==index2)) simweight=1;
	else simweight=0;

	//check only, if heuristics allow to do so
	if(index1<maxindex&&index2<maxindex&&indexshortcut[index1-1]&&indexshortcut[index2-1]){
		if (index1<index2){
			c.row=index1;
			c.col=index2;
		}
		else{				//lookup only in upper triangular part
			c.col=index1;
			c.row=index2;
		}
		it = similarities.find(c);
		if (it!=similarities.end()) simweight=it->second;
	}
	return simweight;
 }
}

extern "C" { int getIndex(char* lemma) {
	      return dict[lemma];
	     }
}

extern "C" {
 void readDictionary(char* filename){
	cout << "Reading from file " << filename << endl;
	ifstream fin;
	fin.open(filename, ios::in);

	if (fin.is_open()) {
		int counter=0;
		int index, frequency;
		string word;
		while (!fin.eof()){
			counter++;
	  		if (counter%1000==0) {
	  			cout << counter << ".." << flush;
	  		}
   			fin >> index;
   			fin >> word;
   			//comment out the next line if files have no frequency information
   			fin >> frequency;
			dict[word]=index;
		}
		fin.close();   // close the streams
		cout << "OK." << endl;
	}
	else{
		printf("Problem reading from file\n");
		exit(1);
	}


}

}
