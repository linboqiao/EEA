package edu.cmu.cs.lti.event_coref.decoding;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import edu.cmu.cs.lti.learning.feature.extractor.SentenceFeatureExtractor;
import edu.cmu.cs.lti.learning.model.*;
import edu.cmu.cs.lti.learning.model.decoding.JointLabelLinkAgenda;
import edu.cmu.cs.lti.learning.model.decoding.LabelLinkAgenda;
import edu.cmu.cs.lti.learning.model.decoding.NodeLinkingState;
import edu.cmu.cs.lti.learning.model.decoding.StateDelta;
import edu.cmu.cs.lti.learning.model.graph.EdgeType;
import edu.cmu.cs.lti.learning.model.graph.LabelledMentionGraphEdge;
import edu.cmu.cs.lti.learning.model.graph.MentionGraph;
import edu.cmu.cs.lti.learning.update.DiscriminativeUpdater;
import edu.cmu.cs.lti.script.type.StanfordCorenlpSentence;
import edu.cmu.cs.lti.script.type.StanfordCorenlpToken;
import edu.cmu.cs.lti.utils.CollectionUtils;
import gnu.trove.map.TObjectDoubleMap;
import gnu.trove.map.hash.TObjectDoubleHashMap;
import org.apache.commons.lang3.builder.CompareToBuilder;
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

    private final int beamSize;

    // A empty feature vector for placeholder, don't use it.
    private final FeatureVector dummyFv;

    private String lossType;
    private final boolean useLaSo;

    private boolean updateRealMentions;

    public BeamCrfLatentTreeDecoder(GraphWeightVector mentionWeights, WekaModel realisModel,
                                    GraphWeightVector corefWeights, SentenceFeatureExtractor realisExtractor,
                                    SentenceFeatureExtractor mentionExtractor, DiscriminativeUpdater updater,
                                    String lossType, int beamSize, boolean useLaSo)
            throws ClassNotFoundException, NoSuchMethodException, InstantiationException, IllegalAccessException,
            InvocationTargetException {
        this(mentionWeights, realisModel, corefWeights, realisExtractor, mentionExtractor,
                lossType, beamSize, useLaSo, true);
        this.updater = updater;
        corefTrainingStats = new TrainingStats(5, "coref");
        typeTrainingStats = new TrainingStats(5, "type");

        logger.info("Starting the Beam Decoder with joint training.");
    }

    public BeamCrfLatentTreeDecoder(GraphWeightVector mentionWeights, WekaModel realisModel,
                                    GraphWeightVector corefWeights, SentenceFeatureExtractor realisExtractor,
                                    SentenceFeatureExtractor mentionExtractor, int beamSize, boolean useLaSo)
            throws ClassNotFoundException, NoSuchMethodException, InstantiationException, IllegalAccessException,
            InvocationTargetException {
        this(mentionWeights, realisModel, corefWeights, realisExtractor, mentionExtractor,
                null, beamSize, useLaSo, false);
        logger.info("Starting the Beam Decoder with joint testing.");
    }

    private BeamCrfLatentTreeDecoder(GraphWeightVector mentionWeights, WekaModel realisModel,
                                     GraphWeightVector corefWeights, SentenceFeatureExtractor realisExtractor,
                                     SentenceFeatureExtractor mentionExtractor, String lossType, int beamSize,
                                     boolean useLaSo, boolean isTraining)
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
        this.beamSize = beamSize;
        this.useLaSo = useLaSo;
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
        return decode(aJCas, mentionGraph, predictionCandidates, new ArrayList<>(), useAverage, false);
    }

    /**
     * Decode for training purpose.
     *
     * @param aJCas
     * @param mentionGraph
     * @param predictionCandidates
     * @param goldCandidates
     * @param skipCoref
     * @return
     */
    public NodeLinkingState decode(JCas aJCas, MentionGraph mentionGraph, List<MentionCandidate> predictionCandidates,
                                   List<MentionCandidate> goldCandidates, boolean skipCoref) {
        return decode(aJCas, mentionGraph, predictionCandidates, goldCandidates, false, skipCoref);
    }

    /**
     * General decoder.
     *
     * @param aJCas                The JCas container.
     * @param mentionGraph         The mention graph.
     * @param predictionCandidates Candidates to be predict.
     * @param goldCandidates       Candidates containing gold standard annotation.
     * @param useAverage           Whether to run with average perceptron.
     * @param skipCoref
     * @return The final decoding state.
     */
    private NodeLinkingState decode(JCas aJCas, MentionGraph mentionGraph, List<MentionCandidate> predictionCandidates,
                                    List<MentionCandidate> goldCandidates, boolean useAverage, boolean skipCoref) {
        List<StanfordCorenlpSentence> allSentences = new ArrayList<>(JCasUtil.select(aJCas,
                StanfordCorenlpSentence.class));
        int numTokens = mentionGraph.numNodes() - 1;

        // Prepare a gold agenda and a decoding agenda.
        JointLabelLinkAgenda goldAgenda = new JointLabelLinkAgenda(beamSize, goldCandidates, mentionGraph);
        JointLabelLinkAgenda decodingAgenda = new JointLabelLinkAgenda(beamSize, predictionCandidates, mentionGraph);

        initWorkspace(aJCas);

        // Current mention node index, starts from 1, after the root node 0.
//        MutableInt curr = new MutableInt(1);
        int curr = 1;
//        Set<Integer> prunedNodes = new HashSet<>();

        int numGoldMentions = 0;

        for (int sentIndex = 0; sentIndex < allSentences.size(); sentIndex++) {
            StanfordCorenlpSentence sentence = allSentences.get(sentIndex);
            resetWorkspace(aJCas, sentence);

            List<StanfordCorenlpToken> sentenceTokens = JCasUtil.selectCovered(StanfordCorenlpToken.class, sentence);

            // Move over the tokens.
            for (int sentTokenIndex = 0; sentTokenIndex < sentenceTokens.size(); sentTokenIndex++) {
                int currentMentionId = MentionGraph.getCandidateIndex(curr);

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
                String predictedRealis = getRealis((StanfordCorenlpToken) predictionCandidate.getHeadWord());

                Queue<Pair<Integer, Double>> sortedClassScores = scoreMentionLocally(nodeFeature, useAverage);

                boolean isNotAGoldMention = false;
                String goldRealis = null;
                if (isTraining) {
                    final MentionCandidate currentGoldCandidate = goldCandidates.get(currentMentionId);
                    if (currentGoldCandidate.getMentionType().equals(ClassAlphabet.noneOfTheAboveClass)) {
                        // We do not need to expand coreference link for tokens that are not gold standard.
                        isNotAGoldMention = true;
                    } else {
                        goldRealis = currentGoldCandidate.getRealis();
                        numGoldMentions++;
                    }
                }

                if (!isTraining) {
                    // At testing time, we prune sure none mentions to speed up.
                    if (localPrune(sortedClassScores)) {
                        sortedClassScores.clear();
                        sortedClassScores.add(Pair.of(mentionTypeClassAlphabet.getNoneOfTheAboveClassIndex(), 0.0));
                        // Keep a record for all nodes pruned, we won't link to them anyway.
//                    prunedNodes.add(curr.getValue());
                    }
                }

                // Only take the top k classes to expand.
                int count = 0;
//                logger.debug("Number of classes scored : " + sortedClassScores.size());
                while (!sortedClassScores.isEmpty()) {
                    Pair<Integer, Double> classScore = sortedClassScores.poll();
                    int classIndex = classScore.getLeft();
                    double nodeTypeScore = classScore.getRight();
                    final MultiNodeKey nodeKeys = setUpCandidate(predictionCandidate, classIndex, predictedRealis);

//                    logger.debug("Type score for " + mentionTypeClassAlphabet.getClassName(classIndex) + " is " +
//                            nodeTypeScore);

                    if (isTraining && !isNotAGoldMention) {
                        //At test time, use the real realis.
                        predictionCandidate.setRealis(goldRealis);
                    }

                    GraphFeatureVector newMentionFeatures = new GraphFeatureVector(mentionTypeClassAlphabet,
                            mentionFeatureAlphabet);

                    newMentionFeatures.extend(nodeFeature, classIndex);

//                    logger.info(String.format("Decoding class %s of score %.2f.", mentionTypeClassAlphabet
// .getClassName(classIndex), nodeTypeScore));

                    for (NodeLinkingState nodeLinkingState : decodingAgenda.getBeamStates()) {
                        FeatureVector globalFeature = new RealValueHashFeatureVector(mentionFeatureAlphabet);
                        // The focus is simply a token index here, which can correctly index actual nodes.
                        mentionExtractor.extractGlobal(currentMentionId, globalFeature,
                                nodeLinkingState.getActualNodeResults());

                        // The previous type class is defined on sentence base, so we will ad a special sentence
                        // boundary class before each one.
                        int prevClassIndex = sentTokenIndex == 0 ?
                                mentionTypeClassAlphabet.getOutsideClassIndex() :
                                mentionTypeClassAlphabet.getClassIndex(nodeLinkingState.getCombinedLastNodeType());

//                        logger.info(nodeLinkingState.showNodes());

                        // Compute the first order CRF edge score.
                        double edgeTypeScore = 0;
                        if (edgeFeatures.contains(prevClassIndex, classIndex)) {
                            FeatureVector edgeFeature = edgeFeatures.get(prevClassIndex, classIndex);
                            edgeTypeScore = useAverage ?
                                    mentionWeights.dotProdAver(edgeFeature, classIndex, prevClassIndex) :
                                    mentionWeights.dotProd(edgeFeature, classIndex, prevClassIndex);
                            newMentionFeatures.extend(edgeFeature, classIndex, prevClassIndex);
                        }

                        double globalScore = useAverage ? mentionWeights.dotProdAver(globalFeature, classIndex) :
                                mentionWeights.dotProd(globalFeature, classIndex);

                        newMentionFeatures.extend(globalFeature, classIndex);

                        expandState(mentionGraph, decodingAgenda, nodeLinkingState, predictionCandidates, curr,
                                newMentionFeatures, nodeKeys, nodeTypeScore + edgeTypeScore + globalScore, skipCoref);
                    }

                    count++;

                    if (count == beamSize) {
                        break; // Only expand top k classes.
                    }
                }

//                logger.info("Update states for decoding.");
//                logger.info(decodingAgenda.toString());
//                DebugUtils.pause();
                decodingAgenda.updateStates();

                if (isTraining) {
                    // Note that most gold candidate are None type and None realis since they are not mentions.
                    final MentionCandidate goldCandidate = goldCandidates.get(currentMentionId);
                    final MultiNodeKey goldResults = goldCandidate.asKey();

                    // Expanding the states for gold agenda.
                    for (NodeLinkingState goldState : goldAgenda.getBeamStates()) {
                        GraphFeatureVector goldMentionFeature = getGoldMentionFeatures(goldState, nodeFeature,
                                edgeFeatures, goldCandidates, currentMentionId);

//                    logger.info(String.format("Decoding antecedent at node %d for gold.", curr.getValue()));

                        expandGoldState(mentionGraph, goldAgenda, goldState, curr, goldMentionFeature, goldResults, 0,
                                skipCoref);
                    }

//                    logger.info("Updating states for gold.");
                    goldAgenda.updateStates();

                    if (useLaSo) {
                        updater.recordLaSOUpdate(decodingAgenda, goldAgenda);
                    }
                }
                curr++;
            } // Finish iterate tokens in a sentence.
        }// Finish iterate sentences.

        if (isTraining) {
            logger.debug("Check for final updates");

            // The final check matches the first item in the agendas, while the searching check only ensure containment.
            updater.recordFinalUpdate(decodingAgenda, goldAgenda);

            // Update based on cumulative errors.
            logger.debug("Applying updates to " + TYPE_MODEL_NAME);
            logger.debug("Applying updates to " + COREF_MODEL_NAME);

            TObjectDoubleMap<String> losses = updater.update();

            typeTrainingStats.addLoss(logger, losses.get(TYPE_MODEL_NAME) / (numTokens));
            corefTrainingStats.addLoss(logger, losses.get(COREF_MODEL_NAME) / (numGoldMentions + 1));
            logger.debug("Type Loss is " + losses.get(TYPE_MODEL_NAME) / (numTokens));
            logger.debug("Coref Loss is " + losses.get(COREF_MODEL_NAME) / (numGoldMentions + 1));
        }

//        noneCounter.addLoss(logger, numNonesPredicted * 1.0 / numTokens);

//        logger.info("Number of decoding links " + decodingAgenda.getBestBeamState().getDecodingTree().getNumEdges());
//        logger.info("Number of gold links " + goldAgenda.getBestBeamState().getDecodingTree().getNumEdges());

        return decodingAgenda.getBestBeamState();
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
                        new CompareToBuilder().append(o2.getRight(), o1.getRight())
                                .append(o2.getLeft(), o1.getLeft()).toComparison()
        );

        // Go over possible crf links.
        mentionTypeClassAlphabet.getNormalClassesRange().forEach(classIndex -> {
            double nodeTypeScore = useAverage ? mentionWeights.dotProdAver(nodeFeature, classIndex) :
                    mentionWeights.dotProd(nodeFeature, classIndex);
            sortedClassScores.add(Pair.of(classIndex, nodeTypeScore));
        });

        return sortedClassScores;
    }

    private GraphFeatureVector getGoldMentionFeatures(NodeLinkingState goldState, FeatureVector nodeFeature,
                                                      Table<Integer, Integer, FeatureVector> edgeFeatures,
                                                      List<MentionCandidate> goldCandidates, int currentIndex) {
        GraphFeatureVector newGoldMentionFeatures = new GraphFeatureVector(mentionTypeClassAlphabet,
                mentionFeatureAlphabet);
        int currentClass = mentionTypeClassAlphabet.getClassIndex(goldCandidates.get(currentIndex).getMentionType());
        int previousClass = currentIndex == 0 ? mentionTypeClassAlphabet.getOutsideClassIndex() :
                mentionTypeClassAlphabet.getClassIndex(goldCandidates.get(currentIndex - 1).getMentionType());
        newGoldMentionFeatures.extend(nodeFeature, currentClass);

        if (edgeFeatures.contains(previousClass, currentClass)) {
            FeatureVector edgeFeature = edgeFeatures.get(previousClass, currentClass);
            newGoldMentionFeatures.extend(edgeFeature, currentClass, previousClass);
        }

        FeatureVector globalFeature = new RealValueHashFeatureVector(mentionFeatureAlphabet);
        mentionExtractor.extractGlobal(currentIndex, globalFeature, goldState.getActualNodeResults());

        newGoldMentionFeatures.extend(globalFeature, currentClass);

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
        boolean willPrune = false;

        for (Pair<Integer, Double> classScore : sortedClassScores) {
            mean += classScore.getRight();
        }

        double delta = 0;
        for (Pair<Integer, Double> classScore : sortedClassScores) {
            delta += Math.pow(classScore.getRight() - mean, 2);
        }

        delta /= size;

        // This is the std deviation.
        delta = Math.sqrt(delta);

        Pair<Integer, Double> best = sortedClassScores.poll();
        if (best.getLeft() == mentionTypeClassAlphabet.getNoneOfTheAboveClassIndex()) {
            Pair<Integer, Double> secondBest = sortedClassScores.peek();
            double diffByDelta = (best.getRight() - secondBest.getRight()) / delta;
            // The larger diff, the larger the gap between first and second.
            willPrune = diffByDelta > 1;
        }
        // Put the best item back.
        sortedClassScores.add(best);

        return willPrune;
    }

    private void expandState(MentionGraph mentionGraph, LabelLinkAgenda decodingAgenda,
                             NodeLinkingState nodeLinkingState, List<MentionCandidate> candidates,
                             int currGraphNodeIndex, GraphFeatureVector newMentionFeatures,
                             MultiNodeKey currNode, double mentionScore, boolean skipCoref) {
        if (currNode.getCombinedType().equals(ClassAlphabet.noneOfTheAboveClass) || skipCoref) {
            StateDelta decision = new StateDelta(nodeLinkingState);
            decision.addNode(currNode, newMentionFeatures, mentionScore);
            decodingAgenda.expand(decision);
            return;
        }

        // For each label of the current decoding.
        List<List<Pair<Pair<NodeKey, LabelledMentionGraphEdge>, Pair<EdgeType, Double>>>> allLinks = new ArrayList<>();

        for (NodeKey currNodeKey : currNode) {
            // First store all the possible links that can be formed from this node.
            List<Pair<Pair<NodeKey, LabelledMentionGraphEdge>, Pair<EdgeType, Double>>> currLinks = new ArrayList<>();

            for (int ant = 0; ant < currGraphNodeIndex; ant++) {
                // Access the antecedent node.
                MultiNodeKey antNodeKeys = nodeLinkingState.getNode(ant);

                if (antNodeKeys.getCombinedType().equals(ClassAlphabet.noneOfTheAboveClass)) {
                    continue;
                }

                for (NodeKey antNodeKey : antNodeKeys) {
                    LabelledMentionGraphEdge mentionGraphEdge = mentionGraph.getLabelledEdge(candidates, antNodeKey,
                            currNodeKey);
//                            .getMentionGraphEdge(currGraphNodeIndex, ant)
//                            .getLabelledEdge(candidates, antNodeKey, currNodeKey);

                    for (Map.Entry<EdgeType, Double> labelScore : mentionGraphEdge.getAllLabelScore(corefWeights)
                            .entrySet()) {
                        double linkScore = labelScore.getValue();
                        EdgeType edgeType = labelScore.getKey();
                        currLinks.add(Pair.of(Pair.of(currNodeKey, mentionGraphEdge), Pair.of(edgeType, linkScore)));
                    }
                }
            }
            allLinks.add(currLinks);
        }

        // So now what if each node choose a link? we have their cartesian number of possible linking state.
        List<List<Pair<Pair<NodeKey, LabelledMentionGraphEdge>, Pair<EdgeType, Double>>>> possibleLinkComb =
                CollectionUtils.cartesian(allLinks);

        for (List<Pair<Pair<NodeKey, LabelledMentionGraphEdge>, Pair<EdgeType, Double>>> links : possibleLinkComb) {
            StateDelta decision = new StateDelta(nodeLinkingState);
            decision.addNode(currNode, newMentionFeatures, mentionScore);
            for (Pair<Pair<NodeKey, LabelledMentionGraphEdge>, Pair<EdgeType, Double>> link : links) {
                NodeKey dep = link.getLeft().getLeft();
                LabelledMentionGraphEdge edge = link.getLeft().getRight();
                NodeKey gov = edge.getGovKey();
                Pair<EdgeType, Double> labelScore = link.getRight();

                decision.addLink(labelScore.getKey(), gov, dep, labelScore.getValue(), edge.getFeatureVector());

                if (logger.isDebugEnabled()) {
                    if (!gov.isRoot()) {
//                        logger.debug("Current node id is " + currGraphNodeIndex);
//                        logger.debug("Link is " + gov + " to " + dep + " : " + labelScore);
//                        logger.debug(edge.getFeatureVector().readableString());
//                        DebugUtils.pause(logger);
                    }
                }
            }
            decodingAgenda.expand(decision);
        }
    }

    private void expandGoldState(MentionGraph mentionGraph, JointLabelLinkAgenda goldAgenda,
                                 NodeLinkingState nodeLinkingState, int currGraphNodeIndex,
                                 GraphFeatureVector newMentionFeatures, MultiNodeKey goldResult, double mentionScore,
                                 boolean skipCoref) {
        if (goldResult.getCombinedType().equals(ClassAlphabet.noneOfTheAboveClass) || skipCoref) {
            StateDelta decision = new StateDelta(nodeLinkingState);
            decision.addNode(goldResult, newMentionFeatures, mentionScore);
            goldAgenda.expand(decision);
            return;
        }

        // Each list store the possible edge for one current key.
        // We need to get the combination of these edges to create all possible linking.
        List<List<Pair<NodeKey, LabelledMentionGraphEdge>>> allLinks = new ArrayList<>();

        for (NodeKey currNodeKey : goldResult) {
            int numPossibleCorrectLinks = 0;

            List<Pair<NodeKey, LabelledMentionGraphEdge>> currLinks = new ArrayList<>();

            for (int ant = 0; ant < currGraphNodeIndex; ant++) {
                Table<NodeKey, NodeKey, LabelledMentionGraphEdge> realGraphEdges = mentionGraph
                        .getMentionGraphEdge(currGraphNodeIndex, ant).getRealLabelledEdges();
                Map<NodeKey, LabelledMentionGraphEdge> correctAntEdges = realGraphEdges.column(currNodeKey);
                numPossibleCorrectLinks += correctAntEdges.size();

                for (Map.Entry<NodeKey, LabelledMentionGraphEdge> correctAntEdge : correctAntEdges.entrySet()) {
                    NodeKey correctAnt = correctAntEdge.getKey();
                    LabelledMentionGraphEdge realGraphEdge = realGraphEdges.get(correctAnt, currNodeKey);

                    currLinks.add(Pair.of(currNodeKey, realGraphEdge));
                }
            }
            allLinks.add(currLinks);

            if (numPossibleCorrectLinks == 0) {
                throw new IllegalStateException("No possible links for for " + currNodeKey);
            }
        }

        List<List<Pair<NodeKey, LabelledMentionGraphEdge>>> possibleLinkCartesian = CollectionUtils.cartesian(allLinks);

        for (List<Pair<NodeKey, LabelledMentionGraphEdge>> links : possibleLinkCartesian) {
            StateDelta decision = new StateDelta(nodeLinkingState);
            decision.addNode(goldResult, newMentionFeatures, mentionScore);
            for (Pair<NodeKey, LabelledMentionGraphEdge> link : links) {
                NodeKey dep = link.getLeft();
                LabelledMentionGraphEdge edge = link.getRight();
                NodeKey gov = edge.getGovKey();
                Pair<EdgeType, Double> correctLabelScore = edge.getCorrectLabelScore(corefWeights);

                decision.addLink(correctLabelScore.getKey(), gov, dep, correctLabelScore.getValue(), edge
                        .getFeatureVector());
            }
            goldAgenda.expand(decision);
        }
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
