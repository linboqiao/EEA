package limo.core;

import java.util.ArrayList;

import limo.core.interfaces.IRelation;

/**
 * A relation is a triple: relationType(arg1, arg2)
 * where arg1 and arg2 are Mentions.
 * 
 * @author Barbara Plank
 *
 */
public class Relation implements IRelation {

	private String id;
	private Mention mention1;
	private Mention mention2;
	
	private String relationType;
	private String relationSubType; //optional
	
	private boolean isSymmetric = false; //optional if relation is symmetric
	
	private boolean visited = false; //optional
	


	public static final String TYPE_SUBTYPE_CONCATSYMB = ".";
	
	private Relation() {} //private constructor
	
	public static Relation createRelation(String id, Mention mention1, Mention mention2, String type) {
		Relation relation = new Relation();
		relation.id = id;
		relation.mention1 = mention1;
		relation.mention2 = mention2;
		relation.relationType = type;
		relation.relationSubType = null;
		return relation;
	}
	
	public static Relation createRelation(String id, Mention mention1, Mention mention2, String type, String subType) {
		Relation relation = new Relation();
		relation.id = id;
		relation.mention1 = mention1;
		relation.mention2 = mention2;
		relation.relationType = type;
		relation.relationSubType = subType;
		return relation;
	}
	
	public static Relation createEmptyRelation(String id, String type, String subType) {
		Relation relation = new Relation();
		relation.id = id;
		relation.relationType = type;
		relation.relationSubType = subType;
		relation.mention1 = null;
		relation.mention2 = null;
		return relation;
	}
	
	public static Relation createRelationFromClassificationResult(String relationId, String sentenceWith2MentionsTaggedAsNER, String relationIdentifier) throws Exception {
		Sentence s = Sentence.createSentenceFromNERtaggedInput(0, sentenceWith2MentionsTaggedAsNER);
		
		String relationType = relationIdentifier;
		String relationSubType = "";
		
		String relationDirection = "A-B"; //for symmetric ones fixed order
		if (relationIdentifier.contains(".")) {
			relationType = relationIdentifier.substring(0,relationIdentifier.lastIndexOf("."));
			relationDirection = relationIdentifier.substring(relationIdentifier.lastIndexOf(".")+1);
		}
		

		Mention m1 = null;
		Mention m2 = null;
		
		if (relationType.contains(".")) {
			relationSubType = relationType.substring(relationType.lastIndexOf(".")+1);
			relationType = relationType.substring(0,relationType.lastIndexOf("."));
		}
		
		if (s.getMentions().size() != 2)
			throw new Exception("Invalid number of mentions when instantiating relation! "+ sentenceWith2MentionsTaggedAsNER +  " " +relationIdentifier);
		else {
			ArrayList<Mention> mentions = s.getMentions();
		
			Mention mentionFirst = (Mention) mentions.get(0);
			mentionFirst.setIsInRelation(true);
			Mention mentionSecond = (Mention)mentions.get(1);
			mentionSecond.setIsInRelation(true);
			
			if (relationDirection.equals("A-B")) {
				m1 = mentionFirst;
				m2 = mentionSecond;
			} else {
				m2 = mentionFirst;
				m1 = mentionSecond;
			}
			
		}
		Relation relation = Relation.createRelation(relationId, m1, m2, relationType, relationSubType);
		
		return relation;
	}

	protected void setFirstMention(Mention mention) {
		this.mention1 = mention;
	}
	
	protected void setSecondMention(Mention mention) {
		this.mention2 = mention;
	}

	public String getId() {
		return id;
	}

	public Mention getFirstMention() {
		return this.mention1;
	}

	public Mention getSecondMention() {
		return this.mention2;
	}
	
	public String getRelationType() {
		return relationType;
	}

	public void setRelationType(String relationType) {
		this.relationType = relationType;
	}
	
	@Override
	public String toString() {
		return "[" + getRelationType()+ "."+getRelationSubType()+ " "  + getFirstMention().getEntityReference().getType() + "|" + getSecondMention().getEntityReference().getType() +
		" "  + getFirstMention().getEntityReference().getSubtype() + "|" + getSecondMention().getEntityReference().getSubtype() + 
		 " "  + getFirstMention().getType() + "-" + getSecondMention().getType() +" "+ getFirstMention().getHead() + " <=> " + getSecondMention().getHead() +"]" + " " + getId();
	}
	
	public String toShortString() {
		return "[" + getRelationType()+ " "+ getFirstMention().getHead() + " <=> " + getSecondMention().getHead() +"]" + " " + getId();
	}
	
	public String getRelationSubType() {
		return relationSubType;
	}

	public void setRelationSubType(String relationSubType) {
		this.relationSubType = relationSubType;
	}
	
	public boolean isSymmetric() {
		return isSymmetric;
	}

	public void setSymmetric(boolean isSymmetric) {
		this.isSymmetric = isSymmetric;
	}
	
	//to check if we have duplicate relations
	//use headToken to check since there might be multiple relations annotated within the same token e.g. groups of US-Swiss-French
	@Override
	public boolean equals(Object object) {
		Relation relObj = (Relation)object;
		if (this.getFirstMention().sameHeadToken(relObj.getFirstMention()) &&
			this.getSecondMention().sameHeadToken(relObj.getSecondMention()) ) //&& 
			//this.getRelationType().equals(relObj.getRelationType())) don't allow different relations between same tokens!
		/*if (this.getFirstMention().getHeadStart() == relation.getFirstMention().getHeadStart() 
		    &&  this.getFirstMention().getHeadEnd() == relation.getFirstMention().getHeadEnd()
			&& this.getSecondMention().getHeadStart() == relation.getSecondMention().getHeadStart() 
			&& this.getSecondMention().getHeadEnd() == relation.getSecondMention().getHeadEnd()
			&& this.getRelationType().equals(relation.getRelationType()))*/
			return true;
		if (this.getFirstMention().sameHeadToken(relObj.getSecondMention()) &&
				this.getSecondMention().sameHeadToken(relObj.getFirstMention()) )
			return true; // also inverse
		return false;
	}
	
	public boolean equalsIncludingRelationType(Object object) {
		Relation relObj = (Relation)object;
		if (!this.getRelationType().equals(relObj.getRelationType()))
			return false;
		if (this.getFirstMention().sameHeadToken(relObj.getFirstMention()) &&
			this.getSecondMention().sameHeadToken(relObj.getSecondMention()))
			return true;
		if (this.getFirstMention().sameHeadToken(relObj.getSecondMention()) &&
				this.getSecondMention().sameHeadToken(relObj.getFirstMention()) )
			return true; // also inverse
		return false;
	}

	public boolean isVisited() {
		return visited;
	}

	public void setVisited(boolean visited) {
		this.visited = visited;
	}

}
