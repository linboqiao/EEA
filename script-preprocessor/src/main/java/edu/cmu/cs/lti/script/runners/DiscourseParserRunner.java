package edu.cmu.cs.lti.script.runners;

import edu.cmu.cs.lti.annotators.DiscourseParserAnnotator;
import edu.cmu.cs.lti.uima.io.reader.CustomCollectionReaderFactory;
import edu.cmu.cs.lti.uima.io.writer.CustomAnalysisEngineFactory;
import org.apache.uima.UIMAException;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.collection.CollectionReaderDescription;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.pipeline.SimplePipeline;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.uimafit.factory.TypeSystemDescriptionFactory;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created with IntelliJ IDEA.
 * User: zhengzhongliu
 * Date: 9/30/14
 * Time: 9:37 PM
 */
public class DiscourseParserRunner {
    private static String className = DiscourseParserRunner.class.getSimpleName();

    static Logger logger = Logger.getLogger(className);

    /**
     * @param args
     * @throws java.io.IOException
     * @throws org.apache.uima.UIMAException
     */
    public static void main(String[] args) throws UIMAException, IOException {
        logger.log(Level.INFO, className + " started...");

        if (args.length < 1) {
            logger.log(Level.INFO, "Please provide input parent and base directory");
            System.exit(1);
        }

        String parentInputDir = args[1];
        String baseInputDir = args[0]; //"data/01_event_tuples";

        String paramBaseOutputDirName = "discourse_parsed"; //"discourse_parsed";

        Integer inputStepNum = null;
        Integer outputStepNum = null;
        if (args.length >= 3) {
            inputStepNum = Integer.parseInt(args[1]);
            outputStepNum = inputStepNum + 1;
        }

        // Parameters for the writer
        String paramOutputFileSuffix = null;

        String paramTypeSystemDescriptor = "TypeSystem";


        // Instantiate the analysis engine.
        TypeSystemDescription typeSystemDescription = TypeSystemDescriptionFactory
                .createTypeSystemDescription(paramTypeSystemDescriptor);

        // Instantiate a collection reader to get XMI as input.
        // Note that you should change the following parameters for your setting.
        CollectionReaderDescription reader =
                CustomCollectionReaderFactory.createTimeSortedGzipXmiReader(typeSystemDescription, parentInputDir, baseInputDir);

        AnalysisEngineDescription discourseParser = AnalysisEngineFactory.createEngineDescription(
                DiscourseParserAnnotator.class, typeSystemDescription);

        AnalysisEngineDescription writer = CustomAnalysisEngineFactory.createGzipWriter(
                parentInputDir, paramBaseOutputDirName, outputStepNum, paramOutputFileSuffix, null);

        SimplePipeline.runPipeline(reader, discourseParser, writer);
    }
}