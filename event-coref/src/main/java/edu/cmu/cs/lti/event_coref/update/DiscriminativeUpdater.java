package edu.cmu.cs.lti.event_coref.update;

import edu.cmu.cs.lti.event_coref.decoding.model.LabelLinkAgenda;
import edu.cmu.cs.lti.event_coref.decoding.model.NodeLinkingState;
import edu.cmu.cs.lti.event_coref.model.graph.MentionGraphEdge;
import edu.cmu.cs.lti.event_coref.train.DelayedLaSOJointTrainer;
import edu.cmu.cs.lti.learning.model.*;
import edu.cmu.cs.lti.utils.DebugUtils;
import gnu.trove.map.TObjectDoubleMap;
import gnu.trove.map.hash.TObjectDoubleHashMap;
import org.javatuples.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

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

    private String corefName = DelayedLaSOJointTrainer.COREF_MODEL_NAME;
    private String mentionName = DelayedLaSOJointTrainer.TYPE_MODEL_NAME;

    public DiscriminativeUpdater() {
        allWeights = new HashMap<>();
        allDelta = new HashMap<>();
        allLoss = new TObjectDoubleHashMap<>();
        classAlphabets = new HashMap<>();
        featureAlphabets = new HashMap<>();
//        trainingStats = new HashMap<>();
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

    public void recordLaSOUpdate(LabelLinkAgenda decodingAgenda, LabelLinkAgenda goldAgenda) {
        if (!decodingAgenda.contains(goldAgenda)) {
            logger.debug("Recording differences for LaSO update.");
            recordUpdate(decodingAgenda, goldAgenda);

            NodeLinkingState bestDecoding = decodingAgenda.getBeamStates().peek();
            NodeLinkingState bestGold = goldAgenda.getBeamStates().peek();

//            logger.debug(String.valueOf(decodingAgenda.getBeamStates().size()));

//            logger.debug("Agenda not matching.");
//
//            logger.debug("Decoding agenda is :");
//            logger.debug(decodingAgenda.showAgendaItems());
//
//            logger.debug("Gold agenda is :");
//            logger.debug(goldAgenda.showAgendaItems());

            logger.debug("Compute losses between best decoding and best gold.");
            Pair<Double, Double> losses = bestDecoding.loss(bestGold);

            logger.debug("Loss value is " + losses);

            addLoss(DelayedLaSOJointTrainer.TYPE_MODEL_NAME, losses.getValue0());
            addLoss(DelayedLaSOJointTrainer.COREF_MODEL_NAME, losses.getValue1());

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
        if (!decodingAgenda.getBeamStates().peek().match(goldAgenda.getBeamStates().peek())) {
            logger.debug("Recording final update difference between the top states.");
            recordUpdate(decodingAgenda, goldAgenda);
        }
    }

    private void recordUpdate(LabelLinkAgenda decodingAgenda, LabelLinkAgenda goldAgenda) {
        logger.debug("Best decoding state is:");
        logger.debug(decodingAgenda.getBeamStates().peek().showTree());

        logger.debug("Best gold state is:");
        logger.debug(goldAgenda.getBeamStates().peek().showTree());

        // Compute delta on coreferecence.
        GraphFeatureVector deltaCorefVector = allDelta.get(corefName);

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

        GraphFeatureVector deltaMentionVector = allDelta.get(mentionName);
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

    public double update(String name) {
        // Update and then clear accumulated stuff.
        if (name.equals(mentionName)) {
            vanillaUpdate(name);
        } else {
            paUpdate(name);
        }

        // Logging the loss.
        double loss = allLoss.get(name);

        allDelta.put(name, newDelta(name));
        allLoss.put(name, 0);

        return loss;
    }

    public void vanillaUpdate(String name) {
        GraphFeatureVector delta = allDelta.get(name);
        GraphWeightVector weights = allWeights.get(name);
        weights.updateWeightsBy(delta, defaultStepSize);
        weights.updateAverageWeights();
    }

    public double paUpdate(String name) {
//        logger.info("PA Updating " + name);

        GraphFeatureVector delta = allDelta.get(name);
        double loss = allLoss.get(name);

        if (loss != 0) {
            double l2 = delta.getFeatureL2();
            double tau = loss / l2;

            // Sometimes we saw INFINITY weight value, it is likely to be cause by a empty l2.
            if (l2 == 0) {
                logger.debug(delta.readableNodeVector());
                logger.debug("Loss is " + loss);
                logger.debug("Feature L2 is " + l2);
                logger.debug("Update features by " + tau);
                DebugUtils.pause(logger);
            }

            GraphWeightVector weights = allWeights.get(name);
            weights.updateWeightsBy(delta, tau);
            weights.updateAverageWeights();

//        weights.nodeWeightIterator().forEachRemaining(nodeWeightVector -> {
//            logger.debug("Weights for node " + classAlphabets.get(name).getClassName(nodeWeightVector.getValue0()));
//            logger.debug(nodeWeightVector.getValue1().toReadableString(featureAlphabets.get(name)));
//        });
//        DebugUtils.pause(logger);
        }
        return loss;
    }

    private GraphFeatureVector newDelta(String name) {
        return new GraphFeatureVector(classAlphabets.get(name), featureAlphabets.get(name));
    }

}
