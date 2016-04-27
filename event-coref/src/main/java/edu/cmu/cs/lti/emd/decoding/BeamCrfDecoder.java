package edu.cmu.cs.lti.emd.decoding;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import edu.cmu.cs.lti.learning.feature.extractor.SentenceFeatureExtractor;
import edu.cmu.cs.lti.learning.model.*;
import edu.cmu.cs.lti.learning.model.decoding.LabelLinkAgenda;
import edu.cmu.cs.lti.learning.model.decoding.NodeLinkingState;
import edu.cmu.cs.lti.learning.update.DiscriminativeUpdater;
import edu.cmu.cs.lti.script.type.StanfordCorenlpSentence;
import edu.cmu.cs.lti.script.type.StanfordCorenlpToken;
import gnu.trove.map.TObjectDoubleMap;
import org.apache.commons.lang3.builder.CompareToBuilder;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.javatuples.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Queue;

import static edu.cmu.cs.lti.learning.model.ModelConstants.TYPE_MODEL_NAME;

/**
 * Approximate CRF decoding with Beam search.
 *
 * @author Zhengzhong Liu
 */
public class BeamCrfDecoder {
    protected final Logger logger = LoggerFactory.getLogger(getClass());

    private DiscriminativeUpdater updater;
    private final boolean isTraining;
    private final FeatureAlphabet mentionFeatureAlphabet;
    private final ClassAlphabet mentionTypeClassAlphabet;
    private final SentenceFeatureExtractor mentionExtractor;

    private final GraphWeightVector mentionWeights;

    private TrainingStats typeTrainingStats;

    private boolean usePaUpdate;

    private boolean delayUpdate;

    private String lossType;

    private int beamSize = 5;

    /**
     * For training
     *
     * @param mentionWeights   The weights.
     * @param mentionExtractor The feature extractor
     * @param updater          The updater.
     * @param usePaUpdate
     * @param lossType
     */
    public BeamCrfDecoder(GraphWeightVector mentionWeights, SentenceFeatureExtractor mentionExtractor,
                          DiscriminativeUpdater updater, boolean usePaUpdate, boolean delayUpdate, String lossType) {
        this(mentionWeights, mentionExtractor, true, usePaUpdate, delayUpdate, lossType);
        this.updater = updater;
    }

    /**
     * For testing.
     *
     * @param mentionWeights   The weights.
     * @param mentionExtractor The feature extractor.
     * @param lossType         The Loss type used for training only.
     */
    public BeamCrfDecoder(GraphWeightVector mentionWeights, SentenceFeatureExtractor mentionExtractor,
                          String lossType) {
        this(mentionWeights, mentionExtractor, false, false, false, lossType);
    }

    private BeamCrfDecoder(GraphWeightVector mentionWeights, SentenceFeatureExtractor mentionExtractor,
                           boolean isTraining, boolean usePaUpdate, boolean delayUpdate, String lossType) {
        this.isTraining = isTraining;
        this.usePaUpdate = usePaUpdate;
        this.delayUpdate = delayUpdate;
        this.mentionWeights = mentionWeights;
        this.mentionExtractor = mentionExtractor;
        mentionTypeClassAlphabet = mentionWeights.getClassAlphabet();
        mentionFeatureAlphabet = mentionWeights.getFeatureAlphabet();

        //If delayed update, we check training stats in 5 sentences, if not, we check in 1000 tokens.
        typeTrainingStats = new TrainingStats(delayUpdate ? 5 : 1000, "Mention_" + lossType);
        this.lossType = lossType;
    }

    public NodeLinkingState decode(JCas aJCas, List<MentionCandidate> predictionCandidates, List<MentionCandidate>
            goldCandidates, boolean useAverage) {
        List<StanfordCorenlpSentence> allSentences = new ArrayList<>(JCasUtil.select(aJCas,
                StanfordCorenlpSentence.class));

        LabelLinkAgenda goldAgenda = new LabelLinkAgenda(beamSize, goldCandidates);
        LabelLinkAgenda decodingAgenda = new LabelLinkAgenda(beamSize, predictionCandidates);

        mentionExtractor.initWorkspace(aJCas);

//        int numTokens = 0;
        int docTokenIndex = 0;
        for (int sentIndex = 0; sentIndex < allSentences.size(); sentIndex++) {
            StanfordCorenlpSentence sentence = allSentences.get(sentIndex);
            mentionExtractor.resetWorkspace(aJCas, sentence);

            List<StanfordCorenlpToken> sentenceTokens = JCasUtil.selectCovered(StanfordCorenlpToken.class, sentence);

            for (int sentTokenIndex = 0; sentTokenIndex < sentenceTokens.size(); sentTokenIndex++) {
//                logger.info(String.format("Decoding sentence %d, token %d.", sentIndex, sentTokenIndex));
                decodingAgenda.prepareExpand();
                goldAgenda.prepareExpand();

                // Extract features for the token.
                FeatureVector nodeFeature = new RealValueHashFeatureVector(mentionFeatureAlphabet);
                Table<Integer, Integer, FeatureVector> edgeFeatures = HashBasedTable.create();

                mentionExtractor.extract(sentTokenIndex, nodeFeature, edgeFeatures);

                Queue<Pair<Integer, Double>> sortedClassScores = scoreMentionLocally(nodeFeature, useAverage);

//                logger.info("Node features:");
//                logger.info(nodeFeature.readableString());

                while (!sortedClassScores.isEmpty()) {
                    Pair<Integer, Double> classScore = sortedClassScores.poll();
                    int classIndex = classScore.getValue0();
                    double nodeTypeScore = classScore.getValue1();

                    final MultiNodeKey nodeKeys = setUpCandidate(predictionCandidates.get(docTokenIndex),
                            classIndex, ClassAlphabet.noneOfTheAboveClass);

                    GraphFeatureVector newMentionFeatures = new GraphFeatureVector(mentionTypeClassAlphabet,
                            mentionFeatureAlphabet);
                    newMentionFeatures.extend(nodeFeature, classIndex);

                    FeatureVector globalFeature = new RealValueHashFeatureVector(mentionFeatureAlphabet);
                    for (NodeLinkingState nodeLinkingState : decodingAgenda.getBeamStates()) {
                        mentionExtractor.extractGlobal(sentTokenIndex, globalFeature,
                                nodeLinkingState.getAvailableNodeLabels());
//
//                        logger.info("Global features:");
//                        logger.info(globalFeature.readableString());

                        int prevClassIndex = sentTokenIndex == 0 ?
                                mentionTypeClassAlphabet.getOutsideClassIndex() :
                                mentionTypeClassAlphabet.getClassIndex(nodeLinkingState.getCombinedLastNodeType());

                        double typeEdgeScore = 0;
                        if (edgeFeatures.contains(prevClassIndex, classIndex)) {
                            FeatureVector edgeFeature = edgeFeatures.get(prevClassIndex, classIndex);
                            typeEdgeScore = useAverage ? mentionWeights.dotProdAver(edgeFeature, classIndex,
                                    prevClassIndex) : mentionWeights.dotProd(edgeFeature, classIndex, prevClassIndex);
                            newMentionFeatures.extend(edgeFeature, classIndex, prevClassIndex);
                        }

                        double globalScore = useAverage ? mentionWeights.dotProdAver(globalFeature, classIndex) :
                                mentionWeights.dotProd(globalFeature, classIndex);

                        newMentionFeatures.extend(globalFeature, classIndex);

//                        logger.info(nodeTypeScore + " " + typeEdgeScore + " " + globalScore);

                        decodingAgenda.expand(nodeLinkingState, nodeKeys, nodeTypeScore + typeEdgeScore + globalScore,
                                newMentionFeatures);
                    }
                }
                decodingAgenda.updateStates();

//                logger.info("Decoding agenda: ");
//                logger.info(decodingAgenda.showAgendaItems());

                if (isTraining) {
                    final MultiNodeKey goldResults = goldCandidates.get(docTokenIndex).asKey();

                    for (NodeLinkingState goldState : goldAgenda.getBeamStates()) {
                        GraphFeatureVector goldMentionFeature = getGoldMentionFeatures(goldState, nodeFeature,
                                edgeFeatures, goldCandidates, sentTokenIndex);
                        // For mention only decoding, the score of the gold mentions doesnt matter, since there is
                        // only one solution.
                        goldAgenda.expand(goldState, goldResults, 0, goldMentionFeature);
                    }
                    goldAgenda.updateStates();
                    updater.recordLaSOUpdate(decodingAgenda, goldAgenda, lossType);

                    if (!delayUpdate) {
                        // If do not delay updates, we immediately update the parameters.
                        TObjectDoubleMap<String> losses = updater.update(usePaUpdate);
//                        logger.info("Loss is " + losses.get(TYPE_MODEL_NAME));
                        typeTrainingStats.addLoss(logger, losses.get(TYPE_MODEL_NAME));
                    }
                }

//                logger.info("Gold agenda: ");
//                logger.info(goldAgenda.showAgendaItems());
//                DebugUtils.pause();

                docTokenIndex++;
            }
        }


        if (isTraining) {
            logger.debug("Check for final updates");
            // The final check matches the first item in the agendas, instead of ensuring containment.
            updater.recordFinalUpdate(decodingAgenda, goldAgenda);

            // Update based on cumulative errors.
            logger.debug("Applying updates to " + TYPE_MODEL_NAME);
            TObjectDoubleMap<String> losses = updater.update(usePaUpdate);
            typeTrainingStats.addLoss(logger, losses.get(TYPE_MODEL_NAME));
        }

        return decodingAgenda.getBeamStates().get(0);
    }

    private Queue<Pair<Integer, Double>> scoreMentionLocally(FeatureVector nodeFeature, boolean useAverage) {
        Queue<Pair<Integer, Double>> sortedClassScores = new PriorityQueue<>(
                (o1, o2) ->
                        new CompareToBuilder().append(o2.getValue1(), o1.getValue1()).
                                append(o2.getValue0(), o1.getValue0()).toComparison()
        );

        // Go over possible crf links.
        mentionTypeClassAlphabet.getNormalClassesRange().forEach(classIndex -> {
            double nodeTypeScore = useAverage ? mentionWeights.dotProdAver(nodeFeature, classIndex) :
                    mentionWeights.dotProd(nodeFeature, classIndex);
            sortedClassScores.add(Pair.with(classIndex, nodeTypeScore));
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
        mentionExtractor.extractGlobal(currentIndex, globalFeature,
                goldState.getAvailableNodeLabels());

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

}
