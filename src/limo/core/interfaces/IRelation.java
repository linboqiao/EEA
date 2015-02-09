package limo.core.interfaces;

import limo.core.Mention;

/**
 * IRelation interface
 * 
 * @author Barbara Plank
 *
 */
public interface IRelation {

	Mention getFirstMention();

	Mention getSecondMention();
	
	public String getRelationType() ;
	

}
