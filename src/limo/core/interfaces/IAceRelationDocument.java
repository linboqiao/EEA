package limo.core.interfaces;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

import limo.core.Entities;
import limo.core.Entity;
import limo.core.Mention;
import limo.core.Relations;
import limo.core.Sentence;
import limo.core.Token;

import org.jdom.Element;

/**
 * Interface for ACE documents
 * 
 * @author Barbara Plank
 *
 */
public abstract class IAceRelationDocument extends IRelationDocument {
	
	protected String URI = null;
	protected String source = null;
	protected String type = null;
	protected String version = null;
	protected String author = null;
    protected String encoding = "UTF-8";
    protected String docId = null;
	
	protected Entities entities;
	protected File xmlFile;
	protected Element docElement; //the main element of the XML file
	protected String sourceFilename;
	protected Relations relations;
	
	
//	protected ArrayList<ArrayList<DependencyInstance>> dependencyTrees;
//	protected ArrayList<ArrayList<ParseTree>> constituentParseTrees;
//	protected ArrayList<ArrayList<ParseTree>> constituentParseTrees2; //parser 2 (optional)
	
	public abstract boolean initFileReader(File file);
	
	public abstract Entities readEntitiesAndMentionsFromFile();
	
	public abstract Relations readRelationsFromFileAndAddToSentences() throws Exception;
	
	public abstract ArrayList<Sentence> readTextFromFileAndTokenize();
	
	public ArrayList<Sentence> matchTokensAndMentions() {
		try {
		ArrayList<Mention> allMentions = this.getAllMentions();
		ArrayList<Sentence> sentences = this.sentences;
		
		for (Mention mention : allMentions) {
			
			if (mention.getHeadEnd() < sentences.get(0).getAnnotationStartIndex() || 
					mention.getHeadStart() > sentences.get(sentences.size()-1).getAnnotationEndIndex()) {
					//ignoring mention as it is outside the "<TEXT>" part (outside annotation part)
					System.err.println("Ignoring mention as it is outside annotated part: "  + mention.getHead());
					mention.setToIgnore(true);
					continue;
				}
			
			// find mention in sentence and token
			//find out whether a token is part of a mention
			for (Sentence sentence : sentences) {
				if (mention.getHeadStart() >= sentence.getAnnotationStartIndex()
						&& mention.getHeadEnd() <= sentence.getAnnotationEndIndex()) {
					boolean foundPart = false; //if mention spans over multiple tokens
					boolean foundMentionHeadToken = false;
					
					for (Token token : sentence.getTokens()) {
		
						// single tokens (start & end coincide)
						if (token.getAnnotationStartIndex() == mention
								.getHeadStart()
								&& token.getAnnotationEndIndex() == mention
										.getHeadEnd()) {
							token.setBegin();
							mention.addHeadToken(token);   foundMentionHeadToken=true;
							sentence.addMention(mention);
						} 
						// if start index coincide but token is longer
						else if (token.getAnnotationStartIndex() == mention.getHeadStart() && 
								mention.getHeadEnd() < token.getAnnotationEndIndex()) {
							mention.addHeadToken(token); // mention ends before token, e.g. "iranian" is mention of "iranian-based"
							foundMentionHeadToken=true;
							token.setBegin();
							sentence.addMention(mention);
						}
						// found first token of multipart
						else if (token.getAnnotationStartIndex() == mention.getHeadStart() &&
								token.getAnnotationEndIndex() < mention.getHeadEnd()) {
							foundPart = true;
							token.setBegin();
							mention.addHeadToken(token); foundMentionHeadToken=true;
						}
						// if end index coincide but token is longer (starts earlier, e.g. 15-member and mention is member)
						else if (token.getAnnotationEndIndex() == mention.getHeadEnd() && mention.getHeadStart() > token.getAnnotationStartIndex()) {
							mention.addHeadToken(token);  	token.setBegin(); foundMentionHeadToken=true;
							sentence.addMention(mention);
						}
						// found start of multipart but start does not coincide but token starts earlier, like prefix pro-united nations
						else if (token.getAnnotationStartIndex() < mention.getHeadStart() && 
								 token.getAnnotationEndIndex() < mention.getHeadEnd() && 
								 token.getAnnotationEndIndex() > mention.getHeadStart()) {
							foundPart = true; 	token.setBegin();
							mention.addHeadToken(token); foundMentionHeadToken=true;
						}
						// found last part of multipart
						else if (foundPart == true && token.getAnnotationEndIndex() == mention.getHeadEnd()) {
							mention.addHeadToken(token); foundMentionHeadToken=true;
							sentence.addMention(mention); 	token.setInside();
							foundPart = false; //end found
						} else if (foundPart == true && token.getAnnotationEndIndex() < mention.getHeadEnd()) {
							foundPart = true; // continue searching as we are in the middle
							token.setInside();
							mention.addHeadToken(token); foundMentionHeadToken=true;
						} else if (foundPart == true && token.getAnnotationEndIndex() > mention.getHeadEnd()) {
							foundPart = false; // we are at end but token might have punctuation or something else more
							mention.addHeadToken(token); foundMentionHeadToken=true;
							token.setInside();
							sentence.addMention(mention);
						} else if (foundPart==true) {
							System.err.println("ACE2005Document.matchTokensAndMentions problematic: " + mention);
						}
						// single token, mention had is substring 
						// token is larger than mention, e.g. US-Swiss-French and mention is Swiss
						else if (mention.getHeadStart() > token.getAnnotationStartIndex() &&
								 mention.getHeadEnd() < token.getAnnotationEndIndex()) {
							mention.addHeadToken(token); foundMentionHeadToken=true;
							sentence.addMention(mention);
							token.setInside();
						} 
					}
					if (foundMentionHeadToken==false) {
						System.err.println();
						System.err.println("Ignore: Could not find head token for mention!!! "+mention.getHead() + " start/end: "+mention.getHeadStart()+"/"+mention.getHeadEnd());
						System.err.println(sentence + " start/end:" + sentence.getAnnotationStartIndex()+"/"+sentence.getAnnotationEndIndex());
					}
						
				}
			}
			if (mention.getTokenIds().length == 0) {
				System.err.println("hmm... could not match mention to token! (most probably outside annotation,e.g. <SPEAKER>). ignore: "+mention.getHead());
				mention.setToIgnore(true);
			}
				
		}
		
		}
		catch (IllegalAccessException e) {
			e.printStackTrace();
		}
		return sentences;
	}
	
	private ArrayList<Mention> getAllMentions() {
		ArrayList<Mention> mentions = new ArrayList<Mention>();
		
		for (IEntity e : this.entities.getEntities()) {
			Entity entity = (Entity)e;
			for (IMention m : entity.getMentions()) {
				Mention mention = (Mention) m;
				mentions.add(mention);
			}
			
			//hack: fix one mention in ACE 2004  TODO: remove
			if (entity.getId().equals("APW20001221.0431.0227-E12")) {
				Mention mention = (Mention)entity.getMentions().get(0);
				if (mention.getHeadStart() == 963 && mention.getHeadEnd() == 979) {
					mention.setHead("Fort Worth");
					mention.setHeadEnd(972);
				}
				
			}
		}
			
		return mentions;
	}

	/** returns relation if mention participates in relation 
	 * or null otherwise 
	 */
	public IRelation findRelationForMention(Mention mention) {
		for (IRelation r : relations.get()) {
			if (r.getFirstMention().equals(mention) || r.getSecondMention().equals(mention)) {
				return r;
			}
		}
		return null;
	}

	public Entities getEntities() {
		return this.entities;
	}

	public Relations getRelations() {
		return this.relations;
	}

	public ArrayList<Sentence> getSentences() {
		return sentences;
	}
	
	public void saveTokenizedTextAsFile(File outDir) {
		saveTokenizedTextAsFile(outDir.getAbsolutePath() + File.separator + this.URI + ".txt");
	}
	
	private void saveTokenizedTextAsFile(String path) {
		try {
			BufferedWriter writer = new BufferedWriter(new FileWriter(path));
			for (Sentence s : this.sentences) {
				//System.out.println(s);
				writer.write(s.toString() + "\n");
			}
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public String getURI() {
		return this.URI;
	}


	public int getNumSentences() {
		return this.sentences.size();
	}
	
	public Sentence getSentenceById(int sentenceIndex) {
		return this.sentences.get(sentenceIndex);
	}
	
	
	public int getNumEntities() {
		return this.getEntities().size();
	}
	
	public int getNumMentions() {
		int count = 0;
		for (IEntity e : this.entities.getEntities()) {
			count += e.getMentions().size();
		}
		return count;
	}


	public int getNumRelations() {
		return this.relations.get().size();
	}

	
}
