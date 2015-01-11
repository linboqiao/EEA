/**
 *
 */
package edu.cmu.cs.lti.script.runners.writers.eval;

import edu.cmu.cs.lti.script.annotators.writers.eval.KmStyleAllEventMentionClozeTaskGenerator;
import edu.cmu.cs.lti.script.utils.DataPool;
import edu.cmu.cs.lti.uima.io.reader.CustomCollectionReaderFactory;
import edu.cmu.cs.lti.uima.io.writer.CustomAnalysisEngineFactory;
import edu.cmu.cs.lti.utils.Configuration;
import org.apache.uima.UIMAException;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.collection.CollectionReaderDescription;
import org.apache.uima.fit.pipeline.SimplePipeline;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.uimafit.factory.TypeSystemDescriptionFactory;

import java.io.File;
import java.io.IOException;
import java.util.logging.Logger;

/**
 * @author zhengzhongliu
 */
public class KmStyleClozeWriter {
    private static String className = KmStyleClozeWriter.class.getSimpleName();

    static Logger logger = Logger.getLogger(className);

    /**
     * @param args
     * @throws java.io.IOException
     * @throws org.apache.uima.UIMAException
     */
    public static void main(String[] args) throws UIMAException, IOException {
        System.out.println(className + " started...");

        Configuration config = new Configuration(new File(args[0]));
        String subPath = args.length > 1 ? args[1] : "";

        String inputDir = config.get("edu.cmu.cs.lti.cds.event_tuple.heldout.path") + "/" + subPath; //"data/02_event_tuples";
        String paramParentOutputDir = config.get("edu.cmu.cs.lti.cds.parent.output"); // "data";
        String paramBaseOutputDirName = config.get("edu.cmu.cs.lti.cds.cloze.base") + "_" + subPath; // "cloze"
        String blackListFile = config.get("edu.cmu.cs.lti.cds.blacklist"); //"duplicate.count.tail"
        boolean ignoreLowFreq = config.getBoolean("edu.cmu.cs.lti.cds.filter.lowfreq");
        int clozeMinSize = config.getInt("edu.cmu.cs.lti.cds.cloze.minsize");

        String paramOutputFileSuffix = ".txt";

        int stepNum = 3;

        DataPool.readBlackList(new File(blackListFile));

        String paramTypeSystemDescriptor = "TypeSystem";

        // Instantiate the analysis engine.
        TypeSystemDescription typeSystemDescription = TypeSystemDescriptionFactory
                .createTypeSystemDescription(paramTypeSystemDescriptor);

        // Instantiate a collection reader to get XMI as input.
        // Note that you should change the following parameters for your setting.
        CollectionReaderDescription reader =
                CustomCollectionReaderFactory.createGzippedXmiReader(typeSystemDescription, inputDir, false);

        AnalysisEngineDescription writer = CustomAnalysisEngineFactory.createAnalysisEngine(
                KmStyleAllEventMentionClozeTaskGenerator.class, typeSystemDescription,
                KmStyleAllEventMentionClozeTaskGenerator.PARAM_BASE_OUTPUT_DIR_NAME, paramBaseOutputDirName,
                KmStyleAllEventMentionClozeTaskGenerator.PARAM_OUTPUT_FILE_SUFFIX, paramOutputFileSuffix,
                KmStyleAllEventMentionClozeTaskGenerator.PARAM_PARENT_OUTPUT_DIR, paramParentOutputDir,
                KmStyleAllEventMentionClozeTaskGenerator.PARAM_STEP_NUMBER, stepNum,
                KmStyleAllEventMentionClozeTaskGenerator.PARAM_DB_DIR_PATH, "data/_db/",
                KmStyleAllEventMentionClozeTaskGenerator.PARAM_IGNORE_LOW_FREQ, ignoreLowFreq,
                KmStyleAllEventMentionClozeTaskGenerator.PARAM_CLOZE_MIN_SIZE, clozeMinSize
        );

        SimplePipeline.runPipeline(reader, writer);

        logger.info("Completed.");
        logger.info("Remeber to clean out empty cloze using the shell scripts");
    }
}