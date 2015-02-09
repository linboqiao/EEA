package limo.cluster.io;

import java.io.IOException;
import java.util.ArrayList;

import limo.cluster.BrownWordCluster;

/**
 * Brown Word Clusters
 * 
 * @author Barbara Plank
 *
 */
public class BrownWordClusterReader extends ScoreReader {

	/**
	 * 
	 * @return BrownWordCluster
	 * @throws IOException
	 */
	public BrownWordCluster createWordCluster() throws IOException {

		ArrayList<String[]> lineList = new ArrayList<String[]>();
		BrownWordCluster wordcluster = new BrownWordCluster();
		
		// LINE contains:   bitcode word count (split by tab)
		String line = inputReader.readLine();
		while (line != null && !line.equals("") && !line.startsWith("*")) {
			lineList.add(line.split("\t"));
			// System.out.println("## "+line);
			line = inputReader.readLine();
		}

		int length = lineList.size();

		if (length == 0) {
			inputReader.close();
			return null;
		}
		
		for (int i =0; i < length; i++) {
			String[] info = lineList.get(i);
			String bitstring = info[0];
			String word = info[1];
			//line[2] contains word count, ignore for now
			wordcluster.add(word,bitstring);
		}
		inputReader.close();
		return wordcluster; 
	}
}
