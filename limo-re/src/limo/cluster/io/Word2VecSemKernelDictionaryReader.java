package limo.cluster.io;

import java.io.IOException;
import java.util.ArrayList;

import limo.cluster.Word2VecSemKernelDictionary;

public class Word2VecSemKernelDictionaryReader  extends ScoreReader {
	
	public Word2VecSemKernelDictionary createWordCluster() throws IOException {

		ArrayList<String[]> lineList = new ArrayList<String[]>();
		Word2VecSemKernelDictionary wordcluster = new Word2VecSemKernelDictionary();
		
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
