package limo.exrel.features.re.linear;

import limo.core.Sentence;
import limo.core.Mention;
import limo.core.Relation;
import limo.core.trees.constituency.ParseTree;


public class MentionDistance extends RelationExtractionLinearFeature {

	@Override
	protected String _extract(ParseTree parseTree, Mention mention1, Mention mention2,
			Relation relation, Sentence sentence, String groupId) {
			if (mention1.getHeadStart() < mention2.getHeadStart()) {
				//mention 1 is before mention2
				int tokenPosM1 = mention1.getTokenIds()[mention1.getTokenIds().length-1]; 
				int tokenPosM2 = mention2.getTokenIds()[0];
				return String.valueOf(tokenPosM2 - tokenPosM1);	
			}
			else {
				int tokenPosM1 = mention2.getTokenIds()[mention2.getTokenIds().length-1]; 
				int tokenPosM2 = mention1.getTokenIds()[0];
				return String.valueOf(tokenPosM2 - tokenPosM1);	
			}
	}

}
