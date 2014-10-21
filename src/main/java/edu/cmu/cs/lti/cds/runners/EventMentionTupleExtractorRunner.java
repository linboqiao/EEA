/**
 *
 */
package edu.cmu.cs.lti.cds.runners;

import edu.cmu.cs.lti.cds.annotators.annos.EventMentionTupleExtractor;
import edu.cmu.cs.lti.cds.annotators.annos.GoalMentionAnnotator;
import edu.cmu.cs.lti.cds.annotators.annos.SyntacticArgumentPropagateAnnotator;
import edu.cmu.cs.lti.cds.annotators.annos.SyntacticDirectArgumentFixer;
import edu.cmu.cs.lti.cds.annotators.clustering.WhRcModResoluter;
import edu.cmu.cs.lti.cds.annotators.patches.HeadWordFixer;
import edu.cmu.cs.lti.uima.annotator.AbstractLoggingAnnotator;
import edu.cmu.cs.lti.uima.io.reader.CustomCollectionReaderFactory;
import edu.cmu.cs.lti.uima.io.writer.CustomAnalysisEngineFactory;
import org.apache.uima.UIMAException;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.collection.CollectionReaderDescription;
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
     * @throws IOException
     * @throws UIMAException
     */
    public static void main(String[] args) throws UIMAException, IOException {
        System.out.println(className + " started...");

        // ///////////////////////// Parameter Setting ////////////////////////////
        // Note that you should change the parameters below for your configuration.
        // //////////////////////////////////////////////////////////////////////////
        // Parameters for the reader
        // Parameters for the reader
        String inputDir = "data/01_discourse_parsed";


        // Parameters for the writer
        String paramParentOutputDir = "data";
        String paramBaseOutputDirName = "event_tuples";
        String paramOutputFileSuffix = null;
        int stepnum = 2;

        // ////////////////////////////////////////////////////////////////

        String paramTypeSystemDescriptor = "TypeSystem";

        // Instantiate the analysis engine.
        TypeSystemDescription typeSystemDescription = TypeSystemDescriptionFactory
                .createTypeSystemDescription(paramTypeSystemDescriptor);

        // Instantiate a collection reader to get XMI as input.
        // Note that you should change the following parameters for your setting.
        CollectionReaderDescription reader =
                CustomCollectionReaderFactory.createTimeSortedGzipXmiReader(typeSystemDescription, inputDir, false);

        AnalysisEngineDescription fixer = CustomAnalysisEngineFactory.createAnalysisEngine(HeadWordFixer.class, typeSystemDescription, AbstractLoggingAnnotator.PARAM_KEEP_QUIET, true);

        AnalysisEngineDescription whLinker = CustomAnalysisEngineFactory.createAnalysisEngine(WhRcModResoluter.class, typeSystemDescription, AbstractLoggingAnnotator.PARAM_KEEP_QUIET, true);

        AnalysisEngineDescription tupleExtractor = CustomAnalysisEngineFactory.createAnalysisEngine(EventMentionTupleExtractor.class, typeSystemDescription);

        AnalysisEngineDescription syntaticDirectArgumentExtractor = CustomAnalysisEngineFactory.createAnalysisEngine(SyntacticDirectArgumentFixer.class, typeSystemDescription, AbstractLoggingAnnotator.PARAM_KEEP_QUIET, true);

        AnalysisEngineDescription SyntacticArgumentPropagater = CustomAnalysisEngineFactory.createAnalysisEngine(SyntacticArgumentPropagateAnnotator.class, typeSystemDescription, AbstractLoggingAnnotator.PARAM_KEEP_QUIET, true);

        AnalysisEngineDescription goalMentionAnnotator = CustomAnalysisEngineFactory.createAnalysisEngine(GoalMentionAnnotator.class, typeSystemDescription, AbstractLoggingAnnotator.PARAM_KEEP_QUIET, true);

        // Instantiate a XMI writer to put XMI as output.
        // Note that you should change the following parameters for your setting.
        AnalysisEngineDescription writer = CustomAnalysisEngineFactory.createGzippedXmiWriter(
                paramParentOutputDir, paramBaseOutputDirName, stepnum, paramOutputFileSuffix);

        // Run the pipeline.
        SimplePipeline.runPipeline(reader, fixer, whLinker, tupleExtractor, syntaticDirectArgumentExtractor, SyntacticArgumentPropagater, goalMentionAnnotator, writer);
        System.out.println(className + " completed.");
    }
}