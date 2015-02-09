package limo.io.ry;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;

import limo.core.Relation;
import limo.core.Sentence;
import limo.core.Token;
import limo.core.interfaces.IRelationDocument;

//similar to CreateRothYihPredictedMentionsFile but with the difference
//that we keep the gold relations (and gold entity boundaries), just add more entities from tagger
public class CreateRothYihGoldMentionsFile {

	// read in gold RY file and output from CRF++ tagger
	public static void main(String[] args) throws Exception {
		
		if (args.length < 2) {
			System.out.println("GOLDFILE TAGGEDFILE");
			System.exit(-1);
		}
		boolean goldBoundaries=true; //if we assume gold boundaries given (take label even if it is O)
				
		String path = args[0];
		String taggedFile = args[1];
	
		int WORDIDX =0;
		
		RothYihConll2004Reader reader = new RothYihConll2004Reader(path);
		
		ArrayList<IRelationDocument> documents = reader.readDocuments();
			
		ArrayList<Sentence> sentencesOutput = new ArrayList<Sentence>();
		
		//will be just one document
		RothYihConll2004Document doc = (RothYihConll2004Document)documents.get(0);
		ArrayList<Sentence> sentences = doc.getSentences();
		int sentIdx = 0;
		Sentence sentenceGold;;
		Sentence sentencePred;
		
		BufferedReader br = new BufferedReader(new FileReader(taggedFile));
		String line;
		StringBuilder sb = new StringBuilder();
	
		int countRelations=0;
		int tokenId = 0;
		while ((line = br.readLine()) != null) {
			line = line.trim();
			String lastLabel ="";
			if (!line.equals("")) {
				
				String[] fields = line.split("\\s+");
				String word = fields[WORDIDX];
				String label = fields[fields.length-1]; //assume last one is label
				if (goldBoundaries) {
					sentenceGold = sentences.get(sentIdx); //get relations from gold
					
					Token tok = sentenceGold.getTokens().get(tokenId);
					//if (tok.isBegin() && label.equals("O"))
					//	label = "B-MISS";
					if (tok.isBegin() && label.startsWith("I-")) //fix wrong boundary
						label = label.replace("I-", "B-");
					if (tok.isInside() && label.equals("O"))
						label = "I-" + lastLabel;
					//if (tok.isOutside() && !label.equals("O"))
					//	label = "O";
					
				}
				sb.append(word+"/"+label+" ");
				lastLabel = label;
				tokenId++;
				
			} else {
				tokenId =0; //reset
				sentenceGold = sentences.get(sentIdx); //get relations from gold
				sentencePred = Sentence.createSentenceFromNERtaggedInput(sentIdx, sb.toString());
				sb = new StringBuilder(); //reset
				
				for (Relation relation : sentenceGold.getRelationsAsList()) {
							sentencePred.addRelation(relation);
				}
				sentencesOutput.add(sentencePred);
				sentIdx++;
			}
		}
		
		
		int countOutRel=0;
		for (Sentence s : sentencesOutput) {
			System.out.println(s.toRothYihString());
			countOutRel+=s.getRelationsAsList().size();
		}
		
		System.err.println("total relations: "+ countRelations);
		System.err.println("total out rels: "+countOutRel );
		br.close();
	}
	
}
