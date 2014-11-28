package edu.cmu.cs.lti.cds.annotators.script.train;

import edu.cmu.cs.lti.cds.dist.UnigramEventDist;
import edu.cmu.cs.lti.cds.ml.features.CompactFeatureExtractor;
import edu.cmu.cs.lti.cds.model.*;
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
import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;
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

    TokenAlignmentHelper align = new TokenAlignmentHelper();
    CompactFeatureExtractor extractor = new CompactFeatureExtractor(DataPool.compactWeights);

    int miniBatchDocNum = 100;
    int numNoise = 25;
    int skipGramN = 2;

    UnigramEventDist noiseDist = new UnigramEventDist();

    TLongShortDoubleHashTable cumulativeGradient = new TLongShortDoubleHashTable();

    double cumulativeObjective = 0;

    @Override
    public void initialize(UimaContext aContext) throws ResourceInitializationException {
        super.initialize(aContext);
        numNoise = (Integer) aContext.getConfigParameterValue(PARAM_NEGATIVE_NUMBERS);
        miniBatchDocNum = (Integer) aContext.getConfigParameterValue(PARAM_MINI_BATCH_SIZE);
    }

    @Override
    public void process(JCas aJCas) throws AnalysisEngineProcessException {
        Article article = JCasUtil.selectSingle(aJCas, Article.class);

        if (DataPool.blackListedArticleId.contains(article.getArticleName())) {
            logger.fine("Ignored black listed file : " + article.getArticleName());
            return;
        }

        align.loadWord2Stanford(aJCas);
        List<ChainElement> chain = new ArrayList<>();
        List<LocalArgumentRepre> arguments = new ArrayList<>();


        for (Sentence sent : JCasUtil.select(aJCas, Sentence.class)) {
            for (EventMention mention : JCasUtil.selectCovered(EventMention.class, sent)) {
                LocalEventMentionRepre eventRep = LocalEventMentionRepre.fromEventMention(mention, align);
                chain.add(new ChainElement(sent, eventRep));
                Collections.addAll(arguments, eventRep.getArgs());
            }
        }

        //for each sample
        for (int sampleIndex = 0; sampleIndex < chain.size(); sampleIndex++) {
            ChainElement realSample = chain.get(sampleIndex);
            TLongShortDoubleHashTable features = extractor.getFeatures(chain, realSample, sampleIndex, skipGramN, false);
            Sentence sampleSent = realSample.getSent();

            TIntList realArgumentEntityIds = new TIntArrayList();
            for (LocalArgumentRepre repre : realSample.getMention().getArgs()) {
                realArgumentEntityIds.add(repre.getEntityId());
            }

            //generate noise samples
            List<TLongShortDoubleHashTable> noiseSamples = new ArrayList<>();
            for (int i = 0; i < numNoise; i++) {
                Pair<MooneyEventRepre, Double> noise = noiseDist.draw();
                MooneyEventRepre noiseMooneyRepre = noise.getKey();
                int[] drawnArguments = noiseMooneyRepre.getAllArguments();
                LocalArgumentRepre[] noiseArguments = new LocalArgumentRepre[drawnArguments.length];

                for (int slotId = 0; slotId < drawnArguments.length; slotId++) {
                    int drawnArgument = drawnArguments[slotId];
                    if (drawnArgument == KmTargetConstants.nullArgMarker) {
                        noiseArguments[slotId] = null;
                    } else {
                        int drawnEntityId = realArgumentEntityIds.get(KmTargetConstants.argMarkerToSlotIndex(drawnArguments[slotId]));
                        //TODO might wanna use a real head word here
                        noiseArguments[slotId] = new LocalArgumentRepre(drawnEntityId, "");
                    }
                }

                LocalEventMentionRepre noiseRep = new LocalEventMentionRepre(noiseMooneyRepre.getPredicate(), noiseArguments);
                TLongShortDoubleHashTable noiseFeature = extractor.getFeatures(chain, new ChainElement(sampleSent, noiseRep), sampleIndex, skipGramN, true);
                if (noiseFeature != null) {
                    noiseSamples.add(noiseFeature);
                }
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
            logger.info("Committing " + cumulativeGradient.getNumRows() + " lexical pairs");
            cumulativeObjective = 0;
            update();
        }
    }

//    /**
//     * Check whether we have seen this before
//     * @return
//     */
//    private boolean isNegative(TLongShortDoubleHashTable noiseFeature) {
//
//    }

    @Override
    public void collectionProcessComplete() throws AnalysisEngineProcessException {
        logger.info("Finish one epoch, totally  " + DataPool.numSampleProcessed + " samples processed so far");
        logger.info("Features lexical pairs learnt " + DataPool.compactWeights.getNumRows());
        logger.info("Average cumulative gain for the residual batch: " + cumulativeObjective / (DataPool.numSampleProcessed % miniBatchDocNum));
        update();
    }

    private double gradientAscent(List<TLongShortDoubleHashTable> noiseSamples, TLongShortDoubleHashTable dataSample) {
        //start by assigning gradient as x_w
        // g = x_w
        TLongShortDoubleHashTable gradient = dataSample;

        double scoreTrue = DataPool.compactWeights.dotProd(dataSample);
        double sigmoidTrue = sigmoid(scoreTrue);

        //multiple x_w by (1- sigmoid)
        // g = ( Lw(u) - sigmoid(\theta * x_w) ) x_w, where Lw(u) = 1
        gradient.multiplyBy((1 - sigmoidTrue));

        //calculate local objective
        // log (sigmoid( score of true))
        double localObjective = Math.log(sigmoidTrue);

        for (TLongShortDoubleHashTable noiseSample : noiseSamples) {
            double scoreNoise = DataPool.compactWeights.dotProd(noiseSample);
            double sigmoidNoise = sigmoid(scoreNoise);

            // log (sigmoid( - score of noise))
            // i.e  log ( 1 - sigmoid(score of noise))
            localObjective += Math.log(1 - sigmoidNoise);

            //multiple sigmiod_noise by noise features x_w'
            // g += ( Lw'(u) - sigmoid(\theta * x_w') ) x_w', where Lw(u) = 0
            //note that this directly change the noise feature itself
            noiseSample.multiplyBy(sigmoidNoise);
            gradient.minusBy(noiseSample);
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

                    DataPool.compactWeights.adjustOrPutValue(rowIter.key(), cellIter.key(), update, update);
                }
            }
        }
    }
}