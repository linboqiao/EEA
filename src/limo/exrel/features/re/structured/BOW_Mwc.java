package limo.exrel.features.re.structured;

import java.io.IOException;
import java.util.ArrayList;

import edu.stanford.nlp.trees.Tree;

import limo.cluster.BrownWordCluster;
import limo.core.Sentence;
import limo.core.Mention;
import limo.core.Relation;
import limo.core.trees.AbstractTree;
import limo.core.trees.constituency.ParseTree;

/***
 * BOW tree with "BOX" as root token -> so that SVMlight-tk does not consider root node
 * 
 * @author Barbara Plank
 *
 */
public class BOW_Mwc extends RelationExtractionStructuredFeature {

	protected String _extract(AbstractTree parseTree, Mention mention1,
			Mention mention2, Relation relation, Sentence sentence, ArrayList<Object> resources) throws IOException  {
		try {
		ParseTree decorated = (ParseTree)parseTree;
		
		BrownWordCluster wordCluster = (BrownWordCluster)resources.get(0);
				
		String tmpFirstTarget = "ENTITY-INFOT1";
		String tmpSecondTarget = "ENTITY-INFOT2";
		
		int[] tokenIds1 = mention1.getTokenIds();
		int[] tokenIds2 = mention2.getTokenIds();
		
		String decoratedStr = decorated.insertNodes(tokenIds1, tmpFirstTarget, tokenIds2, tmpSecondTarget);
		ParseTree parseTreeDecorated = new ParseTree(decoratedStr);
		
		int spanTokenIdStart = min(tokenIds1, tokenIds2);
		int spanTokenIdEnd = max(tokenIds1, tokenIds2);
		
		if (wordCluster == null) {
			System.err.println("Did you load the word cluster file? Check config file.");
			System.exit(-1);
		}
		
		
		String pet = parseTreeDecorated.getPathEnclosedTree(spanTokenIdStart,spanTokenIdEnd);
		
		
		String firstTarget = "E1";
		String secondTarget = "E2";	
				
		pet = pet.replaceAll(tmpFirstTarget, firstTarget);
		pet = pet.replaceAll(tmpSecondTarget, secondTarget);

		ParseTree parseTreePet = new ParseTree(pet);
		
		StringBuilder sb = new StringBuilder();
		sb.append("(BOX "); //do not consider BOX around (SVMLIGHT-tk)
		for (Tree terminal : parseTreePet.getTerminals()) {
			
			Tree parent = terminal.parent(parseTreePet.getRootNode()).parent(parseTreePet.getRootNode());
			
			String word = terminal.label().value();
			String bitstring = wordCluster.getPrefixClusterId(word,10);
			if (bitstring != null)
				word = bitstring;
		
			if (parent.label().value().equals("E1")||parent.label().value().equals("E2")) {
				sb.append("("+parent.label().value() + " ");
				sb.append(word+")");
				} else {
				sb.append("(W ");
				sb.append(word+")");
			}
		}
		sb.append(")");
	
		ParseTree bow = new ParseTree(sb.toString());
		if (!pet.contains(firstTarget) || ! pet.contains(secondTarget)) {
 			System.err.println("Ignoring problematic tree: "+ pet);
			return "";
		}
		
		return bow.toString();
		} catch (Exception e) {
			System.err.println("Sentence: "+ sentence);
			System.err.println("Problem with: "+parseTree.toString());
			System.err.println("Mention1: "+mention1.getHead());
			System.err.println("Mention1: "+mention2.getHead() );
			return "";
		}
	}

}
