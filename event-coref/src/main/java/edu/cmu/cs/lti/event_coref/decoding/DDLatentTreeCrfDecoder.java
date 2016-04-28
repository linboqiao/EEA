package edu.cmu.cs.lti.event_coref.decoding;

import com.google.common.collect.ArrayListMultimap;
import edu.cmu.cs.lti.learning.ChainFeatureExtractor;
import edu.cmu.cs.lti.learning.model.graph.MentionGraph;
import edu.cmu.cs.lti.learning.model.graph.MentionSubGraph;
import edu.cmu.cs.lti.learning.decoding.ViterbiDecoder;
import edu.cmu.cs.lti.learning.feature.mention_pair.extractor.PairFeatureExtractor;
import edu.cmu.cs.lti.learning.model.*;
import edu.cmu.cs.lti.learning.utils.CubicLagrangian;
import edu.cmu.cs.lti.utils.DebugUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created with IntelliJ IDEA.
 * Date: 12/22/15
 * Time: 2:56 PM
 *
 * @author Zhengzhong Liu
 */
public class DDLatentTreeCrfDecoder {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private BestFirstLatentTreeDecoder latentTreeDecoder;
    private ViterbiDecoder viterbiDecoder;

    private CubicLagrangian u;

    private CubicLagrangian v;

    // TODO adjust using Koo's heuristic.
    private double stepSize = 0.01;

    private int maxIteration = 50;

    private int startWithOptimal = 0;

    private int optimalCounter = 0;

    private int decodingCounter = 0;

    private ClassAlphabet typeClassAlphabet;

    private ArrayListMultimap<Integer, Integer> allowedCorefs;

//    private Set<Set<Integer>> equivalentSets;

    public DDLatentTreeCrfDecoder(FeatureAlphabet crfFeatureAlphabet, ClassAlphabet crfClassAlphabet,
                                  ArrayListMultimap<Integer, Integer> allowedCorefs) {
        latentTreeDecoder = new BestFirstLatentTreeDecoder();
        // TODO confirm allowedCorefs conforms exchangibility
        viterbiDecoder = new ViterbiDecoder(crfFeatureAlphabet, crfClassAlphabet, allowedCorefs);
        this.typeClassAlphabet = crfClassAlphabet;

        this.allowedCorefs = allowedCorefs;
    }

    public Pair<SequenceSolution, MentionSubGraph> decode(ChainFeatureExtractor mentionFeatureExtractor,
                                                          GraphWeightVector mentionWeights,
                                                          MentionGraph mentionGraph,
                                                          List<MentionCandidate> allCandidates,
                                                          PairFeatureExtractor corefFeatureExtractor,
                                                          GraphWeightVector corefWeights,
                                                          boolean useAverage) {
        MentionSubGraph subGraph = null;
        SequenceSolution typeSolution = null;
        decodingCounter++;

        int sequenceLength = allCandidates.size();

//        logger.info("Decoding a sentence of length " + sequenceLength);

        u = new CubicLagrangian(sequenceLength, sequenceLength, typeClassAlphabet.size());
        v = new CubicLagrangian(sequenceLength, sequenceLength, typeClassAlphabet.size());

        int iter;
        for (iter = 0; iter < maxIteration; iter++) {
            // Compute sum over each axis before pass in.
            u.computeSum();
            v.computeSum();

            viterbiDecoder.decode(mentionFeatureExtractor, mentionWeights, sequenceLength, u, v, useAverage);

            // NOTE: we annotate predicted type into JCas here.
            annotatePredictedTypes(viterbiDecoder.getDecodedPrediction(), allCandidates);

            subGraph = latentTreeDecoder.decode(mentionGraph, allCandidates, corefWeights, corefFeatureExtractor, u, v);
            typeSolution = viterbiDecoder.getDecodedPrediction();

//            logger.debug(typeSolution.showBestBackPointerMap());

            logger.debug(typeSolution.toString());
            logger.debug(subGraph.toString());

            logger.debug("Iteration " + iter);
            if (matches(typeSolution, subGraph)) {
                logger.debug("Find non conflicting solution.");
                if (iter == 0) {
                    startWithOptimal++;
                }
                optimalCounter++;
                break;
            }
//            DebugUtils.pause(logger);
        }

        if (iter == 0) {
            logger.debug("Start as optimal.");
        } else {
            if (iter == maxIteration) {
                logger.debug("Exit after max iteration.");
            }
            DebugUtils.pause(logger);
        }

        return Pair.of(typeSolution, subGraph);
    }

    private void annotatePredictedTypes(SequenceSolution prediction, Collection<MentionCandidate> predictedMentions) {
        int index = 0;
        for (MentionCandidate mention : predictedMentions) {
            String className = typeClassAlphabet.getClassName(prediction.getClassAt(index));
            mention.setMentionType(className);
            index++;
        }
    }

    public GraphFeatureVector getBestDecodingFeatures() {
        return viterbiDecoder.getBestDecodingFeatures();
    }

    private void updateLagrangian(CubicLagrangian l, int i, int j, int t, double updateValue) {
        // TODO We need the projected version
        // TODO  Lagrangian update is always negative, but we need to make sure this part is correct as well.
        l.projectedUpdate(i, j, t, -updateValue);
    }

    private boolean matches(SequenceSolution typeSolution, MentionSubGraph corefTree) {
        corefTree.resolveCoreference();
        List<Pair<Integer, String>>[] corefChains = corefTree.getCorefChains();
        int[][] corefAdjacentList = chainAsAdjacentList(corefChains, typeSolution.getSequenceLength());

        boolean achieveOptimal = true;

        int sequenceLength = typeSolution.getSequenceLength();

        for (int t = 0; t < typeClassAlphabet.size(); t++) {
            for (int i = 0; i < sequenceLength - 1; i++) {
                int type_i = typeSolution.getClassAt(i);
                int y_it = type_i == t ? 1 : 0;
//                int y_it_sum = 0;
                for (int j = i + 1; j < sequenceLength; j++) {
                    int y_jt = 0;
                    int type_j = typeSolution.getClassAt(j);
                    y_jt += type_j == t ? 1 : 0;

                    int z_ij = corefAdjacentList[i][j];

                    // This also means :
                    // y_it = 1, y_jt = 0 or reverse, and z_ij = 1
                    // which is :
                    // Type(i) != Type(j) but IJ is coref.
                    //
                    // when y_it and y_jt are the same it won't fire in any case.
                    // when z_ij = 0, this will not fire as well.

                    if (z_ij == 1) {
//                    if (y_it_sum - y_jt > 1 - z_ij || y_jt - y_it_sum > 1 - z_ij) {
//                        logger.debug("Checking type is " + typeClassAlphabet.getClassName(t) + " " + t);
                        if (y_it == 1) {
                            int y_jt_sum = 0;
                            for (int y_jt_allowed : allowedCorefs.get(type_i)) {
                                y_jt_sum += typeSolution.getClassAt(j) == y_jt_allowed ? 1 : 0;
                            }
                            if (y_jt_sum == 0) {
                                // if former, y_it - y_jt_sum = 1, hence u will decrease
                                updateLagrangian(u, i, j, t, stepSize * (y_it - y_jt));
                                logger.debug(String.format("Updated U_%d_%d_%d to %.2f",
                                        i, j, t, u.getVariable(i, j, t)));
                                logger.debug(
                                        String.format(
                                                "Not optimal because : y_%d_%s is %d, y_%d_%s_sum is %d, z_%d_%d is %d."
                                                , i, t, y_it, j, t, y_jt_sum, i, j, z_ij)
                                );
                                achieveOptimal = false;
                            }
                        } else if (y_jt == 1) {
                            int y_it_sum = 0;
                            for (int y_it_allowed : allowedCorefs.get(type_j)) {
                                y_it_sum += typeSolution.getClassAt(i) == y_it_allowed ? 1 : 0;
                            }
                            if (y_it_sum == 0) {
                                // if latter, y_jt - y_it_sum = 1, hence v will decrease
                                updateLagrangian(v, i, j, t, stepSize * (y_jt - y_it));
                                logger.debug(String.format("Updated V_%d_%d_%d to %.2f",
                                        i, j, t, v.getVariable(i, j, t)));
                                logger.debug(
                                        String.format(
                                                "Not optimal because : y_%d_%s_sum is %d, y_%d_%s is %d, z_%d_%d is %d."
                                                , i, t, y_it_sum, j, t, y_jt, i, j, z_ij)
                                );
                                achieveOptimal = false;
                            }
                        }
                    }
                }
            }
        }
        return achieveOptimal;
    }

    /**
     * Convert the coreference chain into adjacent list, which also map the graph ids to sequence id.
     *
     * @param corefChains The coreference chains recorded using graph id.
     * @param numNodes    Number of nodes in the sequence.
     * @return A adjacent list represent all coreference, using sequence id.
     */
    private int[][] chainAsAdjacentList(List<Pair<Integer, String>>[] corefChains, int numNodes) {
        int[][] adjacentList = new int[numNodes][numNodes];

        Set<Pair<Integer, Integer>> corefs = new HashSet<>();

        for (List<Pair<Integer, String>> corefChain : corefChains) {
            for (Pair<Integer, String> firstNode : corefChain) {
                int firstNodeId = firstNode.getLeft();
                for (Pair<Integer, String> secondNode : corefChain) {
                    int secondNodeId = secondNode.getLeft();
                    // Root nodes are not considered in coref links.
                    if (firstNodeId > 0 && secondNodeId > 0 && firstNodeId != secondNodeId) {
                        corefs.add(Pair.of(firstNodeId - 1, secondNodeId - 1));
                    }
                }
            }
        }

        for (int from = 0; from < adjacentList.length; from++) {
            for (int to = 0; to < adjacentList.length; to++) {
                if (from != to) {
                    boolean isCoref = corefs.contains(Pair.of(from, to)) || corefs.contains(Pair.of(to, from));
                    adjacentList[from][to] = isCoref ? 1 : 0;
                }
            }
        }
        return adjacentList;
    }

    public int getOptimalCounter() {
        return optimalCounter;
    }

    public int getDecodingCounter() {
        return decodingCounter;
    }

    public int getStartWithOptimal() {
        return startWithOptimal;
    }
}
