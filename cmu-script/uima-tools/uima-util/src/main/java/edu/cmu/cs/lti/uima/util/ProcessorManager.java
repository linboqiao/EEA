package edu.cmu.cs.lti.uima.util;

import org.apache.uima.UIMAException;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.fit.factory.AnalysisEngineFactory;

/**
 * Created with IntelliJ IDEA.
 * Date: 4/2/15
 * Time: 4:16 PM
 *
 * @author Zhengzhong Liu
 */
public class ProcessorManager {
    public static AnalysisEngineDescription[] joinProcessors(AnalysisEngineDescription[]... allAEs) {
        int size = 0;
        for (AnalysisEngineDescription[] ae : allAEs) {
            size += ae.length;
        }

        AnalysisEngineDescription[] joinedAEs = new AnalysisEngineDescription[size];

        int index = 0;
        for (AnalysisEngineDescription[] aes : allAEs) {
            for (AnalysisEngineDescription ae : aes) {
                joinedAEs[index] = ae;
                index++;
            }
        }
        return joinedAEs;
    }

    public static AnalysisEngine[] createEngines(AnalysisEngineDescription... descs) throws UIMAException {
        AnalysisEngine[] engines = new AnalysisEngine[descs.length];
        for (int i = 0; i < engines.length; ++i) {
            if (descs[i].isPrimitive()) {
                engines[i] = AnalysisEngineFactory.createEngine(descs[i]);
            } else {
                engines[i] = AnalysisEngineFactory.createEngine(descs[i]);
            }
        }
        return engines;
    }
}
