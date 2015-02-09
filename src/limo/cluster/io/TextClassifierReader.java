package limo.cluster.io;

import java.io.IOException;
import java.util.ArrayList;

import limo.cluster.ClassificationScores;

/***
 * 
 * Read in classification scores from text classifier
 * Assumes one score (for a sentence) per line (assumes same sequence as training data!!)
 * @author Barbara Plank
 *
 */
public class TextClassifierReader extends ScoreReader {
	
	public ClassificationScores readClassificationScores() throws IOException {

		ArrayList<Double> scoreList = new ArrayList<Double>();
		
		// LINE contains:   score
		String line = inputReader.readLine();
		line = line.trim();
		while (line != null && !line.equals("") && !line.startsWith("*")) {
			scoreList.add(Double.parseDouble(line));
			line = inputReader.readLine();
		}
		System.err.println("# Number of scores read: "+ scoreList.size());
			
		inputReader.close();
		return new ClassificationScores(scoreList); 
	}

}
