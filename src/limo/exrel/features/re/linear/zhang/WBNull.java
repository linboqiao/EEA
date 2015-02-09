package limo.exrel.features.re.linear.zhang;

import limo.core.Sentence;
import limo.core.Mention;
import limo.core.Relation;
import limo.core.trees.constituency.ParseTree;
import limo.exrel.features.re.linear.RelationExtractionLinearFeature;

//when no word in between
public class WBNull extends RelationExtractionLinearFeature {

	@Override
	protected String _extract(ParseTree parseTree, Mention mention1, Mention mention2,
			Relation relation, Sentence sentence, String groupId) {
		int[] tokens1 = mention1.getTokenIds();
		int[] tokens2 = mention2.getTokenIds();
		
		int endM1 = tokens1[tokens1.length-1];
		int startM2 = tokens2[0];
		
		int tokensInBetween = startM2 - endM1 - 1;
		if (tokensInBetween == 0)
			return "true";
		else 
			return null;
		
	}

}
