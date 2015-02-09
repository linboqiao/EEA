package limo.cluster;

import java.util.ArrayList;

public class WordEmbeddingDictionary extends WordEmbedding {
	
	public WordEmbeddingDictionary() {
		super();
	}
	
	public ArrayList<Double> getWordEmbedding(String word) {
		if (word == null) return null;
		if (!this.map.containsKey(word))
			word = word.toLowerCase();
		return this.map.get(word);
	}

}
