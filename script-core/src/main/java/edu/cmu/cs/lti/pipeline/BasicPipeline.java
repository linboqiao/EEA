package edu.cmu.cs.lti.pipeline;

import edu.cmu.cs.lti.uima.io.writer.CustomAnalysisEngineFactory;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.uima.UIMAException;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.CAS;
import org.apache.uima.collection.CollectionException;
import org.apache.uima.collection.CollectionReader;
import org.apache.uima.fit.factory.CollectionReaderFactory;
import org.apache.uima.fit.pipeline.SimplePipeline;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.util.CasCreationUtils;

import java.io.IOException;
import java.util.Arrays;

import static org.apache.uima.fit.factory.AnalysisEngineFactory.createEngine;
import static org.apache.uima.fit.factory.AnalysisEngineFactory.createEngineDescription;

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

    private AnalysisEngine aggregateAnalysisEngine;
    private CAS mergedCas;

    public BasicPipeline(ProcessorWrapper wrapper) throws UIMAException {
        // Create the components
        reader = CollectionReaderFactory.createReader(wrapper.getCollectionReader());
        engines = wrapper.getProcessors();
    }

    /**
     * Run processor from provided reader.
     *
     * @throws IOException
     * @throws UIMAException
     */
    public void run() throws IOException, UIMAException {
        SimplePipeline.runPipeline(reader, engines);
    }

    /**
     * Run processor from provided reader, write processed CAS using provided writer.
     *
     * @param writer THe provided writer
     * @throws UIMAException
     * @throws IOException
     */
    public void runWithOutput(AnalysisEngineDescription writer) throws UIMAException, IOException {
        SimplePipeline.runPipeline(reader, ArrayUtils.add(engines, writer));
    }

    /**
     * RUn processor from provided reader, write processed CAS as XMI to the given directory.
     *
     * @param workingDir Parent directory of the data.
     * @param outputDir  Base directory of the data.
     * @throws UIMAException
     * @throws IOException
     */
    public void runWithOutput(String workingDir, String outputDir) throws UIMAException, IOException {
        AnalysisEngineDescription writer = CustomAnalysisEngineFactory.createXmiWriter(workingDir, outputDir);
        runWithOutput(writer);
    }

    /**
     * Initialize the processor so that process can be called multiple times.
     *
     * @throws ResourceInitializationException
     */
    public void initialize() throws ResourceInitializationException {
        // Create AAE
        final AnalysisEngineDescription aggregateAnalysisEngineDesc = createEngineDescription(engines);

        // Instantiate AAE
        aggregateAnalysisEngine = createEngine(aggregateAnalysisEngineDesc);

        // Create CAS from merged metadata
        mergedCas = CasCreationUtils.createCas(
                Arrays.asList(reader.getMetaData(), aggregateAnalysisEngineDesc.getMetaData())
        );
        reader.typeSystemInit(mergedCas.getTypeSystem());
    }

    /**
     * Process the next CAS in the reader.
     */
    public void process() {
        try {
            // Process.
            while (reader.hasNext()) {
                reader.getNext(mergedCas);
                aggregateAnalysisEngine.process(mergedCas);
                mergedCas.reset();
            }
        } catch (AnalysisEngineProcessException | CollectionException | IOException e) {
            e.printStackTrace();
            // Destroy.
            aggregateAnalysisEngine.destroy();
        }
    }

    /**
     * Process the one CAS in the reader.
     */
    public void processNext() {
        try {
            // Process.
            if (reader.hasNext()) {
                reader.getNext(mergedCas);
                aggregateAnalysisEngine.process(mergedCas);
                mergedCas.reset();
            }
        } catch (AnalysisEngineProcessException | CollectionException | IOException e) {
            e.printStackTrace();
            // Destroy.
            aggregateAnalysisEngine.destroy();
        }
    }

    /**
     * Complete the process.
     *
     * @throws AnalysisEngineProcessException
     */
    public void complete() throws AnalysisEngineProcessException {
        try {
            // Signal end of processing.
            aggregateAnalysisEngine.collectionProcessComplete();
        } finally {
            // Destroy.
            aggregateAnalysisEngine.destroy();
        }
    }

//    /**
//     * Run processors from existing data source.
//     *
//     * @param outputParent Parent directory of the data.
//     * @param outputBase   Directory directly containing the data.
//     * @throws UIMAException
//     * @throws IOException
//     */
//    public void runProcessors(String outputParent, String outputBase) throws
//            UIMAException, IOException {
//        CollectionReaderDescription reader = CustomCollectionReaderFactory.createXmiReader(typeSystemDescription,
//                outputParent, outputBase);
//        SimplePipeline.runPipeline(reader, engines);
//    }
//
//    /**
//     * Run processors from existing data source.
//     *
//     * @throws UIMAException
//     * @throws IOException
//     */
//    public void runProcessors(CollectionReaderDescription reader) throws
//            UIMAException, IOException {
//        SimplePipeline.runPipeline(reader, engines);
//    }
}