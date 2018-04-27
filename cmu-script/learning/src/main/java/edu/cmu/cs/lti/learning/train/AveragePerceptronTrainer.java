package edu.cmu.cs.lti.learning.train;

import com.google.common.collect.HashBasedTable;
import edu.cmu.cs.lti.learning.decoding.SequenceDecoder;
import edu.cmu.cs.lti.learning.feature.extractor.ChainFeatureExtractor;
import edu.cmu.cs.lti.learning.model.*;
import edu.cmu.cs.lti.learning.update.SeqLoss;
import edu.cmu.cs.lti.learning.utils.CubicLagrangian;
import gnu.trove.map.TIntObjectMap;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileNotFoundException;

/**
 * Created with IntelliJ IDEA.
 * Date: 8/20/15
 * Time: 10:44 PM
 * <p>
 * Train an average perceptron model given a sequence decoder.
 *
 * @author Zhengzhong Liu
 */
public class AveragePerceptronTrainer {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private SequenceDecoder decoder;
    private GraphWeightVector weightVector;
    private double defaultStepSize;
    private ClassAlphabet classAlphabet;
    private boolean passiveAggressive;
    private SeqLoss seqLoss;

    /**
     * A vanilla average perceptron, with a fixed step size.
     *
     * @param decoder           The sequence decoder.
     * @param classAlphabet     The alphabet of possible output classes.
     * @param featureAlphabet   The alphabet of features.
     * @param featureSpec       The feature specifications.
     * @param passiveAggressive Whether to use passive aggressive.
     * @param lossType
     */
    public AveragePerceptronTrainer(SequenceDecoder decoder, ClassAlphabet classAlphabet,
                                    FeatureAlphabet featureAlphabet, String featureSpec, boolean passiveAggressive,
                                    String lossType) {

        this.decoder = decoder;
        weightVector = new GraphWeightVector(classAlphabet, featureAlphabet, featureSpec);
        this.defaultStepSize = 0.1;
        this.passiveAggressive = passiveAggressive;
        this.classAlphabet = classAlphabet;
        seqLoss = SeqLoss.getLoss(lossType);
    }

    public double trainNext(SequenceSolution goldSolution, GraphFeatureVector goldFv, ChainFeatureExtractor
            extractor, CubicLagrangian u, CubicLagrangian v, TIntObjectMap<Pair<FeatureVector,
            HashBasedTable<Integer, Integer, FeatureVector>>> featureCache) {
        decoder.decode(extractor, weightVector, goldSolution.getSequenceLength(), u, v, featureCache);
        SequenceSolution prediction = decoder.getDecodedPrediction();
        double loss = seqLoss.compute(goldSolution.asIntArray(), prediction.asIntArray(),
                classAlphabet.getNoneOfTheAboveClassIndex());

//        logger.debug("Prediction: ");
//        logger.debug(prediction.toString());
//        logger.debug("Gold: ");
//        logger.debug(goldSolution.toString());
//        logger.debug("Loss is " + loss);

        if (loss != 0) {
            GraphFeatureVector bestDecodingFeatures = decoder.getBestDecodingFeatures();
            GraphFeatureVector delta = goldFv.newGraphFeatureVector();
            goldFv.diff(bestDecodingFeatures, delta);

            if (passiveAggressive) {
                double l2 = delta.getFeatureL2();
                double tau = loss / l2;
                updateWeights(goldFv, bestDecodingFeatures, tau);
            } else {
                updateWeights(goldFv, bestDecodingFeatures, defaultStepSize);
            }
        }

//        DebugUtils.pause(logger);

        return loss;
    }

    private void updateWeights(GraphFeatureVector goldFv, GraphFeatureVector predictedFv, double stepSize) {
        weightVector.updateWeightsBy(goldFv, stepSize);
        weightVector.updateWeightsBy(predictedFv, -stepSize);
        weightVector.updateAverageWeights();
    }

    public void write(File outputFile) throws FileNotFoundException {
        weightVector.write(outputFile);
    }
}
