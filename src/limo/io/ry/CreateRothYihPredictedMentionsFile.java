package limo.io.ry;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;

import limo.core.Relation;
import limo.core.Sentence;
import limo.core.Token;
import limo.core.interfaces.IRelationDocument;

public class CreateRothYihPredictedMentionsFile {

	// read in gold RY file and output from CRF++ tagger
	public static void main(String[] args) throws Exception {
		
		if (args.length < 2) {
			System.out.println("GOLDFILE TAGGEDFILE");
			System.exit(-1);
		}
		boolean goldBoundaries=false; //if we assume gold boundaries given (take label even if it is O)
		if (args.length == 3 && args[2].equals("keep")) {
			goldBoundaries=true;
			System.err.println("Keeping gold boundaries");
		}
		
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
		int countMissing=0;
		int countRelations=0;
		int tokenId = 0;
		while ((line = br.readLine()) != null) {
			line = line.trim();
			
			if (!line.equals("")) {
				
				String[] fields = line.split("\\s+");
				String word = fields[WORDIDX];
				String label = fields[fields.length-1]; //assume last one is label
				if (goldBoundaries) {
					sentenceGold = sentences.get(sentIdx); //get relations from gold
					
					Token tok = sentenceGold.getTokens().get(tokenId);
					if (tok.isBegin() && label.equals("O"))
						label = "B-MISS";
					else if (tok.isBegin() && label.startsWith("I-")) //fix wrong boundary
						label = label.replace("I-", "B-");
					if (tok.isInside() && label.equals("O"))
						label = "I-MISS";
					if (tok.isOutside() && !label.equals("O"))
						label = "O";
					
				}
				sb.append(word+"/"+label+" ");
				tokenId++;
				
			} else {
				tokenId =0; //reset
				sentenceGold = sentences.get(sentIdx); //get relations from gold
				sentencePred = Sentence.createSentenceFromNERtaggedInput(sentIdx, sb.toString());
				sb = new StringBuilder(); //reset
				
				for (Relation relation : sentenceGold.getRelationsAsList()) {

					//check if predicted sentence has two mentions
					if (!goldBoundaries) {
						
						//check if predicted sentence has two mentions
						if (sentencePred.findMentionWithTokenIdsSafe(relation.getFirstMention()) != null && 
							sentencePred.findMentionWithTokenIdsSafe(relation.getSecondMention()) != null)
							sentencePred.addRelation(relation);
						else {
							System.err.println("missing relation: "+ relation);
							countMissing++;
						}
						countRelations++;
						
					} else {
						//just check for overlap
						if (sentencePred.findMentionWithOverlappingTokenIdsSafe(relation.getFirstMention()) != null && 
								sentencePred.findMentionWithOverlappingTokenIdsSafe(relation.getSecondMention()) != null)
								sentencePred.addRelation(relation);
							else {
								System.err.println("missing relation: "+ relation);
								countMissing++;
							}
							countRelations++;
							
					}
				}
				sentencesOutput.add(sentencePred);
				sentIdx++;
			}
		}
		
		// check leftover
		if (sentIdx == sentences.size()-1) {
			sentenceGold = sentences.get(sentIdx); //get relations from gold
			sentencePred = Sentence.createSentenceFromNERtaggedInput(sentIdx, sb.toString());
			sb = new StringBuilder(); //reset
			
			for (Relation relation : sentenceGold.getRelationsAsList()) {
				
				if (!goldBoundaries) {
				
					//check if predicted sentence has two mentions
					if (sentencePred.findMentionWithTokenIdsSafe(relation.getFirstMention()) != null && 
						sentencePred.findMentionWithTokenIdsSafe(relation.getSecondMention()) != null)
						sentencePred.addRelation(relation);
					else {
						System.err.println("missing relation: "+ relation);
						countMissing++;
					}
					countRelations++;
					
				} else {
					//just check for overlap
					if (sentencePred.findMentionWithOverlappingTokenIdsSafe(relation.getFirstMention()) != null && 
							sentencePred.findMentionWithOverlappingTokenIdsSafe(relation.getSecondMention()) != null)
							sentencePred.addRelation(relation);
						else {
							System.err.println("missing relation: "+ relation);
							countMissing++;
						}
						countRelations++;
						
				}
			}
			sentencesOutput.add(sentencePred);
			sentIdx++;
		}
		
		int countOutRel=0;
		for (Sentence s : sentencesOutput) {
			System.out.println(s.toRothYihString());
			countOutRel+=s.getRelationsAsList().size();
		}
		
		System.err.println("total relations: "+ countRelations);
		System.err.println("missing   rels: "+countMissing  + " (due to 1/2 missing entities)"); 
		System.err.println("total out rels: "+countOutRel + " (must be: "+(countRelations-countMissing)+")");
		br.close();
	}
	
}
