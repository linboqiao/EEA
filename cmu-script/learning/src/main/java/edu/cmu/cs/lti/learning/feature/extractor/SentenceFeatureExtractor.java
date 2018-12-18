package edu.cmu.cs.lti.learning.feature.extractor;

import edu.cmu.cs.lti.learning.model.FeatureAlphabet;
import edu.cmu.cs.lti.script.type.StanfordCorenlpToken;
import edu.cmu.cs.lti.utils.Configuration;
import gnu.trove.map.hash.TObjectIntHashMap;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;

import java.lang.reflect.InvocationTargetException;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * Date: 9/12/15
 * Time: 3:39 PM
 *
 * @author Zhengzhong Liu
 */
public class SentenceFeatureExtractor extends UimaSequenceFeatureExtractor<StanfordCorenlpToken> {
    private List<StanfordCorenlpToken> sentenceTokens;

    public SentenceFeatureExtractor(FeatureAlphabet alphabet, Configuration generalConfig, Configuration
            sentenceFeatureConfig, Configuration mentionFeatureConfig, boolean useStateFeatures)
            throws ClassNotFoundException,
            NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        super(alphabet, generalConfig, sentenceFeatureConfig, mentionFeatureConfig, useStateFeatures,
                StanfordCorenlpToken.class);
    }

    @Override
    public void resetWorkspace(JCas aJCas, int begin, int end) {
        super.resetWorkspace(aJCas, begin, end);

        sentenceTokens = JCasUtil.selectCovered(aJCas, StanfordCorenlpToken.class, begin, end);
        elementIndex = new TObjectIntHashMap<>();

        int i = 0;
        for (StanfordCorenlpToken token : sentenceTokens) {
            elementIndex.put(token, i++);
        }
    }

    @Override
    protected int getSentenceFocus(int sequenceFocus) {
        return sequenceFocus;
    }

    @Override
    protected List<StanfordCorenlpToken> getSentenceTokens(int elementFocus) {
        return sentenceTokens;
    }
}
