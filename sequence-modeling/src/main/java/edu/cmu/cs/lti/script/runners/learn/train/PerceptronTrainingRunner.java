package edu.cmu.cs.lti.script.runners.learn.train;

import com.google.common.base.Joiner;
import edu.cmu.cs.lti.collections.TLongShortDoubleHashTable;
import edu.cmu.cs.lti.script.annotators.learn.train.CompactGlobalNegativeTrainer;
import edu.cmu.cs.lti.script.annotators.learn.train.KarlMooneyScriptCounter;
import edu.cmu.cs.lti.script.annotators.learn.train.PerceptronTraining;
import edu.cmu.cs.lti.script.utils.DataPool;
import edu.cmu.cs.lti.uima.io.reader.CustomCollectionReaderFactory;
import edu.cmu.cs.lti.uima.io.writer.CustomAnalysisEngineFactory;
import edu.cmu.cs.lti.utils.Configuration;
import edu.cmu.cs.lti.utils.Utils;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.collection.CollectionReaderDescription;
import org.apache.uima.fit.pipeline.SimplePipeline;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.uimafit.factory.TypeSystemDescriptionFactory;
import weka.core.SerializationHelper;

import java.io.File;
import java.util.logging.Logger;

/**
 * Created with IntelliJ IDEA.
 * User: zhengzhongliu
 * Date: 1/11/15
 * Time: 11:29 PM
 */
public class PerceptronTrainingRunner {
    private static Logger logger = Logger.getLogger(StochasticGlobalNegativeTrainer.class.getName());

    public static void main(String[] args) throws Exception {
        Configuration config = new Configuration(new File(args[0]));
        String inputDir = config.get("edu.cmu.cs.lti.cds.event_tuple.path");
        int maxIter = config.getInt("edu.cmu.cs.lti.cds.sgd.iter");
        String[] dbNames = config.getList("edu.cmu.cs.lti.cds.db.basenames"); //db names;
        String dbPath = config.get("edu.cmu.cs.lti.cds.dbpath"); //"dbpath"
        String blackListFileName = config.get("edu.cmu.cs.lti.cds.blacklist");
        String modelStoragePath = config.get("edu.cmu.cs.lti.cds.perceptron.model.path");
        int miniBatchNum = config.getInt("edu.cmu.cs.lti.cds.minibatch");
        String modelExt = config.get("edu.cmu.cs.lti.cds.model.ext");
        String[] featureNames = config.getList("edu.cmu.cs.lti.cds.features");
        String featurePackage = config.get("edu.cmu.cs.lti.cds.features.packagename");
        int skipgramN = config.getInt("edu.cmu.cs.lti.cds.skipgram.n");

        int rankListSize = config.getInt("edu.cmu.cs.lti.cds.perceptron.ranklist.size");

        String modelSuffix = Joiner.on("_").join(featureNames);

        //make complete class name
        for (int i = 0; i < featureNames.length; i++) {
            featureNames[i] = featurePackage + "." + featureNames[i];
        }

        String paramTypeSystemDescriptor = "TypeSystem";

        //prepare data
        logger.info("Loading data");
        DataPool.loadHeadStatistics(config, false);
        DataPool.readBlackList(new File(blackListFileName));
        DataPool.loadKmCooccMap(dbPath, dbNames[0], KarlMooneyScriptCounter.defaultCooccMapName);
        DataPool.loadEventUnigramCounts(config);

        logger.info("# predicates " + DataPool.headIdMap.size());

        // Instantiate the analysis engine.
        TypeSystemDescription typeSystemDescription = TypeSystemDescriptionFactory
                .createTypeSystemDescription(paramTypeSystemDescriptor);

        CollectionReaderDescription reader =
                CustomCollectionReaderFactory.createRecursiveGzippedXmiReader(typeSystemDescription, inputDir, false);

        logger.info("Running " + CompactGlobalNegativeTrainer.class.getName());

        AnalysisEngineDescription trainer = CustomAnalysisEngineFactory.createAnalysisEngine(PerceptronTraining.class, typeSystemDescription,
                PerceptronTraining.PARAM_RANK_LIST_SIZE, rankListSize,
                PerceptronTraining.PARAM_MINI_BATCH_SIZE, miniBatchNum,
                PerceptronTraining.PARAM_FEATURE_NAMES, featureNames,
                PerceptronTraining.PARAM_SKIP_GRAM_N, skipgramN);

        Utils.printMemInfo(logger, "Beginning memory");

        for (int i = 0; i < maxIter; i++) {
            String modelOutputPath = modelStoragePath + "_" + modelSuffix + "_" + i + modelExt;
            String averageModelOutputPath = modelOutputPath + "_average";


            SimplePipeline.runPipeline(reader, trainer);
            File modelDirParent = new File(modelStoragePath).getParentFile();

            if (!modelDirParent.exists()) {
                modelDirParent.mkdirs();
            }

            TLongShortDoubleHashTable averageParameters = PerceptronTraining.sumOfFeatures;
            averageParameters.multiplyBy(1.0 / PerceptronTraining.numSamplesProcessed);

            logger.info("Storing this model to " + modelOutputPath);
            SerializationHelper.write(modelOutputPath, PerceptronTraining.trainingFeatureTable);
            logger.info("Storing the averaged model to " + modelOutputPath);
            SerializationHelper.write(averageModelOutputPath, averageParameters);
        }
    }
}
