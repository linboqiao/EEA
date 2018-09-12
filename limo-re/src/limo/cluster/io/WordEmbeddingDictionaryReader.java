package limo.cluster.io;

import java.io.IOException;

import limo.cluster.WordEmbeddingDictionary;

public class WordEmbeddingDictionaryReader  extends ScoreReader {
	
	public WordEmbeddingDictionary createWordEmbedding() throws IOException {
		WordEmbeddingDictionary wedDict = new WordEmbeddingDictionary();
		
		String line = "";
		while ((line = inputReader.readLine()) != null) {
			wedDict.add(line);
		}
		
		inputReader.close();
		return wedDict;
	}

}
