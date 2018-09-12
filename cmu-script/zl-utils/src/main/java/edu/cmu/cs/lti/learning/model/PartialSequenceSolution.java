package edu.cmu.cs.lti.learning.model;

import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * Date: 3/1/16
 * Time: 3:29 PM
 *
 * @author Zhengzhong Liu
 */
public class PartialSequenceSolution extends Solution {
    private int sequenceLength;

    // Container for the solution.
    private List<Integer> solution;

    @Override
    public boolean equals(Object s) {
        if (s == null) {
            return false;
        }

        if (getClass() != s.getClass())
            return false;

        PartialSequenceSolution otherSolution = (PartialSequenceSolution) s;

        if (otherSolution.sequenceLength != sequenceLength) {
            throw new IllegalArgumentException("Cannot compare two solution on difference sequences.");
        } else {
            for (int i = 0; i < sequenceLength; i++) {
                if (otherSolution.getClassAt(i) != solution.get(i)) {
                    return false;
                }
            }
        }

        return true;
    }

    public int getClassAt(int i) {
        return solution.get(i);
    }

    public double lossUntil(Solution s, int index) {
        int differences = 0;
        for (int i = 0; i < index; i++) {
            if (solution.get(i) != s.getClassAt(i)) {
                differences++;
            }
        }
        return differences;
    }

    @Override
    public double loss(Solution s) {
        return lossUntil(s, sequenceLength);
    }
}
