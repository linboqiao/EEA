package edu.cmu.cs.lti.pipeline;

import edu.cmu.cs.lti.uima.util.ProcessorManager;
import org.apache.uima.UIMAException;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.collection.CollectionReader;
import org.apache.uima.fit.factory.CollectionReaderFactory;
import org.apache.uima.fit.pipeline.SimplePipeline;

import java.io.IOException;

/**
 * Created with IntelliJ IDEA.
 * Date: 4/15/15
 * Time: 5:22 PM
 *
 * @author Zhengzhong Liu
 */
public class BasicPipeline {
    private CollectionReader reader;
    private AnalysisEngineDescription[] engines;

    public BasicPipeline(AbstractProcessorBuilder builder) throws UIMAException {
        // Create the components
        reader = CollectionReaderFactory.createReader(builder.buildCollectionReader());
        engines = ProcessorManager.joinProcessors(builder.buildPreprocessors(), builder.buildProcessors(), builder.buildPostProcessors());
    }

    public void run() throws IOException, UIMAException {
        SimplePipeline.runPipeline(reader, engines);
    }
}