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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.lang.reflect.InvocationTargetException;

/**
 * Created with IntelliJ IDEA.
 * Date: 8/23/15
 * Time: 3:05 PM
 *
 * @author Zhengzhong Liu
 */
public class CrfMentionTrainingLooper extends LoopPipeline {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private int maxIteration;
    private int numIteration;
    private String modelBasename;

    public CrfMentionTrainingLooper(String[] classes, Configuration kbpConfig,
                                    String modelOutputBasename, File cacheDirectory,
                                    TypeSystemDescription typeSystemDescription,
                                    CollectionReaderDescription readerDescription) throws
            ResourceInitializationException, ClassNotFoundException, NoSuchMethodException,
            InvocationTargetException, InstantiationException, IllegalAccessException {
        super(readerDescription, setup(typeSystemDescription, classes, cacheDirectory, kbpConfig));
        this.maxIteration = kbpConfig.getInt("edu.cmu.cs.lti.perceptron.maxiter", 20);
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

    private static AnalysisEngineDescription setup(TypeSystemDescription typeSystemDescription, String[] classes,
                                                   File cacheDir, Configuration kbpConfig) throws
            ResourceInitializationException, ClassNotFoundException, NoSuchMethodException, InstantiationException,
            IllegalAccessException, InvocationTargetException {
        MentionTypeCrfTrainer.setup(classes, cacheDir, kbpConfig);
        return AnalysisEngineFactory.createEngineDescription(MentionTypeCrfTrainer.class, typeSystemDescription,
                MentionTypeCrfTrainer.PARAM_GOLD_CACHE_DIRECTORY, cacheDir,
                MentionTypeCrfTrainer.PARAM_GOLD_STANDARD_VIEW_NAME, UimaConst.goldViewName
        );
    }
}
