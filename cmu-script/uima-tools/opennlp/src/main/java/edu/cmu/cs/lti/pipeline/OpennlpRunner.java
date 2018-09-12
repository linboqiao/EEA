package edu.cmu.cs.lti.pipeline;

import edu.cmu.cs.lti.annotators.OpenNlpChunker;
import edu.cmu.cs.lti.uima.io.reader.CustomCollectionReaderFactory;
import edu.cmu.cs.lti.uima.io.writer.CustomAnalysisEngineFactory;
import org.apache.uima.UIMAException;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.collection.CollectionReaderDescription;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.factory.TypeSystemDescriptionFactory;
import org.apache.uima.fit.pipeline.SimplePipeline;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * Created with IntelliJ IDEA.
 * User: zhengzhongliu
 * Date: 9/30/14
 * Time: 9:37 PM
 */
public class OpennlpRunner {
    private static String className = OpennlpRunner.class.getSimpleName();

    static Logger logger = LoggerFactory.getLogger(className);

    /**
     * @param args
     * @throws IOException
     * @throws UIMAException
     */
    public static void main(String[] args) throws UIMAException, IOException {
        logger.info(className + " started...");

        if (args.length < 2) {
            logger.info("Please provide parent, base input directory");
            System.exit(1);
        }

        String parentInput = args[0]; //"data";

        // Parameters for the writer
        String baseInput = args[1]; //"01_event_tuples"

        String paramBaseOutputDirName = "opennlp_chunked";

        Integer outputStepNum = null;
        if (args.length >= 4) {
            outputStepNum = Integer.parseInt(args[3]);
        }

        String paramTypeSystemDescriptor = "TypeSystem";

        // Instantiate the analysis engine.
        TypeSystemDescription typeSystemDescription = TypeSystemDescriptionFactory
                .createTypeSystemDescription(paramTypeSystemDescriptor);

        // Instantiate a collection reader to get XMI as input.
        // Note that you should change the following parameters for your setting.
        CollectionReaderDescription reader = CustomCollectionReaderFactory.createXmiReader(
                typeSystemDescription, parentInput, baseInput);

        AnalysisEngineDescription opennlp = AnalysisEngineFactory.createEngineDescription(
                OpenNlpChunker.class, typeSystemDescription,
                OpenNlpChunker.PARAM_MODEL_PATH, "../models/opennlp/en-chunker.bin");

        AnalysisEngineDescription writer = CustomAnalysisEngineFactory.createXmiWriter(
                parentInput, paramBaseOutputDirName, outputStepNum);

        SimplePipeline.runPipeline(reader, opennlp, writer);
    }
}