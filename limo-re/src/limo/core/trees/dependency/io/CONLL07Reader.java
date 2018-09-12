///////////////////////////////////////////////////////////////////////////////
// Copyright (C) 2007 University of Texas at Austin and (C) 2005
// University of Pennsylvania and Copyright (C) 2002, 2003 University
// of Massachusetts Amherst, Department of Computer Science.
//
// This software is licensed under the terms of the Common Public
// License, Version 1.0 or (at your option) any subsequent version.
// 
// The license is approved by the Open Source Initiative, and is
// available from their website at http://www.opensource.org.
///////////////////////////////////////////////////////////////////////////////

package limo.core.trees.dependency.io;

import java.io.*;
import java.util.*;

import limo.core.trees.dependency.DependencyInstance;


/**
 * A reader for files in CoNLL format.
 *
 * <p>
 * Created: Sat Nov 10 15:25:10 2001
 * </p>
 *
 * @author Jason Baldridge
 * @version $Id: CONLLReader.java 112 2007-03-23 19:19:28Z jasonbaldridge $
 * @see mstparser.io.DependencyReader
 */
public class CONLL07Reader extends DependencyReader {

	protected boolean nBestMode = false;
	protected boolean normalizeScore = true;

	public CONLL07Reader(boolean nBestMode) {
		this.nBestMode = nBestMode;
	}
	
	/**
	 * Gets next instance from CoNLL file, also instantiates semantic graph
	 */
	public ArrayList<DependencyInstance> getNext() throws IOException {
		ArrayList<DependencyInstance> outputList = new ArrayList<DependencyInstance>();

		String line = inputReader.readLine();
		
		int numParses =1;
		ArrayList<Double> scores = null;
		if (nBestMode) { 
		
			if (line != null && line.startsWith("#")) {
				String[] fields=line.split(" ");
				//String sentenceId = fields[1];
				numParses = Integer.parseInt(fields[2]);
				for (int i=3;i<fields.length;i++) {
					Double score = Double.parseDouble(fields[i]);
					if (scores == null) 
						scores = new ArrayList<Double>();
					scores.add(score);
				}
				if (normalizeScore && fields.length > 3) {
					double expSum = 0.0;
					for (double entry : scores)
						expSum+= Math.exp(entry);
					for (int i=0; i < scores.size(); i++) {
						double newVal = Math.exp(scores.get(i)) / expSum;
						scores.set(i, newVal);
					}
					
				}
			}
			line = inputReader.readLine();
		}
		if (scores != null)
			if (numParses != scores.size()) {
				System.err.println("numparses and scores differ!");
				System.exit(-1);
			}
		
		int count=0;
		while (outputList.size() < numParses) {
			ArrayList<String[]> lineList = new ArrayList<String[]>();
			
			//to build grammatical structure later
			List<List<String>> tokenFields = new ArrayList<List<String>>();
			
			// skip empty lines
			while (line != null && line.equals("")) {
				line = inputReader.readLine();
				if (line == null) {
					inputReader.close();
					return null;
				}
			}

			while (line != null && !line.equals("") && !line.startsWith("*")) {
				lineList.add(line.split("\t"));
				// System.out.println("## "+line);
				
				//for grammatical structure
				List<String> fields = Arrays.asList(line.split("\t"));
		        tokenFields.add(fields);
				
				line = inputReader.readLine();
			}

			int length = lineList.size();

			if (length == 0) {
				inputReader.close();
				return null;
			}

			String[] forms = new String[length + 1];
			String[] lemmas = new String[length + 1];
			String[] cpos = new String[length + 1];
			String[] pos = new String[length + 1];
			String[][] feats = new String[length + 1][];
			String[] deprels = new String[length + 1];
			int[] heads = new int[length + 1];

			forms[0] = "<root>";
			lemmas[0] = "<root-LEMMA>";
			cpos[0] = "<root-CPOS>";
			pos[0] = "<root-POS>";
			deprels[0] = "<no-type>";
			heads[0] = -1;

			for (int i = 0; i < length; i++) {
				try {
				String[] info = lineList.get(i);
				forms[i + 1] = normalize(info[1]);
				lemmas[i + 1] = normalize(info[2]);
				//normalize also tokenList
				tokenFields.get(i).set(1, normalize(info[1]));
				tokenFields.get(i).set(2, normalize(info[2]));
				
				cpos[i + 1] = info[3];
				pos[i + 1] = info[4];
				feats[i + 1] = info[5].split("\\|");
				// System.out.println(info.length);
				// in test mode might not have more than 6 columns!
				deprels[i + 1] = labeled ? info[7] : "<no-type>";
				heads[i + 1] = Integer.parseInt(info[6]);
				}
				catch (Exception e) {
					//System.err.println(lineList.get(i).toString());
					//String[] inf = lineList.get(i);
					System.err.println("is it nbest data?");
					e.printStackTrace();
				}
			}

			// index 0 is root feature - add this: <root-feat>0
			feats[0] = new String[feats[1].length];
			for (int i = 0; i < feats[1].length; i++)
				feats[0][i] = "<root-feat>" + i;
			
			//check if we have a root edge
			boolean hasRoot = false;
			for (int i=0; i < deprels.length; i++) {
				if (deprels[i].toLowerCase().equals("root")) {
					hasRoot=true;
					break;
				}
			}
				
			if (hasRoot==false) {
				System.err.println("Instance has no root! Add root.");
				//find index 0
				int i = -1;
				for (int j=0 ; j<heads.length;j++) 
					if (heads[j]==0) {
						i=j;
						break;
					}
					
				if (i == -1) {
					//instance has no "0" indexed, take first one
					tokenFields.get(1-1).set(7, "root");
					deprels[1]="root";
					
				} else {
					//add artificial root token (otherwise issues with semantic graph..)
					tokenFields.get(i-1).set(7, "root");
					deprels[i]="root";
				}
				
			}
			//check for erroneous edges, e.g. 0 "punct" --> all those that go to 0 must be labeled root
			for (int i=0;i<heads.length;i++) {
				if (heads[i]==0)
					if (!deprels[i].equalsIgnoreCase("root")) {
						System.err.println("Correcting label: " + deprels[i] +" --> root");
						deprels[i]="root";
						tokenFields.get(i-1).set(7, "root");
					}
			}
			
			
			DependencyInstance dependencyInstance;
			if (scores == null)
				dependencyInstance = new DependencyInstance(forms, lemmas, cpos, pos, feats, deprels,heads, tokenFields);
			else
				dependencyInstance = new DependencyInstance(forms, lemmas, cpos, pos,
					 	feats, deprels, heads, tokenFields, scores.get(count));
			
			//boolean collapsed = false;
			//dependencyInstance.setGrammaticalStructureAndCreateSemanticGraph(dependencyInstance.createGrammaticalStructure(tokenFields), collapsed);
			
			outputList.add(dependencyInstance);
			
			count++;
		}
		return outputList;

	}

	protected boolean fileContainsLabels(String file) throws IOException {
		BufferedReader in = new BufferedReader(new FileReader(file));
		String line = in.readLine();
		in.close();

		if (line.trim().length() > 0)
			return true;
		else
			return false;
	}

	public static void main(String[] args) {
		CONLL07Reader reader = new CONLL07Reader(false);
		try {
			//reader.startReading("/home/bplank/tmp/xxx/train/parsed_bohnet/NBC20001206_1830_0497.SGM.txt.bohnet");
		//reader.startReading("/home/bplank/project/limosine/tools/limo/run/data/BNEWS_NWIRE_train9/process/parsed_bohnet/APW20001012_1321_0434.SGM.txt.bohnet");
		//reader.startReading("/home/bplank/project/limosine/tools/limo/run/data/BNEWS_NWIRE_train1234/process/parsed_bohnet/NBC20001004_1830_1520.SGM.txt.bohnet");
			reader.startReading(args[0]);
			
		ArrayList<DependencyInstance> instances = reader.getNext();
	
		while (instances != null) {
			DependencyInstance tree = instances.get(0);
			String deptree = tree.getNestedTree(tree.getSemanticGraph());
			if (DependencyInstance.checkParentheses(deptree) == false) {
				System.err.println("Problem with tree/non-matching parentheses: "+ deptree);
				System.exit(-1);
			}
			System.out.println(deptree);
			instances = reader.getNext();
		}
		
		/*instances = reader.getNext();
		instances = reader.getNext();
		instances = reader.getNext();
		instances = reader.getNext();
		instances = reader.getNext();
		instances = reader.getNext(); */
		//instances = reader.getNext();
		
		//DependencyInstance tree = instances.get(0);
		
		//System.out.println(tree.getNestedTree(tree.getSemanticGraph()));
		//System.out.println(tree.getNestedTree());
//		System.out.println(tree.getLOCT());
//		System.out.println(tree.getGRCT());
//		
//		//String g = "(root (dobj (NN time) (prep (IN for) (pobj (punct (`` ``)) (JJ lifeline)) (punct ('' ''))) (punct (: --)) (cc (CC and)) (conj (amod (JJ fresh)) (NN tonight) (nn (NN evidence)) (NN tonight))) (nsubj (det (DT the)) (NN kind) (prep (IN of) (pobj (amod (JJ medical)) (NN care) (rcmod (nsubj (PRP you)) (VBP receive))))) (VBZ depends) (prep (IN to) (pobj (det (DT a)) (amod (JJ large)) (NN degree) (prep (IN on) (pcomp (advmod (WRB where)) (VBP live) (nsubj (PRP you)) (VBP live))))) (punct (. .)))";
//		//String g = "(root (dobj (NN time) (prep (IN for)"
//		ParseTree ctree = new ParseTree(tree.getGRCT());
//		String d = ctree.insertNodes(new int[]{16}, "E1", new int[]{25}, "E2");
//		ParseTree dec = new ParseTree(d);
//		String dpet = dec.getPathEnclosedTree(2,12);
//		System.out.println(d);
//		System.out.println(dpet);
//		System.out.println();
//		System.out.println("Shortest path:");
//	
//		DependencyInstance pet = tree.getShortestPathInstance(11,12);
//		System.out.println(pet);
//		System.out.println(pet.getGRCT());
//		System.out.println(pet.getLOCT());
//		DependencyInstance pet2 = tree.getShortestPathInstance(1, 26);
//		System.out.println(pet2);
//		System.out.println(pet2.getGRCT());
//		ParseTree tt = new ParseTree(pet2.getGRCT());
//				instances = reader.getNext();
//		instances = reader.getNext();
//		instances = reader.getNext();
//		DependencyInstance treeN = instances.get(0);
//		System.out.println(treeN);
//		for (int i=1; i < treeN.length();i++)
//			for (int j=i+1; j< treeN.length();j++) {
//				System.out.println(i + " "+ j + ":");
//		DependencyInstance sh  = treeN.getShortestPathInstance(i, j);
//		System.out.println(sh);
//		String gr = sh.getGRCT();
//		System.out.println(gr);
//		ParseTree t = new ParseTree(gr);
//		System.out.println(t);
//			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
}
