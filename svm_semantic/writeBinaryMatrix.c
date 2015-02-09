#include <iostream>
#include <fstream>
#include <string>
#include <sstream>
#include <vector>
#include <stdlib.h>


using namespace std;
//#include "svm_common.h"

/************************************************************************/
/*                                                                      */
/*   writeBinaryMatrix.c                                                */
/*                                                                      */
/*   Converts matrix.semtk (created with semtk/s-space)                 */
/*         to binary matrix for svm_sem                                 */
/*                                                                      */
/*   Copyright: Barbara Plank                                           */
/*   Date: 16.05.2012                                                   */
/*                                                                      */
/************************************************************************/

// a matrix cell, i.e. a pair of indices
typedef struct cell {
    unsigned int row;
    unsigned int col;
} CELL;

vector<string> &split(const std::string &s, char delim, std::vector<std::string> &elems) {
    stringstream ss(s);
    string item;
    while(getline(ss, item, delim)) {
        elems.push_back(item);
    }
    return elems;
}

vector<string> split(const string &s, char delim) {
    vector<std::string> elems;
    return split(s, delim, elems);
}

int main (int argc, char* argv[])
{
  if (argc<3)  {
    cout << "Please specify an input and output file!\n"<<argv[0] << " MATRIX.semtk  MATRIX.bin\n";
    return 1; 
  }
    
  cout << "Reading svmtk matrix: " << argv[1] << "\n";
  ifstream inStream;
  //inStream.open("/home/bplank/corpora/ukwac/s-space/out/lsa-semantic-space.sspace.matrix.semtk");
  inStream.open(argv[1]);

  cout << "Writing matrix..\n";
  ofstream outStream(argv[2], ios::binary);
 
  int count=0;
  string line;
  while (getline(inStream,line)) {
    //cout << "line " << line << " " << count <<"\n";
    vector<string> x = split(line, ' ');
    int col = atoi(x[0].c_str());
    int row = atoi(x[1].c_str());
    float sim = atof(x[2].c_str());
    /*cout << "col: " << col << "\n";
    cout << "row: " << row << "\n";
    cout << "sim: " << sim << "\n";
    */
    CELL cell;
    cell.col = col;
    cell.row = row;

    outStream.write(reinterpret_cast<const char*>(&cell), sizeof (CELL));
    outStream.write(reinterpret_cast<const char*>(&sim), sizeof (float));

    count+=1;
  }
  

  inStream.close();
  outStream.close();
  cout << "Matrix file written: " << argv[1] << ".bin\n";

  //read written object in again
  /*  ifstream inStream2("coord.dat");
  
  int counter=0;
  CELL curr_cell;
  float simvalue;
  while (!inStream2.eof()) {
    	counter++;
	
	inStream2.read(reinterpret_cast<char *>(&curr_cell),sizeof(CELL));
	inStream2.read(reinterpret_cast<char *>(&simvalue),sizeof(float));
	cout << curr_cell.row << " " << curr_cell.col << ": " << simvalue << "\n";
  }
	printf("%d cells.",counter);
  
  inStream2.close();
  */

  return 0;
}

