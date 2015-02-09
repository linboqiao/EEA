package limo.exrel.features.re.structured;

import java.io.IOException;
import java.util.ArrayList;

import edu.stanford.nlp.trees.Tree;

import limo.cluster.SemKernelDictionary;
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
public class BOW_Mlsa extends RelationExtractionStructuredFeature {

	@Override
	protected String _extract(AbstractTree parseTree, Mention mention1,
			Mention mention2, Relation relation, Sentence sentence, ArrayList<Object> resources) throws IOException {
		
		ParseTree decorated = (ParseTree)parseTree;
		
		
		SemKernelDictionary semkernelDict = (SemKernelDictionary)resources.get(2);
		
		if (semkernelDict== null) {
			System.err.println("Did you load the semkerneldict file? Check config file.");
			System.exit(-1);
		}
		
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


	
		ParseTree p = new ParseTree(pet);
		for (Tree terminal : p.getTerminals()) {
			String word = terminal.label().value();
			String wordPos = semkernelDict.getPrefixedWordIndex(word);
			if (wordPos != null)
				terminal.setValue(wordPos); 
		}
		ParseTree parseTreePet = new ParseTree(p.toString());	
		StringBuilder sb = new StringBuilder();
		sb.append("(BOX "); //do not consider BOX around (SVMLIGHT-tk)
		for (Tree terminal : parseTreePet.getTerminals()) {
			
			Tree parent = terminal.parent(parseTreePet.getRootNode()).parent(parseTreePet.getRootNode());
			
			
			if (parent.label().value().equals("E1")||parent.label().value().equals("E2")) {
				sb.append("("+parent.label().value()+ " ");
				
			} else {
				sb.append("(W ");
			}
			
			
			sb.append(terminal.label().value()+")");
		}
		sb.append(")");
	
		ParseTree bow = new ParseTree(sb.toString());
		if (!pet.contains(firstTarget) || ! pet.contains(secondTarget)) {
 			System.err.println("Ignoring problematic tree: "+ pet);
			return "";
		}
		
		return bow.toString();
	}

}
