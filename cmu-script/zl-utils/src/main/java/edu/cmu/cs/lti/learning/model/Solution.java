package edu.cmu.cs.lti.learning.model;

import java.io.Serializable;

/**
 * Created with IntelliJ IDEA.
 * Date: 8/21/15
 * Time: 3:36 PM
 *
 * @author Zhengzhong Liu
 */
public abstract class Solution implements Comparable, Serializable {
    private static final long serialVersionUID = 2784520773198397612L;
    private double score = Double.NEGATIVE_INFINITY;

    protected void setScore(double score) {
        this.score = score;
    }

    public abstract boolean equals(Object s);

    public abstract double loss(Solution s);

    public int compareTo(Object s) {
        if (s == null) {
            return -1;
        }
        return ((score - ((Solution) s).score) > 0) ? 1 : -1;
    }

    public abstract int getClassAt(int i);
}
