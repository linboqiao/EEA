package limo.io.ace2005;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;

import limo.core.Relations;
import limo.core.Sentence;

import limo.core.interfaces.IRelationDocument;
import limo.io.IRelationReader;
import limo.io.Utils;

public class ACE2005Reader implements IRelationReader {
	
	private String path;
	private ACE2005FilenameFilter filenameFilter; 
	
	// if we ignore certain relations
	ArrayList<String> relationsToIgnoreIds;
	
	//HashMap<String,String> relationMapping; // if we need a mapping, like from ACE 2004 to 2005
	//boolean useMapping = false;
	
	public ACE2005Reader(String path) {
		this.path = path;
		this.filenameFilter = new ACE2005FilenameFilter();
	}
	
	public ACE2005Reader(String path, String pathIgnoreRelations) {
		this(path);
		this.relationsToIgnoreIds = Utils.readIgnoreFile(pathIgnoreRelations);
	}


	public String getPath() {
		return this.path;
	}
	
	public FilenameFilter getFilenameFilter() {
		return this.filenameFilter;
	}
	
	@Override
	public ArrayList<IRelationDocument> readDocuments() throws Exception {
		return readDocuments(false);
	} 
	
	@Override
	public ArrayList<IRelationDocument> readDocuments(boolean skipSentences) throws Exception {
		// read all ACE (APF) files from path (or doc if path is a document)
		ArrayList<IRelationDocument> documents = new ArrayList<IRelationDocument>();
		File file = new File(this.path);
		if (file.isDirectory()) {

			File[] fs_annotated = file.listFiles(this.getFilenameFilter());
			for (int i = 0; i < fs_annotated.length; i++) {
				//System.out.println("Processing file: "+ fs_annotated[i]);
				ACE2005Document doc = new ACE2005Document(fs_annotated[i], this);
				documents.add(doc);
			}
		} else {
			// read single document
			File xmlfile = new File(this.path);
			ACE2005Document doc = new ACE2005Document(xmlfile, this);
			documents.add(doc);
		}
		return documents;
	}
	

	public static void main(String[] args) {
		//String path = "/home/bplank/project/limosine/tools/relation_extraction/data/ace2004/BNEWS_NWIRE/";
		
	    String path = args[0];
	    //String ignore = "/home/bplank/project/limosine/tools/limo/ignoreRelationsACE2005.txt";
		
	    ACE2005Reader ace2005reader = new ACE2005Reader(path);
	    
		
	    try {
	    	
	    	//Corpus corpus = new Corpus(ace2005reader);
			//corpus.init();
			
			
			// populates constituentParseTrees
			//ParsedDataReaderModule.readParsedConstituencyData(corpus);
			
			
		ArrayList<IRelationDocument> documents = ace2005reader.readDocuments();
		int totalRel = 0;
		int totalIgnored=0;
		
		int a = 0,b =0 ,c =0;
		
		for (IRelationDocument d : documents) {
		//IRelationDocument d = corpus.getNextDocument();
		//while (d != null) {

			ACE2005Document doc = (ACE2005Document) d;
			/*System.out.println();
			System.out.println(doc.URI + ": ");
			System.out.print(doc.getNumSentences() + " sentences. ");
			System.out.print(doc.getEntities().size() + " entities.");
			System.out.println("@ " + doc.getRelations().size() + " relations.");
			*/
			ArrayList<Sentence> sentences =  doc.getSentences();
			
			System.out.println(doc.getNumRelations() + " num: "+doc.count + " ignored:"+doc.countIgnored);
			//System.out.println(); 
			a+=doc.getNumRelations();
			b+=doc.count;
			c+=doc.countIgnored;
			
			int countRels = 0;
		
			for (Sentence sentence : sentences) {
				System.out.println(">>> " + sentence); 
				//System.out.println(sentence.getNERAnnotatedSentenceSpaceSeparated());
				//System.out.println(sentence.getAnnotatedSentence());
				System.out.println(sentence.getAnnotatedSentenceRothYi());
				//System.out.println("<s> " + sentence + " </s>");
				//System.out.println();
				//System.out.println(sentence.getMentions().size() + " mentions in sentence ");
				//for (Mention m : sentence.getMentions()) 
					//System.out.println(m + " "+ m.getTokenIds().length);
				
				Relations relations = sentence.getRelations();
				countRels += relations.size();
				//totalIgnored+= relationsDoc.getNumIgnoredRelations(); //counted on document level
				//totalIgnored+=relations.getNumIgnoredRelations();
				//System.out.println("num rel: "+relations.size());
				//System.out.println("ignored: "+relations.getNumIgnoredRelations());
				//if (relations.getNumIgnoredRelations()>0)
				//	System.out.println("yes!");
				//System.out.println("count_rels:"+countRels);
				/*for (IRelation irelation : relations.getRelations()) {
					Relation relation = (Relation) irelation;
					System.out.println(relation + " " + relation.getId());
					//if (relation.getRelationType().startsWith("PART"))
						//System.out.println("**************");
				}*/
				
				/*ArrayList<Mention> mentions = sentence.getMentions();
				for (Mention m : mentions) {
					if (m.getType().equals("NAM"))
						System.out.println("NAM: "+m.getHead().replaceAll("\n", " ") + " " + d.getURI());
				
				}*/
			}
			//System.out.println("Count active relations: "+countRels);
			/*if (countRels != doc.getRelations().size()) {
				System.out.println("Missing relations: check file "+doc.URI);
			}*/
			totalRel += countRels;
			//System.out.println("Total: "+totalRel);
		}	
		//System.out.println();
		System.out.println("Total relations: "+totalRel);
		System.out.println("Ignored relations: "+totalIgnored);
		System.out.println("Documents: " + documents.size());
		
		System.out.println("a(numRel):"+a+" b(count xml):"+b +" c(countIgnored):"+c);
	    } catch (Exception e) {
	    	e.printStackTrace();
	    	
	    }
	}

	
}
