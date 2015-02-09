package limo.core;

import java.util.ArrayList;

/**
 * List of Relations
 * 
 * @author Barbara Plank
 *
 */
public class Relations {

	private ArrayList<Relation> relations;

	public Relations() {
		relations = new ArrayList<Relation>();
	}
	
	public Relations(ArrayList<Relation> relations) {
		this.relations = relations;
	}

	public int size() {
		return this.relations.size();
	}
	
	public ArrayList<Relation> get() {
		return this.relations;
	}
	
	// keep it just for semod
	public ArrayList<Relation> getRelations() {
		return this.relations;
	}

	public void add(Relation relation) {
		Relation checkIfExists = this.getRelation(relation.getFirstMention(), relation.getSecondMention());
		if (checkIfExists !=null) {
			System.err.println("Ignoring relation since there exists already a relation between the two mentions! "+relation);
			return;
		}
		//check if we have it already (don't use id, but check mentions head and relation type) 
		//ACE 2005 contains the same relation but with different ids,thus avoid duplicates
		/*for (IRelation rel : this.relations) {
			if (rel.getFirstMention().getHead().equals(relation.getFirstMention().getHead()) &&
				rel.getSecondMention().getHead().equals(relation.getSecondMention().getHead()) && 
				rel.getRelationType().equals(relation.getRelationType())
	            ) {
				System.out.println(">>>> ignoring since we already have it!");
				return;
			}
		}*/
		this.relations.add(relation);
	}

	/**
	 * Returns IRelation if mention1 and mention2 participate in relation, 
	 * independent of the order!
	 * @param mention1
	 * @param mention2
	 * @return null if relation is not found
	 */
	public Relation getRelation(Mention mention1, Mention mention2) {
		for (Relation relation : this.relations) {
			if (relation.getFirstMention()==null || relation.getSecondMention()==null)
				continue;
			if (relation.getFirstMention().equals(mention1) && relation.getSecondMention().equals(mention2) ||
				relation.getFirstMention().equals(mention2) && relation.getSecondMention().equals(mention1)) {
				return relation;
			}
		}
		return null;
	}
	
}
