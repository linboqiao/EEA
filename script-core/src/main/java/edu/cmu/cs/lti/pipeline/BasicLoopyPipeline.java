package edu.cmu.cs.lti.pipeline;

import edu.cmu.cs.lti.uima.pipeline.LoopPipeline;
import edu.cmu.cs.lti.uima.util.ProcessorManager;
import org.apache.uima.UIMAException;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.collection.CollectionReaderDescription;

import java.io.IOException;

/**
 * Created with IntelliJ IDEA.
 * Date: 4/15/15
 * Time: 5:22 PM
 *
 * @author Zhengzhong Liu
 */
public class BasicLoopyPipeline {
    private CollectionReaderDescription readerDescription;
    private AnalysisEngineDescription[] engines;
    private LoopPipeline loopPipeline;

    public BasicLoopyPipeline(AbstractProcessorBuilder builder, LoopPipeline loopPipeline) throws UIMAException {
        // Create the components
        readerDescription = builder.buildCollectionReader();
        engines = ProcessorManager.joinProcessors(builder.buildPreprocessors(), builder.buildProcessors(), builder.buildPostProcessors());
        this.loopPipeline = loopPipeline;
    }

    public void run() throws IOException, UIMAException {
        loopPipeline.runLoopPipeline(readerDescription, engines);
    }
}