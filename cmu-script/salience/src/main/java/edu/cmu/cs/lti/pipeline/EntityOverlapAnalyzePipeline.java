package edu.cmu.cs.lti.pipeline;

import edu.cmu.cs.lti.annotators.EntityOverlapAnalyzer;
import edu.cmu.cs.lti.salience.annotators.TagmeEntityLinkerResultAnnotator;
import edu.cmu.cs.lti.uima.io.reader.CustomCollectionReaderFactory;
import edu.cmu.cs.lti.uima.io.writer.CustomAnalysisEngineFactory;
import org.apache.uima.UIMAException;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.collection.CollectionReaderDescription;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.factory.TypeSystemDescriptionFactory;
import org.apache.uima.fit.pipeline.SimplePipeline;
import org.apache.uima.resource.metadata.TypeSystemDescription;

import java.io.IOException;

/**
 * Created by hunte on 6/27/2017.
 */
public class EntityOverlapAnalyzePipeline {

    public static void main(String[] argv) throws UIMAException, IOException {
        TypeSystemDescription typeSystemDescription = TypeSystemDescriptionFactory
                .createTypeSystemDescription("TypeSystem");

        String inputDir = argv[0];
        String entityResultDir = argv[1];
        String outputDir = argv[2];

        CollectionReaderDescription reader = CustomCollectionReaderFactory.createXmiReader(typeSystemDescription, inputDir);

        AnalysisEngineDescription linker = AnalysisEngineFactory.createEngineDescription(
                TagmeEntityLinkerResultAnnotator.class, typeSystemDescription,
                TagmeEntityLinkerResultAnnotator.PARAM_ENTITY_RESULT_FOLDER, entityResultDir,
                TagmeEntityLinkerResultAnnotator.PARAM_USE_TOKEN, true
        );

        AnalysisEngineDescription overlap = AnalysisEngineFactory.createEngineDescription(
                EntityOverlapAnalyzer.class, typeSystemDescription
        );

        AnalysisEngineDescription writer = CustomAnalysisEngineFactory.createXmiWriter(outputDir, "xmi");

        SimplePipeline.runPipeline(reader, linker, overlap, writer);
    }
}
