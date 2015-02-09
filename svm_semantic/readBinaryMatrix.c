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
/*   readBinaryMatrix.c                                                 */
/*                                                                      */
/*   Reads an matrix.bin object and outputs to stdout                   */
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

int main (int argc, char* argv[])
{
  if (argc<2)  {
    cout << "Please specify a matrix.bin file!\n"<<argv[0] << " MATRIX.bin\n";
    return 1; 
  }
  //read written object in again
  ifstream inStream2(argv[1]);
  
  int counter=0;
  CELL curr_cell;
  float simvalue;
  while (!inStream2.eof()) {
    	counter++;
	
	inStream2.read(reinterpret_cast<char *>(&curr_cell),sizeof(CELL));
	inStream2.read(reinterpret_cast<char *>(&simvalue),sizeof(float));
	cout << curr_cell.row << " " << curr_cell.col << ": " << simvalue << "\n";
  }
	printf("%d cells.\n",counter);
  
  inStream2.close();


  return 0;
}

