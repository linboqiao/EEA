package limo.exrel.features.re.linear;

import limo.core.Sentence;
import limo.core.Mention;
import limo.core.Relation;
import limo.core.trees.constituency.ParseTree;
//check whether entity types are compatible for given relation
public class CompatibleConll04 extends RelationExtractionLinearFeature {

	@Override
	protected String _extract(ParseTree parseTree, Mention mention1, Mention mention2,
			Relation relation, Sentence sentence, String groupId) {
		
		if (relation == null)
			return ""; //only for actual relations
		
		String m1type = relation.getFirstMention().getType();
		String m2type = relation.getSecondMention().getType();
		
		String rel = relation.getRelationType();
		
		if (rel.equals("Kill") && m1type.equals("Peop") && m2type.equals("Peop"))
			return "YES";
		if (rel.equals("Kill") && m1type.equals("PERSON") && m2type.equals("PERSON"))
			return "YES";
		if ((rel.equals("Live_In")||rel.equals("LiveIn")) && m1type.equals("Peop") && m2type.equals("Loc"))
			return "YES";
		if ((rel.equals("Live_In")||rel.equals("LiveIn")) && m1type.equals("PERSON") && m2type.equals("LOCATION"))
			return "YES";
		if ((rel.equals("Located_In")||rel.equals("LocatedIn")) && m1type.equals("Loc") && m2type.equals("Loc"))
			return "YES";
		if ((rel.equals("Located_In")||rel.equals("LocatedIn")) && m1type.equals("Org") && m2type.equals("Loc"))
				return "YES";
		if ((rel.equals("Located_In")||rel.equals("LocatedIn")) && m1type.equals("LOCATION") && m2type.equals("LOCATION"))
			return "YES";
		if ((rel.equals("Located_In")||rel.equals("LocatedIn")) && m1type.equals("ORGANIZATION") && m2type.equals("LOCATION"))
			return "YES";
		if ((rel.equals("OrgBased_In")||rel.equals("OrgBasedIn")) && m1type.equals("Org") && m2type.equals("Loc"))
			return "YES";
		if ((rel.equals("OrgBased_In")||rel.equals("OrgBasedIn")) && m1type.equals("ORGANIZATION") && m2type.equals("LOCATION"))
			return "YES";
		if ((rel.equals("Work_For")||rel.equals("WorkFor")) && m1type.equals("Peop") && m2type.equals("Org"))
			return "YES";
		if ((rel.equals("Work_For")||rel.equals("WorkFor")) && m1type.equals("PERSON") && m2type.equals("ORGANIZATION"))
			return "YES";
		if (m1type.equals("MISS") || m2type.equals("MISS"))
			return "MAYBE";
		if (m1type.equals("ENTITY") || m2type.equals("ENTITY"))
			return "MAYBE";
		return "NO";
		
	}

}
