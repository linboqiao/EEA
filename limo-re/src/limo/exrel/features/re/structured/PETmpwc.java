package limo.exrel.features.re.structured;

import java.io.IOException;
import java.util.ArrayList;

import limo.cluster.BrownWordCluster;
import limo.core.Mention;
import limo.core.Relation;
import limo.core.Sentence;
import limo.core.trees.AbstractTree;
import limo.core.trees.constituency.ParseTree;
import edu.stanford.nlp.trees.Tree;

/***
 * Path Enclosed Tree, only target entities are marked
 * Use Brown word clusters with multiple prefix lengths as POS tags (currently prefix lengths 4, 6, 8, 10 are used)
 * Adapted from Barbara Plank's class: limo.exrel.features.re.structured.PETwc
 * 
 * @author Thien Huu Nguyen
 *
 */

public class PETmpwc extends RelationExtractionStructuredFeature {
	
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
		
		for (Tree terminal : tree.getTerminals()) {
			String word = terminal.label().value();
			String bitstring4 = wordCluster.getPrefixClusterId(word,4) == null ? "unknownWC4" : wordCluster.getPrefixClusterId(word,4);
			String bitstring6 = wordCluster.getPrefixClusterId(word,6) == null ? "unknownWC6" : wordCluster.getPrefixClusterId(word,6);
			String bitstring8 = wordCluster.getPrefixClusterId(word,8) == null ? "unknownWC8" : wordCluster.getPrefixClusterId(word,8);
			String bitstring10 = wordCluster.getPrefixClusterId(word,10) == null ? "unknownWC10" : wordCluster.getPrefixClusterId(word,10);
			Tree pos = terminal.parent(tree.getRootNode());
			Tree posParent = pos.parent(tree.getRootNode());
			
			//if (posParent.label().value().startsWith("E1") || posParent.label().value().startsWith("E2")) {
				//String wcstring = bitstring4 + " (" + bitstring6 + " (" + bitstring8 + " (" + bitstring10;
				String wcstring = bitstring6 + " (" + bitstring10;
				pos.setValue(wcstring);
				terminal.setValue(word + ")");
			//}
			
			
//			if (bitstring != null) {
//			    	String firstChild = bitstring;
//			    	
//				//				if (posParent.label().value().startsWith("E1") ||
//				//		posParent.label().value().startsWith("E2")) { //smooth also E entities
//				pos.setValue(firstChild);
//				//above pos
//				//pos.setValue("(" + firstChild + " ("+ pos.label().value() );
//				//terminal.setValue(word + ")");
//						//	}
//			    	
//			
//			    }
			    
			}
			
		pet = tree.toString();

		if (!pet.contains(firstTarget) || ! pet.contains(secondTarget)) {
 			System.err.println("Ignoring problematic tree: "+ pet);
			return "";
		}
		
		return pet;
		
		} catch (NullPointerException e) {
			e.printStackTrace();
			System.out.println("Problem with: "+sentence.toString());
			System.out.println(parseTree.toString());
			System.out.println("Mention1: "+mention1.toString());
			System.out.println("Mention2: "+mention2.toString());
			return "";
		}
	}

}
