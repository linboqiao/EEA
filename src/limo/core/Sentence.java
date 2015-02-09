package limo.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

import limo.core.interfaces.IEntity;
import limo.core.interfaces.IRelation;

/***
 * A sentence object.
 * Contains also methods to construct a sentence object from a
 * Brown-like annotated sentence (just mentions or both mentions and relations).
 * 
 * @author Barbara Plank
 *
 */
public class Sentence {
	
	private int sentenceId; //starts at 0 
	
	private ArrayList<Token> tokens = new ArrayList<Token>();
	
	private ArrayList<IEntity> entities = new ArrayList<IEntity>();
	
	private ArrayList<Mention> mentions = new ArrayList<Mention>();
	
	private Relations sentenceRelations = new Relations();
	
	private Sentence(int sentenceIndex) { this.sentenceId = sentenceIndex ; } // private constructor
	
	
	private HashMap<String,Mention> helperRelationMentions;
	
	//optional for instance weighting in the kernel function
	private double weight = 1.0;
	
	
	/***
	 * return an empty sentence object
	 * @param sentenceCount
	 * @return Sentence
	 */
	public static Sentence createEmptySentence(int sentenceCount) {
		Sentence sentence = new Sentence(sentenceCount);
		return sentence;
	}
	
	/***
	 * Populate a sentence from a plain string
	 * assumes single whitespace is token boundary
	 * @param index
	 * @param string
	 * @return Sentence object
	 */
	public static Sentence createSentenceFromPlainInput(int sentenceIndex, String sentencePlain) {
		Sentence sentence = new Sentence(sentenceIndex);
		
		String[] fields = sentencePlain.split("(\\s)+"); //split on whitespace
		if (fields == null)
			throw new IllegalArgumentException("sentenceNER is in wrong format or empty! Example: This is New York");
		int tokenCount =0;
		for (String field : fields) {
			if (field.equals("")) //skip empty fields
				continue;
			Token token = new Token(field, tokenCount, sentence);
			tokenCount++;
			
			sentence.addToken(token);
		}
		return sentence;
	}
	
	/***
	 * Populate a sentence from a string tagged with entities
	 * assumes Stanford/Brown tagging scheme: token/tag
	 * where tag consists of ENTITYTYPE_MENTIONTYPE, where _MENTIONTYPE is optional,
	 * e.g. 
	 * Alfred/PER
	 * Alred/PER_NAME
	 * Assumes sentenceString has tokens separated by a whitespace
	 * Also handles IOB tagging,
	 * e.g. Alfred/B-PER from/O New/B-LOC York/I-LOC
	 * TODO: entity Id might be the same for the same "entity" (co-reference?)
	 * @param sentenceIndex
	 * @param sentenceString
	 */
	public static Sentence createSentenceFromNERtaggedInput(int sentenceIndex, String sentenceNER) {
		sentenceNER = sentenceNER.trim();
		Sentence sentence = new Sentence(sentenceIndex);
		
		String[] fields = sentenceNER.split("(\\s)+"); //split on whitespace
		if (fields == null || !fields[0].contains("/"))
			throw new IllegalArgumentException("sentenceNER is in wrong format or empty! "+sentenceNER+"\n Correct format: This/O is/O New/LOCATION York/LOCATION");
		int tokenCount =0;
		
		String oldAnnotation= "";
		
		int entityId = 0;
		
		ArrayList<Token> tokensOfMention = null;
		
		for (String field : fields) {
			if (field.equals("")) //skip empty fields
				continue;
			int lastSlash = field.lastIndexOf("/");
			String tokenStr = field.substring(0, lastSlash);
			String annotation = field.substring(lastSlash+1);
			
						
			Token token = new Token(tokenStr, tokenCount, sentence);
			tokenCount++;
			
			if (annotation.startsWith("I-"))
				token.setInside();
			if (annotation.startsWith("B-"))
				token.setBegin();
			
			sentence.addToken(token);
			
			//if we find an entity mention
			if (annotation.startsWith("I-")) { 
				if (tokensOfMention==null)
					tokensOfMention = new ArrayList<Token>(); //wrongly tagged intput
				tokensOfMention.add(token); //a multi-part mention
				continue;
			}
			else if (annotation.equals(oldAnnotation) && !annotation.equals("O") && !annotation.startsWith("B-")) { 
				tokensOfMention.add(token); //a multi-part mention
				continue;
			} 	
			if (!annotation.equals("O")) {
				if (tokensOfMention != null)
					sentence.addMentionAndEntity(entityId, oldAnnotation, tokensOfMention);
				tokensOfMention = new ArrayList<Token>();
				tokensOfMention.add(token);
				oldAnnotation = annotation;
			}
			if (annotation.equals("O")) {
				if (tokensOfMention != null)
					sentence.addMentionAndEntity(entityId, oldAnnotation, tokensOfMention);
								
				//reset
				tokensOfMention = null;
				oldAnnotation="";
				continue;
			}
			
			entityId++;
			
		}
		if (tokensOfMention != null)
			sentence.addMentionAndEntity(entityId, oldAnnotation, tokensOfMention);
		return sentence;

	}

	
	public static Sentence createSentenceFromRelationTaggedInput(int idx,
			String annotatedSentence) throws Exception {
		return createSentenceFromRelationTaggedInput(idx, annotatedSentence, null, -1);
	}
	
	
	public static Sentence createSentenceFromRelationTaggedInput(int sentenceIndex, String sentenceString, ArrayList<String> symmetricRelations, int maxNumMentions) throws Exception {
		try {
		Sentence s = Sentence.createSentenceFromNERtaggedInput(sentenceIndex, sentenceString);
		
		if (s.helperRelationMentions == null) {
			//throw new Exception("Did not find info on how to map mentions to relations!");
			return s;
		}
		
		HashMap<String, Mention> helper = s.helperRelationMentions;
		HashMap<String, Relation> relationHelper = new HashMap<String, Relation>();
		for (String key : helper.keySet()) {
			
			String[] relations = key.split("\\|");
			Mention mention = (Mention) helper.get(key);
			
			for (String relInfo : relations) {
				String[] fields = relInfo.split("_");
				String relationIndex = fields[0];
				String relationType = fields[1];
				String argumentNum = fields[2];
			
				String relationSubType = null;
				
				if (relationType.contains(".")) {
					String[] parts = relationType.split("\\.");
					relationType = parts[0];
					relationSubType = parts[1];
				}
				
				Relation relation;
				if (relationHelper.containsKey(relationIndex)) 
					relation = relationHelper.get(relationIndex);
				else 
					relation = Relation.createEmptyRelation(relationIndex, relationType, relationSubType);
				
				if (argumentNum.equals("ARG1"))
					relation.setFirstMention(mention);
				else if (argumentNum.equals("ARG2")) 
					relation.setSecondMention(mention);
				else
					throw new Exception("Error with sentence relation argument number. Is it ARG1 or ARG2?");
				
				if (symmetricRelations != null) {
					if (symmetricRelations.contains(relationType))
						relation.setSymmetric(true);
				}
				
				relationHelper.put(relationIndex, relation);
			
			}
			
		}
		for (String relationKey : relationHelper.keySet()) {
			Relation relation = (Relation)relationHelper.get(relationKey);
			if (relation.getFirstMention() == null || relation.getSecondMention()==null) {
				System.err.println("Ignore relation which has missing entities: "+relation.getId());
				if (relation.getFirstMention() != null)
					System.err.println(relation.getFirstMention().getHead());
				else
					System.err.println(relation.getSecondMention().getHead());
				continue;
			}
			if (maxNumMentions != -1) {
				if (s.getNumMentionsInBetween(relation.getFirstMention(), relation.getSecondMention())>maxNumMentions) {
					System.err.println("Ignoring relation as too far apart: "+relation);
					continue;
				}
			}
			s.addRelation(relation);
		}
		return s;
		}
		catch (Exception e) {
			e.printStackTrace();
		
			return null; 
		}
	}
	
	

	private void addMentionAndEntity(int entityId, String annotation,
			ArrayList<Token> tokensOfMention) {
		String entityType = ""; // must be assigned 
		String mentionType = ""; // "null" by default
		
		annotation = annotation.replace("B-", "");
		annotation = annotation.replace("I-", "");
		
		String relationInfo = null;
		
		if (this.helperRelationMentions == null)
			this.helperRelationMentions = new HashMap<String,Mention>();
		
		//keep relation mention info
		
		if (annotation.contains("@")) {
			relationInfo = annotation.substring(annotation.indexOf("@")+1);  // R1_LOC_ARG1
			annotation = annotation.substring(0,annotation.indexOf("@"));
			
		}
		
		//ENTITYTYPE_MENTIONTYPE or ENTITYTYPE.MENTIONTYPE
		if (annotation.contains(".")) {
			entityType = annotation.substring(0, annotation.indexOf("."));
			mentionType = annotation.substring(annotation.indexOf(".")+1);
		} else if (annotation.contains("_")) {
			entityType = annotation.substring(0, annotation.indexOf("_"));
			mentionType = annotation.substring(annotation.indexOf("_")+1);
		} else {
			entityType = annotation;
		}
		
		Entity entity = new Entity(Integer.toString(entityId), entityType);
		Mention mention = new Mention(Integer.toString(entityId), mentionType);
		mention.setEntityReference(entity);
		String mentionHeadAsString = getHeadString(tokensOfMention);
		mention.setHead(mentionHeadAsString);
		mention.setExtend(mentionHeadAsString); // here extend and head is the same
		mention.setHeadTokens(tokensOfMention);
	
		if (relationInfo != null) {
			mention.setIsInRelation(true);
			
			//add relation info to helper (keeps info in which relations mention participates)
			this.helperRelationMentions.put(relationInfo, mention);
		}
			
		entity.addMention(mention);
		entities.add(entity);
		this.addMention(mention);
	}

	private String getHeadString(ArrayList<Token> tokensOfMention) {
		StringBuilder sb = new StringBuilder();
		for (Token token : tokensOfMention)
			sb.append(token.getValue()+" ");
		return sb.toString().trim();
	}

	public void addToken(Token t) {
		t.setIndex(getIndex());
		tokens.add(t);
	}
	
	public ArrayList<Token> getTokens() {
		return tokens;
	}

	private int getIndex() {
		return tokens.size();
	}
	
	/*** Return sentence annotation with mentions ***/
	public String getAnnotatedMentions(Mention mention1, Mention mention2) {
		HashMap<Integer, Mention> tokenIds2Mentions = new HashMap<Integer,Mention>();
		for (int tokenId : mention1.getTokenIds()) {
				tokenIds2Mentions.put(tokenId, mention1);
		}	
		for (int tokenId : mention2.getTokenIds()) {
			tokenIds2Mentions.put(tokenId, mention2);
		}
		
		StringBuilder sb = new StringBuilder();
		Mention lastMention = null;
		for (Token t : tokens) {
			if (tokenIds2Mentions.containsKey(t.getTokenId())) {
				Mention m = tokenIds2Mentions.get(t.getTokenId());
				String prefix = "B-";
				if (lastMention!=null && m.equals(lastMention)) {
					prefix="I-";
				}
				sb.append(t.getValue() + "/" +prefix+ m.getEntityReference().getType() + (m.getType().length()>0 ? "."+m.getType() : ""));
				sb.append(" ");
				lastMention=m;
				
			}
			else {
				sb.append(t.getValue() + "/O");
				sb.append(" ");
				lastMention = null;
			}
			
			
		}
		return sb.toString().trim();
	}
	
	/**
	 * getPlainSentence()
	 * @return plain sentence string
	 */
	public String getPlainSentence() {
		StringBuilder sb = new StringBuilder();
		for (Token t : tokens) {
			sb.append(t.getValue()+ " ");
		}
		return sb.toString().trim();
	}

	
	public String getAnnotatedSentence() {
		return getAnnotatedSentence(false);
	}
	
	/***
	 * Gets the sentence annotated in Brown-like format, e.g.
	 * xperts/O are/O telling/O florida/B-GPE@R1_EMP-ORG_ARG2 's/O lawmakers/B-PER@R1_EMP-ORG_ARG1 to/O act/O cautiously/O ./O
	 * boolean useSubtypes: whether to include entity and relation subtypes (if there are available)
	 * @return annotated sentences one-liner
	 */
	public String getAnnotatedSentence(boolean useSubtypes) {
		HashMap<Integer, Mention> tokenIds2Mentions = new HashMap<Integer,Mention>();
		for (Mention mention : this.getMentions()) {
			for (int tokenId : mention.getTokenIds())
				tokenIds2Mentions.put(tokenId, mention);
		}			
		
		StringBuilder sb = new StringBuilder();
		Mention lastMention = null;
		for (Token t : tokens) {
			if (tokenIds2Mentions.containsKey(t.getTokenId())) {
				Mention m = tokenIds2Mentions.get(t.getTokenId());
				String prefix = "B-";
				if (lastMention!=null && m.equals(lastMention)) {
					prefix="I-";
				}
				if (useSubtypes) {
					sb.append(t.getValue() + "/" +prefix+ m.getEntityReference().getType() + ((m.getEntityReference().getSubtype() != null && 
						m.getEntityReference().getSubtype().length() > 0) ? "."+m.getEntityReference().getSubtype()  : ""));
				}
				else {
					sb.append(t.getValue() + "/" +prefix+ m.getEntityReference().getType());
				}
				lastMention=m;
				
				if (m.isInRelation()) {
					
					StringBuilder sbRel = new StringBuilder();
					
					for (IRelation irel : this.getRelations().get()) {
						 Relation relation = (Relation)irel;
						 if (relation.getFirstMention()==null || relation.getSecondMention()==null)
							 continue;
						 
						 if (relation.getFirstMention().equals(m)) {
							 if (sbRel.length()>0)
								 sbRel.append("|");
							 if (useSubtypes)
								 sbRel.append(relation.getId().replaceAll("_","-")+"_"+relation.getRelationType()+ 
									 ((relation.getRelationSubType() != null && relation.getRelationSubType().length()>0) ? "."+relation.getRelationSubType() : "") +
									 "_ARG1");
							 else 
								 sbRel.append(relation.getId().replaceAll("_","-")+"_"+relation.getRelationType()+ 
										 "_ARG1");
						 }
						 else if (relation.getSecondMention().equals(m)){
							 if (sbRel.length()>0)
								 sbRel.append("|");
							
							 if (useSubtypes)
								 sbRel.append(relation.getId().replaceAll("_","-")+"_"+relation.getRelationType()+ 
									 ((relation.getRelationSubType() != null && relation.getRelationSubType().length()>0) ? "."+relation.getRelationSubType() : "") +
									 "_ARG2");
							 else
								 sbRel.append(relation.getId().replaceAll("_","-")+"_"+relation.getRelationType()+ 
										 "_ARG2");
						 }
					}
					if (sbRel.length()>0)
						sb.append("@"+sbRel.toString());	
				}
				sb.append(" ");
				
			}
			else {
				sb.append(t.getValue() + "/O");
				sb.append(" ");
				lastMention=null;
			}
			
			
		}
		return sb.toString().trim();
	}
	
	public String getNERAnnotatedSentence() {
		HashMap<Integer, Mention> tokenIds2Mentions = new HashMap<Integer,Mention>();
		for (Mention mention : this.getMentions()) {
			for (int tokenId : mention.getTokenIds())
				tokenIds2Mentions.put(tokenId, mention);
		}			
		
		StringBuilder sb = new StringBuilder();
		Mention lastMention = null;
		for (Token t : tokens) {
			if (tokenIds2Mentions.containsKey(t.getTokenId())) {
				Mention m = tokenIds2Mentions.get(t.getTokenId());
				String prefix = "B-";
				if (lastMention!=null && m.equals(lastMention)) {
					prefix="I-";
				}
				sb.append(t.getValue() + "/"+prefix + m.getEntityReference().getType() + (m.getEntityReference().getSubtype().length() > 0 ? "."+m.getEntityReference().getSubtype()  : ""));
				sb.append(" ");
				lastMention = m;
			}
			else {
				sb.append(t.getValue() + "/O");
				sb.append(" ");
				lastMention=null;
			}
		}
		return sb.toString().trim();
	}
	
	public String getNERAnnotatedSentenceSpaceSeparated() {
		return getNERAnnotatedSentenceSpaceSeparated(false);
	}
	
	public String getNERAnnotatedSentenceSpaceSeparated(boolean includeSubType) {
		// output IOB2 scheme: B-begin, I-inside, O-outside
		HashMap<Integer, Mention> tokenIds2Mentions = new HashMap<Integer,Mention>();
		for (Mention mention : this.getMentions()) {
			for (int tokenId : mention.getTokenIds())
				tokenIds2Mentions.put(tokenId, mention);
		}			
		
		StringBuilder sb = new StringBuilder();
		//some local vars
		Mention lastMention = null;
		
		for (Token t : tokens) {
			if (tokenIds2Mentions.containsKey(t.getTokenId())) {
				Mention m = tokenIds2Mentions.get(t.getTokenId());
				String prefix = "B-";
				if (lastMention!=null && m.equals(lastMention)) {
					prefix="I-";
				}
				if (includeSubType)
					sb.append(t.getValue().trim() + " "+prefix + m.getEntityReference().getType() + (m.getEntityReference().getSubtype().length() > 0 ? "."+m.getEntityReference().getSubtype()  : ""));
				else
					sb.append(t.getValue().trim() + " "+prefix + m.getEntityReference().getType());
				sb.append("\n");
				lastMention = m;
				
			}
			else {
				sb.append(t.getValue() + " " +"O");
				sb.append("\n");
				lastMention=null;
			}
		}
		sb.append("\n");
		return sb.toString().trim();
	}


	public int getSentenceId() {
		return sentenceId;
	}

	public void addMention(Mention mention) {
		if (!this.mentions.contains(mention)) {
			this.mentions.add(mention);
			this.entities.add(mention.getEntityReference());
		} 
	}

	public ArrayList<Mention> getMentions() {
		// return sorted mentions
		Collections.sort(this.mentions);
		return this.mentions;
	}

	public Relations getRelations() {
		return this.sentenceRelations;
	}
	
	public ArrayList<Relation> getRelationsAsList() {
		return this.sentenceRelations.get();
	}

	public void addRelation(Relation relation) {
		boolean foundFirst=false;
		boolean foundSecond=false;
		for (Mention m : this.getMentions()) {
			if (m.equals(relation.getFirstMention())) {
				m.setIsInRelation(true);
				foundFirst=true;
			}
			if (m.equals(relation.getSecondMention())) {
				m.setIsInRelation(true);
				foundSecond=true;
			}
		}
		if (!foundFirst && relation.getFirstMention() != null) {
			this.mentions.add(relation.getFirstMention());
		}
		if (!foundSecond && relation.getSecondMention() != null) {
			this.mentions.add(relation.getSecondMention());
		}
		this.sentenceRelations.add(relation);
	}
	
	public int getAnnotationStartIndex() throws IllegalAccessException {
		if (tokens == null) {
			throw new IllegalAccessException("cannot access empty tokens of sentence!");
		}
        return tokens.get(0).getAnnotationStartIndex();
	}
	public int getAnnotationEndIndex()  throws IllegalAccessException {
		if (tokens == null) {
			throw new IllegalAccessException("cannot access empty tokens of sentence!");
		}
        return tokens.get(tokens.size()-1).getAnnotationEndIndex();
	}
	
	public String getAnnotatedSentenceRothYi() {
		//todo
		return toRothYihString();
	}

	public ArrayList<IEntity> getEntities() {
		return this.entities;
	}

	/**
	 * Counts how many mentions are in between
	 * @param firstMention
	 * @param secondMention
	 * @return
	 */
	public int getNumMentionsInBetween(Mention firstMention,
			Mention secondMention) {
		int count=0;
		boolean found=false;
		for (Mention m : this.getMentions()) {
			if (found) { 
				if (m.equals(firstMention) || m.equals(secondMention)) {
					found = false;
					return count;
				} else
					count++;
			} else 
				if (m.equals(firstMention) || m.equals(secondMention))
					found = true;
		}
		return count;
		
	}

	/***
	 * Searches the list of mentions for a mention with a specific headword
	 * @param mention headWord
	 * @return null if mention not found
	 */
	public Mention findMentionWithHead(String headWord) {
		for (Mention m : this.getMentions()) {
			if (m.getHead().equals(headWord)) {
				return m;
			}
		}
		throw new IllegalArgumentException("Mention with head not found: "+ headWord);
	}
	
	/***
	 * Return null if not found rather than throwing exception
	 * @param headWord
	 * @return
	 */
	public Mention findMentionWithHeadSafe(String headWord) {
		for (Mention m : this.getMentions()) {
			if (m.getHead().equals(headWord)) {
				return m;
			}
		}
		return null;
	}
	

	/*** 
	 * Returns mention by checking token ids, if not identical returns null
	 * @param mentionToFind
	 * @return mention or null
	 */
	public Mention findMentionWithTokenIdsSafe(Mention mentionToFind) {
		for (Mention m : this.getMentions()) {
			if (m.sameHeadToken(mentionToFind)) {
				return m;
			}
		}
		return null;
	}
	
	/*** 
	 * Returns mention by checking token ids, also if they just partially overlap
	 * @param mentionToFind
	 * @return mention or null
	 */
	public Mention findMentionWithOverlappingTokenIdsSafe(Mention mentionToFind) {
		for (Mention m : this.getMentions()) {
			if (m.overlapping(mentionToFind)) {
				return m;
			}
		}
		return null;
	}

	public String toRothYihString() {
		return toRothYihString(false);
	}
	/***
	 * get output in roth yih format
	 * @TODO: POS info
	 */
	public String toRothYihString(boolean includeMentionType) {
		StringBuilder sb = new StringBuilder();
		HashMap<Integer,Integer> orgIdsToNew = new HashMap<Integer,Integer>();
		int id = 0; //token index as output (is different as mentions are condensed!)
		for (int i=0; i < tokens.size(); ) {
			Token token = tokens.get(i);
			
			token.setWord(token.getValue().replaceAll("COMMA",","));
			token.setWord(token.getValue().replaceAll("-RRB-",")"));
			token.setWord(token.getValue().replaceAll("-LRB-", "("));

					
			Mention m = getMention(token);
			if (m == null) {
				sb.append(this.getIndex()+ "\tO\t"+id+"\tO\tPOS\t"+token.getValue()+"\tO\tO\tO\n");
				orgIdsToNew.put(token.getTokenId(),id);
			} else {
				if (m.getTokenIds().length > 1) { //multi-word mention
					//join them by /
					StringBuilder sbm = new StringBuilder();
					sbm.append(token.getValue());
					int[] tokenIds = m.getTokenIds();
					//just add the first
					orgIdsToNew.put(tokenIds[0],id);
					for (int j=1; j < tokenIds.length; j++) { 
						token = tokens.get(tokenIds[j]);
						sbm.append("/"+token.getValue());
						i=i+1;//update i, skip over rest
					}
					if (includeMentionType == true && m.getType() != null)
						sb.append(this.getIndex()+ "\t"+m.getEntityReference().getType()+"_"+m.getType()+"\t"+id+"\tO\tPOS\t"+sbm.toString()+"\tO\tO\tO\n");
					else 
						sb.append(this.getIndex()+ "\t"+m.getEntityReference().getType()+"\t"+id+"\tO\tPOS\t"+sbm.toString()+"\tO\tO\tO\n");
				} else {
					if (includeMentionType == true && m.getType() != null)
						sb.append(this.getIndex()+ "\t"+m.getEntityReference().getType()+"_"+m.getType()+"\t"+id+"\tO\tPOS\t"+token.getValue()+"\tO\tO\tO\n");
					else
						sb.append(this.getIndex()+ "\t"+m.getEntityReference().getType()+"\t"+id+"\tO\tPOS\t"+token.getValue()+"\tO\tO\tO\n");
					orgIdsToNew.put(token.getTokenId(),id);
				}
			}
			i++;
			id++;
		}
		ArrayList<Relation> relations = this.getRelationsAsList();
		if (relations.size()>0) {
			//output relations
			sb.append("\n");
			for (Relation relation : relations) {
				int[] tokens1 = relation.getFirstMention().getTokenIds();
				int[] tokens2 = relation.getSecondMention().getTokenIds();
				if (tokens1==null || tokens2 == null) {
					System.err.println("Problem: one mention is null. " + relation.toString() + " "+ this.toString());
					System.exit(-1);
				}
				int idFirst=tokens1[0];
				int idSecond= tokens2[0];
				
				String label = relation.getRelationType();
				if (orgIdsToNew.get(idFirst)==null) {
					System.err.println("Problem! first id null");
					System.exit(-1);
				}
				if (orgIdsToNew.get(idSecond)==null) {
					System.err.println("Problem! second id null");
					System.exit(-1);
				}
				sb.append(orgIdsToNew.get(idFirst)+"\t"+orgIdsToNew.get(idSecond)+"\t"+label+"\n");
			}
			
		}
		return sb.toString();
		
	}

	//return mention for token
	private Mention getMention(Token token) {
		for (Mention m : this.getMentions()) {
			if (m.containsHeadToken(token))
				return m;
		}
		return null;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		for (Token t : tokens) {
			sb.append(t.getValue() + " ");
		}
		return sb.toString().trim();
	}

	public String getTokensTabular() {
		StringBuilder sb = new StringBuilder();
		for (Token t : tokens) {
			sb.append(t.getValue() + "\n");
		}
		return sb.toString().trim();
	}

	public double getWeight() {
		return weight;
	}

	public void setWeight(double weight) {
		this.weight = weight;
	}

}
