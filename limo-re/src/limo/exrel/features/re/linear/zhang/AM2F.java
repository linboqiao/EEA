package limo.exrel.features.re.linear.zhang;

import limo.core.Sentence;
import limo.core.Mention;
import limo.core.Relation;
import limo.core.trees.constituency.ParseTree;
import limo.exrel.features.re.linear.RelationExtractionLinearFeature;

//first word after M2
public class AM2F extends RelationExtractionLinearFeature {

	@Override
	protected String _extract(ParseTree parseTree, Mention mention1, Mention mention2,
			Relation relation, Sentence sentence, String groupId) {
		int[] tokens2 = mention2.getTokenIds();
		
		int idxFirstAfterM2 = tokens2[tokens2.length-1]+1;
		if (idxFirstAfterM2 < (sentence.getTokens().size()-1)) {
			return sentence.getTokens().get(idxFirstAfterM2).getValue();
		} else 
			return null;
		
	}

}
