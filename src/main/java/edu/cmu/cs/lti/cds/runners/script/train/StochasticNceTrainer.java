package edu.cmu.cs.lti.cds.runners.script.train;

import edu.cmu.cs.lti.cds.annotators.script.train.NceTrainer;
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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.logging.Logger;

/**
 * Created with IntelliJ IDEA.
 * User: zhengzhongliu
 * Date: 11/3/14
 * Time: 1:46 PM
 */
public class StochasticNceTrainer {
    private static Logger logger = Logger.getLogger(StochasticNceTrainer.class.getName());

    public static BufferedWriter trainOut;

    public static void main(String[] args) throws Exception {
        Configuration config = new Configuration(new File(args[0]));
        String inputDir = config.get("edu.cmu.cs.lti.cds.event_tuple.path");
        int maxIter = config.getInt("edu.cmu.cs.lti.cds.sgd.iter");
        String[] dbNames = config.getList("edu.cmu.cs.lti.cds.db.basenames"); //db names;
        String dbPath = config.get("edu.cmu.cs.lti.cds.dbpath"); //"dbpath"
        String[] countingDbFileNames = config.getList("edu.cmu.cs.lti.cds.headcount.files");
        String blackListFileName = config.get("edu.cmu.cs.lti.cds.blacklist");
        String modelStoragePath = config.get("edu.cmu.cs.lti.cds.nce.model.path");

        String paramTypeSystemDescriptor = "TypeSystem";

        //prepare data
        logger.info("Loading data");
        DataPool.loadHeadIds(dbPath, dbNames[0], KarlMooneyScriptCounter.defaltHeadIdMapName);
        DataPool.loadHeadCounts(dbPath, countingDbFileNames, true);
        DataPool.readBlackList(new File(blackListFileName));
        logger.info("# predicates " + DataPool.headIdMap.size());

        // Instantiate the analysis engine.
        TypeSystemDescription typeSystemDescription = TypeSystemDescriptionFactory
                .createTypeSystemDescription(paramTypeSystemDescriptor);

        CollectionReaderDescription reader =
                CustomCollectionReaderFactory.createRecursiveGzippedXmiReader(typeSystemDescription, inputDir, false);

        AnalysisEngineDescription trainer = CustomAnalysisEngineFactory.createAnalysisEngine(NceTrainer.class, typeSystemDescription);

        trainOut = new BufferedWriter(new FileWriter(new File("nce_train_out")));

        //possibly iterate this step
        for (int i = 0; i < maxIter; i++) {
            Utils.printMemInfo(logger);

            SimplePipeline.runPipeline(reader, trainer);
            File modelDirParent = new File(modelStoragePath).getParentFile();

            if (!modelDirParent.exists()) {
                modelDirParent.mkdirs();
            }

            SerializationHelper.write(modelStoragePath + i + ".ser", DataPool.weights);
        }

        try {
            StochasticNceTrainer.trainOut.write("Finished!");
            StochasticNceTrainer.trainOut.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}