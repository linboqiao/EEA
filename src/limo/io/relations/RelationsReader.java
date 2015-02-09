package limo.io.relations;

import java.io.FilenameFilter;
import java.util.ArrayList;

import limo.core.Relation;
import limo.core.Sentence;
import limo.core.interfaces.IRelationDocument;
import limo.io.IRelationReader;

/***
 * Read relation in in our own format
 * Based on Brown-like tagging but relations are added
 * 
 * @author Barbara Plank
 *
 */
public class RelationsReader implements IRelationReader {

	private String fileName;
	private boolean containsParsedData = true; //default assumes parse tree next to sentence
	
	ArrayList<String> symmetricRelations;
	int maxNumMentions = -1; // if -1 then does not apply, otherwise: max num of mentions in between
	
	public RelationsReader(String file) {
		this.fileName = file;
		this.containsParsedData = false;
	}
	
	public RelationsReader(String file, ArrayList<String> symmetricRelations) {
		this(file);
		this.symmetricRelations = symmetricRelations;
	}
	
	public RelationsReader(String file, boolean containsParsedData) {
		this.fileName = file;
		this.containsParsedData = containsParsedData;
	}
	
	public RelationsReader(String file, boolean containsParsedData, ArrayList<String> symmetricRelations) {
		this(file,containsParsedData);
		this.symmetricRelations = symmetricRelations;
	}
	
	public RelationsReader(String file,
			ArrayList<String> symmetricRelations, int maxNumMentions) {
		this(file,symmetricRelations);
		this.maxNumMentions = maxNumMentions;
	}

	
	@Override
	public ArrayList<IRelationDocument> readDocuments()
			throws Exception {
		return readDocuments(false);
	}
	
	@Override
	public ArrayList<IRelationDocument> readDocuments(boolean skipSentences) throws Exception {
		// we here read just one document
		ArrayList<IRelationDocument> documents = new ArrayList<IRelationDocument>();
		RelationsDocument document = new RelationsDocument(this.fileName, this.containsParsedData, this.symmetricRelations, this.maxNumMentions);
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
		String path = "/home/bplank/project/limosine/tools/limo/limo_release_v0.1/run/PETdecoratedZhangIM-t5-F1-L0.4-c2.4-GRtype-Tr1234-Te0-conf-train-test-charniak-noreparse-zhang-noj.xml-onlyTree/test/out.predicted.class.output";
		RelationsReader reader = new RelationsReader(path,false);
		
		
		ArrayList<String> symmetricRelations = new ArrayList<String>();
		symmetricRelations.add("PER-SOC");
		try {
			
			int countRel=0;
			int countSents = 0;
			ArrayList<IRelationDocument> docs = reader.readDocuments();
			for (IRelationDocument doc : docs) {
				//System.out.println(doc.getNumSentences());
				ArrayList<Sentence> sentences = doc.getSentences();
				int idx = 0;
				for (Sentence s : sentences) {
					countSents++;
					//System.out.println(s);
					System.out.println(s.getSentenceId() + "\t" + s.getAnnotatedSentence());
					Sentence s2 = Sentence.createSentenceFromRelationTaggedInput(idx, s.getAnnotatedSentence(),symmetricRelations,-1);
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
