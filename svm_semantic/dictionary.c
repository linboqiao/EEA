#include "dictionary.h"
#include <stdlib.h>
#include <stdio.h>
#include <sys/stat.h>
#include <map>
#include <fstream>
#include <iostream>
#include <string>

using namespace std;

map<string,int> dict;


int getIndex(char* lemma) {
	return dict[lemma];
}


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

