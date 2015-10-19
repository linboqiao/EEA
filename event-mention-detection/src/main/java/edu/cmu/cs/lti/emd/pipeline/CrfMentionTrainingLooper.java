package edu.cmu.cs.lti.emd.pipeline;

import edu.cmu.cs.lti.emd.annotators.crf.MentionTypeCrfTrainer;
import edu.cmu.cs.lti.model.UimaConst;
import edu.cmu.cs.lti.uima.pipeline.LoopPipeline;
import edu.cmu.cs.lti.utils.Configuration;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.collection.CollectionReaderDescription;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.metadata.TypeSystemDescription;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

/**
 * Created with IntelliJ IDEA.
 * Date: 8/23/15
 * Time: 3:05 PM
 *
 * @author Zhengzhong Liu
 */
public class CrfMentionTrainingLooper extends LoopPipeline {
    private int maxIteration;
    private int numIteration;
    private String modelBasename;

    public CrfMentionTrainingLooper(Configuration taskConfig, String modelOutputBasename,
                                    TypeSystemDescription typeSystemDescription,
                                    CollectionReaderDescription readerDescription, File cacheDir) throws
            ResourceInitializationException, ClassNotFoundException, NoSuchMethodException,
            InvocationTargetException, InstantiationException, IllegalAccessException, IOException {
        super(readerDescription, setup(typeSystemDescription, taskConfig, cacheDir));
        this.maxIteration = taskConfig.getInt("edu.cmu.cs.lti.perceptron.maxiter", 20);
        this.numIteration = 0;
        this.modelBasename = modelOutputBasename;
        logger.info("CRF mention trainer started, maximum iteration is " + maxIteration);
    }

    @Override
    protected boolean checkStopCriteria() {
        return numIteration >= maxIteration;
    }

    @Override
    protected void stopActions() {
        logger.info("Finalizing the training ...");
        try {
            logger.info("Saving final models at " + modelBasename);
            MentionTypeCrfTrainer.saveModels(new File(modelBasename));
            MentionTypeCrfTrainer.loopStopActions();
        } catch (java.io.IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void loopActions() {
        numIteration++;
        try {
            MentionTypeCrfTrainer.saveModels(new File(modelBasename + "_iter" + numIteration));
        } catch (java.io.IOException e) {
            e.printStackTrace();
        }
        logger.info(String.format("Iteration %d finished ...", numIteration));
    }

    private static AnalysisEngineDescription setup(TypeSystemDescription typeSystemDescription,
                                                   Configuration kbpConfig, File cacheDir) throws
            ResourceInitializationException, ClassNotFoundException, NoSuchMethodException, InstantiationException,
            IllegalAccessException, InvocationTargetException, IOException {
        return AnalysisEngineFactory.createEngineDescription(
                MentionTypeCrfTrainer.class, typeSystemDescription,
                MentionTypeCrfTrainer.PARAM_GOLD_STANDARD_VIEW_NAME, UimaConst.goldViewName,
                MentionTypeCrfTrainer.PARAM_CONFIGURATION_FILE, kbpConfig.getConfigFile(),
                MentionTypeCrfTrainer.PARAM_CACHE_DIRECTORY, cacheDir
        );
    }
}
