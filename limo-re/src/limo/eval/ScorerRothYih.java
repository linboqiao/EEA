package limo.eval;

import java.text.DecimalFormat;
import java.util.ArrayList;

import limo.core.Relation;
import limo.core.Sentence;
import limo.core.interfaces.IRelationDocument;
import limo.io.ry.RothYihConll2004Document;
import limo.io.ry.RothYihConll2004Reader;

public class ScorerRothYih {
	

	
	public static void main(String[] args) throws Exception {
		//assumes roth yih format
		
		String goldFile = args[0];
		String predFile = args[1];
		
		int maxNumMentionsInBetween = 3;
		
		RothYihConll2004Reader readerGold = new RothYihConll2004Reader(goldFile);
		RothYihConll2004Reader readerPred = new RothYihConll2004Reader(predFile);
		
		ArrayList<IRelationDocument> documentsG, documentsP;
	
		documentsG = readerGold.readDocuments();
		documentsP = readerPred.readDocuments();
			
		//will be just one
		RothYihConll2004Document docGold = (RothYihConll2004Document)documentsG.get(0);
		RothYihConll2004Document docPred = (RothYihConll2004Document)documentsP.get(0);
		
		ArrayList<Sentence> sentencesGold = docGold.getSentences();
		ArrayList<Sentence> sentencesPred = docPred.getSentences();
		
		if (sentencesGold.size() != sentencesPred.size()) {
			System.err.println("not the same number of sentences!");
			System.exit(-1);
		}
		
		int tp=0;
		int fp=0;
		int fn=0;
		int crosstype=0;
		
		int totalGold=0;
		int totalPred=0;
		
		int countSkippedToFar=0;
		
		int realFN=0;
		
/*		for (int i=0; i < sentencesGold.size(); i++) {
			Sentence gold = sentencesGold.get(i);
			Sentence pred = sentencesPred.get(i);
			
			if (!gold.toString().equals(pred.toString())) {
				System.err.println("error evaluating different sentences!");
				System.exit(-1);
			}
				
			
			ArrayList<Relation> goldRels = gold.getRelationsAsList();
			ArrayList<Relation> predRels = pred.getRelationsAsList();
			
			totalGold+=goldRels.size();
			totalPred+=predRels.size();
			
			ArrayList<String> seenRelations = new ArrayList<String>();
			
			if (goldRels.size() == 0 && predRels.size() == 0)
				continue;
			else {
				//relation to evaluate
				for (Relation relation : predRels) {
					seenRelations.add(relation.toShortString());				
					//System.out.println(tp + " " + fp + " " + fn);
					if (goldRels.contains(relation)) {
						//find gold 
						for (Relation g : goldRels)
							if (g.equals(relation)) {  //means same mentions in any order!
								//if (g.getRelationType().equals(relation.getRelationType())) //this does not consider order of mentions! 
								//	tp++;
								//check first if symmetric
								//if (g.isSymmetric() && g.getRelationType().equals(relation.getRelationType())) {
								if (g.getRelationType().equals("PER-SOC") && g.getRelationType().equals(relation.getRelationType())) {
									tp++;
								}
								else if (g.getFirstMention().equals(relation.getFirstMention()) && 
									g.getSecondMention().equals(relation.getSecondMention()) &&
									g.getRelationType().equals(relation.getRelationType()))
									tp++;
								else if (g.getFirstMention().equals(relation.getSecondMention()) && 
										g.getSecondMention().equals(relation.getFirstMention()) &&
										g.getRelationType().equals(relation.getRelationType())) {
									 // inverted mentions!
									//System.out.println("**** INVERTED*");
									crosstype++;
									fp++;
									fn++;
								}
								else {
									//System.out.println("*****");
									///System.out.println("gold: "+ g);
//									//.out.println("pred: "+ relation);
//									crosstype++;
//									fp++;
//									fn++;
								}
								break;
							}
					}
					else {
						fp++;
					}
				}
				for (Relation relation : goldRels) {
					if (gold.getNumMentionsInBetween(relation.getFirstMention(), relation.getSecondMention()) > maxNumMentionsInBetween+1) {
						System.out.println("skipping as too many mentions in between: "+relation + " "+ gold.getNumMentionsInBetween(relation.getFirstMention(), relation.getSecondMention()));
						countSkippedToFar++;
						continue;
					}
					if (seenRelations.contains(relation.toShortString()))
						continue;
					//find if it is in predicted relations
					boolean seen=false;
					for (Relation p : predRels) {
						if (p.equalsIncludingRelationType(relation)) {
							seen=true;
							break;
						}
						else if (p.equals(relation)) { //equals mentions but ignores order
							seen=true;
							break;
						} else if (p.getRelationType().equals("PER-SOC")) {
							if ((p.getFirstMention().equals(relation.getFirstMention()) && p.getSecondMention().equals(relation.getSecondMention())) || 
							 	 p.getFirstMention().equals(relation.getSecondMention()) && p.getSecondMention().equals(relation.getSecondMention())) {
								seen=true;
								break;
							}
							
						}
					}
					if (seen==false) {
						//System.out.println("missing rel " +relation);
						fn++;
					}
				}
				
			}
		}*/
		
		
		
		for (int i = 0; i < sentencesGold.size(); i++) {
			Sentence gold = sentencesGold.get(i);
			Sentence pred = sentencesPred.get(i);
			
		

			if (!gold.toString().equals(pred.toString())) {
				System.err.println("error evaluating different sentences!");
				System.err.println("Gold: "+gold.toString());
				System.err.println("Pred: "+pred.toString());
				System.exit(-1);
			}
		
			ArrayList<Relation> goldRels = gold.getRelationsAsList();
			ArrayList<Relation> predRels = pred.getRelationsAsList();

			totalGold += goldRels.size();
			totalPred += predRels.size();

			//set symmetric relations first
			for (Relation relation : goldRels) {
				if (relation.getRelationType().equals("PER-SOC"))
					relation.setSymmetric(true);
			}

			
			for (Relation relation : predRels) {
				if (goldRels.contains(relation)) { // there is a relation in
					      				// gold between M1 and M2
					// find it
					for (Relation g : goldRels) {
						
						if (relation.equalsIncludingRelationType(g)) {
							if (g.isSymmetric()) {
								tp++;
								g.setVisited(true);
							} else {
								// check order
								if (relation.getFirstMention().equals(
										g.getFirstMention())
										&& relation.getSecondMention().equals(
												g.getSecondMention())) {
									tp++;
									g.setVisited(true);
								} else {
									fp++; // same label but inverted mentions
									fn++;
									crosstype++;
									g.setVisited(true);
								}
							}
						} else if (relation.equals(g)) { // same mentions but
															// not same label
								fp++;
								fn++;
								crosstype++;
								g.setVisited(true);
						}	
					}
					
				} // not in gold
				else {
					fp++;
				}
			}

			
			for (Relation relation : goldRels) {
				if (!relation.isVisited()) {
					if (gold.getNumMentionsInBetween(relation.getFirstMention(), relation.getSecondMention()) > maxNumMentionsInBetween) {
						System.out.println("skipping as too many mentions in between: "+relation + " "+ gold.getNumMentionsInBetween(relation.getFirstMention(), relation.getSecondMention()));
						countSkippedToFar++;
						continue;
					}
					fn++;
					realFN++;
				}
			}
		}

		System.out.println("total gold: " + totalGold + "\ntotal pred: "
				+ totalPred);
		System.out.println("total too far: " + countSkippedToFar);
		System.out.println("TP: " + tp);
		System.out.println("FP: " + fp);
		System.out.println("FN: " + fn);
		System.out.println("realFN: " + realFN);
		System.out.println("Crosstype: " + crosstype);

		System.out.println();
		float precision = (tp / (float) (tp + fp)) * 100;
		float recall = (tp / (float) (tp + fn)) * 100;
		float f1 = (2 * precision * recall) / (float) (precision + recall);
		DecimalFormat df = new DecimalFormat("#0.0");
		System.out.println("P: " + df.format(precision));
		System.out.println("R: " + df.format(recall));
		System.out.println("F: " + df.format(f1));
		
		
			
	}

}
