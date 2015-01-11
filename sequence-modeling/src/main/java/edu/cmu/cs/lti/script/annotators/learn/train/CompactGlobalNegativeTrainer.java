package edu.cmu.cs.lti.script.annotators.learn.train;

import edu.cmu.cs.lti.script.dist.GlobalUnigrmHwLocalUniformArgumentDist;
import edu.cmu.cs.lti.script.features.CompactFeatureExtractor;
import edu.cmu.cs.lti.script.model.ContextElement;
import edu.cmu.cs.lti.script.model.LocalArgumentRepre;
import edu.cmu.cs.lti.script.model.LocalEventMentionRepre;
import edu.cmu.cs.lti.script.utils.DataPool;
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
import java.util.Collections;
import java.util.List;

/**
 * The global negative trainer only sample negative samples that are globally non-negative
 * which means if there are observations of some features, it will not use them
 * <p/>
 * Created with IntelliJ IDEA.
 * User: zhengzhongliu
 * Date: 10/31/14
 * Time: 5:52 PM
 */
public class CompactGlobalNegativeTrainer extends AbstractLoggingAnnotator {
    public static final String PARAM_NEGATIVE_NUMBERS = "negativeNumbers";

    public static final String PARAM_MINI_BATCH_SIZE = "miniBatchDocuments";

    public static final String PARAM_FEATURE_NAMES = "featureNames";

    public static final String PARAM_SKIP_GRAM_N = "skipgramn";

    TokenAlignmentHelper align = new TokenAlignmentHelper();
    CompactFeatureExtractor extractor;

    //some defaults
    int miniBatchSize = 100;
    int numNoise = 25;
    int numArguments = 3;

    int skipGramN = 2;

    //    UnigramEventDist noiseDist = new UnigramEventDist(DataPool.unigramCounts, DataPool.eventUnigramTotalCount);
    GlobalUnigrmHwLocalUniformArgumentDist noiseDist = new GlobalUnigrmHwLocalUniformArgumentDist();

    TLongShortDoubleHashTable cumulativeGradient = new TLongShortDoubleHashTable();

    double cumulativeObjective = 0;

    @Override
    public void initialize(UimaContext aContext) throws ResourceInitializationException {
        super.initialize(aContext);
        numNoise = (Integer) aContext.getConfigParameterValue(PARAM_NEGATIVE_NUMBERS);
        miniBatchSize = (Integer) aContext.getConfigParameterValue(PARAM_MINI_BATCH_SIZE);
        String[] featureImplNames = (String[]) aContext.getConfigParameterValue(PARAM_FEATURE_NAMES);
        skipGramN = (Integer) aContext.getConfigParameterValue(PARAM_SKIP_GRAM_N);
        try {
            extractor = new CompactFeatureExtractor(DataPool.trainingUsedCompactWeights, featureImplNames);
        } catch (IllegalAccessException | InstantiationException | ClassNotFoundException e) {
            e.printStackTrace();
        }
        DataPool.numSampleProcessed = 0;
    }

    @Override
    public void process(JCas aJCas) throws AnalysisEngineProcessException {
        Article article = JCasUtil.selectSingle(aJCas, Article.class);

        if (DataPool.blackListedArticleId.contains(article.getArticleName())) {
            logger.fine("Ignored black listed file : " + article.getArticleName());
            return;
        }

        align.loadWord2Stanford(aJCas);
        List<ContextElement> chain = new ArrayList<>();
        List<LocalArgumentRepre> arguments = new ArrayList<>();

        for (Sentence sent : JCasUtil.select(aJCas, Sentence.class)) {
            for (EventMention mention : JCasUtil.selectCovered(EventMention.class, sent)) {
                LocalEventMentionRepre eventRep = LocalEventMentionRepre.fromEventMention(mention, align);
                chain.add(new ContextElement(aJCas, sent, mention.getHeadWord(), eventRep));
                Collections.addAll(arguments, eventRep.getArgs());
            }
        }

        //for each sample
        for (int sampleIndex = 0; sampleIndex < chain.size(); sampleIndex++) {
            ContextElement realSample = chain.get(sampleIndex);
            TLongShortDoubleHashTable features = extractor.getFeatures(chain, realSample, sampleIndex, skipGramN, false);
            Sentence sampleSent = realSample.getSent();
            //generate noise samples
            List<TLongShortDoubleHashTable> noiseSamples = new ArrayList<>();
            for (int i = 0; i < numNoise; i++) {
                Pair<LocalEventMentionRepre, Double> noise = noiseDist.draw(arguments, numArguments);
                LocalEventMentionRepre noiseRep = noise.getKey();
                TLongShortDoubleHashTable noiseFeature = extractor.getFeatures(chain, new ContextElement(aJCas, sampleSent, realSample.getHead(), noiseRep), sampleIndex, skipGramN, true);
                if (noiseFeature != null) {
                    noiseSamples.add(noiseFeature);
                }
            }
            //cumulative the gradient so far, and compute sample cost
            this.cumulativeObjective += gradientAscent(noiseSamples, features);
        }

        //treat one chain as one sample
        DataPool.numSampleProcessed++;

        if (DataPool.numSampleProcessed % miniBatchSize == 0) {
            logger.info("Features lexical pairs just learnt " + DataPool.trainingUsedCompactWeights.getNumRows());
            logger.info("Processed " + DataPool.numSampleProcessed + " samples");
            logger.info("Average gain for previous batch is : " + cumulativeObjective / miniBatchSize);
            logger.info("Committing " + cumulativeGradient.getNumRows() + " lexical pairs");
            update();
        }
    }

    @Override
    public void collectionProcessComplete() throws AnalysisEngineProcessException {
        logger.info("Finish one epoch, totally  " + DataPool.numSampleProcessed + " samples processed so far");
        logger.info("Processed " + DataPool.numSampleProcessed + " samples. Residual size is " + DataPool.numSampleProcessed % miniBatchSize);
        logger.info("Features lexical pairs learnt " + DataPool.trainingUsedCompactWeights.getNumRows());
        logger.info("Average cumulative gain for the residual batch: " + cumulativeObjective / (DataPool.numSampleProcessed % miniBatchSize));
        update();
//        DataPool.trainingUsedCompactWeights.dump(new PrintWriter(System.err));
    }

    private double gradientAscent(List<TLongShortDoubleHashTable> noiseSamples, TLongShortDoubleHashTable dataSample) {
        //start by assigning gradient as x_w
        // g = x_w
        TLongShortDoubleHashTable gradient = dataSample;

//        double scoreTrue = DataPool.trainingUsedCompactWeights.dotProd(dataSample, extractor.getFeatureNamesByIndex());
        double scoreTrue = DataPool.trainingUsedCompactWeights.dotProd(dataSample);

        double sigmoidTrue = sigmoid(scoreTrue);

        //multiple x_w by (1- sigmoid)
        // g = ( Lw(u) - sigmoid(\theta * x_w) ) x_w, where Lw(u) = 1
        gradient.multiplyBy((1 - sigmoidTrue));

        //calculate local objective
        // log (sigmoid( score of true))
        double localObjective = Math.log(sigmoidTrue);

//        if (scoreTrue != 0) {
//            System.err.println("True scores " + scoreTrue + " " + sigmoidTrue + " ");
//            DataPool.trainingUsedCompactWeights.dotProd(dataSample, extractor.getFeatureNamesByIndex());
//            System.err.println(dataSample.dump(DataPool.headWords, extractor.getFeatureNamesByIndex()));
//        }

        for (TLongShortDoubleHashTable noiseSample : noiseSamples) {
//            double scoreNoise = DataPool.trainingUsedCompactWeights.dotProd(dataSample, extractor.getFeatureNamesByIndex());
            double scoreNoise = DataPool.trainingUsedCompactWeights.dotProd(noiseSample);
            double sigmoidNoise = sigmoid(scoreNoise);

//            if (scoreNoise != 0) {
//                System.err.println("Noise scores " + scoreTrue + " " + sigmoidTrue + " ");
//                DataPool.trainingUsedCompactWeights.dotProd(dataSample, extractor.getFeatureNamesByIndex());
//            }

            // log (sigmoid( - score of noise))
            // i.e  log ( 1 - sigmoid(score of noise))
            localObjective += Math.log(1 - sigmoidNoise);

            //multiple sigmiod_noise by noise features x_w'
            // g += ( Lw'(u) - sigmoid(\theta * x_w') ) x_w', where Lw(u) = 0
            //note that this directly change the noise feature itself
            noiseSample.multiplyBy(sigmoidNoise);
            gradient.minusBy(noiseSample);

//            if (scoreNoise < 0) {
//            System.err.println(scoreNoise + " " + sigmoidNoise);
//            System.err.println(noiseSample.dump(DataPool.headWords, extractor.getFeatureNamesByIndex()));
//            }
        }

        //update the cumulative gradient;
        for (TLongObjectIterator<TShortDoubleMap> rowIter = gradient.iterator(); rowIter.hasNext(); ) {
            rowIter.advance();
            for (TShortDoubleIterator cellIter = rowIter.value().iterator(); cellIter.hasNext(); ) {
                cellIter.advance();
                cumulativeGradient.adjustOrPutValue(rowIter.key(), cellIter.key(), cellIter.value(), cellIter.value());
            }
        }
        //return the objective
        return localObjective;
    }

    private double sigmoid(double x) {
        return 1 / (1 + Math.exp(-x));
    }

    private void update() {
        cumulativeObjective = 0;
        Utils.printMemInfo(logger);
        logger.info("Updating");
        adaGradUpdate(0.1);
        logger.info("Update Done");
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
                    if (Double.isNaN(g)) {
                        System.out.println(rowIter.key() + " " + rowIter.value() + update);
                    }
                    DataPool.trainingUsedCompactWeights.adjustOrPutValue(rowIter.key(), cellIter.key(), update, update);
                }
            }
        }
    }
}