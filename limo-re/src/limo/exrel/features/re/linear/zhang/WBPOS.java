package limo.exrel.features.re.linear.zhang;

import limo.core.Sentence;
import limo.core.Mention;
import limo.core.Relation;
import limo.core.trees.constituency.ParseTree;
//any pos tag inbetween 
import limo.exrel.features.re.linear.RelationExtractionLinearFeature;

public class WBPOS extends RelationExtractionLinearFeature {

	@Override
	protected String _extract(ParseTree parseTree, Mention mention1, Mention mention2,
			Relation relation, Sentence sentence, String groupId) {
		int[] tokens1 = mention1.getTokenIds();
		int[] tokens2 = mention2.getTokenIds();
		
		int idxFirstAfterM1 = tokens1[tokens1.length-1]+1;
		int idxBeforeM2 = tokens2[0]-1;
		
		if (idxBeforeM2 > idxFirstAfterM1) {
			int tokensInBetween = idxBeforeM2 - idxFirstAfterM1 - 1;

			if (tokensInBetween >= 1) {
				StringBuilder sb = new StringBuilder();
				for (int i = idxFirstAfterM1; i <= idxBeforeM2; i++) {
					sb.append(parseTree.getTerminalSurface(i));
					sb.append("--");
				}
				if (sb.toString().length() > 0)
					return sb.toString();
				else
					return null;
			}

		} else
			return null;
		return null;
		
	}

}
