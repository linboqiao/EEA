package edu.cmu.cs.lti.script.annotators.learn.train;

import edu.cmu.cs.lti.script.features.CompactFeatureExtractor;
import edu.cmu.cs.lti.script.model.ContextElement;
import edu.cmu.cs.lti.script.model.LocalArgumentRepre;
import edu.cmu.cs.lti.script.model.LocalEventMentionRepre;
import edu.cmu.cs.lti.script.utils.DataPool;
import edu.cmu.cs.lti.script.type.Article;
import edu.cmu.cs.lti.script.type.EventMention;
import edu.cmu.cs.lti.script.type.Sentence;
import edu.cmu.cs.lti.uima.annotator.AbstractLoggingAnnotator;
import edu.cmu.cs.lti.utils.TokenAlignmentHelper;
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
 * Time: 3:32 PM
 */
public class ConstrastiveEstimationTrainer extends AbstractLoggingAnnotator {
    public static final String PARAM_NEGATIVE_NUMBERS = "negativeNumbers";

    public static final String PARAM_MINI_BATCH_SIZE = "miniBatchDocuments";

    public static final String PARAM_FEATURE_NAMES = "featureNames";

    public static final String PARAM_SKIP_GRAM_N = "skipgramn";

    TokenAlignmentHelper align = new TokenAlignmentHelper();
    CompactFeatureExtractor extractor;

    //some defaults
    int miniBatchSize = 100;
    int numNoise = 25;
    int skipGramN = 2;

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


    }

    private List<List<ContextElement>> neighbourhood(List<ContextElement> chain) {

        return null;
    }

}
