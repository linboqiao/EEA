package edu.cmu.cs.lti.uima.pipeline;

import org.apache.uima.UIMAException;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.cas.CAS;
import org.apache.uima.collection.CollectionReader;
import org.apache.uima.collection.CollectionReaderDescription;
import org.apache.uima.fit.factory.CollectionReaderFactory;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.util.CasCreationUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

import static java.util.Arrays.asList;
import static org.apache.uima.fit.factory.AnalysisEngineFactory.createEngine;
import static org.apache.uima.fit.factory.AnalysisEngineFactory.createEngineDescription;

/**
 * Created with IntelliJ IDEA.
 * Date: 5/1/15
 * Time: 4:43 PM
 *
 * @author Zhengzhong Liu
 */
public abstract class LoopPipeline {
    protected final Logger logger = LoggerFactory.getLogger(getClass());

    final CollectionReaderDescription readerDescription;

    final AnalysisEngineDescription aaeDesc;

    protected abstract boolean checkStopCriteria();

    protected abstract void stopActions();

    protected abstract void loopActions();

    protected LoopPipeline(final CollectionReaderDescription readerDescription,
                           final AnalysisEngineDescription... descs) throws ResourceInitializationException {
        aaeDesc = createEngineDescription(descs);
        this.readerDescription = readerDescription;
    }

    public void runLoopPipeline() throws UIMAException, IOException {
        CollectionReader reader = CollectionReaderFactory.createReader(readerDescription);
        final AnalysisEngine aae = createEngine(aaeDesc);
        final CAS cas = CasCreationUtils.createCas(asList(readerDescription.getMetaData(), aaeDesc.getMetaData()));

        try {
            logger.info("Loop begins!");
            while (true) {
                reader.typeSystemInit(cas.getTypeSystem());

                // Process
                while (reader.hasNext()) {
                    reader.getNext(cas);
                    aae.process(cas);
                    cas.reset();
                }
                // Signal end of processing
                aae.collectionProcessComplete();

                // Call loop actions.
                loopActions();

                if (checkStopCriteria()) {
                    break;
                } else {
                    reader = CollectionReaderFactory.createReader(readerDescription);
                }
            }
            logger.info("Loop finished!");
            stopActions();
        } finally {
            aae.destroy();
            reader.close();
            reader.destroy();
        }
    }
}
