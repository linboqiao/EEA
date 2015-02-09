package limo.io.ner;

import java.io.FilenameFilter;
import java.util.ArrayList;

import limo.core.Relation;
import limo.core.Sentence;
import limo.core.interfaces.IRelationDocument;
import limo.io.IRelationReader;

public class StanfordNerReader implements IRelationReader {

	private String fileName;
	private boolean containsParsedData = true; //default assumes parse tree next to sentence
	
	public StanfordNerReader(String file, boolean containsParsedData) {
		this.fileName = file;
		this.containsParsedData = containsParsedData;
	}
	
	public StanfordNerReader(String file) {
		this.fileName = file;
	}
	
	public ArrayList<IRelationDocument> readDocuments() throws Exception {
		return readDocuments(false);
	}
	
	@Override
	public ArrayList<IRelationDocument> readDocuments(boolean skipSentences) throws Exception {
		// we here read just one document
		ArrayList<IRelationDocument> documents = new ArrayList<IRelationDocument>();
		StanfordNerDocument document = new StanfordNerDocument(this.fileName, this.containsParsedData);
		documents.add(document);
		return documents;
	}

	@Override
	public String getPath() {
		return this.fileName;
	}

	@Override
	public FilenameFilter getFilenameFilter() {
		//not implemented as we only (for now) read a single file
		return null;
	}
	
	public static void main(String[] args) {
		//StanfordNerReader reader = new StanfordNerReader("/home/bplank/project/limosine/tools/limo/nw_bn.gold.parse");
		//StanfordNerReader reader = new StanfordNerReader("/home/bplank/project/limosine/tools/limo/cts.gold.parse");
		//StanfordNerReader reader = new StanfordNerReader("/home/bplank/project/limosine/tools/limo/experiments_ace2005/run/data_ace2005/bc.pred.parse");
		//String path = "/home/bplank/project/limosine/tools/limo/limo_release_v0.1/run/PETdecoratedZhangIM-t5-F1-L0.4-c2.4-GRtype-Tr1234-Te0-conf-train-test-charniak-noreparse-zhang-noj.xml-onlyTree/test/out.predicted.class.output";
		String path = args[0];
		//StanfordNerReader reader = new StanfordNerReader(path,false);
		StanfordNerReader reader = new StanfordNerReader(path);
		try {
			
			int countRel=0;
			int countSents = 0;
			ArrayList<IRelationDocument> docs = reader.readDocuments();
			for (IRelationDocument doc : docs) {
				System.out.println(doc.getNumSentences());
				ArrayList<Sentence> sentences = doc.getSentences();
				int idx = 0;
				for (Sentence s : sentences) {
					countSents++;
					System.out.println(s);
					System.out.println(s.getAnnotatedSentence());
					Sentence s2 = Sentence.createSentenceFromRelationTaggedInput(idx, s.getAnnotatedSentence());
					ArrayList<Relation> relations = s2.getRelationsAsList();
					for (Relation r : relations) {
						System.out.println(r);
						countRel++;
					}
					idx++;
				}
			}
			System.out.println("Relations: "+countRel + " Sentences: "+countSents);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
