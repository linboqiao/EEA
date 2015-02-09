package limo.exrel.features.re.linear;

import limo.core.Sentence;
import limo.core.Mention;
import limo.core.Relation;
import limo.core.trees.constituency.ParseTree;


public class MentionLDCType extends RelationExtractionLinearFeature {

	@Override
	protected String _extract(ParseTree parseTree, Mention mention1,
			Mention mention2, Relation relation, Sentence sentence,
			String groupId) {
		if (groupId.equals("mention1")) {
		    //if (mention1.getHeadStart() < mention2.getHeadStart())
				return mention1.getMentionLDCType();
				//else
				//return mention2.getMentionLDCType();
		} else {
		    //if (mention2.getHeadStart() > mention1.getHeadStart())
				return mention2.getMentionLDCType();
				//else
				//return mention1.getMentionLDCType();
		}
	}

}
