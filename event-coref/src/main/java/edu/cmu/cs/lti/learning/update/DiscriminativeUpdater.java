package edu.cmu.cs.lti.learning.update;

import edu.cmu.cs.lti.learning.model.*;
import edu.cmu.cs.lti.learning.model.decoding.JointLabelLinkAgenda;
import edu.cmu.cs.lti.learning.model.decoding.NodeLinkingState;
import edu.cmu.cs.lti.learning.model.graph.EdgeType;
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

    private SeqLoss labelLosser;

    private boolean usePa;

    private String[] modelNames = new String[]{TYPE_MODEL_NAME, COREF_MODEL_NAME};

    private boolean updateMention;
    private boolean updateCoref;

    public DiscriminativeUpdater(boolean updateMention, boolean updateCoref, boolean usePaUpdate, String lossType) {
        allWeights = new HashMap<>();
        allDelta = new HashMap<>();
        allLoss = new TObjectDoubleHashMap<>();
        classAlphabets = new HashMap<>();
        featureAlphabets = new HashMap<>();
        labelLosser = SeqLoss.getLoss(lossType);
//        trainingStats = new HashMap<>();

        if (!(updateMention || updateCoref)) {
            throw new IllegalArgumentException("Cannot use a updater without updating anything.");
        }

        usePa = usePaUpdate;

        this.updateMention = updateMention;
        this.updateCoref = updateCoref;
    }

    public void addWeightVector(String name, GraphWeightVector weightVector) {
        allWeights.put(name, weightVector);
        classAlphabets.put(name, weightVector.getClassAlphabet());
        featureAlphabets.put(name, weightVector.getFeatureAlphabet());
        allDelta.put(name, newDelta(name));
        allLoss.put(name, 0);

        logger.info("Adding weight vector " + name);
    }

    public GraphWeightVector getWeightVector(String weightKey) {
        return allWeights.get(weightKey);
    }

    public void recordLaSOUpdate(JointLabelLinkAgenda decodingAgenda, JointLabelLinkAgenda goldAgenda) {
        if (!decodingAgenda.contains(goldAgenda)) {
//            logger.info("Recording differences for LaSO update.");
            recordUpdate(decodingAgenda, goldAgenda);

//            NodeLinkingState bestDecoding = decodingAgenda.getBeamStates().get(0);
//            NodeLinkingState bestGold = goldAgenda.getBeamStates().get(0);
//
//            Pair<Double, Double> losses = bestDecoding.loss(bestGold, labelLosser);
//
//            addLoss(TYPE_MODEL_NAME, losses.getLeft());
//            addLoss(COREF_MODEL_NAME, losses.getRight());

//            logger.debug("Label loss is " + losses.getLeft());

            // Copy the gold agenda to decoding agenda (LaSO)
            decodingAgenda.copyFrom(goldAgenda);

            // Clear these features from both agenda, since they are assumed to be the same from here.
            goldAgenda.clearFeatures();
            decodingAgenda.clearFeatures();
        }
    }

    public void recordFinalUpdate(JointLabelLinkAgenda decodingAgenda, JointLabelLinkAgenda goldAgenda) {
        if (!decodingAgenda.getBestBeamState().match(goldAgenda.getBestBeamState())) {
//            logger.debug("Recording final update difference between the top states.");
            recordUpdate(decodingAgenda, goldAgenda);
        }
    }

    private void recordUpdate(JointLabelLinkAgenda decodingAgenda, JointLabelLinkAgenda goldAgenda) {
//        logger.info("Best decoding state is:");
//        logger.info(decodingAgenda.getBestBeamState().toString());
//
//        logger.info("Best gold state is:");
//        logger.info(goldAgenda.getBestBeamState().toString());

        NodeLinkingState bestDecoding = decodingAgenda.getBestBeamState();
        NodeLinkingState bestGold = goldAgenda.getBestBeamState();

        Pair<Double, Double> losses = bestDecoding.loss(bestGold, labelLosser);

        addLoss(TYPE_MODEL_NAME, losses.getLeft());
        addLoss(COREF_MODEL_NAME, losses.getRight());

        // Compute delta on coreferecence.
        if (updateCoref) {
            GraphFeatureVector deltaCorefVector = allDelta.get(COREF_MODEL_NAME);
//            logger.debug("Computing delta on gold and decoding.");

            for (Map.Entry<EdgeType, FeatureVector> goldFv : goldAgenda.getBestDeltaCorefVectors().entrySet()) {
                deltaCorefVector.extend(goldFv.getValue(), goldFv.getKey().name());
//                logger.debug("Gold feature edge type : " + goldFv.getKey());
//                logger.debug(goldFv.getValue().readableString());
            }
            for (Map.Entry<EdgeType, FeatureVector> decoding : decodingAgenda.getBestDeltaCorefVectors().entrySet()) {
                deltaCorefVector.extend(decoding.getValue().negation(), decoding.getKey().name());
//                logger.debug("System feature edge type : " + decoding.getKey());
//                logger.debug(decoding.getValue().readableString());
            }

//            logger.info("Coreference delta is.");
//            logger.info(deltaCorefVector.readableNodeVector());
//            logger.info("Coref loss is " + losses.getRight() + " new loss is now " + allLoss.get(COREF_MODEL_NAME));

            if (!updateMention) {
                if (deltaCorefVector.getFeatureL2() == 0) {
                    double loss = decodingAgenda.getBestBeamState().getDecodingTree().getLoss(goldAgenda
                            .getBestBeamState().getDecodingTree());

                    logger.warn("Loss is  " + loss + " but L2 is 0 for coref vector, features are not good enough.");

                    logger.warn("Best Decoding is : ");
                    logger.warn(decodingAgenda.getBestBeamState().toString());

                    logger.warn("Gold is : ");
                    logger.warn(goldAgenda.getBestBeamState().toString());

                    logger.warn("Delta coref vector is :");
                    logger.warn(deltaCorefVector.readableNodeVector());
                }
            }
        }

        if (updateMention) {
            // Compute delta on labeling.
            GraphFeatureVector goldLabelFeature = goldAgenda.getBestDeltaLabelFv();
            GraphFeatureVector decodingLabelFeature = decodingAgenda.getBestDeltaLabelFv();

//        logger.debug("Showing mention features from  gold");
//        logger.debug(goldLabelFeature.readableNodeVector());
//        logger.debug("Showing mention features from  decoding");
//        logger.debug(decodingLabelFeature.readableNodeVector());

            GraphFeatureVector deltaMentionVector = allDelta.get(TYPE_MODEL_NAME);
            deltaMentionVector.extend(goldLabelFeature);
            deltaMentionVector.extend(decodingLabelFeature, -1);

//            logger.debug("Record the delta");
//            logger.debug("Current delta:");
//            logger.debug(deltaMentionVector.readableNodeVector());

        }
    }

    private void addLoss(String name, double loss) {
        allLoss.adjustValue(name, loss);
//        logger.debug("Loss for " + name + " is " + loss);
    }

    public TObjectDoubleMap<String> update() {
        // Update and then clear accumulated stuff.
        updateInternal(usePa);

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
        GraphFeatureVector corefDelta = allDelta.get(COREF_MODEL_NAME);
        GraphFeatureVector mentionDelta = allDelta.get(TYPE_MODEL_NAME);


        double totalLoss = 0;

        // So when we update one model, we will only add the loss on its part, which literally ignore the other one.
        if (updateCoref){
            totalLoss +=allLoss.get(COREF_MODEL_NAME);
        }
        if (updateMention){
            totalLoss += allLoss.get(TYPE_MODEL_NAME);
        }

        if (totalLoss != 0) {
            double tau = defaultStepSize;

            boolean isValid = true;
            if (paUpdate) {
                double corefL2 = updateCoref ? corefDelta.getFeatureL2() : 0;
                double mentionL2 = updateMention ? mentionDelta.getFeatureL2() : 0;
                double l2 = Math.sqrt(Math.pow(corefL2, 2) + Math.pow(mentionL2, 2));

                tau = totalLoss / l2;

                if (l2 == 0) {
                    //Make sure we don't update when the features are the same, but decoding results are different.
                    isValid = false;
                }
//                logger.info("Total loss is " + totalLoss + ", L2 is " + l2);
            }

//            logger.info("Update with step size " + tau );
//            logger.info("Coref delta is:");
//            logger.info(corefDelta.readableNodeVector());
            if (isValid) {
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
    }

    private GraphFeatureVector newDelta(String name) {
        return new GraphFeatureVector(classAlphabets.get(name), featureAlphabets.get(name));
    }

}
