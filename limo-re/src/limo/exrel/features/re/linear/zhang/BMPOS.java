package limo.exrel.features.re.linear.zhang;

import limo.core.Sentence;
import limo.core.Mention;
import limo.core.Relation;
import limo.core.trees.constituency.ParseTree;
import limo.exrel.features.re.linear.RelationExtractionLinearFeature;

//first and second POS after mention
public class BMPOS extends RelationExtractionLinearFeature {

	@Override
	protected String _extract(ParseTree parseTree, Mention mention1, Mention mention2,
			Relation relation, Sentence sentence, String groupId) {
		int[] tokens1 = mention1.getTokenIds();
		
		int idxFirstBeforeM1 = tokens1[0]-1;
		int idxSecondBeforeM1 = tokens1[0]-2;
		if (idxFirstBeforeM1 >=0 ) {
			StringBuilder sb = new StringBuilder();
			sb.append( parseTree.getTerminalSurface(idxFirstBeforeM1));
			if (idxSecondBeforeM1 >=0) { 
				sb.append("--"+parseTree.getTerminalSurface(idxSecondBeforeM1));
			}
			return sb.toString();
		} else 
			return null;
		
	}

}
