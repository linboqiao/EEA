package limo.core.interfaces;

import limo.core.Mention;

/**
 * IMention interface
 * 
 * @author Barbara Plank
 *
 */
public interface IMention  {
	
	public String getId();
	public String getType();
	
	public String getExtend();
    public void setExtend(String extend);

    public String getHead();
    public void setHead(String head);
    
    public boolean equals(Object o);
	int compareTo(Mention other);
}
