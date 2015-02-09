package limo.exrel.features.re.linear.zhang;

import limo.core.Sentence;
import limo.core.Mention;
import limo.core.Relation;
import limo.core.trees.constituency.ParseTree;
import limo.exrel.features.re.linear.RelationExtractionLinearFeature;

//second word before M1
public class BM1L extends RelationExtractionLinearFeature {

	@Override
	protected String _extract(ParseTree parseTree, Mention mention1, Mention mention2,
			Relation relation, Sentence sentence, String groupId) {
		int[] tokens1 = mention1.getTokenIds();
		
		int idxFirstBeforeM1 = tokens1[0]-1;
		if (idxFirstBeforeM1 >=0) {
			return sentence.getTokens().get(idxFirstBeforeM1).getValue();
		} else 
			return null;
		
	}

}
