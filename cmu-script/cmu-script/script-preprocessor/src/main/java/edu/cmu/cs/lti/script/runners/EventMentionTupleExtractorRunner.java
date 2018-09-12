package edu.cmu.cs.lti.script.runners; /**
 *
 */

import edu.cmu.cs.lti.script.annotators.*;
import edu.cmu.cs.lti.script.annotators.patches.HeadWordFixer;
import edu.cmu.cs.lti.script.type.Entity;
import edu.cmu.cs.lti.uima.annotator.AbstractLoggingAnnotator;
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

/**
 * @author zhengzhongliu
 */
public class EventMentionTupleExtractorRunner {
    private static String className = EventMentionTupleExtractorRunner.class.getSimpleName();

    /**
     * @param args
     * @throws java.io.IOException
     * @throws org.apache.uima.UIMAException
     */
    public static void main(String[] args) throws UIMAException, IOException {
        System.out.println(className + " started...");

        // Parameters for the reader
        String parentInput = args[0]; //"data"
        String baseInput = args[1]; //"01_discourse_parsed";

        String parentDir = "data";

        String paramBaseOutputDirName = args[1];
        String paramOutputFileSuffix = null;
        int stepnum = 2;

        String paramTypeSystemDescriptor = "TypeSystem";

        // Instantiate the analysis engine.
        TypeSystemDescription typeSystemDescription = TypeSystemDescriptionFactory
                .createTypeSystemDescription(paramTypeSystemDescriptor);

        CollectionReaderDescription reader =
                CustomCollectionReaderFactory.createTimeSortedGzipXmiReader(typeSystemDescription, parentInput, baseInput);

        AnalysisEngineDescription fixer = AnalysisEngineFactory.createEngineDescription(HeadWordFixer.class, typeSystemDescription, AbstractLoggingAnnotator.PARAM_KEEP_QUIET, true);

        AnalysisEngineDescription whLinker = AnalysisEngineFactory.createEngineDescription(WhRcModResoluter.class, typeSystemDescription, AbstractLoggingAnnotator.PARAM_KEEP_QUIET, true);

        AnalysisEngineDescription tupleExtractor = AnalysisEngineFactory.createEngineDescription(SemanticParseBasedEventMentionTupleExtractor.class, typeSystemDescription);

//        AnalysisEngineDescription syntaticDirectArgumentExtractor = AnalysisEngineFactory.createEngineDescription(SyntacticDirectArgumentFixer.class, typeSystemDescription, AbstractLoggingAnnotator.PARAM_KEEP_QUIET, true);

        AnalysisEngineDescription SyntacticArgumentPropagater = AnalysisEngineFactory.createEngineDescription(SyntacticArgumentPropagateAnnotator.class, typeSystemDescription, AbstractLoggingAnnotator.PARAM_KEEP_QUIET, true);

        AnalysisEngineDescription goalMentionAnnotator = AnalysisEngineFactory.createEngineDescription(GoalMentionAnnotator.class, typeSystemDescription, AbstractLoggingAnnotator.PARAM_KEEP_QUIET, true);

        String[] needIdTops = {Entity.class.getName()};

        AnalysisEngineDescription idAssigRunner = AnalysisEngineFactory.createEngineDescription(
                IdAssigner.class, typeSystemDescription,
                IdAssigner.PARAM_TOP_NAMES_TO_ASSIGN, needIdTops);


        // Instantiate a XMI writer to put XMI as output.
        // Note that you should change the following parameters for your setting.
        AnalysisEngineDescription writer = CustomAnalysisEngineFactory.createGzippedXmiWriter(
                parentDir, paramBaseOutputDirName, stepnum, paramOutputFileSuffix);

        // Run the pipeline.
        SimplePipeline.runPipeline(reader, fixer, whLinker, tupleExtractor, SyntacticArgumentPropagater, goalMentionAnnotator, idAssigRunner, writer);
        System.out.println(className + " completed.");
    }
}