package edu.cmu.cs.lti.emd.annotators.classification;

import edu.cmu.cs.lti.learning.model.ClassAlphabet;
import edu.cmu.cs.lti.learning.model.FeatureAlphabet;
import edu.cmu.cs.lti.uima.annotator.AbstractLoggingAnnotator;
import gnu.trove.map.TIntDoubleMap;
import org.apache.uima.UIMAException;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.collection.CollectionReaderDescription;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.pipeline.SimplePipeline;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.javatuples.Pair;

import java.io.IOException;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * Date: 9/11/15
 * Time: 1:09 PM
 *
 * @author Zhengzhong Liu
 */
public class RealisFeatureExtractor extends AbstractLoggingAnnotator {
    private static List<Pair<TIntDoubleMap, String>> features;
    private static FeatureAlphabet alphabet;
    private static ClassAlphabet classAlphabet;

    @Override
    public void process(JCas aJCas) throws AnalysisEngineProcessException {

    }

    public static void getFeatures(
            CollectionReaderDescription reader,
            TypeSystemDescription typeSystemDescription,
            List<Pair<TIntDoubleMap, String>> features,
            FeatureAlphabet alphabet,
            ClassAlphabet classAlphabet) throws UIMAException, IOException {
        RealisFeatureExtractor.features = features;
        RealisFeatureExtractor.alphabet = alphabet;
        RealisFeatureExtractor.classAlphabet = classAlphabet;
        SimplePipeline.runPipeline(reader, AnalysisEngineFactory.createEngineDescription(RealisFeatureExtractor
                .class, typeSystemDescription));
    }
}
