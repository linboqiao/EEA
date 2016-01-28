package edu.cmu.cs.lti.pipeline;

import edu.cmu.cs.lti.uima.io.reader.CustomCollectionReaderFactory;
import edu.cmu.cs.lti.uima.io.writer.CustomAnalysisEngineFactory;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.uima.UIMAException;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.CAS;
import org.apache.uima.collection.*;
import org.apache.uima.collection.metadata.CpeDescriptorException;
import org.apache.uima.fit.cpe.CpeBuilder;
import org.apache.uima.fit.factory.CollectionReaderFactory;
import org.apache.uima.fit.pipeline.SimplePipeline;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.util.CasCreationUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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
    protected final Logger logger = LoggerFactory.getLogger(getClass());

    private CollectionReaderDescription readerDescription;
    private CollectionReaderDescription outputReader;
    private AnalysisEngineDescription aggregateAnalysisEngineDesc;

    private CollectionReader cReader;
    private AnalysisEngine aggregateAnalysisEngine;
    private CAS mergedCas;

    private boolean withOutput;

    public BasicPipeline(ProcessorWrapper wrapper) throws UIMAException,
            CpeDescriptorException, SAXException, IOException {
        this(wrapper, null, null);
    }

    public BasicPipeline(ProcessorWrapper wrapper, String workingDir, String outputDir) throws
            UIMAException, CpeDescriptorException, SAXException, IOException {
        readerDescription = wrapper.getCollectionReader();
        AnalysisEngineDescription[] processers = wrapper.getProcessors();

        AnalysisEngineDescription[] engineDescriptions;
        if (workingDir != null && outputDir != null) {
            logger.info("Pipeline with output at " + new File(workingDir, outputDir));
            engineDescriptions = ArrayUtils.add(processers, CustomAnalysisEngineFactory.createXmiWriter(workingDir,
                    outputDir));
            outputReader = CustomCollectionReaderFactory.createXmiReader(workingDir, outputDir);
            withOutput = true;
        } else {
            withOutput = false;
            engineDescriptions = processers;
        }

        aggregateAnalysisEngineDesc = createEngineDescription(engineDescriptions);
    }

    private static class StatusCallbackListenerImpl implements StatusCallbackListener {

        private final List<Exception> exceptions = new ArrayList<Exception>();

        private boolean isProcessing = true;

        public void entityProcessComplete(CAS arg0, EntityProcessStatus arg1) {
            if (arg1.isException()) {
                for (Exception e : arg1.getExceptions()) {
                    exceptions.add(e);
                }
            }
        }

        public void aborted() {
            synchronized (this) {
                if (isProcessing) {
                    isProcessing = false;
                    notify();
                }
            }
        }

        public void batchProcessComplete() {
            // Do nothing
        }

        public void collectionProcessComplete() {
            synchronized (this) {
                if (isProcessing) {
                    isProcessing = false;
                    notify();
                }
            }
        }

        public void initializationComplete() {
            // Do nothing
        }

        public void paused() {
            // Do nothing
        }

        public void resumed() {
            // Do nothing
        }
    }

    /**
     * Run processor from provided reader.
     *
     * @throws IOException
     * @throws UIMAException
     */
    public void run() throws IOException, UIMAException {
        SimplePipeline.runPipeline(readerDescription, aggregateAnalysisEngineDesc);
    }

    /**
     * Run processor from provided reader, write processed CAS as XMI to the given directory.
     *
     * @return A reader description for the processed output.
     * @throws UIMAException
     * @throws IOException
     */
    public CollectionReaderDescription runWithOutput() throws UIMAException, IOException {
        if (withOutput) {
            logger.info("Processing with output.");
            run();
            return outputReader;
        } else {
            throw new IllegalAccessError("Pipeline is not initialized with output.");
        }
    }

    /**
     * Run with a CPE from provided reader. CPE use a asynchronous thread pool, which may die during processing.
     *
     * @throws IOException
     * @throws UIMAException
     */
    public void runWithCpe(int numThread) throws IOException, UIMAException, CpeDescriptorException, SAXException {
        CpeBuilder cpeBuilder = new CpeBuilder();
        cpeBuilder.setMaxProcessingUnitThreadCount(numThread);
        cpeBuilder.setReader(readerDescription);
        cpeBuilder.setAnalysisEngine(aggregateAnalysisEngineDesc);

        final StatusCallbackListenerImpl status = new StatusCallbackListenerImpl();

        CollectionProcessingEngine cpe = cpeBuilder.createCpe(status);

        cpe.process();
        try {
            synchronized (status) {
                while (status.isProcessing) {
                    status.wait();
                }
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        if (status.exceptions.size() > 0) {
            throw new AnalysisEngineProcessException(status.exceptions.get(0));
        }
    }


    /**
     * Run processor from provided reader, write processed CAS as XMI to the given directory.
     *
     * @return A reader description for the processed output.
     * @throws UIMAException
     * @throws IOException
     */
    public CollectionReaderDescription runCpeWithOutput(int numThread) throws UIMAException, IOException,
            CpeDescriptorException, SAXException {
        if (withOutput) {
            logger.info("Processing CPE with output.");
            runWithCpe(numThread);
            return outputReader;
        } else {
            throw new IllegalAccessError("Pipeline is not initialized with output.");
        }
    }

    /**
     * Initialize the processor so that process can be called multiple times.
     *
     * @throws ResourceInitializationException
     */
    public void initialize() throws ResourceInitializationException {
        // Create Reader
        cReader = CollectionReaderFactory.createReader(readerDescription);

        // Instantiate AAE
        aggregateAnalysisEngine = createEngine(aggregateAnalysisEngineDesc);

        // Create CAS from merged metadata
        mergedCas = CasCreationUtils.createCas(
                Arrays.asList(cReader.getMetaData(), aggregateAnalysisEngineDesc.getMetaData())
        );
        cReader.typeSystemInit(mergedCas.getTypeSystem());
    }

    /**
     * Process the next CAS in the reader.
     */
    public void process() {
        try {
            // Process.
            while (cReader.hasNext()) {
                cReader.getNext(mergedCas);
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
            if (cReader.hasNext()) {
                cReader.getNext(mergedCas);
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