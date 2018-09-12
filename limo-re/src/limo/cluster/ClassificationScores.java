package limo.cluster;

import java.util.ArrayList;

/***
 * Class that holds scores per instance
 * @author Barbara Plank
 *
 */
public class ClassificationScores {

	private ArrayList<Double> scoreList;
	private int current = 0;
	
	public ClassificationScores(ArrayList<Double> scoreList) {
		this.scoreList = scoreList;
	}
	
	public Double getNextScore() {
		if (current < this.scoreList.size()) {
			Double score = scoreList.get(current);
			current+=1;
			return score;
		} else
			 throw new IndexOutOfBoundsException();
	}

	public int getNumScores() {
		return scoreList.size();
	}
	
	public void rewind() {
		this.current = 0;
	}

}
