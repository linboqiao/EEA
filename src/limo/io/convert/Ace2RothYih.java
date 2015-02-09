package limo.io.convert;

import java.util.ArrayList;

import limo.core.Relations;
import limo.core.Sentence;
import limo.core.interfaces.IRelationDocument;
import limo.io.IRelationReader;
import limo.io.ace2004.ACE2004Reader;
import limo.io.ace2005.ACE2005Reader;

/***
 * Convert ACE data to tabular Roth & Yih format
 * @author Barbara Plank
 *
 */
public class Ace2RothYih {
	
	/***
	 * Usage: PATH_TO_ACE_FILES [ignorefile] 
	 * @param args
	 * @throws Exception 
	 */
	public static void main(String[] args) throws Exception {
		
		if (args.length == 0) {
			System.out.println("-f FORMAT DIRECTORY_TO_ACE_FILES [ignoreRels]\n FORMAT: ACE2004 [default] or ACE 2005");
			System.exit(-1);
		}
		boolean ace2004=true;
		String path;
		String ignore = "";
		
		if (args.length >= 3 && args[0].equals("-f")) {
			String format = args[1];
			if (!(format.equals("ACE2004") || format.equals("ACE2005"))) {
				System.out.println("format not recognized: "+format);
				System.exit(-1);
			}
			else if (format.equals("ACE2005")) 
				ace2004 = false;
			path = args[2];
			if (args.length == 4)
				ignore = args[3];
		} else {
			//default is ACE 2004
			path = args[0];
			if (args.length == 2) 
				ignore = args[1];
		}
		
		
		IRelationReader aceReader;
		
	
		if (!ignore.equals("")) {
			System.err.println("Reading "+ path + " with ignore: "+ignore);
			if (ace2004 == true)
				aceReader = (ACE2004Reader) new ACE2004Reader(path, ignore);
			else
				aceReader = (ACE2005Reader) new ACE2005Reader(path, ignore);
		}
		else {
			System.err.println(("Reading "+ path));
			if (ace2004 == true)
				aceReader = (ACE2004Reader) new ACE2004Reader(path);
			else
				aceReader = (ACE2005Reader) new ACE2005Reader(path);
		}
	    
		ArrayList<IRelationDocument> documents = aceReader.readDocuments();
		
		boolean includeMentionInfo = true;
		
		int countRels = 0;
		
		for (IRelationDocument d : documents) {

			//ACE2004Document doc = (ACE2004Document) d;
			
			ArrayList<Sentence> sentences =  d.getSentences();
		
		
			for (Sentence sentence : sentences) {
				if (sentence.toString().startsWith("The harsh")) 
					System.err.println("test");
				Relations relations = sentence.getRelations();
				
				countRels += relations.size();
				
				
				System.out.println(sentence.toRothYihString(includeMentionInfo));
				
		
			}
		
		}	
		System.err.println(countRels + " total relations.");
		
	}
}
