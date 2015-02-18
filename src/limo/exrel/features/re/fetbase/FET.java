package limo.exrel.features.re.fetbase;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Stack;

import edu.stanford.nlp.trees.Tree;

import limo.core.Mention;
import limo.core.Relation;
import limo.core.Sentence;
import limo.core.trees.AbstractTree;
import limo.core.trees.constituency.ParseTree;
import limo.exrel.features.re.structured.RelationExtractionStructuredFeature;

public class FET extends RelationExtractionStructuredFeature {
	
	@Override
	protected String _extract(AbstractTree parseTree, Mention mention1,
			Mention mention2, Relation relation, Sentence sentence, ArrayList<Object> resources) throws IOException {
		
		try {
		ParseTree decorated = (ParseTree)parseTree;
		//do not decorate all other mentions in sentence

		String tmpFirstTarget = "ENTITY-INFOT1";
		String tmpSecondTarget = "ENTITY-INFOT2";
		
		int[] tokenIds1 = mention1.getTokenIds();
		int[] tokenIds2 = mention2.getTokenIds();
		
		String decoratedStr = decorated.insertNodes(tokenIds1, tmpFirstTarget, tokenIds2, tmpSecondTarget);
		ParseTree parseTreeDecorated = new ParseTree(decoratedStr);
		
		int spanTokenIdStart = min(tokenIds1, tokenIds2);
		int spanTokenIdEnd = max(tokenIds1, tokenIds2);
	
		String pet = parseTreeDecorated.getPathEnclosedTree(spanTokenIdStart,spanTokenIdEnd);
		
		
		String firstTarget = "E1";
		String secondTarget = "E2";
				
		pet = pet.replaceAll(tmpFirstTarget, firstTarget);
		pet = pet.replaceAll(tmpSecondTarget, secondTarget);
				

		if (!pet.contains(firstTarget) || ! pet.contains(secondTarget)) {
 			System.err.println("Ignoring problematic tree: "+ pet);
			return "EMPTY";
		}
		
		ParseTree tree = new ParseTree(pet);
		
		String fets = extractingFeatures(tree);
		
		return fets.isEmpty() ? "EMPTY" : fets;
		
		} catch (NullPointerException e) {
			e.printStackTrace();
			System.out.println("Problem with: "+sentence.toString());
			System.out.println("Mention1: "+mention1.toString());
			System.out.println("Mention2: "+mention2.toString());
			return "EMPTY";
		}
	}
	
	private String extractingFeatures(ParseTree tree) {
		String res = "";
		ArrayList<String> words = new ArrayList<String>();
		ArrayList<Integer> idx1 = new ArrayList<Integer>(), idx2 = new ArrayList<Integer>();
		int id = 0;
		Tree tree1 = null, tree2 = null;
		for (Tree terminal : tree.getTerminals()) {
			String word = terminal.label().value();
			Tree pos = terminal.parent(tree.getRootNode());
			Tree posParent = pos.parent(tree.getRootNode());
			
			if (posParent.label().value().startsWith("E1")) {
				idx1.add(id);
				tree1 = terminal;
			}
			else if (posParent.label().value().startsWith("E2")) {
				idx2.add(id);
				tree2 = terminal;
			}
			
			words.add(word.replaceAll("\n", " ").replaceAll("\t", " ").replaceAll(" ", "_"));
			id++;
		}
		
		if (idx1.isEmpty() || idx2.isEmpty() || tree1 == null || tree2 == null) {
			System.err.println("Cannot find entities in PETFET.extractingFeatures: " + tree.toString());
			System.exit(-1);
		}
		
		ArrayList<Integer> left = null, right = null;
		Tree treeLeft = null, treeRight = null;
		if (idx1.get(0).intValue() > idx2.get(0).intValue()) {
			left = idx2;
			right = idx1;
			treeLeft = tree2;
			treeRight = tree1;
		}
		else {
			left = idx1;
			right = idx2;
			treeLeft = tree1;
			treeRight = tree2;
		}
		
		int startL = left.get(0).intValue();
		int endL = left.get(left.size()-1).intValue();
		int startR = right.get(0).intValue();
		int endR = right.get(right.size()-1).intValue();
		
		if (endL >= startR) {
			System.err.println("Wrong non-overlapping assumption in PETFET.extractingFeatures: " + tree.toString());
			System.exit(-1);
		}
		
		//lexical features
		res += generateLexicalFeatures(words, startL, endL, startR, endR);
		res = res.trim();
		//mention head features
		res += " " + generateMentionFeatures(words, idx1, idx2);
		res = res.trim();
		//syntactic tree features
		res += " " + generatePathFeatures(tree, treeLeft, treeRight);
		res = res.trim();
		
		
		return res;
	}
	
	private String generatePathFeatures(ParseTree tree, Tree left, Tree right) {
		try {
			Stack<Tree> leftPath = treesToRoot(tree, left);
			Stack<Tree> rightPath = treesToRoot(tree, right);
			
			Tree common = null;
			Tree leftTemp = null, rightTemp = null;
			Tree root = tree.getRootNode();
			
			leftTemp = leftPath.pop();
			rightTemp = rightPath.pop();
			if (!leftTemp.equals(root) || !rightTemp.equals(root)) {
				System.err.println("Root node required(generatePathFeatures)!: " + tree.toString());
				return "";
			}
			common = leftTemp;
			while (true) {
				leftTemp = leftPath.pop();
				rightTemp = rightPath.pop();
				if (!leftTemp.equals(rightTemp)) {
					break;
				}
				common = leftTemp;
			}
			
			ArrayList<String> elementPath = new ArrayList<String>();
			while (!leftPath.isEmpty()) {
				leftTemp = leftPath.pop();
				if (leftTemp.label().value().startsWith("E1") || leftTemp.label().value().startsWith("E2")) continue;
				elementPath.add(0, leftTemp.label().value());
			}
			elementPath.add(common.label().value());
			while (!rightPath.isEmpty()) {
				rightTemp = rightPath.pop();
				if (rightTemp.label().value().startsWith("E1") || rightTemp.label().value().startsWith("E2")) continue;
				elementPath.add(rightTemp.label().value());
			}
			
			String res = "";
			
			//unigrams
			HashSet<String> UNIGRAMS = new HashSet<String>();
			for (int i = 1; i < (elementPath.size()-1); i++) {
				UNIGRAMS.add("SPUN=" + elementPath.get(i));
			}
			for (String ug : UNIGRAMS) {
				res += ug + " ";
			}
			
			//bigrams
			HashSet<String> BIGRAMS = new HashSet<String>();
			for (int i = 1; i < (elementPath.size()-2); i++) {
				BIGRAMS.add("SPBI=" + elementPath.get(i) + "_" + elementPath.get(i+1));
			}
			for (String bg : BIGRAMS) {
				res += bg + " ";
			}
			
			//trigrams
			HashSet<String> TRIGRAMS = new HashSet<String>();
			for (int i = 1; i < (elementPath.size()-3); i++) {
				TRIGRAMS.add("SPTI=" + elementPath.get(i) + "_" + elementPath.get(i+1) + "_" + elementPath.get(i+2));
			}
			for (String tg : TRIGRAMS) {
				res += tg + " ";
			}
			
			//PTP
			String ptp = "";
			for (int i = 1; i < (elementPath.size()-1); i++) {
				ptp += elementPath.get(i) + "_";
			}
			if (!ptp.isEmpty())
				res += "PTP=" + ptp.substring(0, ptp.length()-1) + " ";
			
			//PTPH
			String ptph = "";
			for (int i = 0; i < elementPath.size(); i++) {
				ptph += elementPath.get(i) + "_";
			}
			if (!ptph.isEmpty())
				res += "PTPH=" + ptph.substring(0, ptph.length()-1) + " ";
			
			return res.trim();
		}
		
		catch (Exception e) {
			System.err.println("Some erorr occurs on generatePathFeatures: " + tree.toString());
			e.printStackTrace();
			return "";
		}
		
	}
	
	private Stack<Tree> treesToRoot(ParseTree tree, Tree node) {
		Stack<Tree> st = new Stack<Tree>();
		Tree run = node;
		while (run != null) {
			st.push(run);
			run = run.parent(tree.getRootNode());
		}
		return st;
	}
	
	private String generateMentionFeatures(ArrayList<String> words, ArrayList<Integer> idx1, ArrayList<Integer> idx2) {
		String res = "";
		String head1 = "", head2 = "";
		for (Integer id : idx1) {
			head1 += words.get(id) + "_";
		}
		for (Integer id : idx2) {
			head2 += words.get(id) + "_";
		}
		
		//mention heads
		if (!head1.isEmpty())
			res += "HM1=" + head1.substring(0, head1.length()-1) + " ";
		if (!head2.isEmpty())
			res += "HM2=" + head2.substring(0, head2.length()-1) + " ";
		if (!head1.isEmpty() && !head2.isEmpty())
			res += "HM12=" + head1.substring(0, head1.length()-1) + "--" + head2.substring(0, head2.length()-1) + " ";
		
		int start1 = idx1.get(0).intValue();
		int end1 = idx1.get(idx1.size()-1).intValue();
		int start2 = idx2.get(0).intValue();
		int end2 = idx2.get(idx2.size()-1).intValue();
		
		//mention order
		if (end1 <= start2)
			res += "orderM=1 ";
		else if (end2 <= start1)
			res += "orderM=2 ";
		
		return res.trim();
	}
	
	private String generateLexicalFeatures(ArrayList<String> words, int startL, int endL, int startR, int endR) {
		String res = "";
		
		//words in between
		if (startR == (endL+1)) res += "WBNULL=true ";
		else if (startR == (endL+2)) res += "WBFL=" + words.get(endL+1) + " ";
		else {
			res += "WBF=" + words.get(endL+1) + " ";
			res += "WBL=" + words.get(startR-1) + " ";
			for (int i = (endL+2); i <= (startR-2); i++) {
				res += "WBO=" + words.get(i) + " ";
			}
		}
		
		//words before
		if (startL >= 1)
			res += "BM1F=" + words.get(startL-1) + " ";
		if (startL >= 2)
			res += "BM1L=" + words.get(startL-2) + " ";
		
		//words after
		if ((endR+1) < words.size())
			res += "AM2F=" + words.get(endR+1) + " ";
		if ((endR+2) < words.size())
			res += "AM2L=" + words.get(endR+2) + " ";
		
		//bigrams
		HashSet<String> BIGRAMS = new HashSet<String>();
		for (int i = (endL+1); i < (startR-1); i++) {
			if ((i+1) < words.size())
				BIGRAMS.add("BIGRAM_" + words.get(i) + words.get(i+1) + "=TRUE");
		}
		for (String bg : BIGRAMS)
			res += bg + " ";
		
		//T-patterns
		String pattern = "";
		for (int i = (endL+1); i < startR; i++) {
			pattern += words.get(i) + "_";
		}
		if (!pattern.isEmpty())
			res += "TPattern=" + pattern.substring(0, pattern.length()-1) + " ";
		
		//NUMBER OF WORDS IN BETWEEN
		if (startR > endL)
			res += "NUMWB=" + (startR - endL - 1) + " ";
		
		return res.trim();
	}

}
