package limo.exrel.features.re.linear;

import limo.core.Sentence;
import limo.core.Mention;
import limo.core.Relation;
import limo.core.trees.constituency.ParseTree;

//Between-After. Tokens between and after the two entities (unigrams only)
public class BA2 extends RelationExtractionLinearFeature {

	@Override
	protected String _extract(ParseTree parseTree, Mention mention1, Mention mention2,
			Relation relation, Sentence sentence, String groupId) {
		
		int[] tokens1 = mention1.getTokenIds();
		int[] tokens2 = mention2.getTokenIds();
		
		
		int endM1 = tokens1[tokens1.length-1];
		
		int startM2 = tokens2[0];
		int endM2 = tokens2[tokens2.length-1];
		
		StringBuilder sb = new StringBuilder();
		int i=endM1+1;
		
		while (i < sentence.getTokens().size()-1) {
		
			if (i<startM2 || i >endM2) {
				sb.append(sentence.getTokens().get(i).getValue());
				sb.append(RelationExtractionLinearFeature.BOWseparator);
			}
			i++;
		} 
		//add bigrams
		String[] parts = sb.toString().split(RelationExtractionLinearFeature.BOWseparator);
		for (int idx=0; idx < parts.length-1; idx++) {
			sb.append(parts[idx]+"_"+parts[idx+1]);
			sb.append(RelationExtractionLinearFeature.BOWseparator);			
		}
		
		return sb.toString();
		
	}

}
