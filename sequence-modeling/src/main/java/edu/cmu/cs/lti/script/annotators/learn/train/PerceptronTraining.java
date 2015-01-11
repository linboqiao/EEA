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
 * Created with IntelliJ IDEA.
 * User: zhengzhongliu
 * Date: 1/7/15
 * Time: 10:48 PM
 */
public class PerceptronTraining extends AbstractLoggingAnnotator {
    public static final String PARAM_RANK_LIST_SIZE = "rankListSize";

    public static final String PARAM_MINI_BATCH_SIZE = "miniBatchDocuments";

    public static final String PARAM_FEATURE_NAMES = "featureNames";

    public static final String PARAM_SKIP_GRAM_N = "skipgramn";

    TokenAlignmentHelper align = new TokenAlignmentHelper();
    CompactFeatureExtractor extractor;

    //some defaults
    int miniBatchSize = 100;
    int rankListSize = 25;
    int numArguments = 3;

    int skipGramN = 2;

    //    UnigramEventDist noiseDist = new UnigramEventDist(DataPool.unigramCounts, DataPool.eventUnigramTotalCount);
    GlobalUnigrmHwLocalUniformArgumentDist noiseDist = new GlobalUnigrmHwLocalUniformArgumentDist();

    TLongShortDoubleHashTable cumulativeGradient = new TLongShortDoubleHashTable();

    double cumulativeObjective = 0;

    @Override
    public void initialize(UimaContext aContext) throws ResourceInitializationException {
        super.initialize(aContext);
        rankListSize = (Integer) aContext.getConfigParameterValue(PARAM_RANK_LIST_SIZE);
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
            TLongShortDoubleHashTable correctFeatures = extractor.getFeatures(chain, realSample, sampleIndex, skipGramN, false);
            Sentence sampleSent = realSample.getSent();

            double bestSampleScore = Double.MIN_VALUE;
            LocalEventMentionRepre bestSample = null;
            TLongShortDoubleHashTable currentBestSampleFeature = null;

            for (LocalEventMentionRepre sample : sampleCandidiates(arguments, sampleSent)) {
                TLongShortDoubleHashTable sampleFeature = extractor.getFeatures(chain, new ContextElement(aJCas, sampleSent, realSample.getHead(), sample), sampleIndex, skipGramN, true);
                double sampleScore = DataPool.trainingUsedCompactWeights.dotProd(sampleFeature);
                if (currentBestSampleFeature == null || sampleScore > bestSampleScore) {
                    currentBestSampleFeature = sampleFeature;
                    bestSample = sample;
                    bestSampleScore = sampleScore;
                }
            }

            if (!realSample.getMention().mooneyMatch(bestSample)) {
                perceptronUpdate(correctFeatures, currentBestSampleFeature);
            }
        }
    }

    private List<LocalEventMentionRepre> sampleCandidiates(List<LocalArgumentRepre> arguments, Sentence sampleSent) {
        List<LocalEventMentionRepre> samples = new ArrayList<>();

        for (int i = 0; i < rankListSize; i++) {
            Pair<LocalEventMentionRepre, Double> noise = noiseDist.draw(arguments, numArguments);
            LocalEventMentionRepre noiseRep = noise.getKey();
            samples.add(noiseRep);
        }
        return samples;
    }

    private void perceptronUpdate(TLongShortDoubleHashTable correctFeatures, TLongShortDoubleHashTable currentBest) {
        DataPool.trainingUsedCompactWeights.adjustBy(correctFeatures, 1);
        DataPool.trainingUsedCompactWeights.adjustBy(currentBest, -1);
    }
}
