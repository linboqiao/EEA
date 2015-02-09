package limo.io.ner;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;

import limo.core.Entities;
import limo.core.Mention;
import limo.core.Relation;
import limo.core.Relations;
import limo.core.Sentence;
import limo.core.interfaces.IEntity;
import limo.core.interfaces.IRelationDocument;
import limo.core.trees.AbstractTree;
import limo.core.trees.TreeManager;
import limo.core.trees.constituency.ParseTree;


public class StanfordNerDocument extends IRelationDocument{
	
	String URI = null;
	
	private ArrayList<Sentence> sentences;

	private Entities entities;
	private Relations relations;

	private TreeManager treeManager;
	
	public StanfordNerDocument(String fileName, boolean containsParseTrees) throws Exception {
		this.URI = fileName;
		this.sentences = new ArrayList<Sentence>();
		this.entities=new Entities();
		this.relations = null;
		this.treeManager = new TreeManager();
		
		if (URI != null)
			readDocumentFromStdInOrFile(containsParseTrees, fileName);
		else
			readDocumentFromStdInOrFile(containsParseTrees);
	}
	
	private void readDocumentFromStdInOrFile(boolean containsParseTrees) throws Exception {
		readDocumentFromStdInOrFile(containsParseTrees, null);
	}

	private void readDocumentFromStdInOrFile(boolean containsParseTrees, String filename) throws Exception {
		//assumes sentenceNertagged \t tree
		
		BufferedReader br;
		if (filename == null)
			br= new BufferedReader(new InputStreamReader(System.in, "UTF-8"));
		else
			br= new BufferedReader(new FileReader(filename));
	 
	    String str;
		int sentenceIndex=0;
		
		Sentence lastSentence = null; //only used in case we have 3 fields, means that a sentence can appear several times
		ParseTree lastParseTree = null;
	    while ((str = br.readLine()) != null && str.trim().length() > 0) {

	    	String[] fields;
	    	String nerTaggedSentenceStr;
	    	String parseTreeStr;
	 
	    	
	    	if (containsParseTrees) {
	    		fields = str.split("\t"); //assumes ner-tagged-sentence followed by tree 
	    	
	    		if (fields.length < 2)
	    			throw new Exception(StanfordNerDocument.class.getCanonicalName() + " input not in right format! " + str);
	    		
	    		if (fields.length==2) {
	    		
	    			//assume one sentence per line
	    			nerTaggedSentenceStr = fields[0];
	    			parseTreeStr = fields[1];
	    			
	    			ParseTree tree = new ParseTree(parseTreeStr);
	    			Sentence sentence = Sentence.createSentenceFromNERtaggedInput(sentenceIndex, nerTaggedSentenceStr);
	    			this.treeManager.addConstituentTree(sentenceIndex, tree);
	    			
	    			this.entities.addEntities(sentence.getEntities());
	    			this.sentences.add(sentence);
	    			
	    			sentenceIndex++;
	    			
	    		} else if (fields.length==3) {
	    			
	    			int sentenceId = Integer.parseInt(fields[0]);
	    			nerTaggedSentenceStr = fields[1];
	    			parseTreeStr = fields[2];
	    		   	
	    	    	//System.out.println(sentenceId + "\t" + sentenceIndex + "\t" +nerTaggedSentenceStr);
	    	    	//if (sentences.size()>0)
	    	    	
	    	    	
	    	    	lastParseTree = new ParseTree(parseTreeStr);
	    	        if (sentences.size()==0 || (sentences.size()>=1 && sentences.get(sentences.size()-1).getSentenceId() != sentenceId)) {
	    				//assume first column is sentence index, might have same sentence on multiple lines (different marking)
		    		
		    			lastSentence = Sentence.createSentenceFromNERtaggedInput(sentenceIndex, nerTaggedSentenceStr);
		    			this.treeManager.addConstituentTree(sentenceIndex, lastParseTree);
		    		
		    			this.entities.addEntities(lastSentence.getEntities());
		    			this.sentences.add(lastSentence);
		    			
		    			//reset lastSentence otherwise would add it twice
		    			lastSentence = null;
		    			sentenceIndex++;
	    			}
	    			else {
							// sentence still the same
							Sentence sentenceToUpdate = sentences.get(sentences
									.size() - 1); // last sentence
							Sentence sentence = Sentence.createSentenceFromNERtaggedInput(sentenceIndex, nerTaggedSentenceStr);
							// only  mentions
							for (Mention mention : sentence.getMentions())
									sentenceToUpdate.addMention(mention);
							
							lastSentence = sentenceToUpdate;
				
	    			}
					
	    	
	    		}
	    	}
	    	else {
	    		throw new Exception("not yet implemented to read ner tagged sentence only!");
	    	}
	    } 
	 
	
	    br.close();
	
		/*//for "debugging"
		String parseTreeStr = "(S (NP (DT The) (NN president)) (VP (VBD was) (PP (IN in) (NP (NNP Missouri) (NNP Gov.)))) (. .))";
		 
		String nerTaggedSentenceStr = "The/O president/PERSON was/O in/O Missouri/LOCATION Gov./PERSON ./O";
    	ArrayList<ParseTree> treeList = new ArrayList<ParseTree>();
		ParseTree tree = new ParseTree(parseTreeStr);
		treeList.add(tree);
		Sentence sentence = Sentence.createSentenceFromNERtaggedInput(sentenceIndex, nerTaggedSentenceStr);
		this.constituenctParseTrees.add(sentenceIndex, treeList);
		this.entities.addEntities(sentence.getEntities());
		this.sentences.add(sentence);
		sentenceIndex++;
		System.err.println(nerTaggedSentenceStr);
		System.err.println(parseTreeStr);
		*/
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
	public String getURI() {
		return this.URI;
	}

	@Override
	public void saveTokenizedTextAsFile(File outFile) {
		try {
			BufferedWriter writer = new BufferedWriter(new FileWriter(outFile));
			for (Sentence sentence : this.sentences) {
				writer.write(sentence.toString());
			}
			writer.close();
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
		int count = 0;
		for (IEntity e : this.entities.getEntities()) {
			count += e.getMentions().size();
		}
		return count;
	}

	@Override
	public int getNumEntities() {
		return this.entities.size();
	}

	@Override
	public int getNumRelations() {
		if (this.relations == null)
			return 0;
		else
			return this.relations.size();
	}

	@Override
	public AbstractTree getBestConstituentParseTreeBySentenceId(int id) {
		return (ParseTree) this.treeManager.getBestConstituentTreeForSentence(id);
	}

}
