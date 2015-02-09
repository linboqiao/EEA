package limo.exrel.features.re.linear;

import limo.core.Sentence;
import limo.core.Mention;
import limo.core.Relation;
import limo.core.trees.constituency.ParseTree;

// tokens between the two entities (unigrams only)
public class B1 extends RelationExtractionLinearFeature {

	@Override
	protected String _extract(ParseTree parseTree, Mention mention1, Mention mention2,
			Relation relation, Sentence sentence, String groupId) {
		
		int[] tokens1 = mention1.getTokenIds();
		int[] tokens2 = mention2.getTokenIds();
		
	
		int endM1 = tokens1[tokens1.length-1];	
		int startM2 = tokens2[0];
		
		StringBuilder sb = new StringBuilder();
		int i=endM1+1;
		
		while (i < startM2) {
		
			sb.append(sentence.getTokens().get(i).getValue());
			sb.append(RelationExtractionLinearFeature.BOWseparator);
			
			i++;
		} 
		return sb.toString();
		
	}

}
