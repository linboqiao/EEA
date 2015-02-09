package limo.io.relations;

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
import limo.core.Relation;
import limo.core.Relations;
import limo.core.Sentence;
import limo.core.interfaces.IEntity;
import limo.core.interfaces.IRelationDocument;

/***
 * Represents a document of Relations
 * 
 * @author Barbara Plank
 *
 */
public class RelationsDocument extends IRelationDocument{
	
	String URI = null;
	
	private ArrayList<Sentence> sentences;

	private Entities entities;
	private Relations relations;
	
	ArrayList<String> symmetricRelations;
	int maxNumMentions=-1;

	public RelationsDocument(String fileName, boolean containsParseTrees, ArrayList<String> symmetricRelations, int maxNumMentions) throws Exception {
		this.URI = fileName;
		sentences = new ArrayList<Sentence>();
		entities=new Entities();
		relations = null;
		this.maxNumMentions = maxNumMentions;
		this.symmetricRelations = symmetricRelations;
		if (URI != null)
			readDocumentFromFile(fileName);
		else
			readDocumentFromStdIn();
	}

	private void readDocumentFromStdIn() throws Exception {
		//assumes relationTaggedSentence
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in, "UTF-8"));

	    String str;
		int sentenceIndex=0;
	    while ((str = br.readLine()) != null && str.trim().length() > 0) {
    	
	    	String relationTaggedSentenceStr = str.trim();
	    	
			Sentence sentence = Sentence.createSentenceFromRelationTaggedInput(sentenceIndex, relationTaggedSentenceStr, symmetricRelations,maxNumMentions);
			this.entities.addEntities(sentence.getEntities());
			this.sentences.add(sentence);
			sentenceIndex++;
	    } 

	    br.close();
	}

	private void readDocumentFromFile(String fileName) {
		try {
			
			BufferedReader reader = new BufferedReader(new FileReader(fileName));
			ArrayList<Relation> relations = new ArrayList<Relation>();
			
			String line;
			int sentenceIndex=0;
			while ((line = reader.readLine()) != null) {
				
				Sentence sentence = Sentence.createSentenceFromRelationTaggedInput(sentenceIndex, line.trim(), symmetricRelations, maxNumMentions);
				sentenceIndex++;
				this.entities.addEntities(sentence.getEntities());
				this.sentences.add(sentence);
				relations.addAll(sentence.getRelationsAsList());
				sentenceIndex++;
			}
			reader.close();
			this.relations = new Relations(relations);
			
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
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

}
