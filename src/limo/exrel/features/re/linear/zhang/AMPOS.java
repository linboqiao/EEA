package limo.exrel.features.re.linear.zhang;

import limo.core.Sentence;
import limo.core.Mention;
import limo.core.Relation;
import limo.core.trees.constituency.ParseTree;
import limo.exrel.features.re.linear.RelationExtractionLinearFeature;

//first and second POS after mention
public class AMPOS extends RelationExtractionLinearFeature {

	@Override
	protected String _extract(ParseTree parseTree, Mention mention1, Mention mention2,
			Relation relation, Sentence sentence, String groupId) {
		int[] tokens2 = mention2.getTokenIds();
		
		int idxFirstAfterM2 = tokens2[tokens2.length-1]+1;
		int idxSecondAfterM2 = tokens2[tokens2.length-1]+2;
		if (idxFirstAfterM2 < (sentence.getTokens().size()-1)) {
			StringBuilder sb = new StringBuilder();
			sb.append(parseTree.getTerminalSurface(idxFirstAfterM2));
			if (idxSecondAfterM2 < (sentence.getTokens().size()-1)) { 
				sb.append("--"+parseTree.getTerminalSurface(idxSecondAfterM2));
			}
			return sb.toString();
		} else 
			return null;
		
	}

}
