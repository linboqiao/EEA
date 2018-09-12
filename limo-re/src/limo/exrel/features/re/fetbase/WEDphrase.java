package limo.exrel.features.re.fetbase;

import java.io.IOException;
import java.util.ArrayList;

import limo.cluster.WordEmbeddingDictionary;
import limo.core.Mention;
import limo.core.Relation;
import limo.core.Sentence;
import limo.core.trees.AbstractTree;
import limo.core.trees.constituency.ParseTree;
import limo.exrel.features.re.structured.RelationExtractionStructuredFeature;
import edu.stanford.nlp.trees.Tree;

public class WEDphrase extends RelationExtractionStructuredFeature {
	
	protected String _extract(AbstractTree parseTree, Mention mention1,
			Mention mention2, Relation relation, Sentence sentence,  ArrayList<Object> resources) throws IOException {
		
		int dimension = 50;
		try {
			ParseTree decorated = (ParseTree)parseTree;
			
			WordEmbeddingDictionary wordEmbeddingDictionary = (WordEmbeddingDictionary) resources.get(4);
			
			if (wordEmbeddingDictionary== null) {
				System.err.println("WEDphrase: Did you load the Word Embedding file? Check config file.");
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
			ArrayList<ArrayList<Double>> vecs = new ArrayList<ArrayList<Double>>();
			
			for (Tree terminal : tree.getTerminals()) {
				if (isTerminalInPET(terminal, root)) {
					String word = terminal.label().value();
					vecs.add(wordEmbeddingDictionary.getWordEmbedding(word));
				}
			}
			
			ArrayList<Double> vec = average(vecs, dimension);
			if (vec == null || vec.isEmpty()) return zeroFeature(dimension);
			String res = "";
			for (int i = 0; i < vec.size(); i++) {
				res += "PHRWED" +  (i+1) + "==" + vec.get(i).toString() + " ";
			}
			
			return res.trim();			
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
	
	private boolean isTerminalInPET(Tree terminal, Tree root) {
		if (terminal == null || root == null) return false;
		Tree run = terminal.parent(root);
		while (run != null) {
			if (run.getHide())
				return false;
			run = run.parent(root);
		}
		return true;
	}
	
	private ArrayList<Double> average(ArrayList<ArrayList<Double>> vecs, int dimension) {
		if (vecs == null || vecs.isEmpty()) return null;
		ArrayList<Double> res = new ArrayList<Double>();
		for (int i = 0; i < dimension; i++)
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
		
		if (total == 0) return null;
		for (int i = 0; i < res.size(); i++)
			res.set(i, res.get(i).doubleValue() / total);
		
		return res;
	}

	private String zeroFeature(int dimension) {
		String res = "";
		for (int i = 1; i <= dimension; i++) {
			res += "PHRWED" +  i + "==0.0" + " ";
		}
		return res.trim();
	}

}
