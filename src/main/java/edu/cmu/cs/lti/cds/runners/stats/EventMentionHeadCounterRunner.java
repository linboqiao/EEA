package edu.cmu.cs.lti.cds.runners.stats;

import edu.cmu.cs.lti.cds.annotators.stats.EventMentionHeadCounter;
import edu.cmu.cs.lti.cds.utils.DataPool;
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

/**
 * Created with IntelliJ IDEA.
 * User: zhengzhongliu
 * Date: 10/25/14
 * Time: 5:30 PM
 */
public class EventMentionHeadCounterRunner {
    private static String className = EventMentionHeadCounterRunner.class.getSimpleName();

    /**
     * @param args
     * @throws java.io.IOException
     * @throws org.apache.uima.UIMAException
     */
    public static void main(String[] args) throws UIMAException, IOException {
        System.out.println(className + " started...");

        Configuration config = new Configuration(new File(args[0]));

        String inputDir = config.get("edu.cmu.cs.lti.cds.event_tuple.path"); //"data/02_event_tuples";
        String dbPath = config.get("edu.cmu.cs.lti.cds.dbpath");
        String blackListFile = config.get("edu.cmu.cs.lti.cds.blacklist"); //"duplicate.count.tail"

        String paramTypeSystemDescriptor = "TypeSystem";

        DataPool.readBlackList(new File(blackListFile));

        // Instantiate the analysis engine.
        TypeSystemDescription typeSystemDescription = TypeSystemDescriptionFactory
                .createTypeSystemDescription(paramTypeSystemDescriptor);

        CollectionReaderDescription reader =
                CustomCollectionReaderFactory.createRecursiveGzippedXmiReader(typeSystemDescription, inputDir, false);

        AnalysisEngineDescription kmScriptCounter = CustomAnalysisEngineFactory.createAnalysisEngine(
                EventMentionHeadCounter.class, typeSystemDescription,
                EventMentionHeadCounter.PARAM_DB_DIR_PATH, dbPath,
                EventMentionHeadCounter.PARAM_KEEP_QUIET, false);

        SimplePipeline.runPipeline(reader, kmScriptCounter);

        System.out.println(className + " completed.");
    }
}
