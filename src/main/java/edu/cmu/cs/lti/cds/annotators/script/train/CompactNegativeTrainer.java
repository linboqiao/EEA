package edu.cmu.cs.lti.cds.annotators.script.train;

import edu.cmu.cs.lti.cds.dist.GlobalUnigrmHwLocalUniformArgumentDist;
import edu.cmu.cs.lti.cds.ml.features.CompactFeatureExtractor;
import edu.cmu.cs.lti.cds.model.ChainElement;
import edu.cmu.cs.lti.cds.model.LocalEventMentionRepre;
import edu.cmu.cs.lti.cds.utils.DataPool;
import edu.cmu.cs.lti.collections.TLongShortDoubleHashTable;
import edu.cmu.cs.lti.script.type.Article;
import edu.cmu.cs.lti.script.type.EventMention;
import edu.cmu.cs.lti.script.type.Sentence;
import edu.cmu.cs.lti.uima.annotator.AbstractLoggingAnnotator;
import edu.cmu.cs.lti.utils.TokenAlignmentHelper;
import edu.cmu.cs.lti.utils.Utils;
import gnu.trove.iterator.TLongObjectIterator;
import gnu.trove.iterator.TShortDoubleIterator;
import gnu.trove.map.TShortDoubleMap;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;

import java.util.ArrayList;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: zhengzhongliu
 * Date: 10/31/14
 * Time: 5:52 PM
 */
public class CompactNegativeTrainer extends AbstractLoggingAnnotator {
    public static final String PARAM_NEGATIVE_NUMBERS = "negativeNumbers";

    TokenAlignmentHelper align = new TokenAlignmentHelper();
    CompactFeatureExtractor extractor = new CompactFeatureExtractor();

    int miniBatchDocNum = 100;
    int numNoise = 25;
    int skipGramN = 2;

    int numArguments = 3;

    double stepSize = 0.01;

    GlobalUnigrmHwLocalUniformArgumentDist noiseDist = new GlobalUnigrmHwLocalUniformArgumentDist();

    TLongShortDoubleHashTable cumulativeGradient = new TLongShortDoubleHashTable();

    double cumulativeObjective = 0;

    @Override
    public void initialize(UimaContext aContext) throws ResourceInitializationException {
        super.initialize(aContext);
        numNoise = (Integer) aContext.getConfigParameterValue(PARAM_NEGATIVE_NUMBERS);
    }

    @Override
    public void process(JCas aJCas) throws AnalysisEngineProcessException {
        Article article = JCasUtil.selectSingle(aJCas, Article.class);
//        try {
//            StochasticNegativeTrainer.trainOut.write(progressInfo(aJCas) + "\n");
//        } catch (IOException e) {
//            e.printStackTrace();
//        }

        if (DataPool.blackListedArticleId.contains(article.getArticleName())) {
            //ignore this blacklisted file;
            logger.info("Ignored black listed file");
            return;
        }

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
            TLongShortDoubleHashTable features = extractor.getFeatures(chain, align, realSample, sampleIndex, skipGramN);
            Sentence sampleSent = realSample.getSent();

            //generate noise samples
            List<TLongShortDoubleHashTable> noiseSamples = new ArrayList<>();
            for (int i = 0; i < numNoise; i++) {
                Pair<LocalEventMentionRepre, Double> noise = noiseDist.draw(arguments, numArguments);
                TLongShortDoubleHashTable noiseFeature = extractor.getFeatures(chain, align, new ChainElement(sampleSent, noise.getLeft()), sampleIndex, skipGramN);
                noiseSamples.add(noiseFeature);
            }

            //cumulative the gradient so far, and compute sample cost
            double cumulativeObjective = gradientAscent(noiseSamples, features);
            this.cumulativeObjective += cumulativeObjective;
        }

        DataPool.numSampleProcessed++;

        if (DataPool.numSampleProcessed % miniBatchDocNum == 0) {
            logger.info("Features lexical pairs learnt " + DataPool.compactWeights.getNumRows());
            logger.info("Processed " + DataPool.numSampleProcessed + " samples");
            logger.info("Average gain for previous batch is : " + cumulativeObjective / miniBatchDocNum);
            cumulativeObjective = 0;
            update();
        }
    }

    @Override
    public void collectionProcessComplete() throws AnalysisEngineProcessException {
        logger.info("Finish one epoch, totally  " + DataPool.numSampleProcessed + " samples processed so far");
        logger.info("Features lexical pairs " + DataPool.compactWeights.getNumRows());
        logger.info("Average cumulative gain for the residual batch: " + cumulativeObjective / (DataPool.numSampleProcessed % miniBatchDocNum));
        update();
    }

    private double gradientAscent(List<TLongShortDoubleHashTable> noiseSamples, TLongShortDoubleHashTable dataSample) {
        //start by assigning gradient as x_w
        // g = x_w
        TLongShortDoubleHashTable gradient = dataSample;

        double scoreTrue = DataPool.compactWeights.dotProd(dataSample);
        double sigmoidTrue = sigmoid(scoreTrue);

//        try {
//            StochasticNegativeTrainer.trainOut.write("Sigmoid true for  " + dataSample + "  is " + scoreTrue + "\n");
//        } catch (IOException e) {
//            e.printStackTrace();
//        }

        //multiple x_w by (1- sigmoid)
        // g = ( Lw(u) - sigmoid(\theta * x_w) ) x_w, where Lw(u) = 1
        gradient.multiplyBy((1 - sigmoidTrue));

        //calculate local objective
        // log (sigmoid( score of true))
        double localObjective = Math.log(sigmoidTrue);

        for (TLongShortDoubleHashTable noiseSample : noiseSamples) {
            double scoreNoise = DataPool.compactWeights.dotProd(noiseSample);
            double sigmoidNoise = sigmoid(scoreNoise);

//            try {
//                StochasticNegativeTrainer.trainOut.write("Sigmoid noise for  " + noiseSample + "  is " + scoreNoise + "\n");
//            } catch (IOException e) {
//                e.printStackTrace();
//            }

            TLongShortDoubleHashTable noiseFeatures = noiseSample;

            // log (sigmoid( - score of noise))
            // i.e  log ( 1 - sigmoid(score of noise))
            localObjective += Math.log(1 - sigmoidNoise);

            //multiple sigmiod_noise by noise features x_w'
            // g += ( Lw'(u) - sigmoid(\theta * x_w') ) x_w', where Lw(u) = 0
            //note that this directly change the noise feature itself
            noiseFeatures.multiplyBy(sigmoidNoise);
            gradient.minusBy(noiseFeatures);
        }

        //update the cumulative gradient;
        for (TLongObjectIterator<TShortDoubleMap> rowIter = gradient.iterator(); rowIter.hasNext(); ) {
            rowIter.advance();
            for (TShortDoubleIterator cellIter = rowIter.value().iterator(); cellIter.hasNext(); ) {
                cellIter.advance();
                cumulativeGradient.adjustOrPutValue(rowIter.key(), cellIter.key(), cellIter.value(), cellIter.value());
//            try {
//                StochasticNegativeTrainer.trainOut.write(rowIter.key() + " " + rowIter.value() + "\n");
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
            }
        }

        //return the objective
        return localObjective;
    }

    private double sigmoid(double x) {
        return 1 / (1 + Math.exp(-x));
    }

    private void update() {
        Utils.printMemInfo(logger);
//        adaDeltaUpdate(1e-3, 0.95);
        adaGradUpdate(0.01);
//        stepSizeUpdate();
    }

    private void stepSizeUpdate() {
        // update parameters
        for (TLongObjectIterator<TShortDoubleMap> rowIter = cumulativeGradient.iterator(); rowIter.hasNext(); ) {
            rowIter.advance();
            for (TShortDoubleIterator cellIter = rowIter.value().iterator(); cellIter.hasNext(); ) {
                cellIter.advance();

                double u = stepSize * cellIter.value();
//            try {
//                StochasticNegativeTrainer.trainOut.write("Update for " + rowIter.key() + " is " + u + " : " + stepSize + "*" + rowIter.value() + "\n");
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
                DataPool.compactWeights.adjustOrPutValue(rowIter.key(), cellIter.key(), u, u);
            }
        }
        // empty the cumulative gradient
        cumulativeGradient.clear();
    }

    private void adaGradUpdate(double eta) {
        for (TLongObjectIterator<TShortDoubleMap> rowIter = cumulativeGradient.iterator(); rowIter.hasNext(); ) {
            rowIter.advance();

            for (TShortDoubleIterator cellIter = rowIter.value().iterator(); cellIter.hasNext(); ) {
                cellIter.advance();

                double g = cellIter.value();
                if (g != 0) {
                    double gSq = g * g;
                    double cumulativeGsq = DataPool.compactAdaGradMemory.adjustOrPutValue(rowIter.key(), cellIter.key(), gSq, gSq);
                    double update = eta * g / Math.sqrt(cumulativeGsq);
//                    if (Double.isNaN(g)) {
//                        System.out.println(rowIter.key() + " " + rowIter.value() + update);
//                    }
                    DataPool.compactWeights.adjustOrPutValue(rowIter.key(), cellIter.key(), update, update);
                }
            }
        }
    }
}