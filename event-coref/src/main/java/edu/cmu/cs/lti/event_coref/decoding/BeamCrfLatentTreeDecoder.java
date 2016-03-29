package edu.cmu.cs.lti.event_coref.decoding;

import edu.cmu.cs.lti.event_coref.decoding.model.LabelLinkAgenda;
import edu.cmu.cs.lti.event_coref.decoding.model.NodeLinkingState;
import edu.cmu.cs.lti.event_coref.model.graph.LabelledMentionGraphEdge;
import edu.cmu.cs.lti.event_coref.model.graph.MentionGraph;
import edu.cmu.cs.lti.event_coref.model.graph.MentionGraphEdge;
import edu.cmu.cs.lti.event_coref.model.graph.MentionGraphEdge.EdgeType;
import edu.cmu.cs.lti.event_coref.train.DelayedLaSOJointTrainer;
import edu.cmu.cs.lti.event_coref.update.DiscriminativeUpdater;
import edu.cmu.cs.lti.learning.feature.extractor.SentenceFeatureExtractor;
import edu.cmu.cs.lti.learning.feature.mention_pair.extractor.PairFeatureExtractor;
import edu.cmu.cs.lti.learning.model.*;
import edu.cmu.cs.lti.learning.model.MentionCandidate.DecodingResult;
import edu.cmu.cs.lti.script.type.StanfordCorenlpSentence;
import edu.cmu.cs.lti.script.type.StanfordCorenlpToken;
import edu.cmu.cs.lti.utils.Functional;
import gnu.trove.map.TObjectDoubleMap;
import gnu.trove.map.hash.TObjectDoubleHashMap;
import org.apache.commons.lang3.builder.CompareToBuilder;
import org.apache.commons.lang3.mutable.MutableDouble;
import org.apache.commons.lang3.mutable.MutableInt;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.javatuples.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.function.BiFunction;

/**
 * Created with IntelliJ IDEA.
 * Date: 2/28/16
 * Time: 4:22 PM
 *
 * @author Zhengzhong Liu
 */
public class BeamCrfLatentTreeDecoder {
    protected final Logger logger = LoggerFactory.getLogger(getClass());

    //    private final MentionGraph mentionGraph;
    private final GraphWeightVector corefWeights;
    private final FeatureAlphabet mentionFeatureAlphabet;
    private final ClassAlphabet mentionTypeClassAlphabet;
    private final WekaModel realisModel;

    private final SentenceFeatureExtractor realisExtractor;
    private final SentenceFeatureExtractor crfExtractor;
    private final PairFeatureExtractor mentionPairExtractor;

    private final GraphWeightVector mentionWeights;

    private DiscriminativeUpdater updater;
    private final boolean isTraining;

    private TrainingStats corefTrainingStats;
    private TrainingStats typeTrainingStats;

    // TODO use a smaller beam size for debug.
    private int beamSize = 5;

    // A empty feature vector for placeholder, don't use it.
    private final FeatureVector dummyFv;


    public BeamCrfLatentTreeDecoder(GraphWeightVector mentionWeights, WekaModel realisModel,
                                    GraphWeightVector corefWeights, SentenceFeatureExtractor realisExtractor,
                                    SentenceFeatureExtractor crfExtractor, PairFeatureExtractor mentionPairExtractor,
                                    DiscriminativeUpdater updater)
            throws ClassNotFoundException, NoSuchMethodException, InstantiationException, IllegalAccessException,
            InvocationTargetException {
        this(mentionWeights, realisModel, corefWeights, realisExtractor, crfExtractor, mentionPairExtractor, true);
        this.updater = updater;
        logger.info("Starting the Beam Decoder with Training mode.");
    }

    public BeamCrfLatentTreeDecoder(GraphWeightVector mentionWeights, WekaModel realisModel,
                                    GraphWeightVector corefWeights, SentenceFeatureExtractor realisExtractor,
                                    SentenceFeatureExtractor crfExtractor, PairFeatureExtractor mentionPairExtractor)
            throws ClassNotFoundException, NoSuchMethodException, InstantiationException, IllegalAccessException,
            InvocationTargetException {
        this(mentionWeights, realisModel, corefWeights, realisExtractor, crfExtractor, mentionPairExtractor, false);

        logger.info("Starting the Beam Decoder with Testing mode.");

    }

    public BeamCrfLatentTreeDecoder(GraphWeightVector mentionWeights, WekaModel realisModel,
                                    GraphWeightVector corefWeights, SentenceFeatureExtractor realisExtractor,
                                    SentenceFeatureExtractor crfExtractor, PairFeatureExtractor mentionPairExtractor,
                                    boolean isTraining)
            throws ClassNotFoundException, NoSuchMethodException, InstantiationException, IllegalAccessException,
            InvocationTargetException {
        this.corefWeights = corefWeights;

        mentionTypeClassAlphabet = mentionWeights.getClassAlphabet();
        mentionFeatureAlphabet = mentionWeights.getFeatureAlphabet();

        this.mentionWeights = mentionWeights;
        this.realisModel = realisModel;

        this.realisExtractor = realisExtractor;
        this.crfExtractor = crfExtractor;
        this.mentionPairExtractor = mentionPairExtractor;

        this.isTraining = isTraining;

        if (isTraining) {
            corefTrainingStats = new TrainingStats(5, "coref");
            typeTrainingStats = new TrainingStats(5, "type");
        }

        dummyFv = new RealValueHashFeatureVector(corefWeights.getFeatureAlphabet());
    }

    public NodeLinkingState decode(JCas aJCas, MentionGraph mentionGraph, List<MentionCandidate> predictionCandidates,
                                   boolean useAverage) {
        // TODO: Need to handle the empty gold standard.
        return decode(aJCas, mentionGraph, predictionCandidates, new ArrayList<>(), useAverage);
    }


    public NodeLinkingState decode(JCas aJCas, MentionGraph mentionGraph, List<MentionCandidate> predictionCandidates,
                                   List<MentionCandidate> goldCandidates, boolean useAverage) {
        List<StanfordCorenlpSentence> allSentences = new ArrayList<>(JCasUtil.select(aJCas, StanfordCorenlpSentence
                .class));

//        logger.debug(String.format("Sentence size : %d, Candidate size : %d, Graph nodes : %d", allSentences.size(),
//                predictionCandidates.size(), mentionGraph.numNodes()));

        // Dot product function on the node (i.e. only take features depend on current class)
        BiFunction<FeatureVector, Integer, Double> nodeDotProd = useAverage ?
                mentionWeights::dotProdAver : mentionWeights::dotProd;
        // Dot product function on the edge (i.e. take features depend on two classes)
        Functional.TriFunction<FeatureVector, Integer, Integer, Double> edgeDotProd = useAverage ?
                mentionWeights::dotProdAver : mentionWeights::dotProd;

        // Prepare a gold agenda and a decoding agenda.
        LabelLinkAgenda goldAgenda = new LabelLinkAgenda(beamSize, goldCandidates, mentionGraph);
        LabelLinkAgenda decodingAgenda = new LabelLinkAgenda(beamSize, predictionCandidates, mentionGraph);

//        mentionPairExtractor.initWorkspace(aJCas);
        realisExtractor.initWorkspace(aJCas);
        crfExtractor.initWorkspace(aJCas);

        // Current mention node index, starts from 1, after the root node 0.
        MutableInt curr = new MutableInt(1);
        Set<Integer> prunedNodes = new HashSet<>();

        for (int sentIndex = 0; sentIndex < allSentences.size(); sentIndex++) {
            StanfordCorenlpSentence sentence = allSentences.get(sentIndex);
            realisExtractor.resetWorkspace(aJCas, sentence);
            crfExtractor.resetWorkspace(aJCas, sentence);

            List<StanfordCorenlpToken> sentenceTokens = JCasUtil.selectCovered(StanfordCorenlpToken.class, sentence);

            // Move over the tokens.
            for (int sentTokenIndex = 0; sentTokenIndex < sentenceTokens.size(); sentTokenIndex++) {
                int currentMentionId = mentionGraph.getCandidateIndex(curr.getValue());

//                logger.debug(String.format("Decoding sentence %d, token %d, mention node index %d, token is %s",
//                        sentIndex, sentTokenIndex, curr.getValue(),
//                        predictionCandidates.get(currentMentionId).getHeadWord().getCoveredText()));

                // Prepare expansion, initialize all the deltas.
                decodingAgenda.prepareExpand();
                goldAgenda.prepareExpand();

                // Extract features for the token.
                FeatureVector nodeFeature = new RealValueHashFeatureVector(mentionFeatureAlphabet);
                FeatureVector edgeFeature = new RealValueHashFeatureVector(mentionFeatureAlphabet);
                crfExtractor.extract(sentTokenIndex, nodeFeature, edgeFeature);

                // Take the current candidate.
                final MentionCandidate predictionCandidate = predictionCandidates.get(currentMentionId);

                // Note that most gold candidate are None type and None realis since they are not mentions.
                final MentionCandidate goldCandidate = goldCandidates.get(currentMentionId);
                final List<DecodingResult> goldResults = goldCandidate.asKey();
                GraphFeatureVector goldMentionFeature = getGoldFeatures(nodeFeature, edgeFeature, goldCandidates,
                        currentMentionId);

                // Predicate the realis for each word, the features are not related to mention types.
                String realis = getRealis((StanfordCorenlpToken) predictionCandidate.getHeadWord());

                Queue<Pair<Integer, Double>> sortedClassScores = new PriorityQueue<>(
                        (o1, o2) ->
                                new CompareToBuilder().append(o2.getValue1(), o1.getValue1()).
                                        append(o2.getValue0(), o1.getValue0()).toComparison()
                );

//                Map<Integer, Double> classNodeScores = new HashMap<>();

                MutableDouble noneScore = new MutableDouble(0);
                MutableDouble maxTypedScore = new MutableDouble(0);
                MutableInt maxType = new MutableInt(-1);

                // Go over possible crf links.
                mentionTypeClassAlphabet.getNormalClassesRange().forEach(classIndex -> {
                    double nodeTypeScore = nodeDotProd.apply(nodeFeature, classIndex);
//                    classNodeScores.put(classIndex, nodeTypeScore);

                    sortedClassScores.add(Pair.with(classIndex, nodeTypeScore));

//                    // Do a filtering here.
                    if (classIndex == mentionTypeClassAlphabet.getNoneOfTheAboveClassIndex()) {
                        noneScore.setValue(nodeTypeScore);
                    } else {
                        if (nodeTypeScore > maxTypedScore.getValue() || maxType.getValue() == -1) {
                            maxTypedScore.setValue(nodeTypeScore);
                            maxType.setValue(classIndex);
                        }
                    }
                });

//                logger.debug(String.format("None score is %.4f, best typed score is %.4f, which is class %s",
//                        noneScore.getValue(), maxTypedScore.getValue(),
//                        mentionTypeClassAlphabet.getClassName(maxType.getValue())));

                boolean prune = localPrune(sortedClassScores);

                if (prune) {
                    sortedClassScores.clear();
                    sortedClassScores.add(Pair.with(mentionTypeClassAlphabet.getNoneOfTheAboveClassIndex(), 0.0));
                    prunedNodes.add(curr.getValue());
                    logger.debug("Pruned node " + curr.getValue());
                }

                // Only take the top 5 classes to expand.
                int count = 0;
                while (!sortedClassScores.isEmpty()) {
                    Pair<Integer, Double> classScore = sortedClassScores.poll();
                    int classIndex = classScore.getValue0();
                    double nodeTypeScore = classScore.getValue1();
                    final List<DecodingResult> decodingResults = setUpCandidate(predictionCandidate, classIndex,
                            realis);

                    GraphFeatureVector newDecodingMentionFeatures = new GraphFeatureVector(mentionTypeClassAlphabet,
                            mentionFeatureAlphabet);
                    newDecodingMentionFeatures.extend(nodeFeature, classIndex);

                    for (NodeLinkingState nodeLinkingState : decodingAgenda.getBeamStates()) {
                        // The previous type class is defined on sentence base, so we will ad a special sentence
                        // boundary class before each one.
                        int prevClassIndex;
                        if (sentTokenIndex == 0) {
                            prevClassIndex = mentionTypeClassAlphabet.getOutsideClassIndex();
                        } else {
                            prevClassIndex = mentionTypeClassAlphabet.getClassIndex(
                                    nodeLinkingState.getCombinedLastNodeType());
                        }
//                        logger.info(String.format("Decoding class %d for system.", classIndex));
//                        logger.info(nodeLinkingState.showNodes());
                        // Compute the first order CRF edge score.
                        double newTypeEdgeScore = edgeDotProd.apply(edgeFeature, classIndex, prevClassIndex);
                        newDecodingMentionFeatures.extend(edgeFeature, classIndex, prevClassIndex);

                        linkToAntecedent(mentionGraph, decodingAgenda, nodeLinkingState, predictionCandidates,
                                curr.getValue(), newDecodingMentionFeatures, decodingResults,
                                nodeTypeScore + newTypeEdgeScore, prunedNodes);
                    }

//                    DebugUtils.pause(logger);

                    count++;
                    if (count == 5) {
                        break; // Only expand top 5 classes.
                    }
                }


//                logger.debug("Updating states for prediction.");
                decodingAgenda.updateStates();

                if (isTraining) {
                    // Expanding the states for gold agenda.
                    for (NodeLinkingState nodeLinkingState : goldAgenda.getBeamStates()) {
//                    logger.info(String.format("Decoding antecedent at node %d for gold.", curr.getValue()));
                        linkToCorrectAntecedent(mentionGraph, goldAgenda, nodeLinkingState, curr
                                .getValue(), goldMentionFeature, goldResults, 0);
                    }

//                    logger.debug("Updating states for gold.");
                    goldAgenda.updateStates();
                }

//                logger.debug("Showing decoding agenda");
//                logger.debug(decodingAgenda.showAgendaItems());
//
//                logger.debug("Showing gold agenda");
//                logger.debug(goldAgenda.showAgendaItems());

                // Check if the agenda matches, otherwise record the difference. i.e. add delta to the Updater
                // And then copy over the agendas
                if (isTraining) {
                    updater.recordLaSOUpdate(decodingAgenda, goldAgenda);
                }
                curr.increment();
            } // Finish iterate tokens in a sentence.

//            if (isTraining) {
////                logger.debug("Applying updates to " + DelayedLaSOJointTrainer.TYPE_MODEL_NAME);
//                updater.update(DelayedLaSOJointTrainer.TYPE_MODEL_NAME);
//            }

//            decodingAgenda.showActualSequence();
//            goldAgenda.showActualSequence();
//            DebugUtils.pause(logger);

        }// Finish iterate sentences.

//        logger.debug("Pruned nodes " + prunedNodes.size());

//        logger.debug("Checking gold nodes.");
//        logger.debug(goldAgenda.getBeamStates().peek().showNodes());
//        logger.debug("Checking system nodes.");
//        logger.debug(decodingAgenda.getBeamStates().peek().showNodes());
//        DebugUtils.pause(logger);

        if (isTraining) {
            logger.debug("Check for final updates");

            // The final check matches the first item in the agendas, while the searching check only ensure containment.
            updater.recordFinalUpdate(decodingAgenda, goldAgenda);

            // Update based on cumulative errors.
            logger.debug("Applying updates to " + DelayedLaSOJointTrainer.TYPE_MODEL_NAME);
            double typeLoss = updater.update(DelayedLaSOJointTrainer.TYPE_MODEL_NAME);
            typeTrainingStats.addLoss(logger, typeLoss / mentionGraph.numNodes());

            logger.debug("Applying updates to " + DelayedLaSOJointTrainer.COREF_MODEL_NAME);
            double corefLoss = updater.update(DelayedLaSOJointTrainer.COREF_MODEL_NAME);
            corefTrainingStats.addLoss(logger, corefLoss / mentionGraph.numNodes());
        }

        return decodingAgenda.getBeamStates().peek();
    }

    private GraphFeatureVector getGoldFeatures(FeatureVector nodeFeature, FeatureVector edgeFeature,
                                               List<MentionCandidate> goldCandidates, int currentIndex) {
        GraphFeatureVector newGoldMentionFeatures = new GraphFeatureVector(mentionTypeClassAlphabet,
                mentionFeatureAlphabet);
        int currentClass = mentionTypeClassAlphabet.getClassIndex(goldCandidates.get(currentIndex).getMentionType());
        int previousClass = currentIndex == 0 ? mentionTypeClassAlphabet.getOutsideClassIndex() :
                mentionTypeClassAlphabet.getClassIndex(goldCandidates.get(currentIndex - 1).getMentionType());
        newGoldMentionFeatures.extend(nodeFeature, currentClass);
        newGoldMentionFeatures.extend(edgeFeature, currentClass, previousClass);

        return newGoldMentionFeatures;
    }

    private List<DecodingResult> setUpCandidate(MentionCandidate currPredictionCandidate, int typeIndex, String
            realis) {
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
            mean += classScore.getValue1();
        }

        double delta = 0;
        for (Pair<Integer, Double> classScore : sortedClassScores) {
            delta += Math.pow(classScore.getValue1() - mean, 2);
        }

        delta /= size;

        delta = Math.sqrt(delta);

        Pair<Integer, Double> best = sortedClassScores.poll();
        if (best.getValue0() == mentionTypeClassAlphabet.getNoneOfTheAboveClassIndex()) {
            Pair<Integer, Double> secondBest = sortedClassScores.peek();
            double diffByDelta = (best.getValue1() - secondBest.getValue1()) / delta;

            // The larger diff, the larger the gap between first and second.
            return diffByDelta > 1;
        }

        // Put the best item back.
        sortedClassScores.add(best);

        return false;
    }

    private void linkToAntecedent(MentionGraph mentionGraph, LabelLinkAgenda decodingAgenda,
                                  NodeLinkingState nodeLinkingState, List<MentionCandidate> candidates,
                                  int currGraphNodeIndex, GraphFeatureVector newDecodingCrfFeatures,
                                  List<DecodingResult> currDecodingResults, double mentionScore,
                                  Set<Integer> prunedNodes) {
//        logger.debug("Linking to antecedent, number of candidates: " + (currGraphNodeIndex - prunedNodes.size()));
        if (currDecodingResults.size() == 1) {
            if (currDecodingResults.get(0).getMentionType().equals(ClassAlphabet.noneOfTheAboveClass)) {
                // If this is non-type, we link it directly to root.
                List<Integer> zeros = Collections.singletonList(0);
                List<EdgeType> edgeTypes = Collections.singletonList(EdgeType.Root);
                List<DecodingResult> govKeys = MentionCandidate.getRootKey();
                decodingAgenda.expand(nodeLinkingState, zeros, edgeTypes, govKeys,
                        currDecodingResults, mentionScore, newDecodingCrfFeatures,
                        Collections.singletonList(Pair.with(MentionGraphEdge.EdgeType.Root, dummyFv)));
                return;
            }
        }

        List<DecodingResult> bestGovKeys = new ArrayList<>();
        List<Integer> bestAntecedents = new ArrayList<>();
        List<EdgeType> bestEdgeTypes = new ArrayList<>();
        double fullScore = mentionScore;
        List<Pair<MentionGraphEdge.EdgeType, FeatureVector>> newCorefFeatures = new ArrayList<>();

        // For each label of the current decoding.
        for (DecodingResult currDecodingResult : currDecodingResults) {
            double bestLinkScore = Double.NEGATIVE_INFINITY;
            DecodingResult bestAntecedentNode = null;
            MentionGraphEdge.EdgeType bestEdgeType = null;
            int bestAnt = 0;
            Pair<MentionGraphEdge.EdgeType, FeatureVector> bestNewCorefFeature = null;

            for (int ant = 0; ant < currGraphNodeIndex; ant++) {
                if (prunedNodes.contains(ant)) {
//                logger.debug("Antecedent is pruned, not considered");
                    continue;
                }

//                logger.debug("Training " + ant);
                // Access the antecedent node.
                List<DecodingResult> antDecodingResults = nodeLinkingState.getNode(ant);

//                logger.debug("Possible results size " + antDecodingResults.size());

                for (DecodingResult antDecodingResult : antDecodingResults) {
                    LabelledMentionGraphEdge mentionGraphEdge = mentionGraph
                            .getMentionGraphEdge(currGraphNodeIndex, ant)
                            .getLabelledEdge(candidates, antDecodingResult, currDecodingResult);
                    Pair<MentionGraphEdge.EdgeType, Double> bestLabelScore = mentionGraphEdge
                            .getBestLabelScore(corefWeights);

                    double linkScore = bestLabelScore.getValue1();
                    MentionGraphEdge.EdgeType edgeType = bestLabelScore.getValue0();

//                    logger.debug("Link score is " + linkScore);

                    if (linkScore > bestLinkScore) {
                        bestLinkScore = linkScore;
                        bestAntecedentNode = antDecodingResult;
                        bestEdgeType = edgeType;
                        bestAnt = ant;
                        bestNewCorefFeature = Pair.with(bestEdgeType, mentionGraphEdge.getFeatureVector());
                    }
                }
            }

            bestGovKeys.add(bestAntecedentNode);
            bestAntecedents.add(bestAnt);
            bestEdgeTypes.add(bestEdgeType);
            newCorefFeatures.add(bestNewCorefFeature);
            fullScore += bestLinkScore;
        }

        decodingAgenda.expand(nodeLinkingState, bestAntecedents, bestEdgeTypes, bestGovKeys,
                currDecodingResults, fullScore, newDecodingCrfFeatures, newCorefFeatures);
    }

    private void linkToCorrectAntecedent(MentionGraph mentionGraph, LabelLinkAgenda goldAgenda,
                                         NodeLinkingState nodeLinkingState,
                                         int currGraphNodeIndex, GraphFeatureVector newDecodingCrfFeatures,
                                         List<DecodingResult> currDecodingResults, double mentionScore) {
        List<DecodingResult> correctGovKeys = new ArrayList<>();
        List<Integer> correctAntecedents = new ArrayList<>();
        List<EdgeType> correctEdgeTypes = new ArrayList<>();
        double fullScore = mentionScore;
        List<Pair<MentionGraphEdge.EdgeType, FeatureVector>> newCorefFeatures = new ArrayList<>();


        for (DecodingResult currDecodingResult : currDecodingResults) {
            double bestLinkScore = Double.NEGATIVE_INFINITY;
            int bestAntecedent = -1;
            EdgeType bestEdgeType = null;
            Pair<MentionGraphEdge.EdgeType, FeatureVector> bestFeature = null;
            DecodingResult bestGovKey = null;

            for (int ant = 0; ant < currGraphNodeIndex; ant++) {
                List<LabelledMentionGraphEdge> realGraphEdges = mentionGraph
                        .getMentionGraphEdge(currGraphNodeIndex, ant).getRealLabelledEdges();

                for (LabelledMentionGraphEdge realGraphEdge : realGraphEdges) {
                    if (realGraphEdge.getDepKey().equals(currDecodingResult)) {
                        Pair<MentionGraphEdge.EdgeType, Double> correctLabelScore = realGraphEdge
                                .getCorrectLabelScore(corefWeights);
                        double linkScore = correctLabelScore.getValue1();
                        EdgeType edgeType = correctLabelScore.getValue0();

                        if (linkScore > bestLinkScore) {
                            bestLinkScore = linkScore;
                            bestAntecedent = ant;
                            bestEdgeType = correctLabelScore.getValue0();
                            bestFeature = Pair.with(edgeType, realGraphEdge.getFeatureVector());
                            bestGovKey = realGraphEdge.getGovKey();
                        }
                    }
                }
            }

            if (bestGovKey == null) {
                logger.debug("Correct gov key is null?");
                logger.debug(currDecodingResult.toString());
                logger.debug(String.format("Best link score is %.2f.", bestLinkScore));
                for (int ant = 0; ant < currGraphNodeIndex; ant++) {
                    List<LabelledMentionGraphEdge> realGraphEdges = mentionGraph
                            .getMentionGraphEdge(currGraphNodeIndex, ant).getRealLabelledEdges();
                    for (LabelledMentionGraphEdge realGraphEdge : realGraphEdges) {
                        logger.debug("Real edge here " + realGraphEdge);
                    }
                }

                for (int ant = 0; ant < currGraphNodeIndex; ant++) {
                    List<LabelledMentionGraphEdge> realGraphEdges = mentionGraph
                            .getMentionGraphEdge(currGraphNodeIndex, ant).getRealLabelledEdges();

                    for (LabelledMentionGraphEdge realGraphEdge : realGraphEdges) {
                        if (realGraphEdge.getDepKey().equals(currDecodingResult)) {
                            Pair<MentionGraphEdge.EdgeType, Double> correctLabelScore = realGraphEdge
                                    .getCorrectLabelScore(corefWeights);
                            double linkScore = correctLabelScore.getValue1();
                            EdgeType edgeType = correctLabelScore.getValue0();


                            FeatureVector fv = realGraphEdge.getFeatureVector();

                            logger.debug(String.format("Link score is %.2f, edge type is %s, antecedent is %d",
                                    linkScore, edgeType, ant));

                            logger.debug(fv.readableString());

                            for (FeatureVector.FeatureIterator iter = fv.featureIterator(); iter.hasNext(); ) {
                                iter.next();
                                logger.debug(String.format("Weight for feature [%s] is [%.2f], feature value is [%.2f]",
                                        corefWeights.getFeatureAlphabet().getFeatureNameRepre(iter.featureIndex()),
                                        corefWeights.getNodeWeights(edgeType.name()).getWeightAt(iter.featureIndex()),
                                        iter.featureValue()));
                            }


                        } else {
                            logger.debug("Real graph edge is does not contain the correct result.");
                            logger.debug(realGraphEdge.toString());
                            logger.debug(currDecodingResult.toString());

                            logger.debug("There are " + currDecodingResults.size() + " current labels.");
                            for (DecodingResult decodingResult : currDecodingResults) {
                                logger.debug("They are " + decodingResult);
                            }

                            logger.debug("There are " + realGraphEdges.size() + " graph edges.");
                            for (LabelledMentionGraphEdge graphEdge : realGraphEdges) {
                                logger.debug("They are " + graphEdge);
                            }
                        }
                    }
                }
            }

            correctGovKeys.add(bestGovKey);
            correctAntecedents.add(bestAntecedent);
            correctEdgeTypes.add(bestEdgeType);
            newCorefFeatures.add(bestFeature);

            // So, scores from multiple edges are considered.
            fullScore += bestLinkScore;
        }

        goldAgenda.expand(nodeLinkingState, correctAntecedents, correctEdgeTypes, correctGovKeys,
                currDecodingResults, fullScore, newDecodingCrfFeatures, newCorefFeatures);
    }

    private String getRealis(StanfordCorenlpToken headToken) {
        TObjectDoubleMap<String> rawFeatures = new TObjectDoubleHashMap<>();

        FeatureVector mentionFeatures = new RealValueHashFeatureVector(realisModel.getAlphabet());
        int head = realisExtractor.getElementIndex(headToken);
        realisExtractor.extract(head, mentionFeatures, new RealValueHashFeatureVector(realisModel.getAlphabet()));

        for (FeatureVector.FeatureIterator iter = mentionFeatures.featureIterator(); iter.hasNext(); ) {
            iter.next();
            rawFeatures.put(realisModel.getAlphabet().getFeatureNames(iter.featureIndex())[0], iter.featureValue());
        }

        try {
            Pair<Double, String> prediction = realisModel.classify(rawFeatures);
            return prediction.getValue1();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return ClassAlphabet.noneOfTheAboveClass;
    }
}
