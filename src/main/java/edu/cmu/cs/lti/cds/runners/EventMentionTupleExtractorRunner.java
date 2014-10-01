/**
 *
 */
package edu.cmu.cs.lti.cds.runners;

import edu.cmu.cs.lti.cds.annotators.EventMentionTupleExtractor;
import edu.cmu.cs.lti.cds.annotators.SingletonAnnotator;
import edu.cmu.cs.lti.cds.annotators.patches.DuplicatedMentionRemover;
import edu.cmu.cs.lti.cds.annotators.patches.RepresentativeMentionFinder;
import edu.cmu.cs.lti.cds.annotators.patches.TreeLeafFixer;
import edu.cmu.cs.lti.uima.io.writer.CustomAnalysisEngineFactory;
import org.apache.uima.UIMAException;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.collection.CollectionReaderDescription;
import org.apache.uima.examples.xmi.XmiCollectionReader;
import org.apache.uima.fit.factory.CollectionReaderFactory;
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
        String paramInputDir = "data/00_agiga_xmi";

        // Parameters for the writer
        String paramParentOutputDir = "data";
        String paramBaseOutputDirName = "event_tuples";
        String paramOutputFileSuffix = null;
        int stepnum = 1;

        // ////////////////////////////////////////////////////////////////

        String paramTypeSystemDescriptor = "TypeSystem";

        // Instantiate the analysis engine.
        TypeSystemDescription typeSystemDescription = TypeSystemDescriptionFactory
                .createTypeSystemDescription(paramTypeSystemDescriptor);

        // Instantiate a collection reader to get XMI as input.
        // Note that you should change the following parameters for your setting.
        CollectionReaderDescription reader = CollectionReaderFactory.createReaderDescription(
                XmiCollectionReader.class, typeSystemDescription, XmiCollectionReader.PARAM_INPUTDIR,
                paramInputDir);

        AnalysisEngineDescription tupleExtractor = CustomAnalysisEngineFactory.createAnalysisEngine(
                EventMentionTupleExtractor.class, typeSystemDescription);

        AnalysisEngineDescription duplicateMentionRemover = CustomAnalysisEngineFactory
                .createAnalysisEngine(DuplicatedMentionRemover.class, typeSystemDescription);

        AnalysisEngineDescription singletonCreator = CustomAnalysisEngineFactory.createAnalysisEngine(
                SingletonAnnotator.class, typeSystemDescription);

        AnalysisEngineDescription representativeMentionFinder = CustomAnalysisEngineFactory
                .createAnalysisEngine(RepresentativeMentionFinder.class, typeSystemDescription);

        AnalysisEngineDescription treeFixer = CustomAnalysisEngineFactory.createAnalysisEngine(TreeLeafFixer.class, typeSystemDescription);

        // Instantiate a XMI writer to put XMI as output.
        // Note that you should change the following parameters for your setting.
        AnalysisEngineDescription writer = CustomAnalysisEngineFactory.createXmiWriter(
                paramParentOutputDir, paramBaseOutputDirName, stepnum, paramOutputFileSuffix);

        // Run the pipeline.
        SimplePipeline.runPipeline(reader, tupleExtractor, duplicateMentionRemover, singletonCreator,
                representativeMentionFinder,treeFixer, writer);
        System.out.println(className + " completed.");
    }

}
