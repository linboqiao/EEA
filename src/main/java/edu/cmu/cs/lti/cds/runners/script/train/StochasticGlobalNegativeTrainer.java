package edu.cmu.cs.lti.cds.runners.script.train;

import edu.cmu.cs.lti.cds.annotators.script.train.CompactGlobalNegativeTrainer;
import edu.cmu.cs.lti.cds.annotators.script.train.CompactNegativeTrainer;
import edu.cmu.cs.lti.cds.annotators.script.train.KarlMooneyScriptCounter;
import edu.cmu.cs.lti.cds.utils.DataPool;
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
 * Date: 11/3/14
 * Time: 1:46 PM
 */
public class StochasticGlobalNegativeTrainer {
    private static Logger logger = Logger.getLogger(StochasticGlobalNegativeTrainer.class.getName());

    public static void main(String[] args) throws Exception {
        Configuration config = new Configuration(new File(args[0]));
        String inputDir = config.get("edu.cmu.cs.lti.cds.event_tuple.path");
        int maxIter = config.getInt("edu.cmu.cs.lti.cds.sgd.iter");
        String[] dbNames = config.getList("edu.cmu.cs.lti.cds.db.basenames"); //db names;
        String dbPath = config.get("edu.cmu.cs.lti.cds.dbpath"); //"dbpath"
        String[] countingDbFileNames = config.getList("edu.cmu.cs.lti.cds.headcount.files");
        String blackListFileName = config.get("edu.cmu.cs.lti.cds.blacklist");
        String modelStoragePath = config.get("edu.cmu.cs.lti.cds.negative.model.path");
        int noiseNum = config.getInt("edu.cmu.cs.lti.cds.negative.noisenum");
        int miniBatchNum = config.getInt("edu.cmu.cs.lti.cds.minibatch");
        String modelSuffix = config.get("edu.cmu.cs.lti.cds.model.suffix");

        String paramTypeSystemDescriptor = "TypeSystem";

        //prepare data
        logger.info("Loading data");
        DataPool.loadHeadIds(dbPath, dbNames[0], KarlMooneyScriptCounter.defaltHeadIdMapName);
        DataPool.loadHeadCounts(dbPath, countingDbFileNames);
        DataPool.readBlackList(new File(blackListFileName));
        DataPool.loadCooccMap(dbPath, dbNames[0], KarlMooneyScriptCounter.defaultCooccMapName);
        logger.info("# predicates " + DataPool.headIdMap.size());

        // Instantiate the analysis engine.
        TypeSystemDescription typeSystemDescription = TypeSystemDescriptionFactory
                .createTypeSystemDescription(paramTypeSystemDescriptor);

        CollectionReaderDescription reader =
                CustomCollectionReaderFactory.createRecursiveGzippedXmiReader(typeSystemDescription, inputDir, false);

        AnalysisEngineDescription trainer = CustomAnalysisEngineFactory.createAnalysisEngine(CompactNegativeTrainer.class, typeSystemDescription,
                CompactGlobalNegativeTrainer.PARAM_NEGATIVE_NUMBERS, noiseNum,
                CompactGlobalNegativeTrainer.PARAM_MINI_BATCH_SIZE, miniBatchNum);

        Utils.printMemInfo(logger, "Beginning memory");

        //possibly iterate this step
        for (int i = 0; i < maxIter; i++) {
            SimplePipeline.runPipeline(reader, trainer);
            File modelDirParent = new File(modelStoragePath).getParentFile();

            if (!modelDirParent.exists()) {
                modelDirParent.mkdirs();
            }

            SerializationHelper.write(modelStoragePath + i + modelSuffix, DataPool.compactWeights);
        }
    }
}