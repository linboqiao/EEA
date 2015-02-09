package limo.eval;

import java.util.ArrayList;

import limo.core.Relation;
import limo.core.Relations;
import limo.core.Sentence;
import limo.core.interfaces.IRelationDocument;
import limo.io.relations.RelationsReader;

/***
 * Evaluate relation extraction 
 * Takes brown-like relation annotated data as input.
 * (alternative to scoring SVM predictions with eval.py script)
 * 
 * @author Barbara Plank
 *
 */
public class Eval {
	
	private String goldFile;
	private String predictedFile;
	
	private int maxNumMentions = 3;

	public Eval(String gold, String out) {
		this.goldFile = gold;
		this.predictedFile = out;
	}

	public void setMaxNumMentions(int maxNum) {
		this.maxNumMentions = maxNum;
		
	}

	public void evaluate() throws Exception {
		//read files
 		ArrayList<String> symmetricRelations = new ArrayList<String>();
		symmetricRelations.add("PER-SOC");
		
		RelationsReader goldRelationReader = new RelationsReader(this.goldFile, symmetricRelations, maxNumMentions);
		RelationsReader predRelationReader = new RelationsReader(this.predictedFile,symmetricRelations);
		
		ArrayList<IRelationDocument> docsGold = goldRelationReader.readDocuments();
		ArrayList<IRelationDocument> docsPred = predRelationReader.readDocuments();
		
		if (docsGold.size() != docsPred.size()) {
			System.err.println("Different number of documents");
			System.exit(-1);
		}
		
		int tp=0;
		int fn=0;
		int fp=0;
		int cross=0;
		
		int docIndex = 0;
		for ( ;docIndex < docsGold.size(); docIndex++) {
			IRelationDocument docGold = docsGold.get(docIndex);
			IRelationDocument docPred = docsPred.get(docIndex);
			
			ArrayList<Sentence> sentencesGold = docGold.getSentences(); //assume it contains only sentences with >= 2 mentions
			ArrayList<Sentence> sentencesPred = docPred.getSentences(); //assume it contains same as gold
			
			System.out.println("sentences in gold file: "+sentencesGold.size());
			System.out.println("sentences in pred file: "+sentencesPred.size());
			
			System.out.println("Gold relations:"+docGold.getRelations().size());
			System.out.println("Predicted relations:"+docPred.getRelations().size());
			
			int sentIndexGold = 0;
			for (int sentIndex =0; sentIndex < sentencesPred.size(); sentIndex++) {
				//find gold sentence corresponding to this.
				boolean found = false;
				Sentence predicted = sentencesPred.get(sentIndex);
				Sentence gold = sentencesGold.get(sentIndexGold);
				if (predicted.getPlainSentence().equals(gold.getPlainSentence())) {
					found = true;
					sentIndexGold++;
				}
				else {
					found = false;
					for (int i = sentIndexGold+1; i < sentencesGold.size()-sentIndexGold; i++) { //check following
						gold = sentencesGold.get(i);
						if (predicted.getPlainSentence().equals(gold.getPlainSentence())) {
							found=true;
							sentIndexGold=i;
							break;
						}
					}
					if (found==false)
						System.out.println("not in gold: "+predicted.getPlainSentence());
				}
				if (!predicted.getPlainSentence().equals(gold.getPlainSentence())) {
					System.err.println("something went wrong!");
					System.exit(-1);
				}
				Relations relPred = predicted.getRelations();
				Relations relGold = gold.getRelations();
				
				if (predicted.getPlainSentence().startsWith("WHITE"))
					System.out.println("found");
				
			
				System.out.println(predicted.getRelations().size() + " predicted: "+predicted.getAnnotatedSentence());
				System.out.println(gold.getRelations().size() + " gold:      " + gold.getAnnotatedSentence());
			
				for (Relation p: relPred.get()) {
				
					Relation toCheck = (Relation) relGold.getRelation(p.getFirstMention(), p.getSecondMention());
					System.out.println(p + " " + toCheck);
					if (toCheck == null)
						fp++;
					else {
						if (toCheck.getRelationType().equals(p.getRelationType())) {
							if (toCheck.isSymmetric())
								tp++;
							else {	
								if (toCheck.getFirstMention().equals(p.getFirstMention()) && 
									toCheck.getSecondMention().equals(p.getSecondMention())) {
									tp++;	
								}
								else {
									fp++;//inverted! (same as cross-type)
									fn++;
									cross++;
									//System.out.println(p + " " + toCheck);
								}
							}
						} else {
							fp++;
							fn++;
							cross++;
						}
							
					}
					
				}
				//count those in gold that we didn't see yet (false negatives)
				for (Relation g: relGold.get()) {
					if (relPred.getRelation(g.getFirstMention(),g.getSecondMention())==null) {
						fn++;
						System.out.println("--->"+ g);
					}
				}
				
				System.out.println("TP: "+tp + " FP: "+fp + " FN: " +fn + " cross:" +cross);
				System.out.println("----------------");
					
				
				
			}
		}
		
		System.out.println("TP: "+tp);
		System.out.println("FP: "+fp);
		System.out.println("FN: "+fn);
		System.out.println("cross: "+cross);
		
		System.out.println();
		double p = (double) tp/(tp+fp) *100.0;
		double r = (double) tp/(tp+fn) *100.0;
		double f = (double) (2*p*r)/(p+r);
		System.out.printf("P: %.2f\n", p);
		System.out.printf("R: %.2f\n", r);
		System.out.printf("F: %.2f\n", f);
		
	}
	
	/*public static void main(String[] args) throws Exception {
		CommandLineParser parser = new PosixParser();
		
		//create options
		Options options = new Options();
		options.addOption("g",true, "gold file");
		options.addOption("s",true, "system output");
		options.addOption("m",true, "maximum number of mentions in between to consider in gold file. Default: 3");
		
		try {
			
		
			CommandLine cmd = parser.parse(options, args);

			if (cmd.hasOption("g") && cmd.hasOption("s")) {
		
				String gold = cmd.getOptionValue("g");
				String system = cmd.getOptionValue("s");
				
				Eval eval = new Eval(gold,system);
				
				if (cmd.hasOption("m"))
					eval.setMaxNumMentions(Integer.parseInt(cmd.getOptionValue("m")));
				
				
				eval.evaluate();
			}
			else {
				HelpFormatter formatter = new HelpFormatter();
				formatter.printHelp( "Eval", options );
			}
		} 
		catch (ParseException exp) {
			System.err.println("Unexpected exception: "+exp.getMessage());
		}
		
	}*/

}
