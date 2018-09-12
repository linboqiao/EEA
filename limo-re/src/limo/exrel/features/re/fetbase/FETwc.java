package limo.exrel.features.re.fetbase;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import limo.cluster.BrownWordCluster;
import limo.core.Mention;
import limo.core.Relation;
import limo.core.Sentence;
import limo.core.trees.AbstractTree;
import limo.core.trees.constituency.ParseTree;
import limo.exrel.features.re.structured.RelationExtractionStructuredFeature;
import edu.stanford.nlp.trees.Tree;

public class FETwc extends RelationExtractionStructuredFeature {

	protected String _extract(AbstractTree parseTree, Mention mention1,
			Mention mention2, Relation relation, Sentence sentence, ArrayList<Object> resources) throws IOException{
		
		
		try {
		ParseTree decorated = (ParseTree)parseTree;
		//do not decorate all other mentions in sentence
		BrownWordCluster wordCluster = (BrownWordCluster)resources.get(0);
		
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
			
		if (wordCluster == null) {
			System.err.println("Did you load the word cluster file? Check config file.");
			System.exit(-1);
		}
		
		String res = "";
		HashMap<String, String> wcs = new HashMap<String, String>();
		String b4 = "", b6 = "", b8 = "", b10 = "";
		for (Tree terminal : tree.getTerminals()) {
			String word = terminal.label().value();
			Tree pos = terminal.parent(tree.getRootNode());
			Tree posParent = pos.parent(tree.getRootNode());
			
			b4 = wordCluster.getPrefixClusterId(word,4);
			b6 = wordCluster.getPrefixClusterId(word,6);
			b8 = wordCluster.getPrefixClusterId(word,8);
			b10 = wordCluster.getPrefixClusterId(word,10);
			
			if (posParent.label().value().startsWith("E1")) { //smooth also E entities
				wcs.put("HM1_WC4", b4);
				wcs.put("HM1_WC6", b6);
				wcs.put("HM1_WC8", b8);
				wcs.put("HM1_WC10", b10);
				//wc1 = "HM1_WC10_" + bitstring + "=T";
			}
			else if (posParent.label().value().startsWith("E2")) {
				wcs.put("HM2_WC4", b4);
				wcs.put("HM2_WC6", b6);
				wcs.put("HM2_WC8", b8);
				wcs.put("HM2_WC10", b10);
				//wc2 = "HM2_WC10_" + bitstring + "=T";
			}
		}

		if (!pet.contains(firstTarget) || ! pet.contains(secondTarget)) {
 			System.err.println("Ignoring problematic tree: "+ pet);
			return "DUMMY_WC==0.0";
		}
		
		//res = wc1 + " " + wc2;
		for (String key : wcs.keySet()) {
			res += (wcs.get(key) != null) ? (key + "_" + wcs.get(key) + "=T ") : ""; 
		}
		res = res.trim();
		
		return res.isEmpty() ? "DUMMY_WC==0.0" : res;
		
		} catch (NullPointerException e) {
			e.printStackTrace();
			System.out.println("Problem with: "+sentence.toString());
			System.out.println(parseTree.toString());
			System.out.println("Mention1: "+mention1.toString());
			System.out.println("Mention2: "+mention2.toString());
			return "DUMMY_WC==0.0";
		}
	}

}

