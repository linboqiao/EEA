package limo.io.ry;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;

import limo.core.Entities;
import limo.core.Mention;
import limo.core.Relation;
import limo.core.Relations;
import limo.core.Sentence;
import limo.core.interfaces.IEntity;
import limo.core.interfaces.IRelationDocument;


/***
 * Represents a Roth Yih Relation Document
 * @author Barbara Plank
 *
 */
public class RothYihConll2004Document extends IRelationDocument  {

	private BufferedReader inputReader;
	private String URI;
	
	private static int TOKEN = 5;
	//private static int POS = 4;
	private static int NER = 1;
	private static int TOKENINDEX = 2;
	//private static int SENTENCE_INDEX = 0;
	
	private static int ARG1 = 0;
	private static int ARG2 = 1;
	private static int LABEL = 2;
	
	//private ArrayList<Sentence> sentences;
	private Relations relations;
	private Entities entities;
	
	//private boolean skipSentences;
	
	/***
	 * Example: 
	 *  
	 *  23      Loc     0       O       NNP     PERUGIA O       O       O
	 *  23      O       1       O       ,       COMMA   O       O       O
	 *  23      Loc     2       O       NNP     Italy   O       O       O
	 *  23      O       3       O       -LRB-   -LRB-   O       O       O
	 *  23      Org     4       O       NNP     AP      O       O       O
	 *  23      O       5       O       -RRB-   -RRB-   O       O       O
	 *  
	 *  0       2       Located_In
	 *  4      	0       OrgBased_In
	 *  4       2       OrgBased_In
	 * 
	 * col0: sentenceId
	 * col1: entity label
	 * col2: tokenId
	 * col3: --not used--
	 * col4: pos
	 * col5: word
	 * col6-8: --not used--
	 */
	public RothYihConll2004Document(File file,
			RothYihConll2004Reader rothYihConll2004Reader) throws IOException {
		
		this.URI = file.getName();
		initReader(file);
		readDocument();
	}

	public RothYihConll2004Document(File file,
			RothYihConll2004Reader rothYihConll2004Reader,
			boolean skipSentencesWithoutRelation) throws IOException {
		this.URI = file.getName();
		initReader(file);
		readDocument(skipSentencesWithoutRelation);
	}

	private void readDocument() throws IOException {
		readDocument(false);
	}
	
	private void readDocument(boolean skipSentences) throws IOException {
		
		ArrayList<String[]> lineList;
		ArrayList<Relation> relations = new ArrayList<Relation>();
		ArrayList<IEntity> entities = new ArrayList<IEntity>();
		ArrayList<Sentence> sentences = new ArrayList<Sentence>();
		
		int sentenceIndex = 0; //overall 
		
		while ((lineList = this.getNext()) != null) {
			StringBuilder sentenceNER = new StringBuilder();
			
			int relationCount = 0;
			Sentence sentence = null;
			HashMap<Integer, ArrayList<Integer>> map = new HashMap<Integer,ArrayList<Integer>>(); // keep old and new token indices 
			int tokenIndex = 0;
			for (String[] fields : lineList) {
				validateFields(fields);
				
				ArrayList<Integer> indices = new ArrayList<Integer>(); // keep new indices of old token
				if (fields.length > 3) {
					String token = fields[TOKEN];
					
					token = token.replaceAll("COMMA",",");
					
					//replace ( with -RRB- for parser
					token = token.replaceAll("\\)","-RRB-");
					token = token.replaceAll("\\(","-LRB-");
					
					//String ner = getMappedNer(fields[NER]); //mapping for roth
					String ner = fields[NER];
					
					//String pos = fields[POS];
					
					ArrayList<String> tokens = splitToken(token);
					
					//if (pos.contains("/") || token.contains("/")) {
					if (tokens.size() > 1) {
						//multi-word-unit
						int numParts = tokens.size();
						for (int i=0; i<numParts;i++) {
							if (i==0)
								sentenceNER.append(tokens.get(i)+"/B-"+ner+ " ");
							else
								sentenceNER.append(tokens.get(i)+"/I-"+ner+ " ");
							indices.add(tokenIndex);
							tokenIndex++;
						}
						map.put(Integer.parseInt(fields[TOKENINDEX]), indices);
					}
					else {
						if (ner.equals("O"))
							sentenceNER.append(token+"/"+ner+ " ");
						else {
							sentenceNER.append(token+"/B-"+ner+ " ");
						}
						indices.add(tokenIndex);
						map.put(Integer.parseInt(fields[TOKENINDEX]), indices); //map old token index to new one: key=RothTokenIndex value=tokenIds
						tokenIndex++;
					}
					
					//sentenceIndex = Integer.parseInt(fields[SENTENCE_INDEX]);
				}
				else {
					if (sentence == null)
						sentence = Sentence.createSentenceFromNERtaggedInput(sentenceIndex, sentenceNER.toString().trim());
					
					
					//relation
					String relationLabel = fields[LABEL];
					int indexArg1 = Integer.parseInt(fields[ARG1]);
					int indexArg2 = Integer.parseInt(fields[ARG2]);
					
					
					
					relationCount++;
					String uniqueRelationId = sentenceIndex + "_" + relationCount;
					
					
					Mention mention1 = getMentionFromFields(indexArg1,sentence, map);
					Mention mention2 = getMentionFromFields(indexArg2,sentence, map);
					
					if (mention1 == null && mention2 == null)
						System.err.println("Both mentions not found!");
					else if (mention1 == null) {
						entities.add(mention2.getEntityReference());
						System.err.println("mention1 not found");
					}
					else if (mention2 == null) {
						entities.add(mention1.getEntityReference());
						System.err.println("mention 2 not found");
					}
					else {
						//full relation found
						Relation relation = Relation.createRelation(uniqueRelationId, mention1, mention2, relationLabel, null);
						sentence.addRelation(relation);
						relations.add(relation);
					
						entities.add(mention1.getEntityReference());
						entities.add(mention2.getEntityReference());
					}
				}
			
			}
			if (sentence == null)
				sentence = Sentence.createSentenceFromNERtaggedInput(sentenceIndex, sentenceNER.toString().trim());
			
			if (!sentences.contains(sentence))
				if (skipSentences == true) {
					if (sentence.getRelationsAsList().size()>0) {
						sentences.add(sentence);
						sentenceIndex++;
						//add entities that are not in relation
						for (Mention m : sentence.getMentions()) {
							if (!m.isInRelation())
								entities.add(m.getEntityReference());
						}
						
					}
				} else {
					sentences.add(sentence);
					sentenceIndex++;
					//add entities that are not in relation
					for (Mention m : sentence.getMentions()) {
						if (!m.isInRelation())
							entities.add(m.getEntityReference());
					}
				}
			//System.out.println(">> "+sentenceNER.toString());
			//System.out.println(">> "+sentence.getAnnotatedSentence());
			//System.out.println(sentence);
			
			//Relation relation =Relation.createRelation(uniqueRelationId,
			//		mention1, mention2, type, subtype);
			//relations.add(relation);
			
		}		
		this.relations = new Relations(relations);
		this.entities = new Entities(entities);
		System.out.println(this.entities.size());
		this.sentences = sentences;
	}
	
	private ArrayList<String> splitToken(String token) {
		StringBuilder out = new StringBuilder();
		ArrayList<String> fields = new ArrayList<String>();
		for (int i=0; i<token.length(); i++) {
			char current = token.charAt(i);
			if (current == '/') {
				if (i==0)
					continue;
				
				char before = token.charAt(i-1);
				if (before == '\\')
					out.append(current);
				else {
					// split
					fields.add(out.toString());
					out = new StringBuilder();
				}
			}
			else
				out.append(current);
		}
		//append last
		fields.add(out.toString());
		
		return fields;
	}
	
	// check input
	private void validateFields(String[] fields) {
		if (fields.length == 3) {
			try {
				Integer.parseInt(fields[ARG1]);
				Integer.parseInt(fields[ARG2]);
			} catch (Exception e) {
				System.err.println("Problem reading relation. Check file!");
				System.exit(-1);
			}
		}
		else if (fields.length == 9) {
			try {
				Integer.parseInt(fields[0]);
				Integer.parseInt(fields[TOKENINDEX]);
			} catch (Exception e) {
				System.err.println("Problem reading instance. Check file!");
				System.exit(-1);
			}
		}
		else {
			System.err.println("Problem with input! Not in right format");
			System.exit(-1);
		}
		
	}

	/*
	 * create mention from index plus original fields;
	 * need to take care of multi-word-units
	 */
	private Mention getMentionFromFields(int indexArg,Sentence sentence, HashMap<Integer, ArrayList<Integer>> map) {
		ArrayList<Integer> indices = map.get(indexArg);
		if (indices==null) {
			System.err.println("problem in reading data");
			System.exit(-1);
		}

		int newIndex = indices.get(0); //just use first
		for (Mention m : sentence.getMentions()) {
			int[] tokenIds = m.getTokenIds();
			for (int i=0; i < tokenIds.length; i++) {
				if (tokenIds[i]==newIndex)
					return m;
			}
		}
			
		return null;
	}

//	private String getMappedNer(String ner) {
//		if (ner.equals("Loc"))
//			return "LOCATION";
//		else if (ner.equals("Org"))
//			return "ORGANIZATION";
//		else if (ner.equals("Peop"))
//			return "PERSON";
//		else if (ner.equals("Other"))
//			return "ENTITY";
//		else
//			return "O";
//			
//	}

	/**
	 * get next instance from tabular file
	 */
	private String line = null;
	private ArrayList<String[]> getNext() throws IOException {

		ArrayList<String[]> lineList = new ArrayList<String[]>();
		
		if (line == null || line.length()==0)
			line = inputReader.readLine();
		
		boolean readAnnotation = false;
		boolean done = false;
		while (line != null && !line.startsWith("*") && done==false) {
			lineList.add(line.split("\t"));
			// System.out.println("## "+line);
			line = inputReader.readLine();
			if (line.equals("") && readAnnotation == false) {
				readAnnotation = true;
				//get next and see if it contains relations
				line = inputReader.readLine();
				while (readAnnotation==true &&  line != null && !line.startsWith("*")) {
					if (line.split("\t").length == 3) {
						lineList.add(line.split("\t"));
						line = inputReader.readLine();
					//relations to read
					} 
					else {
						readAnnotation=false;
					}
				}
				done=true;	
			}
		}

		int length = lineList.size();

		//finish
		if (length == 0) {
			inputReader.close();
			return null;
		}
		return lineList;

	}

	private void initReader(File file) throws IOException {
		inputReader = new BufferedReader(new InputStreamReader(
				new FileInputStream(file), "UTF8"));
	}

	@Override
	public Entities getEntities() {
		return this.entities;
	}

	@Override
	public Relations getRelations() {
		return this.relations;
	}

	@Override
	public ArrayList<Sentence> getSentences() {
		return this.sentences;
	}

	@Override
	public int getNumSentences() {
		return this.sentences.size();
	}

	@Override
	public void saveTokenizedTextAsFile(File outDir) {
		saveTokenizedText(outDir.getAbsolutePath() + File.separator + this.URI + ".txt", false); 
		// NOOOO don't assume here default is to include only sentences where a relation exists (assumed also in kate.conll10)
	}
	
	public void saveTokenizedTextAsFile(File outDir, boolean onlySentencesWithRelations) {
		saveTokenizedText(outDir.getAbsolutePath() + File.separator + this.URI + ".txt", onlySentencesWithRelations);
	}
	
	private void saveTokenizedText(String path, boolean onlySentsWithRelations) {
		try {
			ArrayList<Sentence> sentencesNew = new ArrayList<Sentence>();
			 
			BufferedWriter writer = new BufferedWriter(new FileWriter(path));
			for (Sentence s : this.sentences) {
				//System.out.println(s);
				if (onlySentsWithRelations==true) {
					if (s.getRelationsAsList().size()>0) {
						writer.write(s.toString() + "\n");
						sentencesNew.add(s);
					}
				}
				else
					writer.write(s.toString() + "\n");
			}
			writer.close();
			if (onlySentsWithRelations==true)
				this.sentences = sentencesNew; //reduce the sentences!
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	@Override
	public Sentence getSentenceById(int currentIndex) {
		return this.sentences.get(currentIndex);
	}

	@Override
	public int getNumMentions() {
		return this.entities.size(); //in here there is an entity for every mention (unlike in ACE)
	}

	@Override
	public int getNumEntities() {
		return this.entities.size();
	}

	@Override
	public int getNumRelations() {
		return this.relations.size();
	}

	@Override
	public String getURI() {
		return this.URI;
	}
}
