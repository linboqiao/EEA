package edu.cmu.lti.event_coref.pipeline;

import edu.cmu.cs.lti.pipeline.AbstractProcessorBuilder;
import edu.cmu.cs.lti.pipeline.BasicPipeline;
import edu.cmu.cs.lti.uima.io.reader.CustomCollectionReaderFactory;
import edu.cmu.cs.lti.uima.io.writer.CustomAnalysisEngineFactory;
import edu.cmu.lti.event_coref.annotators.ArgumentExtractor;
import edu.cmu.lti.event_coref.annotators.GoldStandardEventMentionAnnotator;
import org.apache.uima.UIMAException;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.collection.CollectionReaderDescription;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.uimafit.factory.TypeSystemDescriptionFactory;

import java.io.File;
import java.io.IOException;

/**
 * A pipeline structure that create mentions first and then create coref links
 * <p/>
 * Created with IntelliJ IDEA.
 * Date: 4/15/15
 * Time: 5:20 PM
 *
 * @author Zhengzhong Liu
 */
public class CorefPipeline {
    public void prepareEventMentions(String typeSystemName, final String parentDir, final String baseInputDir, final String xmiOutputBase) throws UIMAException, IOException {
        final TypeSystemDescription typeSystemDescription = TypeSystemDescriptionFactory
                .createTypeSystemDescription(typeSystemName);

        final String inputDir = new File(parentDir, baseInputDir).getAbsolutePath();

        BasicPipeline pipeline = new BasicPipeline(new AbstractProcessorBuilder() {
            @Override
            public CollectionReaderDescription buildCollectionReader() throws ResourceInitializationException {
                return CustomCollectionReaderFactory.createXmiReader(typeSystemDescription, inputDir, false);
            }

            @Override
            public AnalysisEngineDescription[] buildPreprocessors() throws ResourceInitializationException {
                return new AnalysisEngineDescription[0];
            }

            @Override
            public AnalysisEngineDescription[] buildProcessors() throws ResourceInitializationException {
                AnalysisEngineDescription eventMentionAnnotator = AnalysisEngineFactory.createEngineDescription(
                        GoldStandardEventMentionAnnotator.class, typeSystemDescription
                );
                AnalysisEngineDescription argumentExtractor = AnalysisEngineFactory.createEngineDescription(
                        ArgumentExtractor.class, typeSystemDescription
                );
                return new AnalysisEngineDescription[]{eventMentionAnnotator, argumentExtractor};
            }

            @Override
            public AnalysisEngineDescription[] buildPostProcessors() throws ResourceInitializationException {
                AnalysisEngineDescription xmiWriter = CustomAnalysisEngineFactory.createXmiWriter(parentDir, xmiOutputBase);
                return new AnalysisEngineDescription[]{xmiWriter};
            }
        });

        pipeline.run();
    }

    public static void main(String[] argv) throws UIMAException, IOException {
        CorefPipeline pipeline = new CorefPipeline();
        String typeSystemName = "TypeSystem";
        String parentDir = argv[0];
        String baseInputDir = argv[1];
        String xmiOutputBase = "argument_extracted";
        pipeline.prepareEventMentions(typeSystemName, parentDir, baseInputDir, xmiOutputBase);
    }
}