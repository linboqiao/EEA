package edu.cmu.cs.lti.after.train;

import edu.cmu.cs.lti.event_coref.decoding.BFAfterLatentTreeDecoder;
import edu.cmu.cs.lti.learning.feature.mention_pair.extractor.PairFeatureExtractor;
import edu.cmu.cs.lti.learning.model.GraphWeightVector;
import edu.cmu.cs.lti.learning.model.MentionCandidate;
import edu.cmu.cs.lti.learning.model.TrainingStats;
import edu.cmu.cs.lti.learning.model.graph.MentionGraph;
import edu.cmu.cs.lti.learning.model.graph.MentionSubGraph;
import edu.cmu.cs.lti.learning.update.DiscriminativeUpdater;
import edu.cmu.cs.lti.learning.utils.LearningUtils;
import edu.cmu.cs.lti.uima.annotator.AbstractLoggingAnnotator;
import edu.cmu.cs.lti.uima.util.UimaConvenience;
import edu.cmu.cs.lti.utils.Configuration;
import edu.cmu.cs.lti.utils.MentionUtils;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.List;

import static edu.cmu.cs.lti.learning.model.ModelConstants.AFTER_MODEL_NAME;

/**
 * Train a pairwise based after linking model. zb
 * <p>
 * Date: 12/12/16
 * Time: 3:35 PM
 *
 * @author Zhengzhong Liu
 */
public class LatentTreeAfterTrainer extends AbstractLoggingAnnotator {
    private PairFeatureExtractor extractor;
    private static DiscriminativeUpdater updater;

    private BFAfterLatentTreeDecoder decoder;

    private TrainingStats trainingStats;


    public static final String PARAM_CONFIG_PATH = "configPath";
    @ConfigurationParameter(name = PARAM_CONFIG_PATH)
    private Configuration config;

    @Override
    public void initialize(UimaContext aContext) throws ResourceInitializationException {
        super.initialize(aContext);
        logger.info("Pairwise After Trainer Initializing...");

        updater = new DiscriminativeUpdater(false, true, true, null, 0, 0);

        String afterFeatureSpec = "edu.cmu.cs.lti.features.after.spec";

        GraphWeightVector weights = LearningUtils.preareGraphWeights(config, afterFeatureSpec);
        updater.addWeightVector(AFTER_MODEL_NAME, weights);

        extractor = LearningUtils.initializeMentionPairExtractor(config, afterFeatureSpec,
                weights.getFeatureAlphabet());

        int trainingStrategy = config.getInt("edu.cmu.cs.lti.after.train.strategy", 1);

        decoder = new BFAfterLatentTreeDecoder(trainingStrategy);
        trainingStats = new TrainingStats(5, "AfterLink");
        logger.info("After link decoder initialized.");
    }

    @Override
    public void process(JCas aJCas) throws AnalysisEngineProcessException {
        UimaConvenience.printProcessLog(aJCas, logger);

        extractor.initWorkspace(aJCas);

        List<MentionCandidate> candidates = MentionUtils.getSpanBasedCandidates(aJCas);

        MentionGraph mentionGraph = MentionUtils.createSpanBasedMentionGraph(aJCas, candidates, extractor, true);

//        logger.info("The mention graph is:");
//        System.out.println(mentionGraph.toString());
//
//        DebugUtils.pause();

        GraphWeightVector weights = updater.getWeightVector(AFTER_MODEL_NAME);

        MentionSubGraph predictedTree = decoder.decode(mentionGraph, candidates, weights, false);

//        logger.info("Predicted tree is :");
//        logger.info(predictedTree.fullTree());

        if (!predictedTree.graphMatch()) {
//            logger.info("Gold tree is :");

            MentionSubGraph latentTree = decoder.decode(mentionGraph, candidates, weights, true);

//            logger.info(latentTree.fullTree());

            double loss = predictedTree.paUpdate(latentTree, weights);

//            logger.info("Loss is " + loss);

            trainingStats.addLoss(logger, loss / mentionGraph.numNodes());
        }else{
            trainingStats.addLoss(logger, 0);
        }

//        DebugUtils.pause();
    }

    public static File saveModels(File modelOutputDirectory) throws FileNotFoundException {
        edu.cmu.cs.lti.utils.FileUtils.ensureDirectory(modelOutputDirectory);
        File modelOutput = new File(modelOutputDirectory, AFTER_MODEL_NAME);
        updater.getWeightVector(AFTER_MODEL_NAME).write(modelOutput);

        return modelOutput;
    }
}
