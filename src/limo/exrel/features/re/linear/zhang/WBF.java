package limo.exrel.features.re.linear.zhang;

import limo.core.Sentence;
import limo.core.Mention;
import limo.core.Relation;
import limo.core.trees.constituency.ParseTree;
import limo.exrel.features.re.linear.RelationExtractionLinearFeature;

//first word in between when at least two words in between
public class WBF extends RelationExtractionLinearFeature {

	@Override
	protected String _extract(ParseTree parseTree, Mention mention1, Mention mention2,
			Relation relation, Sentence sentence, String groupId) {
		int[] tokens1 = mention1.getTokenIds();
		int[] tokens2 = mention2.getTokenIds();

		
		int endM1 = tokens1[tokens1.length-1];
		int startM2 = tokens2[0];
		
		int tokensInBetween = startM2 - endM1 - 1;
		if (tokensInBetween >= 2) {
			return sentence.getTokens().get(endM1+1).getValue();
		} else 
			return null;
		
	}

}
