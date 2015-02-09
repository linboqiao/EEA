package limo.core.interfaces;

import java.util.ArrayList;

import limo.core.Mention;

/**
 * IEntity interface
 * 
 * @author Barbara Plank
 *
 */
public interface IEntity {
	
	public String getId();
	public void setId(String id);
	
	public String getType();
	public void setType(String type);	
	
	public ArrayList<IMention> getMentions();
	public void addMention(Mention mention);
	
	public boolean equals(Object o);
}
