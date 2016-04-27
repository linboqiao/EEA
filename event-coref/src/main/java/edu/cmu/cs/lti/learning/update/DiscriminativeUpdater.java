package edu.cmu.cs.lti.learning.update;

import edu.cmu.cs.lti.learning.model.*;
import edu.cmu.cs.lti.learning.model.decoding.LabelLinkAgenda;
import edu.cmu.cs.lti.learning.model.decoding.NodeLinkingState;
import edu.cmu.cs.lti.learning.model.graph.MentionGraphEdge;
import gnu.trove.map.TObjectDoubleMap;
import gnu.trove.map.hash.TObjectDoubleHashMap;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

import static edu.cmu.cs.lti.learning.model.ModelConstants.COREF_MODEL_NAME;
import static edu.cmu.cs.lti.learning.model.ModelConstants.TYPE_MODEL_NAME;

/**
 * Created with IntelliJ IDEA.
 * Date: 2/28/16
 * Time: 4:31 PM
 *
 * @author Zhengzhong Liu
 */
public class DiscriminativeUpdater {
    protected final Logger logger = LoggerFactory.getLogger(getClass());

    private Map<String, GraphWeightVector> allWeights;

    private Map<String, GraphFeatureVector> allDelta;

    private TObjectDoubleMap<String> allLoss;

    private Map<String, ClassAlphabet> classAlphabets;

    private Map<String, FeatureAlphabet> featureAlphabets;

//    private Map<String, TrainingStats> trainingStats;

    private double defaultStepSize = 0.1; // Default step size used by the perceptron trainer.

    private String[] modelNames = new String[]{TYPE_MODEL_NAME, COREF_MODEL_NAME};

    private boolean updateMention;
    private boolean updateCoref;

    public DiscriminativeUpdater(boolean updateMention, boolean updateCoref) {
        allWeights = new HashMap<>();
        allDelta = new HashMap<>();
        allLoss = new TObjectDoubleHashMap<>();
        classAlphabets = new HashMap<>();
        featureAlphabets = new HashMap<>();
//        trainingStats = new HashMap<>();

        if (!(updateMention || updateCoref)) {
            throw new IllegalArgumentException("Cannot use a updater without updating anything.");
        }

        this.updateMention = updateMention;
        this.updateCoref = updateCoref;
    }

    public void addWeightVector(String name, GraphWeightVector weightVector) {
        allWeights.put(name, weightVector);
        classAlphabets.put(name, weightVector.getClassAlphabet());
        featureAlphabets.put(name, weightVector.getFeatureAlphabet());
        allDelta.put(name, newDelta(name));
        allLoss.put(name, 0);
//        trainingStats.put(name, new TrainingStats(5, name));
    }

    public GraphWeightVector getWeightVector(String weightKey) {
        return allWeights.get(weightKey);
    }

    public void recordLaSOUpdate(LabelLinkAgenda decodingAgenda, LabelLinkAgenda goldAgenda, String lossType) {
        if (!decodingAgenda.contains(goldAgenda)) {
            logger.debug("Recording differences for LaSO update.");
            recordUpdate(decodingAgenda, goldAgenda);

            NodeLinkingState bestDecoding = decodingAgenda.getBeamStates().get(0);
            NodeLinkingState bestGold = goldAgenda.getBeamStates().get(0);

//            logger.debug(String.valueOf(decodingAgenda.getBeamStates().size()));

//            logger.debug("Agenda not matching.");
//
//            logger.debug("Decoding agenda is :");
//            logger.debug(decodingAgenda.showAgendaItems());
//
//            logger.debug("Gold agenda is :");
//            logger.debug(goldAgenda.showAgendaItems());

            Pair<Double, Double> losses = bestDecoding.loss(bestGold, lossType);

            addLoss(TYPE_MODEL_NAME, losses.getLeft());
            addLoss(COREF_MODEL_NAME, losses.getRight());

            // Copy the gold agenda to decoding agenda (LaSO)
            decodingAgenda.copyFrom(goldAgenda);

            // Clear these features from both agenda, since they are assumed to be the same from here.
            goldAgenda.clearFeatures();
            decodingAgenda.clearFeatures();

//            logger.debug("Copied agenda, now showing new system:");
//            logger.debug("Now showing system:");
//            logger.debug(decodingAgenda.showAgendaItems());

//            DebugUtils.pause(logger);
        } else {
//            addLoss(DelayedLaSOJointTrainer.TYPE_MODEL_NAME, 0);
//            logger.debug("Agendas contains same element, skipping update.");
        }

//        DebugUtils.pause();
    }

    public void recordFinalUpdate(LabelLinkAgenda decodingAgenda, LabelLinkAgenda goldAgenda) {
        if (!decodingAgenda.getBeamStates().get(0).match(goldAgenda.getBeamStates().get(0))) {
            logger.debug("Recording final update difference between the top states.");
            recordUpdate(decodingAgenda, goldAgenda);
        }
    }

    private void recordUpdate(LabelLinkAgenda decodingAgenda, LabelLinkAgenda goldAgenda) {
        logger.debug("Best decoding state is:");
        logger.debug(decodingAgenda.getBeamStates().get(0).showTree());

        logger.debug("Best gold state is:");
        logger.debug(goldAgenda.getBeamStates().get(0).showTree());

        // Compute delta on coreferecence.
        GraphFeatureVector deltaCorefVector = allDelta.get(COREF_MODEL_NAME);

//        logger.debug("Computing delta on gold and decoding.");

        for (Map.Entry<MentionGraphEdge.EdgeType, FeatureVector> goldFv : goldAgenda.getBestDeltaCorefVectors()
                .entrySet()) {
            deltaCorefVector.extend(goldFv.getValue(), goldFv.getKey().name());
            logger.debug("Gold feature edge type : " + goldFv.getKey());
            logger.debug(goldFv.getValue().toString());
        }
        for (Map.Entry<MentionGraphEdge.EdgeType, FeatureVector> decoding : decodingAgenda.getBestDeltaCorefVectors()
                .entrySet()) {
            deltaCorefVector.extend(decoding.getValue().negation(), decoding.getKey().name());
            logger.debug("System feature edge type : " + decoding.getValue());
            logger.debug(decoding.getValue().toString());
        }

        // Compute delta on labeling.
        GraphFeatureVector goldLabelFeature = goldAgenda.getBestDeltaLabelFv();
        GraphFeatureVector decodingLabelFeature = decodingAgenda.getBestDeltaLabelFv();

//        logger.info("Showing mention features from  gold");
//        logger.info(goldLabelFeature.readableNodeVector());
//        logger.info("Showing mention features from  decoding");
//        logger.info(decodingLabelFeature.readableNodeVector());

        GraphFeatureVector deltaMentionVector = allDelta.get(TYPE_MODEL_NAME);
        deltaMentionVector.extend(goldLabelFeature);
        deltaMentionVector.extend(decodingLabelFeature, -1);

//        logger.debug("Record the delta");
//        logger.debug("Current delta:");
//        logger.debug(deltaMentionVector.readableNodeVector());
//        DebugUtils.pause(logger);
    }

    private void addLoss(String name, double loss) {
        allLoss.adjustValue(name, loss);
//        logger.debug("Loss for " + name + " is " + loss);
    }

    public TObjectDoubleMap<String> update(boolean paUpdate) {
        // Update and then clear accumulated stuff.
        updateInternal(paUpdate);

        // Logging the loss.
        TObjectDoubleMap<String> currentLoss = new TObjectDoubleHashMap<>();
        currentLoss.putAll(allLoss);

        for (String name : modelNames) {
            allDelta.put(name, newDelta(name));
            allLoss.put(name, 0);
        }
        return currentLoss;
    }

    private void updateInternal(boolean paUpdate) {
//        logger.info("PA Updating " + name);
        GraphFeatureVector corefDelta = allDelta.get(COREF_MODEL_NAME);
        GraphFeatureVector mentionDelta = allDelta.get(TYPE_MODEL_NAME);
        double corefLoss = allLoss.get(COREF_MODEL_NAME);
        double mentionLoss = allLoss.get(TYPE_MODEL_NAME);

        double totalLoss = corefLoss + mentionLoss;

        if (totalLoss != 0) {
            double tau = defaultStepSize;

            if (paUpdate) {
                double corefL2 = updateCoref ? corefDelta.getFeatureL2() : 0;
                double mentionL2 = updateMention ? mentionDelta.getFeatureL2() : 0;
                double l2 = Math.sqrt(Math.pow(corefL2, 2) + Math.pow(mentionL2, 2));
                tau = totalLoss / l2;
            }

            if (updateCoref) {
                GraphWeightVector corefWeights = allWeights.get(COREF_MODEL_NAME);
                corefWeights.updateWeightsBy(corefDelta, tau);
                corefWeights.updateAverageWeights();
            }

            if (updateMention) {
                GraphWeightVector mentionWeights = allWeights.get(TYPE_MODEL_NAME);
                mentionWeights.updateWeightsBy(mentionDelta, tau);
                mentionWeights.updateAverageWeights();
            }
        }
    }

    private GraphFeatureVector newDelta(String name) {
        return new GraphFeatureVector(classAlphabets.get(name), featureAlphabets.get(name));
    }

}
