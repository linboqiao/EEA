package limo.exrel.features.re.linear;

import limo.core.Sentence;
import limo.core.Mention;
import limo.core.Relation;
import limo.core.trees.constituency.ParseTree;


public class MentionType1 extends RelationExtractionLinearFeature {

	@Override
	protected String _extract(ParseTree parseTree, Mention mention1, Mention mention2,
			Relation relation, Sentence sentence, String groupId) {
		return mention1.getType();
	}

}
