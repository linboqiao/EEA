package limo.exrel.features.re.structured;

import java.io.IOException;
import java.util.ArrayList;

import limo.cluster.Word2VecSemKernelDictionary;
import limo.core.Mention;
import limo.core.Relation;
import limo.core.Sentence;
import limo.core.trees.AbstractTree;
import limo.core.trees.constituency.ParseTree;
import edu.stanford.nlp.trees.Tree;

public class PETw2v extends RelationExtractionStructuredFeature {
	
	@Override
	protected String _extract(AbstractTree parseTree, Mention mention1,
			Mention mention2, Relation relation, Sentence sentence,  ArrayList<Object> resources) throws IOException {
		
		ParseTree decorated = (ParseTree)parseTree;
		//do not decorate all other mentions in sentence
		
		Word2VecSemKernelDictionary word2vecSemkernelDict = (Word2VecSemKernelDictionary)resources.get(3);
		
		if (word2vecSemkernelDict== null) {
			System.err.println("Did you load the word2vecSemkerneldict file? Check config file.");
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

		if (!pet.contains(firstTarget) || ! pet.contains(secondTarget)) {
 			System.err.println("Ignoring problematic tree: "+ pet);
			return "";
		}
		

		ParseTree p = new ParseTree(pet);
		for (Tree terminal : p.getTerminals()) {
			String word = terminal.label().value();
			String wordPos = word2vecSemkernelDict.getPrefixedWordIndex(word);
			terminal.setValue(wordPos); 
		}
		
		
		return p.toString();
	}

}
