package edu.cmu.cs.lti.event_coref.decoding;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import edu.cmu.cs.lti.learning.feature.extractor.SentenceFeatureExtractor;
import edu.cmu.cs.lti.learning.model.*;
import edu.cmu.cs.lti.learning.model.decoding.LabelLinkAgenda;
import edu.cmu.cs.lti.learning.model.decoding.NodeLinkingState;
import edu.cmu.cs.lti.learning.model.decoding.StateDelta;
import edu.cmu.cs.lti.learning.model.graph.LabelledMentionGraphEdge;
import edu.cmu.cs.lti.learning.model.graph.MentionGraph;
import edu.cmu.cs.lti.learning.model.graph.EdgeType;
import edu.cmu.cs.lti.learning.update.DiscriminativeUpdater;
import edu.cmu.cs.lti.script.type.StanfordCorenlpSentence;
import edu.cmu.cs.lti.script.type.StanfordCorenlpToken;
import edu.cmu.cs.lti.uima.util.UimaConvenience;
import edu.cmu.cs.lti.utils.DebugUtils;
import gnu.trove.map.TObjectDoubleMap;
import gnu.trove.map.hash.TObjectDoubleHashMap;
import org.apache.commons.lang3.builder.CompareToBuilder;
import org.apache.commons.lang3.mutable.MutableInt;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.util.*;

import static edu.cmu.cs.lti.learning.model.ModelConstants.COREF_MODEL_NAME;
import static edu.cmu.cs.lti.learning.model.ModelConstants.TYPE_MODEL_NAME;

/**
 * Created with IntelliJ IDEA.
 * Date: 2/28/16
 * Time: 4:22 PM
 *
 * @author Zhengzhong Liu
 */
public class BeamCrfLatentTreeDecoder {
    protected final Logger logger = LoggerFactory.getLogger(getClass());

    private final GraphWeightVector corefWeights;
    private final FeatureAlphabet mentionFeatureAlphabet;
    private final ClassAlphabet mentionTypeClassAlphabet;
    private final WekaModel realisModel;

    private final SentenceFeatureExtractor realisExtractor;
    private final SentenceFeatureExtractor mentionExtractor;

    private final GraphWeightVector mentionWeights;

    private DiscriminativeUpdater updater;
    private final boolean isTraining;

    private TrainingStats corefTrainingStats;
    private TrainingStats typeTrainingStats;

    private int beamSize = 5;

    // A empty feature vector for placeholder, don't use it.
    private final FeatureVector dummyFv;

    private String lossType;

    public BeamCrfLatentTreeDecoder(GraphWeightVector mentionWeights, WekaModel realisModel,
                                    GraphWeightVector corefWeights, SentenceFeatureExtractor realisExtractor,
                                    SentenceFeatureExtractor mentionExtractor, DiscriminativeUpdater updater,
                                    String lossType)
            throws ClassNotFoundException, NoSuchMethodException, InstantiationException, IllegalAccessException,
            InvocationTargetException {
        this(mentionWeights, realisModel, corefWeights, realisExtractor, mentionExtractor,
                lossType, true);
        this.updater = updater;
        corefTrainingStats = new TrainingStats(5, "coref");
        typeTrainingStats = new TrainingStats(5, "type");

        logger.info("Starting the Beam Decoder with joint training.");
    }

    public BeamCrfLatentTreeDecoder(GraphWeightVector mentionWeights, WekaModel realisModel,
                                    GraphWeightVector corefWeights, SentenceFeatureExtractor realisExtractor,
                                    SentenceFeatureExtractor mentionExtractor)
            throws ClassNotFoundException, NoSuchMethodException, InstantiationException, IllegalAccessException,
            InvocationTargetException {
        this(mentionWeights, realisModel, corefWeights, realisExtractor, mentionExtractor,
                null, false);
        logger.info("Starting the Beam Decoder with joint testing.");
    }

    private BeamCrfLatentTreeDecoder(GraphWeightVector mentionWeights, WekaModel realisModel,
                                     GraphWeightVector corefWeights, SentenceFeatureExtractor realisExtractor,
                                     SentenceFeatureExtractor mentionExtractor, String lossType, boolean isTraining)
            throws ClassNotFoundException, NoSuchMethodException, InstantiationException, IllegalAccessException,
            InvocationTargetException {
        this.lossType = lossType;
        mentionTypeClassAlphabet = mentionWeights.getClassAlphabet();
        mentionFeatureAlphabet = mentionWeights.getFeatureAlphabet();
        dummyFv = new RealValueHashFeatureVector(corefWeights.getFeatureAlphabet());

        this.corefWeights = corefWeights;
        this.mentionWeights = mentionWeights;
        this.realisModel = realisModel;
        this.realisExtractor = realisExtractor;
        this.mentionExtractor = mentionExtractor;

        this.isTraining = isTraining;
    }

    /**
     * Deocde method for testing purpose.
     *
     * @param aJCas                The JCas container.
     * @param mentionGraph         The mention graph.
     * @param predictionCandidates Candidates to be predict.
     * @param useAverage           Whether to run with average perceptron.
     * @return The final decoding state.
     */
    public NodeLinkingState decode(JCas aJCas, MentionGraph mentionGraph, List<MentionCandidate> predictionCandidates,
                                   boolean useAverage) {
        return decode(aJCas, mentionGraph, predictionCandidates, new ArrayList<>(), useAverage);
    }

    /**
     * Decode method for training purpose.
     *
     * @param aJCas                The JCas container.
     * @param mentionGraph         The mention graph.
     * @param predictionCandidates Candidates to be predict.
     * @param goldCandidates       Candidates containing gold standard annotation.
     * @param useAverage           Whether to run with average perceptron.
     * @return The final decoding state.
     */
    public NodeLinkingState decode(JCas aJCas, MentionGraph mentionGraph, List<MentionCandidate> predictionCandidates,
                                   List<MentionCandidate> goldCandidates, boolean useAverage) {
        List<StanfordCorenlpSentence> allSentences = new ArrayList<>(JCasUtil.select(aJCas,
                StanfordCorenlpSentence.class));

        logger.debug(String.format("Processing document %s, Sentence size : %d, Candidate size : %d, Graph nodes : %d",
                UimaConvenience.getShortDocumentName(aJCas), allSentences.size(), predictionCandidates.size(),
                mentionGraph.numNodes()));

        // Prepare a gold agenda and a decoding agenda.
        LabelLinkAgenda goldAgenda = new LabelLinkAgenda(beamSize, goldCandidates, mentionGraph);
        LabelLinkAgenda decodingAgenda = new LabelLinkAgenda(beamSize, predictionCandidates, mentionGraph);

        initWorkspace(aJCas);

        // Current mention node index, starts from 1, after the root node 0.
        MutableInt curr = new MutableInt(1);
        Set<Integer> prunedNodes = new HashSet<>();

        for (int sentIndex = 0; sentIndex < allSentences.size(); sentIndex++) {
            StanfordCorenlpSentence sentence = allSentences.get(sentIndex);
            resetWorkspace(aJCas, sentence);

            List<StanfordCorenlpToken> sentenceTokens = JCasUtil.selectCovered(StanfordCorenlpToken.class, sentence);

            // Move over the tokens.
            for (int sentTokenIndex = 0; sentTokenIndex < sentenceTokens.size(); sentTokenIndex++) {
                int currentMentionId = MentionGraph.getCandidateIndex(curr.getValue());

//                logger.debug(String.format("Decoding sentence %d, token %d, mention node index %d, token is %s",
//                        sentIndex, sentTokenIndex, curr.getValue(),
//                        predictionCandidates.get(currentMentionId).getHeadWord().getCoveredText()));

                // Prepare expansion, initialize all the deltas.
                decodingAgenda.prepareExpand();
                if (isTraining) {
                    goldAgenda.prepareExpand();
                }

                // Extract features for the token.
                FeatureVector nodeFeature = new RealValueHashFeatureVector(mentionFeatureAlphabet);
                Table<Integer, Integer, FeatureVector> edgeFeatures = HashBasedTable.create();
                mentionExtractor.extract(sentTokenIndex, nodeFeature, edgeFeatures);

                // Take the current candidate.
                final MentionCandidate predictionCandidate = predictionCandidates.get(currentMentionId);

                // Predicate the realis for each word, the features are not related to mention types.
                String realis = getRealis((StanfordCorenlpToken) predictionCandidate.getHeadWord());

                Queue<Pair<Integer, Double>> sortedClassScores = scoreMentionLocally(nodeFeature, useAverage);

                boolean pruneSureNone = localPrune(sortedClassScores);

                if (pruneSureNone) {
                    sortedClassScores.clear();
                    sortedClassScores.add(Pair.of(mentionTypeClassAlphabet.getNoneOfTheAboveClassIndex(), 0.0));
                    prunedNodes.add(curr.getValue());
                }

                // Only take the top k classes to expand.
                int count = 0;
                while (!sortedClassScores.isEmpty()) {
                    Pair<Integer, Double> classScore = sortedClassScores.poll();
                    int classIndex = classScore.getLeft();
                    double nodeTypeScore = classScore.getRight();
                    final MultiNodeKey nodeKeys = setUpCandidate(predictionCandidate, classIndex, realis);

                    GraphFeatureVector newDecodingMentionFeatures = new GraphFeatureVector(mentionTypeClassAlphabet,
                            mentionFeatureAlphabet);
                    newDecodingMentionFeatures.extend(nodeFeature, classIndex);

                    for (NodeLinkingState nodeLinkingState : decodingAgenda.getBeamStates()) {
                        // The previous type class is defined on sentence base, so we will ad a special sentence
                        // boundary class before each one.
                        int prevClassIndex = sentTokenIndex == 0 ?
                                mentionTypeClassAlphabet.getOutsideClassIndex() :
                                mentionTypeClassAlphabet.getClassIndex(nodeLinkingState.getCombinedLastNodeType());

//                        logger.info(String.format("Decoding class %d for system.", classIndex));
//                        logger.info(nodeLinkingState.showNodes());

                        // Compute the first order CRF edge score.
                        FeatureVector edgeFeature = edgeFeatures.get(prevClassIndex, classIndex);

                        double newTypeEdgeScore = useAverage ?
                                mentionWeights.dotProdAver(edgeFeature, classIndex, prevClassIndex) :
                                mentionWeights.dotProd(edgeFeature, classIndex, prevClassIndex);

                        newDecodingMentionFeatures.extend(edgeFeature, classIndex, prevClassIndex);

                        linkToAntecedent(mentionGraph, decodingAgenda, nodeLinkingState, predictionCandidates,
                                curr.getValue(), newDecodingMentionFeatures, nodeKeys,
                                nodeTypeScore + newTypeEdgeScore, prunedNodes);
                    }

                    count++;
                    if (count == 5) {
                        break; // Only expand top 5 classes.
                    }
                }

//                logger.info("Update states for decoding.");
                decodingAgenda.updateStates();

                if (isTraining) {
                    // Note that most gold candidate are None type and None realis since they are not mentions.
                    final MentionCandidate goldCandidate = goldCandidates.get(currentMentionId);
                    final MultiNodeKey goldResults = goldCandidate.asKey();
                    GraphFeatureVector goldMentionFeature = getGoldMentionFeatures(nodeFeature, edgeFeatures,
                            goldCandidates, currentMentionId);

                    // Expanding the states for gold agenda.
                    for (NodeLinkingState nodeLinkingState : goldAgenda.getBeamStates()) {
//                    logger.info(String.format("Decoding antecedent at node %d for gold.", curr.getValue()));
                        linkToCorrectAntecedent(mentionGraph, goldAgenda, nodeLinkingState, curr
                                .getValue(), goldMentionFeature, goldResults, 0);
                    }

//                    logger.info("Updating states for gold.");
                    goldAgenda.updateStates();

                    updater.recordLaSOUpdate(decodingAgenda, goldAgenda);
                }

                if (!pruneSureNone) {
                    logger.debug("Showing decoding agenda");
                    logger.debug(decodingAgenda.toString());
                    DebugUtils.pause(logger);
                }

                curr.increment();
            } // Finish iterate tokens in a sentence.
        }// Finish iterate sentences.

        if (isTraining) {
            logger.debug("Check for final updates");

            // The final check matches the first item in the agendas, while the searching check only ensure containment.
            updater.recordFinalUpdate(decodingAgenda, goldAgenda);

            // Update based on cumulative errors.
            logger.debug("Applying updates to " + TYPE_MODEL_NAME);
            TObjectDoubleMap<String> losses = updater.update();
            typeTrainingStats.addLoss(logger, losses.get(TYPE_MODEL_NAME) / mentionGraph.numNodes());
            logger.debug("Applying updates to " + COREF_MODEL_NAME);
            corefTrainingStats.addLoss(logger, losses.get(COREF_MODEL_NAME) / mentionGraph.numNodes());
        }

        return decodingAgenda.getBeamStates().get(0);
    }

    private void initWorkspace(JCas aJCas) {
        realisExtractor.initWorkspace(aJCas);
        mentionExtractor.initWorkspace(aJCas);
    }

    private void resetWorkspace(JCas aJCas, Annotation sentence) {
        realisExtractor.resetWorkspace(aJCas, sentence);
        mentionExtractor.resetWorkspace(aJCas, sentence);
    }

    private Queue<Pair<Integer, Double>> scoreMentionLocally(FeatureVector nodeFeature, boolean useAverage) {
        Queue<Pair<Integer, Double>> sortedClassScores = new PriorityQueue<>(
                (o1, o2) ->
                        new CompareToBuilder().append(o2.getLeft(), o1.getLeft()).
                                append(o2.getRight(), o1.getRight()).toComparison()
        );

        // Go over possible crf links.
        mentionTypeClassAlphabet.getNormalClassesRange().forEach(classIndex -> {
            double nodeTypeScore = useAverage ? mentionWeights.dotProdAver(nodeFeature, classIndex) :
                    mentionWeights.dotProd(nodeFeature, classIndex);
            sortedClassScores.add(Pair.of(classIndex, nodeTypeScore));
        });

        return sortedClassScores;
    }

    private GraphFeatureVector getGoldMentionFeatures(FeatureVector nodeFeature,
                                                      Table<Integer, Integer, FeatureVector> edgeFeatures,
                                                      List<MentionCandidate> goldCandidates, int currentIndex) {
        GraphFeatureVector newGoldMentionFeatures = new GraphFeatureVector(mentionTypeClassAlphabet,
                mentionFeatureAlphabet);
        int currentClass = mentionTypeClassAlphabet.getClassIndex(goldCandidates.get(currentIndex).getMentionType());
        int previousClass = currentIndex == 0 ? mentionTypeClassAlphabet.getOutsideClassIndex() :
                mentionTypeClassAlphabet.getClassIndex(goldCandidates.get(currentIndex - 1).getMentionType());
        newGoldMentionFeatures.extend(nodeFeature, currentClass);
        FeatureVector edgeFeature = edgeFeatures.get(previousClass, currentClass);
        newGoldMentionFeatures.extend(edgeFeature, currentClass, previousClass);

        return newGoldMentionFeatures;
    }

    private MultiNodeKey setUpCandidate(MentionCandidate currPredictionCandidate, int typeIndex, String realis) {
        currPredictionCandidate.setMentionType(mentionTypeClassAlphabet.getClassName(typeIndex));
        if (currPredictionCandidate.getMentionType().equals(ClassAlphabet.noneOfTheAboveClass)) {
            // If the type of the candidate is none, then set realis to a special value as well.
            currPredictionCandidate.setRealis(ClassAlphabet.noneOfTheAboveClass);
        } else {
            // Otherwise, use the predicted realis.
            currPredictionCandidate.setRealis(realis);
        }
        //After setting type and realis, we can make it a key.
        return currPredictionCandidate.asKey();
    }

    private boolean localPrune(Queue<Pair<Integer, Double>> sortedClassScores) {
        double mean = 0;
        double size = sortedClassScores.size();

        for (Pair<Integer, Double> classScore : sortedClassScores) {
            mean += classScore.getRight();
        }

        double delta = 0;
        for (Pair<Integer, Double> classScore : sortedClassScores) {
            delta += Math.pow(classScore.getRight() - mean, 2);
        }

        delta /= size;

        delta = Math.sqrt(delta);

        Pair<Integer, Double> best = sortedClassScores.poll();
        if (best.getLeft() == mentionTypeClassAlphabet.getNoneOfTheAboveClassIndex()) {
            Pair<Integer, Double> secondBest = sortedClassScores.peek();
            double diffByDelta = (best.getRight() - secondBest.getRight()) / delta;

            // The larger diff, the larger the gap between first and second.
            return diffByDelta > 1;
        }

        // Put the best item back.
        sortedClassScores.add(best);

        return false;
    }

    private void linkToAntecedent(MentionGraph mentionGraph, LabelLinkAgenda decodingAgenda,
                                  NodeLinkingState nodeLinkingState, List<MentionCandidate> candidates,
                                  int currGraphNodeIndex, GraphFeatureVector newMentionFeatures,
                                  MultiNodeKey currNode, double mentionScore, Set<Integer> prunedNodes) {
//        logger.debug("Linking to antecedent, number of candidates: " + (currGraphNodeIndex - prunedNodes.size()));

        // Create a new decoding decision, representing the new type assignment, and new link assignment.
        StateDelta decision = new StateDelta(nodeLinkingState);
//        StateDelta decision = decodingAgenda.expand(nodeLinkingState);
        decision.addNode(currNode, newMentionFeatures, mentionScore);

        // Handling the root node case.
        if (currNode.size() == 1) {
            if (currNode.isRoot()) {
                // If this is non-type, we link it directly to root.
                NodeKey govKey = MultiNodeKey.rootKey().takeFirst();
                for (NodeKey nodeKey : currNode) {
                    decision.addLink(EdgeType.Root, govKey, nodeKey, 0, dummyFv);
                }
                return;
            }
        }

        // For each label of the current decoding.
        for (NodeKey currNodeKey : currNode) {
            double bestLinkScore = Double.NEGATIVE_INFINITY;

            NodeKey bestAntecedentNode = null;
            EdgeType bestEdgeType = null;
            FeatureVector bestNewCorefFeature = null;

            for (int ant = 0; ant < currGraphNodeIndex; ant++) {
                if (prunedNodes.contains(ant)) {
                    continue;
                }

                // Access the antecedent node.
                MultiNodeKey antNodeKeys = nodeLinkingState.getNode(ant);

                for (NodeKey antNodeKey : antNodeKeys) {
                    LabelledMentionGraphEdge mentionGraphEdge = mentionGraph
                            .getMentionGraphEdge(currGraphNodeIndex, ant)
                            .getLabelledEdge(candidates, antNodeKey, currNodeKey);
                    Pair<EdgeType, Double> bestLabelScore = mentionGraphEdge
                            .getBestLabelScore(corefWeights);

                    double linkScore = bestLabelScore.getRight();
                    EdgeType edgeType = bestLabelScore.getLeft();

                    if (linkScore > bestLinkScore) {
                        bestLinkScore = linkScore;
                        bestAntecedentNode = antNodeKey;
                        bestEdgeType = edgeType;
                        bestNewCorefFeature = mentionGraphEdge.getFeatureVector();
                    }
                }
            }

            decision.addLink(bestEdgeType, bestAntecedentNode, currNodeKey, bestLinkScore, bestNewCorefFeature);
        }

       decodingAgenda.expand(decision);
    }

    private void linkToCorrectAntecedent(MentionGraph mentionGraph, LabelLinkAgenda goldAgenda,
                                         NodeLinkingState nodeLinkingState, int currGraphNodeIndex,
                                         GraphFeatureVector newMentionFeatures, MultiNodeKey currNode,
                                         double mentionScore) {
        StateDelta decision = new StateDelta(nodeLinkingState);
        decision.addNode(currNode, newMentionFeatures, mentionScore);

        for (NodeKey currNodeKey : currNode) {
            double bestLinkScore = Double.NEGATIVE_INFINITY;
            EdgeType bestEdgeType = null;
            FeatureVector bestFeature = null;
            NodeKey bestGovKey = null;

            for (int ant = 0; ant < currGraphNodeIndex; ant++) {
                Table<NodeKey, NodeKey, LabelledMentionGraphEdge> realGraphEdges = mentionGraph
                        .getMentionGraphEdge(currGraphNodeIndex, ant).getRealLabelledEdges();
                MultiNodeKey antNodeKeys = nodeLinkingState.getNode(ant);

                for (NodeKey antNodeKey : antNodeKeys) {
                    LabelledMentionGraphEdge realGraphEdge = realGraphEdges.get(antNodeKey, currNodeKey);
                    Pair<EdgeType, Double> correctLabelScore = realGraphEdge
                            .getCorrectLabelScore(corefWeights);
                    double linkScore = correctLabelScore.getRight();

                    if (linkScore > bestLinkScore) {
                        bestLinkScore = linkScore;
                        bestEdgeType = correctLabelScore.getLeft();
                        bestFeature = realGraphEdge.getFeatureVector();
                        bestGovKey = realGraphEdge.getGovKey();
                    }
                }
            }
            decision.addLink(bestEdgeType, bestGovKey, currNodeKey, bestLinkScore, bestFeature);
        }

        goldAgenda.expand(decision);
    }

    private String getRealis(StanfordCorenlpToken headToken) {
        TObjectDoubleMap<String> rawFeatures = new TObjectDoubleHashMap<>();

        FeatureVector mentionFeatures = new RealValueHashFeatureVector(realisModel.getAlphabet());
        int head = realisExtractor.getElementIndex(headToken);
        realisExtractor.extract(head, mentionFeatures);

        for (FeatureVector.FeatureIterator iter = mentionFeatures.featureIterator(); iter.hasNext(); ) {
            iter.next();
            rawFeatures.put(realisModel.getAlphabet().getFeatureNames(iter.featureIndex())[0], iter.featureValue());
        }

        try {
            Pair<Double, String> prediction = realisModel.classify(rawFeatures);
            return prediction.getRight();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return ClassAlphabet.noneOfTheAboveClass;
    }
}
