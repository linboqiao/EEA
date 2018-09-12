package limo.core;

import java.util.ArrayList;

import limo.core.interfaces.IEntities;
import limo.core.interfaces.IEntity;

/**
 * Class that implements IEntities interface and contains a
 * list of entities (an entity is the object an entity mention refers to)
 * 
 * @author Barbara Plank
 *
 */
public class Entities implements IEntities {

	private ArrayList<IEntity> entities;

	public Entities() {
		this.entities = new ArrayList<IEntity>();
	}
	
	public Entities(ArrayList<IEntity> entities) {
		this.entities = entities;
	}

	public ArrayList<IEntity> getEntities() {
		return this.entities;
	}
	
	public void addEntities(ArrayList<IEntity> entities){
		this.entities.addAll(entities);
	}

	public int size() {
		return this.entities.size();
	}

	public Entity getEntityById(String entityId) {
		for (IEntity e : this.entities) {
			if (e.getId().equals(entityId))
				return (Entity) e;
		}
		throw new IllegalArgumentException("Entity with ID not found: "+entityId);
	}

}
