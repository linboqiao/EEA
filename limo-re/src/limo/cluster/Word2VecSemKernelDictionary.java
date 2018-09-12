package limo.cluster;

import java.io.IOException;

import limo.cluster.io.ScoreReader;
import limo.cluster.io.Word2VecSemKernelDictionaryReader;

/**
 * Object that holds index for words
 * 
 * index, word
 * Adapted from Barbara's class
 * 
 * @author Thien Huu Nguyen
 *
 */

public class Word2VecSemKernelDictionary  extends WordCluster   {
	
	int maxIndexLength=15;
	
	public Word2VecSemKernelDictionary() {
		 super();
	}

	/**
	 * Return bitstring of cluster word belongs to
	 * @param word
	 * @return bitstring
	 */
	public String getPlainWordIndex(String word) {
		return this.map.get(word);
	}
	
	/**
	 * svm-semantic assumes tokens are prefixed with: +000000
	 * @param word
	 * @return
	 */
	public String getPrefixedWordIndex(String word) {
		String index =  this.map.get(word);
		if (index == null) {
			index = "1";
		}
		//	return null;
		StringBuilder sb = new StringBuilder();
		sb.append("+");
		for (int i=0; i < (maxIndexLength - index.length()); i++)
			sb.append("0");
		sb.append(index);
		return sb.toString();
	}	

	public static void main(String[] args) throws IOException {
		Word2VecSemKernelDictionary dict = new Word2VecSemKernelDictionary();
		dict.map.put("this", "0");
		dict.map.put("that", "22");
		System.out.println(dict.getPlainWordIndex("this"));
		System.out.println(dict.getPrefixedWordIndex("this"));
		System.out.println(dict.getPrefixedWordIndex("that"));
		System.out.println(dict.getPrefixedWordIndex("this").length());
		
//		Word2VecSemKernelDictionaryReader dictionaryReader = (Word2VecSemKernelDictionaryReader) ScoreReader.createScoreReader("Word2VecSemKernelDictionary");
//		System.out.println();
//		dictionaryReader.startReading("lsa-semantic-space.sspace.dict");
//		SemKernelDictionary dictionary = (Word2VecSemKernelDictionaryReader) dictionaryReader.createWordCluster();
//		
//		dictionary.getPlainWordIndex("computer");
//		System.out.println(dictionary.getPlainWordIndex("computer"));
//		System.out.println(dictionary.getPrefixedWordIndex("computer"));
	}

}
