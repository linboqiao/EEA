package limo.eval;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

import limo.core.Relation;
import limo.core.Relations;
import limo.core.Sentence;
import limo.core.interfaces.IRelationDocument;
import limo.io.ace2005.ACE2005Document;
import limo.io.ace2005.ACE2005Reader;

/***
 * Prepare relations file from ACE 2004 (e.g. to prepare gold file)
 * 
 * @author Barbara Plank
 *
 */
public class PrepareData {
	
	private String path;
	private String ignore;
	
	public PrepareData(String path, String ignore) {
		this.path = path;
		this.ignore = ignore;
	}
		
	public void readFile() {
		try {
			BufferedReader reader = new BufferedReader(new FileReader(this.path +".rel"));
			String line;
			
			int countRels=0;
			int sentenceIndex=0;
			while ((line = reader.readLine())!= null) {
				Sentence sentence = Sentence.createSentenceFromRelationTaggedInput(sentenceIndex, line);
				countRels+=sentence.getRelations().size();
				sentenceIndex++;
					
			}
			reader.close();
			System.out.println("File read. Relations found: " + countRels);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}

	public void writeFile() throws Exception {
		//ACE2004Reader ace2004reader = new ACE2004Reader(this.path, this.ignore);
		ACE2005Reader ace2004reader = new ACE2005Reader(this.path, this.ignore);
		ArrayList<IRelationDocument> documents = ace2004reader.readDocuments();

		BufferedWriter writer;
		try {
			writer = new BufferedWriter(new FileWriter(path+".rel"));
			int countRels = 0;
			int skipSent=0; //if less than 2 mentions
			//int skipToFar=0; //if mentions too far away
			for (IRelationDocument d : documents) {
					//ACE2004Document doc = (ACE2004Document) d;
				ACE2005Document doc = (ACE2005Document) d;
					ArrayList<Sentence> sentences =  doc.getSentences();
					
					
					
					for (Sentence sentence : sentences) {
						if (sentence.getMentions().size()<2) {
							skipSent++;
							continue;
						}
						
						Relations relations = sentence.getRelations();
						//if (relations.size() ==0)
						//	System.out.println("SENTENCE HAS NO RELATIONS");
						countRels += relations.size();
						String out = sentence.getAnnotatedSentence(false);
						if (relations.size()>0) {
							System.out.println(relations.size()+ "\t"+out);
							for (Relation r : relations.get())
								System.out.println(r);
							System.out.println();
						}
						writer.write(out);
						writer.newLine();
						
					}
			}
			writer.close();
			System.out.println("File written: "+ path+".rel");
			System.out.println("Relations: "+countRels);
			System.out.println("Sentences skipped: "+skipSent);
		
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}
	

	public static void main(String[] args) throws Exception  {
		
		//prepare data for ACE 2004 in different format
		//String pathTrain = "/home/bplank/project/limosine/tools/limo/run/data/BNEWS_NWIRE_train1234";
		//String pathTrain = "/home/bplank/project/limosine/tools/limo/run/data/BNEWS_NWIRE_trainTRAINSMALL";
		//String pathTest = "/home/bplank/project/limosine/tools/limo/run/data/BNEWS_NWIRE_test0";
		//String ignore = "/home/bplank/project/limosine/tools/limo/ignoreRelations.txt";
		
		String pathTest = "/home/bplank/project/limosine/tools/limo/experiments_ace2005/run/data_ace2005/cts";
		String ignore = "/home/bplank/project/limosine/tools/limo/experiments_ace2005/ignoreRelationsACE2005.txt";
		
		//PrepareData dataTrain = new PrepareData(pathTrain,ignore);
		//dataTrain.writeFile();
		//dataTrain.readFile();
		
		
		PrepareData dataTest = new PrepareData(pathTest,ignore);
		dataTest.writeFile();
		dataTest.readFile();
	
	}

}
