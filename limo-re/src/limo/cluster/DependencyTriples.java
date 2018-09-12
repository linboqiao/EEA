package limo.cluster;

import java.util.HashMap;

/***
 * 
 * Holds dependency triples
 * 
 * @author Barbara Plank
 *
 */
public class DependencyTriples {
	HashMap<String, Double> map;

	public DependencyTriples() {
		 this.map = new HashMap<String, Double>();
	}

	public void add(String triple, Double score) {
		this.map.put(triple, score);			
	}

}