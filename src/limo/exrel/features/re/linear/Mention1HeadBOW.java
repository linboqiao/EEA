package limo.exrel.features.re.linear;

import limo.core.Sentence;
import limo.core.Mention;
import limo.core.Relation;
import limo.core.trees.constituency.ParseTree;


public class Mention1HeadBOW extends RelationExtractionLinearFeature {

	@Override
	protected String _extract(ParseTree parseTree, Mention mention1, Mention mention2,
			Relation relation, Sentence sentence, String groupId) {
			String restrictedExtend = mention1.getExtend();
			String head = mention1.getHead();
			int rm = restrictedExtend.indexOf(head) + head.length();
			restrictedExtend = restrictedExtend.substring(0, rm);
			
			String[] parts =  restrictedExtend.split("\\s+");
			StringBuilder sb = new StringBuilder();
			for (String p : parts) {
				sb.append(p);
				sb.append(RelationExtractionLinearFeature.BOWseparator);
			}
			
			return sb.toString(); 
	}

}
