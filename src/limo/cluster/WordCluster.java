package limo.cluster;

import java.util.HashMap;

/**
 * Holds word clusters (string word, string bitstring)
 * @author Barbara Plank
 *
 */
public abstract class WordCluster {
	HashMap<String, String> map;
	
	public WordCluster() {
		 this.map = new HashMap<String, String>();
	}

	public void add(String word, String bitstring) {
		this.map.put(word, bitstring);			
	}

}
