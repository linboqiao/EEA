package limo.cluster;

import java.util.ArrayList;
import java.util.HashMap;

public class WordEmbedding {

	HashMap<String, ArrayList<Double>> map;
	int dimension;
	
	public WordEmbedding() {
		 this.map = new HashMap<String, ArrayList<Double>>();
		 this.dimension = -1;
	}

	public void add(String line) {
		if (line == null || line.isEmpty()) return;
		String[] args = line.split(" ");
		ArrayList<Double> wed = new ArrayList<Double>();
		for (int i = 1; i < args.length; i++) {
			wed.add(Double.parseDouble(args[i]));
		}
		if (this.dimension < 0) {
			this.dimension = wed.size();
		}
		else {
			if (this.dimension != wed.size()) {
				System.out.println("Inconsistent dimensions: " + this.dimension + " vs " + wed.size());
				System.exit(-1);
			}
		}
		this.map.put(args[0], wed);
	}
	
	public int getDimension() {
		return dimension;
	}

}
