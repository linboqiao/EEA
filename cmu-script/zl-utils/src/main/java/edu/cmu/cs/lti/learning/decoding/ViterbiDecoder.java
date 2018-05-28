package edu.cmu.cs.lti.learning.decoding;

import com.google.common.collect.ArrayListMultimap;
import edu.cmu.cs.lti.learning.model.*;
import edu.cmu.cs.lti.learning.training.SequenceDecoder;
import edu.cmu.cs.lti.learning.utils.CubicLagrangian;
import edu.cmu.cs.lti.learning.utils.DummyCubicLagrangian;
import edu.cmu.cs.lti.utils.Functional;
import gnu.trove.map.TIntObjectMap;
import org.apache.commons.lang3.mutable.MutableInt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.BiFunction;

/**
 * Created with IntelliJ IDEA.
 * Date: 8/20/15
 * Time: 10:41 PM
 *
 * @author Zhengzhong Liu
 */
public class ViterbiDecoder extends SequenceDecoder {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private SequenceSolution solution;

    private GraphFeatureVector bestVector;

    private int kBest;

    private CubicLagrangian dummyLagrangian = new DummyCubicLagrangian();

    private ArrayListMultimap<Integer, Integer> constraints;

    public ViterbiDecoder(FeatureAlphabet featureAlphabet, ClassAlphabet classAlphabet) {
        this(featureAlphabet, classAlphabet, false, ArrayListMultimap.create());
    }

    public ViterbiDecoder(FeatureAlphabet featureAlphabet, ClassAlphabet classAlphabet,
                          ArrayListMultimap<Integer, Integer> constraints) {
        this(featureAlphabet, classAlphabet, false, constraints);
    }

    public ViterbiDecoder(FeatureAlphabet featureAlphabet, ClassAlphabet classAlphabet, boolean binaryFeature,
                          ArrayListMultimap<Integer, Integer> constraints) {
        this(featureAlphabet, classAlphabet, binaryFeature, 1, constraints);
    }

    public ViterbiDecoder(FeatureAlphabet featureAlphabet, ClassAlphabet classAlphabet, boolean binaryFeature,
                          int kBest, ArrayListMultimap<Integer, Integer> constraints) {
        super(featureAlphabet, classAlphabet, binaryFeature);
        this.kBest = kBest;
        this.constraints = constraints;
    }

    private FeatureVector newFeatureVector() {
        return new RealValueHashFeatureVector(featureAlphabet);
    }

    private GraphFeatureVector newGraphFeatureVector() {
        return new GraphFeatureVector(classAlphabet, featureAlphabet);
    }

    @Override
    public void decode(ChainFeatureExtractor extractor, GraphWeightVector weightVector, int sequenceLength,
                       CubicLagrangian u, CubicLagrangian v, TIntObjectMap<FeatureVector[]> featureCache,
                       boolean useAverage) {
        solution = new SequenceSolution(classAlphabet, sequenceLength, kBest);

        // Dot product function on the node (i.e. only take features depend on current class)
        BiFunction<FeatureVector, Integer, Double> nodeDotProd = useAverage ?
                weightVector::dotProdAver :
                weightVector::dotProd;

        // Dot product function on the edge (i.e. take features depend on two classes)
        Functional.TriFunction<FeatureVector, Integer, Integer, Double> edgeDotProd = useAverage ?
                weightVector::dotProdAver :
                weightVector::dotProd;

        final GraphFeatureVector[] currentFeatureVectors = new GraphFeatureVector[classAlphabet.size()];
        final GraphFeatureVector[] previousColFeatureVectors = new GraphFeatureVector[classAlphabet.size()];

        for (int i = 0; i < currentFeatureVectors.length; i++) {
            currentFeatureVectors[i] = newGraphFeatureVector();
        }

        for (; !solution.finished(); solution.advance()) {
            int sequenceIndex = solution.getCurrentPosition();
            if (sequenceIndex < 0) {
                continue;
            }

            // Feature vector to be extracted or loaded from cache.
            FeatureVector nodeFeature;
            FeatureVector edgeFeature;

            FeatureVector[] allBaseFeatures = null;
            if (featureCache != null) {
                allBaseFeatures = featureCache.get(sequenceIndex);
            }

            // The extraction part is not parallelized.
            if (allBaseFeatures == null) {
                nodeFeature = newFeatureVector();
                edgeFeature = newFeatureVector();
                extractor.extract(sequenceIndex, nodeFeature, edgeFeature);
                if (featureCache != null) {
                    featureCache.put(sequenceIndex, new FeatureVector[]{nodeFeature, edgeFeature});
                }
            } else {
                nodeFeature = allBaseFeatures[0];
                edgeFeature = allBaseFeatures[1];
            }

            // Before move on to calculate the features of current index, copy the vector of the previous column,
            // which are all candidates for the final feature of the prediction.
            System.arraycopy(currentFeatureVectors, 0, previousColFeatureVectors, 0, previousColFeatureVectors.length);

            for (int i = 0; i < currentFeatureVectors.length; i++) {
                currentFeatureVectors[i] = newGraphFeatureVector();
            }

            // Fill up lattice score for each of class in the current column.
            // TODO currently this creates a IllegalThreadState, some threads didn't exits.
            solution.getCurrentPossibleClassIndices().parallel().forEach(classIndex -> {
                double lagrangianPenalty = solution.isRightLimit() ? 0 :
                        u.getSumOverJVariable(sequenceIndex, classIndex)
                                - getConstraintSumI(constraints, u, sequenceIndex, classIndex)
                                + v.getSumOverIVariable(sequenceIndex, classIndex)
                                - getConstraintSumJ(constraints, v, sequenceIndex, classIndex);

                double newNodeScore = nodeDotProd.apply(nodeFeature, classIndex) + lagrangianPenalty;

                MutableInt argmaxPreviousState = new MutableInt(-1);

                // Check which previous state gives the best score.
                solution.getPreviousPossibleClassIndices().forEach(prevState -> {
                    for (SequenceSolution.LatticeCell previousBest : solution.getPreviousBests(prevState)) {
                        double newEdgeScore = edgeDotProd.apply(edgeFeature, classIndex, prevState);

                        int addResult = solution.scoreNewEdge(classIndex, previousBest, newEdgeScore, newNodeScore);
                        if (addResult == 1) {
                            // The new score is the best.
                            argmaxPreviousState.setValue(prevState);
                        } else if (addResult == -1) {
                            // The new score is worse than the worst, i.e. rejected by the heap. We don't
                            // need to check any scores that is worse than this.
                            break;
                        }
                    }
                });

                // Add feature vector from previous state, also added new features of current state.
                int bestPrev = argmaxPreviousState.getValue();

                // Adding features for the new cell.
                currentFeatureVectors[classIndex].extend(nodeFeature, classIndex);
                // Taking features from previous best cell.
                currentFeatureVectors[classIndex].extend(previousColFeatureVectors[bestPrev]);
                // Adding features for the edge.
                currentFeatureVectors[classIndex].extend(edgeFeature, classIndex, bestPrev);
            });
        }
        solution.backTrace();
        bestVector = currentFeatureVectors[classAlphabet.getOutsideClassIndex()];
    }

    private double getConstraintSumJ(ArrayListMultimap<Integer, Integer> allowedCorefs, CubicLagrangian l,
                                     int decodingIndex, int decodingType) {
        double constraintSum = 0;

        for (int allowedType : allowedCorefs.get(decodingType)) {
            constraintSum += l.getSumOverJVariable(decodingIndex, allowedType);
        }
        return constraintSum;
    }

    private double getConstraintSumI(ArrayListMultimap<Integer, Integer> allowedCorefs, CubicLagrangian l,
                                     int decodingIndex, int decodingType) {
        double constraintSum = 0;
        for (int allowedType : allowedCorefs.get(decodingType)) {
            constraintSum += l.getSumOverIVariable(decodingIndex, allowedType);
        }
        return constraintSum;
    }

    @Override
    public SequenceSolution getDecodedPrediction() {
        return solution;
    }

    @Override
    public GraphFeatureVector getBestDecodingFeatures() {
        return bestVector;
    }

    @Override
    public GraphFeatureVector getSolutionFeatures(ChainFeatureExtractor extractor, SequenceSolution solution) {
        GraphFeatureVector fv = newGraphFeatureVector();

        for (int solutionIndex = 0; solutionIndex <= solution.getSequenceLength(); solutionIndex++) {
            FeatureVector nodeFeatures = newFeatureVector();
            FeatureVector edgeFeatures = newFeatureVector();

            extractor.extract(solutionIndex, nodeFeatures, edgeFeatures);

            int classIndex = solution.getClassAt(solutionIndex);

            fv.extend(nodeFeatures, classIndex);
        }
        return fv;
    }
}
