package limo.cluster;

import java.io.IOException;
import limo.cluster.io.ScoreReader;
import limo.cluster.io.SemKernelDictionaryReader;


/**
 * Object that holds index for words
 * 
 * index, word
 *
 * 
 * @author Barbara Plank
 *
 */
public class SemKernelDictionary extends WordCluster {
	
		int maxIndexLength=15;
	
	public SemKernelDictionary() {
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
	 * svm-semantic assumes tokens are prefixed with 6 0's: 000000
	 * @param word
	 * @return
	 */
	public String getPrefixedWordIndex(String word) {
		String index =  this.map.get(word);
		if (index == null)
			return null;
		StringBuilder sb = new StringBuilder();
		for (int i=0; i < (maxIndexLength - index.length()); i++)
			sb.append("0");
		sb.append(index);
		return sb.toString();
	}	

	public static void main(String[] args) throws IOException {
		SemKernelDictionary dict = new SemKernelDictionary();
		dict.map.put("this", "1");
		dict.map.put("that", "22");
		System.out.println(dict.getPlainWordIndex("this"));
		System.out.println(dict.getPrefixedWordIndex("this"));
		System.out.println(dict.getPrefixedWordIndex("that"));
		System.out.println(dict.getPrefixedWordIndex("this").length());
		
		SemKernelDictionaryReader dictionaryReader = (SemKernelDictionaryReader) ScoreReader.createScoreReader("SemKernelDictionary");
		System.out.println();
		dictionaryReader.startReading("lsa-semantic-space.sspace.dict");
		SemKernelDictionary dictionary = (SemKernelDictionary) dictionaryReader.createWordCluster();
		
		dictionary.getPlainWordIndex("computer");
		System.out.println(dictionary.getPlainWordIndex("computer"));
		System.out.println(dictionary.getPrefixedWordIndex("computer"));
	}

	
}
