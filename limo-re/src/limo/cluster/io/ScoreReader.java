package limo.cluster.io;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * Abstract class to represent reader for dependency triples, text classification scores etc.
 * 
 * @author Barbara Plank
 *
 */
public abstract class ScoreReader {
	
	protected BufferedReader inputReader;

	public static ScoreReader createScoreReader(String format) throws IOException {
		 if (format.equals("Brown")) {
			return new BrownWordClusterReader();
		} else if (format.equals("TextClassifier")) { 
			return new TextClassifierReader();
		}
		else if (format.equals("SemKernelDictionary")) { 
			return new SemKernelDictionaryReader();
		}
		//thien
		else if (format.equals("Word2VecSemKernelDictionary")) { 
			return new Word2VecSemKernelDictionaryReader();
		}
		else if (format.equals("WordEmbeddingDictionary")) {
			return new WordEmbeddingDictionaryReader();
		}
		//end thien
		else {
			System.out.println("!!!!!!!  Not a supported input format - return: " + format);
			return null;
		}
	}

	public static ScoreReader createScoreReader() throws IOException {
		return createScoreReader("DependencyTriples");
	}

	public boolean startReading(String file) throws IOException {
		inputReader = new BufferedReader(new InputStreamReader(
				new FileInputStream(file), "UTF8"));
		return true;
	}

}
