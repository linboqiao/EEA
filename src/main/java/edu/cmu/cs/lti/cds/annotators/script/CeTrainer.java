package edu.cmu.cs.lti.cds.annotators.script;

import edu.cmu.cs.lti.cds.dist.GlobalUnigrmHwLocalUniformArgumentDist;
import edu.cmu.cs.lti.cds.ml.features.FeatureExtractor;
import edu.cmu.cs.lti.cds.model.ChainElement;
import edu.cmu.cs.lti.cds.model.LocalEventMentionRepre;
import edu.cmu.cs.lti.cds.utils.DataPool;
import edu.cmu.cs.lti.script.type.EventMention;
import edu.cmu.cs.lti.script.type.Sentence;
import edu.cmu.cs.lti.uima.annotator.AbstractLoggingAnnotator;
import edu.cmu.cs.lti.utils.TokenAlignmentHelper;
import gnu.trove.iterator.TObjectDoubleIterator;
import gnu.trove.map.TObjectDoubleMap;
import gnu.trove.map.hash.TObjectDoubleHashMap;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;

import java.util.ArrayList;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: zhengzhongliu
 * Date: 10/31/14
 * Time: 5:52 PM
 */
public class CeTrainer extends AbstractLoggingAnnotator {
    TokenAlignmentHelper align = new TokenAlignmentHelper();
    FeatureExtractor extractor = new FeatureExtractor();

    int documentProcessed = 0;

    int miniBatchDocNum = 100;
    int numNoise = 25;
    int skipGramN = 2;

    int numArguments = 3;

    double stepSize = 0.01;

    GlobalUnigrmHwLocalUniformArgumentDist noiseDist = new GlobalUnigrmHwLocalUniformArgumentDist();

    TObjectDoubleMap<String> cumulativeGradient = new TObjectDoubleHashMap<>();

    @Override
    public void process(JCas aJCas) throws AnalysisEngineProcessException {
        logger.info(progressInfo(aJCas));

        align.loadWord2Stanford(aJCas);
        List<ChainElement> chain = new ArrayList<>();
        List<Pair<Integer, String>> arguments = new ArrayList<>();
        for (Sentence sent : JCasUtil.select(aJCas, Sentence.class)) {
            for (EventMention mention : JCasUtil.selectCovered(EventMention.class, sent)) {
                LocalEventMentionRepre eventRep = LocalEventMentionRepre.fromEventMention(mention, align);
                chain.add(new ChainElement(sent, eventRep));
                for (Pair<Integer, String> arg : eventRep.getArgs()) {
                    arguments.add(arg);
                }
            }
        }

        //for each sample
        for (int sampleIndex = 0; sampleIndex < chain.size(); sampleIndex++) {
            ChainElement realSample = chain.get(sampleIndex);
            TObjectDoubleMap<String> features = extractor.getFeatures(chain, align, realSample, sampleIndex, skipGramN);
            Sentence sampleSent = realSample.getSent();

            //generate noise samples
            List<Pair<TObjectDoubleMap<String>, Double>> noiseSamples = new ArrayList<>();
            for (int i = 0; i < numNoise; i++) {
                Pair<LocalEventMentionRepre, Double> noise = noiseDist.draw(arguments, numArguments);
                TObjectDoubleMap<String> noiseFeature = extractor.getFeatures(chain, align, new ChainElement(sampleSent, noise.getLeft()), sampleIndex, skipGramN);
                noiseSamples.add(Pair.of(noiseFeature, noise.getRight()));
            }

            //cumulative the gradient so far
            gradientAscent(DataPool.weights, noiseSamples,
                    Pair.of(features, noiseDist.probOf(realSample.getMention(), arguments.size(), numArguments)), cumulativeGradient);

            documentProcessed++;
            if (documentProcessed % miniBatchDocNum == 0) {
                update();
            }
        }
    }

    private void gradientAscent(TObjectDoubleMap<String> params, List<Pair<TObjectDoubleMap<String>, Double>> noiseSamples,
                                Pair<TObjectDoubleMap<String>, Double> dataSample, TObjectDoubleMap<String> cumulativeGradient) {
        TObjectDoubleMap<String> gradient = unNormalizedLogisticDerivative(dataSample.getKey());

        double unlogisticSample = unnormalizedLogistic(dataSample.getKey());
        double kNoiseSampleProb = numNoise * dataSample.getValue();

        vectorScalarProduct(gradient, (1 / (1 + unlogisticSample / kNoiseSampleProb)));

        for (Pair<TObjectDoubleMap<String>, Double> noiseSample : noiseSamples) {
            double unlogisticNoise = unnormalizedLogistic(noiseSample.getKey());
            double kNoiseNoiseProb = numNoise * noiseSample.getValue();
            TObjectDoubleMap<String> noiseGradient = unNormalizedLogisticDerivative(noiseSample.getKey());
            vectorScalarProduct(noiseGradient, (1 / (1 + kNoiseNoiseProb / unlogisticNoise)));
            vectorMinus(gradient, noiseGradient);
        }

        //update the cumulative gradient;
        for (TObjectDoubleIterator<String> iter = gradient.iterator(); iter.hasNext(); ) {
            iter.advance();
            cumulativeGradient.adjustOrPutValue(iter.key(), iter.value(), iter.value());
        }
    }

    private void vectorMinus(TObjectDoubleMap<String> v1, TObjectDoubleMap<String> v2) {
        for (TObjectDoubleIterator<String> it = v1.iterator(); it.hasNext(); ) {
            it.advance();
            if (v2.containsKey(it.key())) {
                it.setValue(it.value() - v2.get(it.key()));
                v2.remove(it.key());
            }
        }

        for (TObjectDoubleIterator<String> it = v2.iterator(); it.hasNext(); ) {
            it.advance();
            v1.put(it.key(), -it.value());
        }
    }

    private void vectorScalarProduct(TObjectDoubleMap<String> vector, double scalar) {
        for (TObjectDoubleIterator<String> it = vector.iterator(); it.hasNext(); ) {
            it.setValue(it.value() * scalar);
        }
    }

    //TODO so trivial?
    private TObjectDoubleMap<String> unNormalizedLogisticDerivative(TObjectDoubleMap<String> features) {
        return features;
    }

    private double unnormalizedLogistic(TObjectDoubleMap<String> features) {
        return dotProd(features, DataPool.weights);
    }

    private double dotProd(TObjectDoubleMap<String> smallVec, TObjectDoubleMap<String> largerVec) {
        double dotProd = 0;
        for (TObjectDoubleIterator<String> it = smallVec.iterator(); it.hasNext(); ) {
            it.advance();
            if (largerVec.containsKey(it.key())) {
                dotProd += it.value() * largerVec.get(it.key());
            }
        }
        return dotProd;
    }

    //TODO see AdaDelta
    private void update() {
        // update parameters
        for (TObjectDoubleIterator<String> iter = cumulativeGradient.iterator(); iter.hasNext(); ) {
            iter.advance();

            double u = stepSize * iter.value();

            DataPool.weights.adjustOrPutValue(iter.key(), u, u);
        }
        // empty the cumulative gradient
        cumulativeGradient.clear();
    }

    //TODO compute objective, and check gradient correct or not
    private double objective() {
        return -1;
    }
}