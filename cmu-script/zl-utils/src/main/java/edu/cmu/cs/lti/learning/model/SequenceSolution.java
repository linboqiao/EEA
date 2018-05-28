package edu.cmu.cs.lti.learning.model;

import com.google.common.collect.MinMaxPriorityQueue;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;
import org.apache.commons.lang3.builder.CompareToBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Created with IntelliJ IDEA.
 * Date: 8/21/15
 * Time: 3:44 PM
 *
 * @author Zhengzhong Liu
 */
public class SequenceSolution extends Solution {
    private static final long serialVersionUID = 4963833442738553688L;
//    private final Logger logger = LoggerFactory.getLogger(getClass());

    private ClassAlphabet classAlphabet;
    private int sequenceLength;

    // Container for the solutions, the k-th row corresponding to k-th best solution.
    private int[][] solution;

    // Container for the scores at each step, the k-th row corresponding to k-th best solution.
    // The score is the additional score given by each step, to get the solution score, sum them up.
    private double[][] elementScores;

    // Each cells stores a list of best states that ended here, stored as a queue (heap).
    // Because the MinMaxQueue will remove the greatest element when full, we reverse the comparator.
    // To retrieve the best score for each pointer, just do poll (remove afterwards), and take the negative.
    // This also implies tie is broken on the smaller state index.
    // The queue can be iterate in order only once, hence it is temporary and will be iterate and convert as a list.
    private MinMaxPriorityQueue<LatticeCell>[] temporaryCells;

    // A full back pointer lattice that store all the cells, each cell store a list of k best back pointers.
    private List<LatticeCell>[][] latticeCells;

    private int currentPosition;

    // K of the best K solution.
    private int bestK;

    private TIntSet outsideSet;

    /**
     * Create a solution with existing sequence set, should be used for gold standard. Decoding support such as
     * back pointer saving are not provided in this constructor.
     *
     * @param classAlphabet The Class Alphabet contains state information.
     * @param sequence      The known sequence.
     */
    public SequenceSolution(ClassAlphabet classAlphabet, int[] sequence) {
        this.classAlphabet = classAlphabet;
        this.sequenceLength = sequence.length;
        solution = new int[1][sequenceLength];
        System.arraycopy(sequence, 0, solution[0], 0, sequenceLength);
    }

    /**
     * Create a empty solution object, which can be later filled during decoding. This is will create a best solution
     * container, if you need a best k container, use SequenceSolution(ClassAlphabet, int, int).
     *
     * @param classAlphabet  The Class Alphabet contains state information.
     * @param sequenceLength The length of the solution.
     */
    public SequenceSolution(ClassAlphabet classAlphabet, int sequenceLength, int bestK) {
        this(classAlphabet, sequenceLength, bestK, new int[0]);
    }

    /**
     * Create a empty solution object, which can be later filled during decoding. This is will create a best solution
     * container, if you need a best k container, use SequenceSolution(ClassAlphabet, int, int).
     *
     * @param classAlphabet  The Class Alphabet contains state information.
     * @param sequenceLength The length of the solution.
     */
    public SequenceSolution(ClassAlphabet classAlphabet, int sequenceLength, int... outsideIndices) {
        this(classAlphabet, sequenceLength, 1, outsideIndices);
    }

    /**
     * Create a empty solution object, which can be later filled during decoding. This is will create a best k solution
     * container.
     *
     * @param classAlphabet  The Class Alphabet contains state information.
     * @param sequenceLength The length of the solution.
     * @param bestK          How many sequence should be contained in the solution.
     */
    @SuppressWarnings("unchecked")
    public SequenceSolution(ClassAlphabet classAlphabet, int sequenceLength, int bestK, int... outsideIndices) {
        this.bestK = bestK;
        this.classAlphabet = classAlphabet;
        this.sequenceLength = sequenceLength;
        solution = new int[bestK][sequenceLength];
        elementScores = new double[bestK][sequenceLength];
        temporaryCells = new MinMaxPriorityQueue[classAlphabet.size()];
        latticeCells = new List[sequenceLength + 1][classAlphabet.size()];

        outsideSet = new TIntHashSet();
        for (Integer indice : outsideIndices) {
            outsideSet.add(indice);
        }

        // TODO: it might be useless to start at -1.
//        currentPosition = -1;
        currentPosition = 0;
    }


    public class LatticeCell implements Serializable, Comparable<LatticeCell> {
        private static final long serialVersionUID = 5198803127418485563L;

        private double score;
        private double currentScore;
        private double previousScore;
        private double transitionScore;
        private int classIndex;
        private LatticeCell backPointer;

        public LatticeCell(double score, int classIndex, LatticeCell backPointer, double currentScore, double
                previousScore, double transitionScore) {
            this.score = score;
            this.classIndex = classIndex;
            this.backPointer = backPointer;
            // These 3 scores are used for debug.
            this.currentScore = currentScore;
            this.previousScore = previousScore;
            this.transitionScore = transitionScore;
        }

        /**
         * This is a reverse comparator, the greatest scored element will be treated as the least.
         *
         * @param o The other cell.
         * @return Negative if this is larger, 0 if equal, positive if this is smaller.
         */
        @Override
        public int compareTo(LatticeCell o) {
            return new CompareToBuilder().append(o.score, score).build();
        }

        @Override
        public String toString() {
            return new ToStringBuilder(this).append(score).append(classIndex).toString();
        }

        public String shortString() {
            return String.format("[%.2f=%.2f+%.2f+%.2f,%s <- %s]", score, previousScore, currentScore, transitionScore,
                    classAlphabet.getClassName(classIndex), classAlphabet.getClassName(backPointer.classIndex));
        }
    }

    public LatticeCell getEmptyCell() {
        return new LatticeCell(0, classAlphabet.getOutsideClassIndex(), null, 0, 0, 0);
    }

    /**
     * When the iteration pointer is larger than the sequence length, i.e., after the sequence end outside position.
     *
     * @return Whether the solution is finished.
     */
    public boolean finished() {
        return currentPosition > sequenceLength;
    }

    public boolean isRightLimit() {
        return currentPosition == sequenceLength;
    }

    public int getNumClasses() {
        if (currentPosition == sequenceLength) {
            return 1;
        } else {
            return classAlphabet.size();
        }
    }

    public IntStream getCurrentPossibleClassIndices() {
        return getPossibleClassIndices(currentPosition);
    }

    public IntStream getPreviousPossibleClassIndices() {
        return getPossibleClassIndices(currentPosition - 1);
    }

    public IntStream getPossibleClassIndices(int position) {
        if (position >= sequenceLength || position < 0 || outsideSet.contains(position)) {
            return classAlphabet.getOutsideClassRange();
        } else {
            return classAlphabet.getNormalClassesRange();
        }
    }

    @SuppressWarnings("unchecked")
    public void advance() {
        // Store all temporary back pointers from the heap to a list for further access.
        if (currentPosition >= 0) {
            getPossibleClassIndices(currentPosition).forEach(classIndex -> {
                MinMaxPriorityQueue<LatticeCell> tempBackPointer = temporaryCells[classIndex];
                if (tempBackPointer == null) {
                    throw new IllegalStateException(String.format("Temp pointers are not updated before call, cannot " +
                            "find cell at position %d, class %d", currentPosition - 1, classIndex));
                }
                latticeCells[currentPosition][classIndex] = new ArrayList<>();
                while (!tempBackPointer.isEmpty()) {
                    latticeCells[currentPosition][classIndex].add(tempBackPointer.poll());
//                System.out.println("Lattice cell at " + currentPosition + " " + classIndex + " updated.");
                }
            });
        }

        ++currentPosition;
        temporaryCells = new MinMaxPriorityQueue[classAlphabet.size()];
    }

    public int getBestK() {
        return bestK;
    }

    public int getCurrentPosition() {
        return currentPosition;
    }

    /**
     * Return the class of the best solution contained.
     *
     * @param sequenceIndex The class index
     * @return
     */
    public int getClassAt(int sequenceIndex) {
        return getClassAt(0, sequenceIndex);
    }

    /**
     * Return the class of the k-th best solution.
     *
     * @param k
     * @param sequenceIndex
     * @return
     */
    public int getClassAt(int k, int sequenceIndex) {
        if (sequenceIndex >= solution[k].length) {
            return classAlphabet.getOutsideClassIndex();
        }
        return solution[k][sequenceIndex];
    }

    public List<LatticeCell> getPreviousBests(int classIndex) {
        if (currentPosition == 0) {
            return Collections.singletonList(getEmptyCell());
        }
        return latticeCells[currentPosition - 1][classIndex];
    }

    private MinMaxPriorityQueue<LatticeCell> createMaxFirstQueue() {
        // Note that the comparison is revered within LatticCell, this make the Queue always discard the element with
        // minimum value.
        // You could consider this Queue compare elements by their rank (higher value with lower rank number), maximum
        // value rank number 1.
        return MinMaxPriorityQueue.maximumSize(bestK).create();
    }

    public int scoreNewEdge(int toCellClassIndex, LatticeCell fromCell, double newEdgeScore, double newNodeScore) {
        if (fromCell == null) {
            return 0;
        }

        int addResult = 0;

        double newScoreTillHere = fromCell.score + newEdgeScore + newNodeScore;

        MinMaxPriorityQueue<LatticeCell> queue = temporaryCells[toCellClassIndex];
        if (queue == null) {
            temporaryCells[toCellClassIndex] = createMaxFirstQueue();
            queue = temporaryCells[toCellClassIndex];
            addResult = 1;
        } else if (queue.isEmpty()) {
            addResult = 1;
        } else if (queue.peek().score < newScoreTillHere) {
            addResult = 1;
        }

        boolean rejected = !queue.offer(new LatticeCell(newScoreTillHere, toCellClassIndex, fromCell, newNodeScore,
                fromCell.score, newEdgeScore));
        if (rejected) {
            addResult = -1;
        }

        return addResult;
    }

    public String showBestBackPointerMap() {
        StringBuilder sb = new StringBuilder();
        String rowSep = "\t";
        String colSep = "";

        int rowNumber = 0;
        for (List<LatticeCell>[] backPointer : latticeCells) {
            sb.append(colSep);
            sb.append(rowNumber++);
            sb.append(rowSep);
            colSep = "\n";
            for (List<LatticeCell> aBackPointer : backPointer) {
                sb.append(rowSep);
                if (aBackPointer == null) {
                    sb.append("<EMPTY>");
                } else {
                    sb.append(aBackPointer.get(0).shortString());
                }
            }
        }
        return sb.toString();
    }

    public void backTrace() {
        for (int kthSolution = 0; kthSolution < bestK; kthSolution++) {
            int[] oneSolution = new int[sequenceLength];
            double[] oneScores = new double[sequenceLength];
            backTraceOne(latticeCells[sequenceLength][0].get(kthSolution), oneSolution, oneScores);
            solution[kthSolution] = oneSolution;

            for (int i = sequenceLength - 1; i > 0; i--) {
                oneScores[i] = oneScores[i] - oneScores[i - 1];
            }
            elementScores[kthSolution] = oneScores;
        }
    }

    /**
     * Back trace starting from one particular cell.
     *
     * @param cell The cell to start from
     * @return The decoded solution.
     */
    private int[] backTraceOne(LatticeCell cell, int[] oneSolution, double[] oneScores) {
        LatticeCell currentCell = cell;
        for (int backCol = sequenceLength - 1; backCol >= 0; backCol--) {
            currentCell = currentCell.backPointer;
            oneSolution[backCol] = currentCell.classIndex;
            oneScores[backCol] = currentCell.score;
//            System.out.println("Class index at " + backCol + " is " + currentCell.getClassIndex());
        }
        return oneSolution;
    }

    public int getSequenceLength() {
        return sequenceLength;
    }

    public int getRealSequenceLength() {
        return sequenceLength - outsideSet.size();
    }

    /**
     * Make the best solution to string by default.
     *
     * @return The string representation of the best solution.
     */
    public String toString() {
        int[] bestSolution = solution[0];
        return IntStream.range(0, bestSolution.length).mapToObj(solutionIndex -> solutionIndex + ":" + classAlphabet
                .getClassName(bestSolution[solutionIndex])).collect(Collectors.joining(", "));
    }

    public ClassAlphabet getClassAlphabet() {
        return classAlphabet;
    }

    /**
     * Equals is defined on the best solution contained.
     *
     * @param s The other solution to compare.
     * @return
     */
    @Override
    public boolean equals(Object s) {
        if (s == null) {
            return false;
        }

        if (getClass() != s.getClass())
            return false;

        SequenceSolution otherSolution = (SequenceSolution) s;

        if (otherSolution.sequenceLength != sequenceLength) {
            throw new IllegalArgumentException("Cannot compare two solution on difference sequences.");
        } else {
            for (int i = 0; i < sequenceLength; i++) {
                if (otherSolution.getClassAt(i) != solution[0][i]) {
                    return false;
                }
            }
        }

        return true;
    }

    /**
     * Compute loss of two solutions, always computed on the best solution.
     *
     * @param s The other solution to compute the loss against.
     * @return
     */
    @Override
    public double loss(Solution s) {
        if (getClass() != s.getClass())
            throw new IllegalArgumentException("Must compare with a sequence solution.");

        SequenceSolution otherSolution = (SequenceSolution) s;

        if (otherSolution.sequenceLength != sequenceLength) {
            throw new IllegalArgumentException("Cannot compare two solutions on difference sequences.");
        } else {
            int[] thisBest = solution[0];
            int mismatch = 0;
            int tp = 0;
            int numGold = 0;
            int numSys = 0;
            for (int i = 0; i < sequenceLength; i++) {
                int otherClass = classAlphabet.getNoneOfTheAboveClassIndex();
                if (thisBest[i] != otherClass) {
                    numGold += 1;
                    if (otherSolution.getClassAt(i) == thisBest[i]) {
                        tp += 1;
                    }
                }

                if (otherSolution.getClassAt(i) != otherClass) {
                    numSys += 1;
                }

                if (otherSolution.getClassAt(i) != thisBest[i]) {
                    mismatch += 1;
                }
            }

            double precision = numSys > 0 ? tp * 1.0 / numSys : 1;
            double recall = numGold > 0 ? tp * 1.0 / numGold : 1;

            double f1 = precision + recall == 0 ? 0 : 2 * precision * recall / (precision + recall);

            return 1 - f1;
//            return mismatch * 1.0 / sequenceLength;
        }
    }

    private void testQueue() {
        MinMaxPriorityQueue<LatticeCell> q = this.createMaxFirstQueue();

        for (int i = 0; i < 6; i++) {
            q.add(new LatticeCell(i, 0, null, 0, 0, 0));
        }

        while (!q.isEmpty()) {
            System.out.println(q.poll());
        }
    }

    public static void main(String[] args) {
        SequenceSolution test = new SequenceSolution(new ClassAlphabet(true, true), 0, 3);

        test.testQueue();
    }

}
