package limo.modules;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.NavigableSet;
import java.util.Set;

import limo.cluster.BrownWordCluster;
import limo.cluster.io.BrownWordClusterReader;
import limo.cluster.io.ScoreReader;
import limo.core.Mention;
import limo.core.Relation;
import limo.core.Sentence;
import limo.exrel.data.Classification;
import limo.exrel.data.ClassificationScores;
import limo.exrel.utils.Index;
import limo.exrel.utils.Index.OpenMode;
import limo.exrel.slots.BooleanSlot;
import limo.exrel.slots.FileSlot;
import limo.exrel.slots.OutputFileSlot;
import limo.exrel.modules.AbstractModule;

/***
 * Take classifiers' output and compute label
 * - if we have a NONE classifier, take max
 * - else take relation if above 0 (=threshold)
 * 
 * Outputs result to stdout or file -> assembles sentence that can have multiple relations (starting with R1)
 * 
 * @author Barbara Plank
 *
 */
public class Disambiguator extends AbstractModule {

	public Disambiguator(String moduleId, String configId) {
		super(moduleId,configId);
	}

	public FileSlot TRMScoresIdxFile = new FileSlot(true);
	public OutputFileSlot outPropositionsIdxFile = new OutputFileSlot(true);
	
	public OutputFileSlot goldIdxFile = new OutputFileSlot(true);
	
	public BooleanSlot writeOutputToFile = new BooleanSlot("false"); //default is to write to stdout; if true: write to outPropositionsIdxFile+.output
	public FileSlot compatibilityCheckFile = new FileSlot(false); //list of accepted entity for relation
	
	 //columns in gold file
	// 0: label of instance, 2: entity types in format LOC@PER 
	private static int COL_QID_TREE =1;
	private static int COL_ENTITIES=2;
	private static int COL_ANNSEN=3; //answer annotation in our own annotation format
	
	@Override
	protected void _run() throws UnsupportedEncodingException {
		Index scores = new Index(TRMScoresIdxFile.get(), OpenMode.READ);
		Index outProps = new Index(outPropositionsIdxFile.get(), OpenMode.WRITE);
		
		Index gold = new Index(goldIdxFile.get(), OpenMode.READ);
		
		message("Disambiguating... One vs. all.");
		boolean includeNoneClassifier = false;
		
		ArrayList<String> compatibleEnitities = null;
		
		if (compatibilityCheckFile.get() != null) {
			try {
			message("Loading compatibility check file.. " + compatibilityCheckFile.get().getAbsolutePath());
			compatibleEnitities = new ArrayList<String>();
			
			BufferedReader inputReader = new BufferedReader(new InputStreamReader(
					new FileInputStream(compatibilityCheckFile.get().getAbsolutePath()), "UTF8"));
			
			String line = inputReader.readLine();
			while (line != null && !line.equals("") && !line.startsWith("#")) {
				String[] lineList = line.split("\t");
				if (lineList.length!=3) {
					System.err.println("Problem with line (not 3 columns): "+line);
					System.exit(-1);
				}
				
				String relAndEntities = lineList[0]+":"+lineList[1]+"@"+lineList[2]; //format:   relation:TypeArg1@TypeArg2
				compatibleEnitities.add(relAndEntities);
				line = inputReader.readLine();
			}

			System.err.println("Compatibility list loaded:");
			for (String c : compatibleEnitities)
				System.err.println(c);
			System.err.println("************************");
			
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
		}
		
		
		//result
		ArrayList<Sentence> sentences = new ArrayList<Sentence>();
		
		//for (IndexKey key : scores.allKeys()) {
		for (Integer key : scores.allKeys()) {
			//String line = scores.get(key);
			ClassificationScores allScores = ClassificationScores.deserialize(scores.get(key));
			Classification last = allScores.last();  //get the max
			
			//check if compatible (if active):
			if (compatibleEnitities != null) {
				
				boolean isCompatible = checkCompatibility(last,gold,key,compatibleEnitities);
				if (isCompatible==false) {
					Iterator<Classification> descScores = allScores.descendingIterator();
					descScores.next(); //skip current last
					while (descScores.hasNext() && isCompatible==false) {
						last = descScores.next();
						isCompatible = checkCompatibility(last,gold,key,compatibleEnitities);
						if (isCompatible==false)
							System.err.println("not compatible, take next. Skipping: "+last.getLabel());
					}	
					if (isCompatible == false) {
						//still false, backup to last one
						last = allScores.last();
					}
				}
			}
			
			
			if (allScores.get("NONE") != null) {
				//we've trained the NONE classifier
				includeNoneClassifier=true;
			}
			
			if (includeNoneClassifier) {
				String label = last.getLabel();
				outProps.put(label, key);
				
				outputGold(gold, label, key, sentences);
			} 
			else {
				// if no NONE classifier, take NONE if no classifier gave positive result
				if (last.getScore() <= 0) { //default threshold (needs tuning)
					//default fallback to none
					outProps.put("NONE", key);
					
					outputGold(gold, "NONE", key, sentences);
				} else {
					//take label
					String label = last.getLabel();
					outProps.put(label, key);
					
					outputGold(gold, label, key, sentences);
				}
			}
			
		
		}
		
		if (writeOutputToFile.get()==false) {
			PrintWriter pw = new PrintWriter(new OutputStreamWriter(System.out, "UTF-8"));
			for (Sentence s : sentences) {
					pw.println(s.getAnnotatedSentence());
					//pw.println(s.toRothYihString(true));
					pw.flush();
			}
			pw.close();
		}
		else {
			String fileName=outPropositionsIdxFile.get()+".output";
			PrintWriter pw;
			try {
				pw = new PrintWriter(new OutputStreamWriter(new FileOutputStream(fileName), "UTF-8"));
		
				for (Sentence s : sentences) {
					//pw.println(s.getAnnotatedSentence());
					//System.out.println(s);
					pw.println(s.toRothYihString(true));
					pw.flush();
				}
				pw.close();
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}
		}
		message("Done.");
		outProps.close();
		scores.close();
	}

	private boolean checkCompatibility(Classification last, Index gold,
			Integer key, ArrayList<String> compatibleEnitities) {
	
		String label = last.getLabel();
		if (!label.equals("NONE")) {
		
			String goldLine = gold.get(key);
			String[] fields = goldLine.split("\t");
			String entities = fields[COL_ENTITIES];
			String sentenceWithEntities = fields[COL_ANNSEN];
			try {
				Relation relation = Relation.createRelationFromClassificationResult("R1", sentenceWithEntities, label);
				
				String rel = relation.getRelationType() + ":"+entities;
				if (entities.equals("MISS@MISS"))
					rel = "*:MISS@MISS";
				if (entities.contains("MISS@"))
					rel = "*:MISS@*";
				if (entities.contains("@MISS"))
					rel = "*:@MISS*";
				
				
				if (label.endsWith(".B-A")) { //inverted, so have to invert entities
					String[] split = entities.split("@");
					entities = split[1]+"@"+split[0];
					rel = relation.getRelationType() + ":"+entities;
				}
				System.err.println(rel);
				if (!compatibleEnitities.contains(rel)) {
					return false;
				}
				else 
					return true;
				
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		return true; //None is always compatible
		
	}

	private void outputGold(Index gold, String label, Integer key, ArrayList<Sentence> sentences) {
		String goldLine = gold.get(key);
		String[] fields = goldLine.split("\t");
		String qid = fields[COL_QID_TREE].split(" ")[0];
		int sentenceIndex = Integer.parseInt(qid.replace("qid:",""));

		String sentenceWithEntities = fields[COL_ANNSEN]; // contains only 2 target mentions, need to aggregate them!
		
		//System.out.println(goldLine);
		Relation relation;
		try {
			Sentence sentence = Sentence.createSentenceFromNERtaggedInput(sentenceIndex, sentenceWithEntities);
			
			if (sentences.size()==0) {
				if (!label.equals("NONE")) {
					//if we found the first relation, add it to sentence
					relation = Relation.createRelationFromClassificationResult("R1", sentenceWithEntities, label);
					sentence.addRelation(relation);
				}
				sentences.add(sentence);
				
			}
			else {
				int lastSentenceIndex = sentences.get(sentences.size()-1).getSentenceId();
				if (sentenceIndex == lastSentenceIndex) {
					//sentence still the same
					Sentence sentenceToUpdate = sentences.get(sentences.size()-1); // last sentence
					int numRel = sentenceToUpdate.getRelations().size();
					if (!label.equals("NONE")) {
						relation = Relation.createRelationFromClassificationResult("R"+(numRel+1), sentenceWithEntities, label);
						sentenceToUpdate.addRelation(relation);
					} else {
						//only add mentions
						for (Mention mention : sentence.getMentions())
							sentenceToUpdate.addMention(mention);
					}
				} else {
					//new sentence
					if (!label.equals("NONE")) {
						relation = Relation.createRelationFromClassificationResult("R1", sentenceWithEntities, label);
						sentence.addRelation(relation);
					}
					sentences.add(sentence);
				}
							
			}
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}