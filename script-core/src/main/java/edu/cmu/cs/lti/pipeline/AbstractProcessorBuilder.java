package edu.cmu.cs.lti.pipeline;

import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.collection.CollectionReaderDescription;
import org.apache.uima.resource.ResourceInitializationException;

/**
 * Created with IntelliJ IDEA.
 * Date: 3/25/15
 * Time: 8:08 PM
 *
 * @author Zhengzhong Liu
 */
public abstract class AbstractProcessorBuilder {
    public abstract CollectionReaderDescription buildCollectionReader() throws ResourceInitializationException;

    public abstract AnalysisEngineDescription[] buildProcessors() throws ResourceInitializationException;

    public AnalysisEngineDescription[] buildPreprocessors() throws ResourceInitializationException {
        return new AnalysisEngineDescription[0];
    }
}