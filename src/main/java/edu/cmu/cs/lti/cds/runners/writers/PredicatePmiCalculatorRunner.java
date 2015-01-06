package edu.cmu.cs.lti.cds.runners.writers;

import edu.cmu.cs.lti.cds.annotators.writers.PredicatePmiCalculator;
import edu.cmu.cs.lti.cds.utils.DataPool;
import edu.cmu.cs.lti.uima.io.reader.CustomCollectionReaderFactory;
import edu.cmu.cs.lti.uima.io.writer.CustomAnalysisEngineFactory;
import edu.cmu.cs.lti.utils.Configuration;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.collection.CollectionReaderDescription;
import org.apache.uima.fit.pipeline.SimplePipeline;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.uimafit.factory.TypeSystemDescriptionFactory;

import java.io.File;

/**
 * Created with IntelliJ IDEA.
 * User: zhengzhongliu
 * Date: 12/18/14
 * Time: 8:57 PM
 */
public class PredicatePmiCalculatorRunner {
    private static String className = PredicatePmiCalculatorRunner.class.getSimpleName();

    /**
     * @param args
     * @throws java.io.IOException
     * @throws org.apache.uima.UIMAException
     */
    public static void main(String[] args) throws Exception {
        System.out.println(className + " started...");

        Configuration config = new Configuration(new File(args[0]));

        String inputDir = config.get("edu.cmu.cs.lti.cds.event_tuple.path"); //"data/02_event_tuples";
        String blackListFile = config.get("edu.cmu.cs.lti.cds.blacklist"); //"duplicate.count.tail"

        DataPool.readBlackList(new File(blackListFile));
        DataPool.loadHeadStatistics(config, true);

        String paramTypeSystemDescriptor = "TypeSystem";

        // Instantiate the analysis engine.
        TypeSystemDescription typeSystemDescription = TypeSystemDescriptionFactory
                .createTypeSystemDescription(paramTypeSystemDescriptor);

        CollectionReaderDescription reader =
                CustomCollectionReaderFactory.createRecursiveGzippedXmiReader(typeSystemDescription, inputDir, false);

        AnalysisEngineDescription pmiCounter = CustomAnalysisEngineFactory.createAnalysisEngine(
                PredicatePmiCalculator.class, typeSystemDescription,
                PredicatePmiCalculator.PARAM_PARENT_OUTPUT_DIR, "data",
                PredicatePmiCalculator.PARAM_BASE_OUTPUT_DIR_NAME, "predicate_pmi",
                PredicatePmiCalculator.PARAM_STEP_NUMBER, 1
        );

        SimplePipeline.runPipeline(reader, pmiCounter);
        System.out.println(className + " completed.");
    }
}
