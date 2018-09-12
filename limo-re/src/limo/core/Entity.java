package limo.core;

import java.util.ArrayList;

import limo.core.interfaces.IEntity;
import limo.core.interfaces.IMention;

/**
 * Class that implements an entity.
 * It has an entityType (e.g. LOC, PER), and id and an (optional) subtype.
 * 
 * @author Barbara Plank
 *
 */
public class Entity implements IEntity {

	//required
	private String id;
	private String type; //e.g. PER, LOC, ORG

	private ArrayList<IMention> mentions;

	//optional
	private String subtype = "";
	private double confidence = 0.0;
	
	public Entity(String id, String type) {
		this.id = id;
		this.type = type;
		this.mentions = new ArrayList<IMention>();
	}

	public String getId() {
		return id;
	}

	public String getType() {
		return type;
	}

	public void setId(String id) {
		this.id = id;
	}

	public void setType(String type) {
		this.type = type;
	}

	public ArrayList<IMention> getMentions() {
		return this.mentions;
	}

	public void addMention(Mention mention) {
		this.mentions.add(mention);
	}

	public Mention getMentionById(String id) {
		for (IMention e : this.mentions) {
			if (e.getId().equals(id))
				return (Mention) e;
		}
		throw new IllegalArgumentException("Mention with ID not found: "+id);
	}

	//optional subtype, e.g. government for "ORG"
	public String getSubtype() {
		return subtype;
	}

	public void setSubtype(String subtype) {
		this.subtype = subtype;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		result = prime * result
				+ ((mentions == null) ? 0 : mentions.hashCode());
		result = prime * result + ((type == null) ? 0 : type.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Entity other = (Entity) obj;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		if (mentions == null) {
			if (other.mentions != null)
				return false;
		} else if (!mentions.equals(other.mentions))
			return false;
		if (type == null) {
			if (other.type != null)
				return false;
		} else if (!type.equals(other.type))
			return false;
		return true;
	}

	public double getConfidence() {
		return confidence;
	}
	
	public void setConfidence(double confidence) {
		this.confidence = confidence;
	}
}
