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
import org.apache.commons.lang3.tuple.Pair;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Queue;

import static edu.cmu.cs.lti.learning.model.ModelConstants.TYPE_MODEL_NAME;

//import org.javatuples.Pair;

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

    private boolean delayUpdate;

    private int beamSize = 5;

    /**
     * For training
     *
     * @param mentionWeights   The weights.
     * @param mentionExtractor The feature extractor
     * @param updater          The updater.
     */
    public BeamCrfDecoder(GraphWeightVector mentionWeights, SentenceFeatureExtractor mentionExtractor,
                          DiscriminativeUpdater updater, boolean delayUpdate) {
        this(mentionWeights, mentionExtractor, true, delayUpdate);
        this.updater = updater;
    }

    /**
     * For testing.
     *
     * @param mentionWeights   The weights.
     * @param mentionExtractor The feature extractor.
     */
    public BeamCrfDecoder(GraphWeightVector mentionWeights, SentenceFeatureExtractor mentionExtractor) {
        this(mentionWeights, mentionExtractor, false, false);
    }

    private BeamCrfDecoder(GraphWeightVector mentionWeights, SentenceFeatureExtractor mentionExtractor,
                           boolean isTraining, boolean delayUpdate) {
        this.isTraining = isTraining;
        this.delayUpdate = delayUpdate;
        this.mentionWeights = mentionWeights;
        this.mentionExtractor = mentionExtractor;
        mentionTypeClassAlphabet = mentionWeights.getClassAlphabet();
        mentionFeatureAlphabet = mentionWeights.getFeatureAlphabet();

        //If delayed update, we check training stats in 5 sentences, if not, we check in 2000 tokens.
        typeTrainingStats = new TrainingStats(delayUpdate ? 5 : 2000, "Mention");
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

//            logger.debug("[Sentence] " + sentence.getCoveredText());

            List<StanfordCorenlpToken> sentenceTokens = JCasUtil.selectCovered(StanfordCorenlpToken.class, sentence);

            for (int sentTokenIndex = 0; sentTokenIndex < sentenceTokens.size(); sentTokenIndex++) {
//                logger.info(String.format("Decoding sentence %d, token %d: %s", sentIndex, sentTokenIndex,
//                        sentenceTokens.get(sentTokenIndex).getCoveredText()));
                decodingAgenda.prepareExpand();
                goldAgenda.prepareExpand();

                // Extract features for the token.
                FeatureVector nodeFeature = new RealValueHashFeatureVector(mentionFeatureAlphabet);
                Table<Integer, Integer, FeatureVector> edgeFeatures = HashBasedTable.create();

                mentionExtractor.extract(sentTokenIndex, nodeFeature, edgeFeatures);

//                logger.debug(nodeFeature.readableString());

                Queue<Pair<Integer, Double>> sortedClassScores = scoreMentionLocally(nodeFeature, useAverage);

                while (!sortedClassScores.isEmpty()) {
                    Pair<Integer, Double> classScore = sortedClassScores.poll();
                    int classIndex = classScore.getKey();
                    double nodeTypeScore = classScore.getValue();

//                    logger.debug("Type score for " + mentionTypeClassAlphabet.getClassName(classIndex) + " is " + nodeTypeScore);

                    final MultiNodeKey nodeKeys = setUpCandidate(predictionCandidates.get(docTokenIndex),
                            classIndex, ClassAlphabet.noneOfTheAboveClass);

                    GraphFeatureVector newMentionFeatures = new GraphFeatureVector(mentionTypeClassAlphabet,
                            mentionFeatureAlphabet);
                    newMentionFeatures.extend(nodeFeature, classIndex);

                    for (NodeLinkingState nodeLinkingState : decodingAgenda.getBeamStates()) {
                        FeatureVector globalFeature = new RealValueHashFeatureVector(mentionFeatureAlphabet);
                        // The focus is simply a token index here, which can correctly index actual nodes.
                        mentionExtractor.extractGlobal(docTokenIndex, globalFeature,
                                nodeLinkingState.getActualNodeResults());

                        // Here we are adding invisible sentence boundary class at each sentence start.
                        int prevClassIndex = sentTokenIndex == 0 ?
                                mentionTypeClassAlphabet.getOutsideClassIndex() :
                                mentionTypeClassAlphabet.getClassIndex(nodeLinkingState.getCombinedLastNodeType());

                        double edgeTypeScore = 0;
                        if (edgeFeatures.contains(prevClassIndex, classIndex)) {
                            FeatureVector edgeFeature = edgeFeatures.get(prevClassIndex, classIndex);
                            edgeTypeScore = useAverage ? mentionWeights.dotProdAver(edgeFeature, classIndex,
                                    prevClassIndex) : mentionWeights.dotProd(edgeFeature, classIndex, prevClassIndex);
                            newMentionFeatures.extend(edgeFeature, classIndex, prevClassIndex);
                        }

                        double globalScore = useAverage ? mentionWeights.dotProdAver(globalFeature, classIndex) :
                                mentionWeights.dotProd(globalFeature, classIndex);

//                        logger.debug(globalFeature.readableString());
//                        logger.debug("Global score is " + globalScore);

                        newMentionFeatures.extend(globalFeature, classIndex);

                        decodingAgenda.expand(nodeLinkingState, nodeKeys, nodeTypeScore + edgeTypeScore + globalScore,
                                newMentionFeatures);
                    }
                }

                decodingAgenda.updateStates();

//                logger.debug("Decoding agenda: ");
//                logger.debug(decodingAgenda.showAgendaItems());

                if (isTraining) {
                    final MultiNodeKey goldResults = goldCandidates.get(docTokenIndex).asKey();

                    for (NodeLinkingState goldState : goldAgenda.getBeamStates()) {
                        GraphFeatureVector goldMentionFeature = getGoldMentionFeatures(goldState, nodeFeature,
                                edgeFeatures, goldCandidates, docTokenIndex);
                        // For mention only decoding, the score of the gold mentions doesnt matter, since there is
                        // only one solution.
                        goldAgenda.expand(goldState, goldResults, 0, goldMentionFeature);
                    }
                    goldAgenda.updateStates();
//                    logger.debug("Gold agenda: ");
//                    logger.debug(goldAgenda.showAgendaItems());

                    updater.recordLaSOUpdate(decodingAgenda, goldAgenda);

                    if (!delayUpdate) {
                        // If do not delay updates, we immediately update the parameters.
                        TObjectDoubleMap<String> losses = updater.update();
//                        logger.info("Loss is " + losses.get(TYPE_MODEL_NAME));
                        typeTrainingStats.addLoss(logger, losses.get(TYPE_MODEL_NAME));
                    }
                }


//                DebugUtils.pause(logger);
                docTokenIndex++;
            }
        }


        if (isTraining) {
//            logger.debug("Check for final updates");
            // The final check matches the first item in the agendas, instead of ensuring containment.
            updater.recordFinalUpdate(decodingAgenda, goldAgenda);

            // Update based on cumulative errors.
//            logger.debug("Applying updates to " + TYPE_MODEL_NAME);
            TObjectDoubleMap<String> losses = updater.update();
            typeTrainingStats.addLoss(logger, losses.get(TYPE_MODEL_NAME));
        }

        return decodingAgenda.getBeamStates().get(0);
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

    private GraphFeatureVector getGoldMentionFeatures(NodeLinkingState goldState, FeatureVector nodeFeature,
                                                      Table<Integer, Integer, FeatureVector> edgeFeatures,
                                                      List<MentionCandidate> goldCandidates, int docTokenIndex) {

        GraphFeatureVector newGoldMentionFeatures = new GraphFeatureVector(mentionTypeClassAlphabet,
                mentionFeatureAlphabet);
        int currentClass = mentionTypeClassAlphabet.getClassIndex(goldCandidates.get(docTokenIndex).getMentionType());
        int previousClass = docTokenIndex == 0 ? mentionTypeClassAlphabet.getOutsideClassIndex() :
                mentionTypeClassAlphabet.getClassIndex(goldCandidates.get(docTokenIndex - 1).getMentionType());

        newGoldMentionFeatures.extend(nodeFeature, currentClass);

        if (edgeFeatures.contains(previousClass, currentClass)) {
            FeatureVector edgeFeature = edgeFeatures.get(previousClass, currentClass);
            newGoldMentionFeatures.extend(edgeFeature, currentClass, previousClass);
        }

        FeatureVector globalFeature = new RealValueHashFeatureVector(mentionFeatureAlphabet);
        mentionExtractor.extractGlobal(docTokenIndex, globalFeature, goldState.getActualNodeResults());

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
