package limo.exrel.features.re.structured;

import java.io.IOException;
import java.util.ArrayList;

import limo.core.Sentence;
import limo.core.Mention;
import limo.core.Relation;
import limo.core.trees.AbstractTree;
import limo.core.trees.constituency.ParseTree;

/***
 * Path Enclosed Tree, decorated like Zhang including mentions in between
 * 
 * @author Barbara Plank
 *
 */
public class PETdecoratedZhangIM extends RelationExtractionStructuredFeature {

	@Override
	protected String _extract(AbstractTree parseTree, Mention mention1,
			Mention mention2, Relation relation, Sentence sentence,ArrayList<Object> resources) throws IOException {
		
		ParseTree decorated = (ParseTree)parseTree;
	
		// decorate all other mentions in sentence
		for (Mention mention : sentence.getMentions()) {
			if (!mention.equals(mention1) && !mention.equals(mention2) ) { 
				//decorate mention
				String label = "E-"+mention.getType()+"-"+mention.getEntityReference().getType();
			      			
				int[] tokenIds = mention.getTokenIds();
				
				ParseTree tmpDecorated = new ParseTree(decorated.insertNode(tokenIds, label));
				decorated=tmpDecorated;
				tmpDecorated=null;
			}
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
	
		
		String firstTarget = "E1-"+mention1.getType()+"-"+mention1.getEntityReference().getType();
		String secondTarget = "E2-"+mention2.getType()+"-"+mention2.getEntityReference().getType();
			
		
		pet = pet.replaceAll(tmpFirstTarget, firstTarget);
		pet = pet.replaceAll(tmpSecondTarget, secondTarget);
	
		if (!pet.contains(firstTarget) || ! pet.contains(secondTarget)) {
 			System.err.println("Ignoring problematic tree: "+ pet);
			return "";
		}
		
		return pet;
	}
}
