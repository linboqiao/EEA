package edu.cmu.cs.lti.pipeline;

import edu.cmu.cs.lti.uima.io.reader.CustomCollectionReaderFactory;
import edu.cmu.cs.lti.uima.io.writer.CustomAnalysisEngineFactory;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.uima.UIMAException;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.collection.CollectionReader;
import org.apache.uima.collection.CollectionReaderDescription;
import org.apache.uima.fit.factory.CollectionReaderFactory;
import org.apache.uima.fit.pipeline.SimplePipeline;
import org.apache.uima.resource.metadata.TypeSystemDescription;

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

    private TypeSystemDescription typeSystemDescription;

    public BasicPipeline(ProcessorWrapper wrapper, TypeSystemDescription typeSystemDescription) throws
            UIMAException {
        // Create the components
        reader = CollectionReaderFactory.createReader(wrapper.getCollectionReader());
        engines = wrapper.getProcessors();
        this.typeSystemDescription = typeSystemDescription;
    }

    public void run() throws IOException, UIMAException {
        SimplePipeline.runPipeline(reader, engines);
    }


    public void runWithOutput(AnalysisEngineDescription writer) throws UIMAException, IOException {
        SimplePipeline.runPipeline(reader, ArrayUtils.add(engines, writer));
    }

    public void runWithOutput(String workingDir, String outputDir) throws UIMAException, IOException {
        AnalysisEngineDescription writer = CustomAnalysisEngineFactory.createXmiWriter(workingDir, outputDir);
        runWithOutput(writer);
    }

    /**
     * Run processors from existing data source.
     *
     * @param outputParent Parent directory of the data.
     * @param outputBase   Directory directly containing the data.
     * @throws UIMAException
     * @throws IOException
     */
    public void runProcessors(String outputParent, String outputBase) throws
            UIMAException, IOException {
        CollectionReaderDescription reader = CustomCollectionReaderFactory.createXmiReader(typeSystemDescription,
                outputParent,
                outputBase);
        SimplePipeline.runPipeline(reader, engines);
    }

    /**
     * Run processors from existing data source.
     *
     * @throws UIMAException
     * @throws IOException
     */
    public void runProcessors(CollectionReaderDescription reader) throws
            UIMAException, IOException {
        SimplePipeline.runPipeline(reader, engines);
    }
}