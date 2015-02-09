package limo.io.ry;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;

import limo.core.Relation;
import limo.core.Relations;
import limo.core.Sentence;
import limo.core.interfaces.IRelationDocument;
import limo.io.IRelationReader;

/***
 * Class to read Roth and Yih corpus
 * See: http://cogcomp.cs.illinois.edu/Data/ER/
 * @author Barbara Plank
 *
 */
public class RothYihConll2004Reader implements IRelationReader {
	
	private String path;
	private RothYihConll2004FilenameFilter filenameFilter;

	public RothYihConll2004Reader(String path) {
		this.path = path;
		this.filenameFilter = new RothYihConll2004FilenameFilter();
	}
	
	@Override
	public ArrayList<IRelationDocument> readDocuments()
			throws Exception {
		return readDocuments(false); //default is true for Roth & Yi dataset
	}

	@Override
	public ArrayList<IRelationDocument> readDocuments(boolean skipSentences) throws IOException {
		// read all files from path (or doc if path is a document)
		ArrayList<IRelationDocument> documents = new ArrayList<IRelationDocument>();
		File file = new File(this.path);
		if (file.isDirectory()) {

			File[] fs_annotated = file.listFiles(this.filenameFilter);
			for (int i = 0; i < fs_annotated.length; i++) {
				// System.out.println("Processing file: "+ fs_annotated[i]);
				RothYihConll2004Document doc = new RothYihConll2004Document(fs_annotated[i], this, skipSentences);
				documents.add(doc);
			}
		} else {
			// read single document
			File annfile = new File(this.path);
			RothYihConll2004Document doc = new RothYihConll2004Document(annfile, this, skipSentences);
			documents.add(doc);
		}
		return documents;
	}

	
	//for quick testing ...
	public static void main(String[] args) throws Exception {
		
		String path = args[0];
		
		RothYihConll2004Reader reader = new RothYihConll2004Reader(path);
		
		ArrayList<IRelationDocument> documents;
		try {
			documents = reader.readDocuments();
			
			
			for (IRelationDocument d : documents) {
				//will be just one document
				
				RothYihConll2004Document doc = (RothYihConll2004Document)documents.get(0);
				
				
				ArrayList<Sentence> sentences = doc.getSentences();
				
				//ArrayList<Sentence> sentencesWithRelations = new ArrayList<Sentence>();
				
				//ArrayList<Integer> sentIds = new ArrayList<Integer>();
				//int i=0;
			/*	for (Sentence sentence : sentences) {
					if (args.length==2 && args[1].equals("gold"))
						System.out.println(sentence.getNERAnnotatedSentenceSpaceSeparated());
					else
						System.out.println(sentence.getTokensTabular());
					System.out.println();
					if (sentence.getRelationsAsList().size()>0) {
						sentIds.add(i); i++;
						sentencesWithRelations.add(sentence);					
					}
					//System.out.println(sentence.toString());
				}*/
				
				for (Sentence s : sentences) {
					for (Relation r : s.getRelationsAsList()) 
						System.out.println(r);
				}
				
				//System.out.println(sentencesWithRelations.size());
				
				
				//random permutation
				//java.util.Collections.shuffle(sentIds);
				//for (Integer id : sentIds)
					//System.out.println(id);

				//for (Relation r : documents.get(0).getRelations().get())
				//	System.out.println(r);
				
				//CREATE CROSSVALIDATION DATA
				
		/*		//put 288 in each fold
				int overallIndex = 0;
				BufferedWriter bwCV = new BufferedWriter(new FileWriter(path+".cv"));
				
				for (int fold=0; fold < 5; fold++) {
					
					BufferedWriter bw = new BufferedWriter(new FileWriter(path+".fold"+fold+".corp"));
					bwCV.write("# fold "+fold +"\n");
					if (fold < 4) {
						for (int count=0; count < 288; count++) {
							int sentenceId = sentIds.get(overallIndex);
							bwCV.write(sentenceId+"\n");
							Sentence s = sentencesWithRelations.get(sentenceId);
							bw.write(s.toRothYihString());
							bw.write("\n");
							overallIndex++;
						}
					}
					else {
						//last fold has 1 more
						for (int count=0; count < 289; count++) {
							int sentenceId = sentIds.get(overallIndex);
							bwCV.write(sentenceId+"\n");
							Sentence s = sentencesWithRelations.get(sentenceId);
							bw.write(s.toRothYihString());
							bw.write("\n");
							overallIndex++;
						}
					}
					
					bw.close();
				}
				bwCV.close();*/
				
				
				System.err.println(d.getNumRelations() + " relations.");
//				Relations rels = d.getRelations();
//				for (Relation r : rels.get())
//					System.err.println(r.getRelationType());
				
			}
			
			
/*			File outdir = new File("/tmp/");
			boolean onlySentsWithRealations=true;//here assumed to be true, not the case for ACE!
 
			RothYihConll2004Document doc = (RothYihConll2004Document)documents.get(0);
			doc.saveTokenizedTextAsFile(outdir,onlySentsWithRealations);*/
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}

	@Override
	public String getPath() {
		return this.path;
	}

	@Override
	public FilenameFilter getFilenameFilter() {
		return this.filenameFilter;
	}

	
	

}
