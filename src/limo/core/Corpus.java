package limo.core;

import java.util.ArrayList;
import java.util.Iterator;

import limo.core.interfaces.IRelationDocument;
import limo.io.IRelationReader;
import limo.io.ace2004.ACE2004Reader;

/**
 * Corpus of documents for relation extraction
 * 
 * @author Barbara Plank
 *
 */
public class Corpus {
	
	private ArrayList<IRelationDocument> documents;
	private IRelationReader relationReader;
	private boolean isInitialized;
	private Iterator<IRelationDocument> docIter;
	private boolean skipSentences;

	public Corpus(IRelationReader reader) {
		this.relationReader = reader;
		this.skipSentences = false;
	}
	
	public Corpus(IRelationReader reader, boolean skipSentencesWithoutRelation) {
		this.relationReader = reader;
		this.skipSentences = skipSentencesWithoutRelation;
	}

	public void init() {
		try {
		
		if (this.skipSentences==true)
			this.documents = this.relationReader.readDocuments(skipSentences);
		else
			this.documents = this.relationReader.readDocuments();
		} catch (Exception e) {
			e.printStackTrace();
		}
		this.docIter = documents.iterator();
		this.isInitialized = true;
		
		
	}
	
	public IRelationDocument getNextDocument() {
		if (!isInitialized) 
			this.init();
		else
			if (docIter.hasNext())
				return docIter.next();
		return null;
	}

	public int getNumDocuments() {
		return this.documents.size();
	}

	/***
	 * Get document whose URI starts with startName
	 * or null if document is not found
	 * @param startName
	 * @return
	 */
	public IRelationDocument getDocumentByURI(String startName) {
		Iterator<IRelationDocument> docIter = documents.iterator();
		IRelationDocument doc = docIter.next();
		while (doc != null) {
			if (startName.startsWith(doc.getURI()))
				return doc;
			doc = docIter.next();
		}
		throw new IllegalArgumentException("Document with URI not found: "+startName);
	}
	
	public static void main(String[] args) {
		String path = args[0]; //path to ACE 2004 files
		
		Corpus corpus = new Corpus(new ACE2004Reader(path));
		corpus.init();
		
		IRelationDocument doc = corpus.getNextDocument();
		if (doc != null) {
			doc.getRelations();
			for (Sentence s : doc.getSentences()) {
				System.out.println(s);
				for (Mention m : s.getMentions()) {
					System.out.println(m);
				}
			}
		}
	}
}
