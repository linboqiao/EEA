package limo.io.ace2004;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.HashMap;

import limo.core.Relation;
import limo.core.Relations;
import limo.core.Sentence;
import limo.core.interfaces.IRelation;
import limo.core.interfaces.IRelationDocument;
import limo.io.IRelationReader;
import limo.io.Utils;

public class ACE2004Reader implements IRelationReader {
	
	private String path;
	private ACE2004FilenameFilter filenameFilter; 
	
	// if we ignore certain relations
	ArrayList<String> relationsToIgnoreIds;
	
	//HashMap<String,String> relationMapping; // if we need a mapping, like from ACE 2004 to 2005 //removed code now
	//boolean useMapping = false;
	
	
	HashMap<String, String> entityMapping;
	ArrayList<String> keepRelations;
	
	public ACE2004Reader(String path) {
		this.path = path;
		this.filenameFilter = new ACE2004FilenameFilter();
	}
	
	public ACE2004Reader(String path, String pathIgnoreRelations) {
		this.path = path;
		this.filenameFilter = new ACE2004FilenameFilter();
		this.relationsToIgnoreIds = Utils.readIgnoreFile(pathIgnoreRelations);
	}
	
	public ACE2004Reader(String path, String entityMappingFile, String keepRelationsFile) throws Exception {
		this.path = path;
		this.filenameFilter = new ACE2004FilenameFilter();
		this.entityMapping = Utils.readEntityMappingFile(entityMappingFile);
		this.keepRelations = Utils.readIgnoreFile(keepRelationsFile);
	}

	public String getPath() {
		return this.path;
	}
	
	public FilenameFilter getFilenameFilter() {
		return this.filenameFilter;
	}
	
	@Override
	public ArrayList<IRelationDocument> readDocuments() {
		return readDocuments(false);
	}
	
	@Override
	public ArrayList<IRelationDocument> readDocuments(boolean skipSentences) {
		// read all ACE (APF) files from path (or doc if path is a document)
		ArrayList<IRelationDocument> documents = new ArrayList<IRelationDocument>();
		File file = new File(this.path);
		if (file.isDirectory()) {

			File[] fs_annotated = file.listFiles(this.getFilenameFilter());
			for (int i = 0; i < fs_annotated.length; i++) {
				//System.out.println("Processing file: "+ fs_annotated[i]);
				ACE2004Document doc = new ACE2004Document(fs_annotated[i], this);
				documents.add(doc);
			}
		} else {
			// read single document
			File xmlfile = new File(this.path);
			ACE2004Document doc = new ACE2004Document(xmlfile, this);
			documents.add(doc);
		}
		return documents;
	}
	
	/**
	 * Using mapping of relations as specified in file
	 * If relation not in file, ignore it
	 * @param pathToRelationMappingFile
	 */
	/*public void setRelationMapping(String pathToRelationMappingFile) {
		this.relationMapping = Utils.readRelationMapping(pathToRelationMappingFile);
	}*/

	public static void main(String[] args) {
		//String path = "/home/bplank/project/limosine/tools/relation_extraction/data/ace2004/BNEWS_NWIRE/";
		
	    String path = args[0];
	    String ignore = "/home/bplank/project/limosine/tools/limo/ignoreRelations.txt";
		ACE2004Reader ace2004reader = new ACE2004Reader(path, ignore);
	    //ACE2004Reader ace2004reader = new ACE2004Reader(path);
		ArrayList<IRelationDocument> documents = ace2004reader.readDocuments();
		int lineNum= 1;
		int countLess = 0;
		
		int countTotalSents = 0;
		int countSentsWith1Rel = 0;
		int countSentsWithMoreThan1Rel = 0;
		
		for (IRelationDocument d : documents) {

			ACE2004Document doc = (ACE2004Document) d;
			/*System.out.println();
			System.out.println(doc.URI + ": ");
			System.out.print(doc.getNumSentences() + " sentences. ");
			System.out.print(doc.getEntities().size() + " entities.");
			System.out.println("@ " + doc.getRelations().size() + " relations.");
			*/
			ArrayList<Sentence> sentences =  doc.getSentences();
			
			int countRels = 0;
		
			for (Sentence sentence : sentences) {
				Relations relations = sentence.getRelations();
				//if (relations.size() ==0)
				//	System.out.println("SENTENCE HAS NO RELATIONS");
				countRels += relations.size();
				
				if (relations.size()==1)
					countSentsWith1Rel++;
				else
					if (relations.size()>1)
						countSentsWithMoreThan1Rel++;
				countTotalSents++;
				
				for (IRelation irelation : sentence.getRelationsAsList()) {
					Relation relation = (Relation) irelation;
					//System.out.print(lineNum+ " " +relation);
					//System.out.println(" "+ sentence);
					String head1 = relation.getFirstMention().getHead();
					String head2 = relation.getSecondMention().getHead();
					
					//String output = sentence.getAnnotatedRelation(relation);
					String output = "[arg1: "+head1 + ", arg2: " +head2 + "] -- " +sentence.toString();
					sentence.getNumMentionsInBetween(relation.getFirstMention(),relation.getSecondMention());
					
					
					//String s = sentence.toString().replace(head1, "["+head1+"]-1");
					//s = s.replace(head2, "["+head2+"]-2");
					//System.out.println(relation.getRelationType() + " "+relation.getRelationSubType() + " " +relation.getId());
					//System.out.println( lineNum + " " +relation.getRelationType()+"."+relation.getRelationSubType()+ " "+" "+s);
					System.out.println( lineNum + " " +relation.getRelationType()+"."+relation.getRelationSubType()+ " "+output);
					lineNum++;
					if (sentence.getNumMentionsInBetween(relation.getFirstMention(), relation.getSecondMention())>=4) {
						System.out.println("tooo far apart");
						countLess++;
					}
							
				}
				System.out.println(sentence.toRothYihString());
				System.out.println("*****");
			
				/*ArrayList<Mention> mentions = sentence.getMentions();
				for (Mention m : mentions) {
					if (m.getType().equals("NAM"))
						System.out.println("NAM: "+m.getHead().replaceAll("\n", " ") + " " + d.getURI());
				
				}*/
			}
			System.out.println("total number sentences: "+countTotalSents);
			System.out.println("sentences with 1 rel: "+countSentsWith1Rel);
			System.out.println("sentences with >1 rel: "+countSentsWithMoreThan1Rel);
			//System.out.println("Count active relations: "+countRels);
			if (countRels != doc.getRelations().size()) {
				System.out.println("Missing relations: check file "+doc.getURI());
			}
		}	
		System.out.println(countLess);
		
	}

}
