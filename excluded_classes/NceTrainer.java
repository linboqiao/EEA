package edu.cmu.cs.lti.cds.annotators.script.train;

import edu.cmu.cs.lti.cds.dist.GlobalUnigrmHwLocalUniformArgumentDist;
import edu.cmu.cs.lti.cds.ml.features.FeatureExtractor;
import edu.cmu.cs.lti.cds.model.ContextElement;
import edu.cmu.cs.lti.cds.model.LocalArgumentRepre;
import edu.cmu.cs.lti.cds.model.LocalEventMentionRepre;
import edu.cmu.cs.lti.cds.runners.script.train.StochasticNceTrainer;
import edu.cmu.cs.lti.cds.utils.DataPool;
import edu.cmu.cs.lti.cds.utils.VectorUtils;
import edu.cmu.cs.lti.script.type.Article;
import edu.cmu.cs.lti.script.type.EventMention;
import edu.cmu.cs.lti.script.type.Sentence;
import edu.cmu.cs.lti.uima.annotator.AbstractLoggingAnnotator;
import edu.cmu.cs.lti.utils.TokenAlignmentHelper;
import gnu.trove.iterator.TObjectDoubleIterator;
import gnu.trove.map.TObjectDoubleMap;
import gnu.trove.map.hash.TObjectDoubleHashMap;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: zhengzhongliu
 * Date: 10/31/14
 * Time: 5:52 PM
 */
public class NceTrainer extends AbstractLoggingAnnotator {
    TokenAlignmentHelper align = new TokenAlignmentHelper();
    FeatureExtractor extractor = new FeatureExtractor();

    int miniBatchDocNum = 100;
    int numNoise = 25;
    int skipGramN = 2;

    int numArguments = 3;

    double stepSize = 0.0001;

    double globalNormalization = 1;

    GlobalUnigrmHwLocalUniformArgumentDist noiseDist = new GlobalUnigrmHwLocalUniformArgumentDist();

    TObjectDoubleMap<String> cumulativeGradient = new TObjectDoubleHashMap<>();

    double cumulativeObjective = 0;

    @Override
    public void initialize(UimaContext aContext) throws ResourceInitializationException {
        super.initialize(aContext);
    }

    @Override
    public void process(JCas aJCas) throws AnalysisEngineProcessException {
        Article article = JCasUtil.selectSingle(aJCas, Article.class);


        try {
            StochasticNceTrainer.trainOut.write(progressInfo(aJCas) + "\n");
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (DataPool.blackListedArticleId.contains(article.getArticleName())) {
            //ignore this blacklisted file;
//            logger.info("Ignored black listed file");
            return;
        }

        align.loadWord2Stanford(aJCas);
        List<ContextElement> chain = new ArrayList<>();
        List<LocalArgumentRepre> arguments = new ArrayList<>();
        for (Sentence sent : JCasUtil.select(aJCas, Sentence.class)) {
            for (EventMention mention : JCasUtil.selectCovered(EventMention.class, sent)) {
                LocalEventMentionRepre eventRep = LocalEventMentionRepre.fromEventMention(mention, align);
                chain.add(new ContextElement(sent, eventRep));
                for (LocalArgumentRepre arg : eventRep.getArgs()) {
                    arguments.add(arg);
                }
            }
        }

        //for each sample
        for (int sampleIndex = 0; sampleIndex < chain.size(); sampleIndex++) {
            ContextElement realSample = chain.get(sampleIndex);
            TObjectDoubleMap<String> features = extractor.getFeatures(chain, realSample, sampleIndex, skipGramN, false);
            Sentence sampleSent = realSample.getSent();

            //generate noise samples
            List<Pair<TObjectDoubleMap<String>, Double>> noiseSamples = new ArrayList<>();
            for (int i = 0; i < numNoise; i++) {
                Pair<LocalEventMentionRepre, Double> noise = noiseDist.draw(arguments, numArguments);
                TObjectDoubleMap<String> noiseFeature = extractor.getFeatures(chain, new ContextElement(sampleSent, noise.getLeft()), sampleIndex, skipGramN, true);
                if (noiseFeature != null) {
                    noiseSamples.add(Pair.of(noiseFeature, noise.getRight()));
                }
            }

            //cumulative the gradient so far, and compute sample cost
            double cumulativeObjective = gradientAscent(noiseSamples, Pair.of(features, noiseDist.probOf(realSample.getMention(), arguments.size(), numArguments)));
//            logger.info("Sample cost " + c);
            this.cumulativeObjective += cumulativeObjective;
        }

        DataPool.numSampleProcessed++;

        if (DataPool.numSampleProcessed % miniBatchDocNum == 0) {
            logger.info("Features learnt " + DataPool.weights.size());
            logger.info("Processed " + DataPool.numSampleProcessed + " samples");
            logger.info("Average gain for previous batch is : " + cumulativeObjective / miniBatchDocNum);
            cumulativeObjective = 0;
            update();
        }
    }

    @Override
    public void collectionProcessComplete() throws AnalysisEngineProcessException {
        logger.info("Finish one epoch, totally  " + DataPool.numSampleProcessed + " samples processed so far");
        logger.info("Features stored " + DataPool.weights.size());
        logger.info("Average cumulative gain for the residual batch: " + cumulativeObjective / (DataPool.numSampleProcessed % miniBatchDocNum));
        update();
    }

    private double gradientAscent(List<Pair<TObjectDoubleMap<String>, Double>> noiseSamples,
                                  Pair<TObjectDoubleMap<String>, Double> dataSample) {

        for (TObjectDoubleIterator<String> iter = dataSample.getKey().iterator(); iter.hasNext(); ) {
            iter.advance();
        }

        //start by assigning gradient as  d/d\theta logP_\theta(w)
        TObjectDoubleMap<String> gradient = unNormalizedLogLogisticDerivative(dataSample.getKey());

        //collect cost
        double localObjective = 0;

        //sample prob given the parameters
        double estimatedLogisticSample = estimatedLogistic(dataSample.getKey());
        //noise prob of the real sample
        double kNoiseSampleProb = numNoise * dataSample.getValue();

        VectorUtils.vectorScalarProduct(gradient, (1 / (1 + estimatedLogisticSample / kNoiseSampleProb)));

        localObjective += 1 / (1 + kNoiseSampleProb / estimatedLogisticSample);

        for (Pair<TObjectDoubleMap<String>, Double> noiseSample : noiseSamples) {
            //noise sample prob given the parameters
            double estimatedLogisticNoise = estimatedLogistic(noiseSample.getKey());
            //noise prob of the noise * k
            double kNoiseNoiseProb = numNoise * noiseSample.getValue();
            TObjectDoubleMap<String> noiseGradient = unNormalizedLogLogisticDerivative(noiseSample.getKey());
            VectorUtils.vectorScalarProduct(noiseGradient, (1 / (1 + kNoiseNoiseProb / estimatedLogisticNoise)));
            VectorUtils.vectorMinus(gradient, noiseGradient);
            localObjective += 1 / (1 + estimatedLogisticNoise / kNoiseNoiseProb);
        }

        //update the cumulative gradient;
        for (TObjectDoubleIterator<String> iter = gradient.iterator(); iter.hasNext(); ) {
            iter.advance();
            cumulativeGradient.adjustOrPutValue(iter.key(), iter.value(), iter.value());
            try {
                StochasticNceTrainer.trainOut.write(iter.key() + " " + iter.value() + "\n");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return localObjective;
    }

    //TODO so trivial?
    private TObjectDoubleMap<String> unNormalizedLogLogisticDerivative(TObjectDoubleMap<String> features) {
        return features;
    }


    private double estimatedLogistic(TObjectDoubleMap<String> features) {
        return Math.exp(VectorUtils.dotProd(features, DataPool.weights)) - globalNormalization;
    }

    //TODO see AdaDelta
    private void update() {
//        adaDeltaUpdate(1e-3, 0.95);
        stepSizeUpdate();
    }

    private void stepSizeUpdate() {
        // update parameters
        for (TObjectDoubleIterator<String> iter = cumulativeGradient.iterator(); iter.hasNext(); ) {
            iter.advance();
            double u = stepSize * iter.value();
            DataPool.weights.adjustOrPutValue(iter.key(), u, u);
        }
        // empty the cumulative gradient
        cumulativeGradient.clear();
    }

    private void adaDeltaUpdate(double decay, double epsilon) {
        // update parameters
        for (TObjectDoubleIterator<String> iter = cumulativeGradient.iterator(); iter.hasNext(); ) {
            iter.advance();
            //get the gradient
            double g = iter.value();

            double accum_g;
            //accumulate gradient for adaDelta
            if (DataPool.deltaGradientSq.containsKey(iter.key())) {
                accum_g = DataPool.deltaGradientSq.get(iter.key()) * decay + (1 - decay) * g * g;
            } else {
                accum_g = (1 - decay) * g * g;
            }
            DataPool.deltaGradientSq.put(iter.key(), accum_g);

            //compute delta i.e. step size
            double delta;
            //multiply update by RMS[delta X]_{t-1}
            if (DataPool.deltaVarSq.containsKey(iter.key())) {
                double oldDeltaVar = DataPool.deltaVarSq.get(iter.key());
                delta = g * (Math.sqrt(oldDeltaVar) + epsilon) / Math.sqrt(accum_g + epsilon);
                DataPool.deltaVarSq.put(iter.key(), oldDeltaVar * decay + (1 - decay) * delta * delta);
            } else {
                delta = g * epsilon / Math.sqrt(accum_g + epsilon);
                DataPool.deltaVarSq.put(iter.key(), (1 - decay) * delta * delta);
            }

//            System.out.println("delta "+ delta);
            //update weight for feature
            DataPool.weights.adjustOrPutValue(iter.key(), -delta, -delta);
        }
        // empty the cumulative gradient
        cumulativeGradient.clear();
    }
}