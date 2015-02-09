package limo.exrel.features.re.structured;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import edu.stanford.nlp.trees.Tree;

import limo.cluster.WordEmbeddingDictionary;
import limo.core.Mention;
import limo.core.Relation;
import limo.core.Sentence;
import limo.core.trees.AbstractTree;
import limo.core.trees.constituency.ParseTree;

public class VECtree extends RelationExtractionStructuredFeature {
	
	protected String _extract(AbstractTree parseTree, Mention mention1,
			Mention mention2, Relation relation, Sentence sentence,  ArrayList<Object> resources) throws IOException {
		
		int dimension = 50;
		try {
			ParseTree decorated = (ParseTree)parseTree;
			
			WordEmbeddingDictionary wordEmbeddingDictionary = (WordEmbeddingDictionary) resources.get(4);
			
			if (wordEmbeddingDictionary== null) {
				System.err.println("VECtree: Did you load the Word Embedding file? Check config file.");
				System.exit(-1);
			}
			
			dimension = wordEmbeddingDictionary.getDimension();
			
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
		
			
			if (pet.length()==0)
				System.err.println("no pet found???");
			
			ParseTree tree = new ParseTree(pet);
			Tree root = tree.getRootNode();
			
			ArrayList<Double> vec = computeTreeEmbeddings(root, wordEmbeddingDictionary);
			if (vec == null || vec.isEmpty()) return zeroFeature(dimension);
			String res = "";
			int start = wordEmbeddingDictionary.getDimension()*3 + 1;
			for (Double d : vec) {
				res += start + ":" + d.toString() + " ";
				start++;
			}
			
			return res.trim();
			//res = res.trim();
			//return res.isEmpty() ? zeroFeature(dimension) : res;		
		}
		catch (NullPointerException e) {
			e.printStackTrace();
			System.out.println("Problem with: "+sentence.toString());
			System.out.println(parseTree.toString());
			System.out.println("Mention1: "+mention1.toString());
			System.out.println("Mention2: "+mention2.toString());
			return zeroFeature(dimension);
		}
		
	}
	
	private ArrayList<Double> computeTreeEmbeddings(Tree root, WordEmbeddingDictionary wordEmbeddingDictionary) {
		if (root.isLeaf()) {
			String word = root.label().value();
			ArrayList<Double> wed = wordEmbeddingDictionary.getWordEmbedding(word);
			if (wed == null) {
				wed = zeroVec(wordEmbeddingDictionary);
			}
			return wed;
		}
		List<Tree> children = root.getChildrenAsList();
		if (children == null || children.isEmpty()) return zeroVec(wordEmbeddingDictionary);
		ArrayList<ArrayList<Double>> vecs = new ArrayList<ArrayList<Double>>();
		for (Tree child : children) {
			if (child != null && !child.getHide())
				vecs.add(computeTreeEmbeddings(child, wordEmbeddingDictionary));
		}
		return average(vecs, wordEmbeddingDictionary);
	}
	
	private ArrayList<Double> zeroVec(WordEmbeddingDictionary wordEmbeddingDictionary) {
		ArrayList<Double> res = new ArrayList<Double>();
		for (int i = 0; i < wordEmbeddingDictionary.getDimension(); i++) {
			res.add(0.0);
		}
		return res;
	}
	
	private ArrayList<Double> average(ArrayList<ArrayList<Double>> vecs, WordEmbeddingDictionary wordEmbeddingDictionary) {
		if (vecs == null || vecs.isEmpty()) return zeroVec(wordEmbeddingDictionary);
		ArrayList<Double> res = new ArrayList<Double>();
		for (int i = 0; i < wordEmbeddingDictionary.getDimension(); i++)
			res.add(0.0);
		int total = 0;
		
		for (ArrayList<Double> vec : vecs) {
			if (vec == null) continue;
			total++;
			if (vec.size() != res.size()) {
				System.out.println("Inconsistent wed dimensions: " + res.size() + " : " + vec.size());
				System.exit(-1);
			}
			
			for (int i = 0; i < res.size(); i++) {
				res.set(i, res.get(i).doubleValue() + vec.get(i).doubleValue());
			}
		}
		
		if (total == 0) return zeroVec(wordEmbeddingDictionary);
		for (int i = 0; i < res.size(); i++)
			res.set(i, res.get(i).doubleValue() / total);
		
		return res;
	}
	
	private String zeroFeature(int dimension) {
		String res = "";
		for (int i = 3*dimension+1; i <= 4*dimension; i++) {
			res += i + ":0.0" + " ";
		}
		return res.trim();
	}

}
