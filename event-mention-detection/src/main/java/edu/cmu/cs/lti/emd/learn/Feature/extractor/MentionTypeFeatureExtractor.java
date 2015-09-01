package edu.cmu.cs.lti.emd.learn.feature.extractor;

import edu.cmu.cs.lti.emd.learn.feature.sentence.SentenceFeatureWithFocus;
import edu.cmu.cs.lti.emd.learn.feature.sentence.WordFeatures;
import edu.cmu.cs.lti.learning.model.Alphabet;
import edu.cmu.cs.lti.script.type.StanfordCorenlpSentence;
import edu.cmu.cs.lti.script.type.StanfordCorenlpToken;
import gnu.trove.map.TObjectDoubleMap;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * Date: 8/31/15
 * Time: 4:41 PM
 *
 * @author Zhengzhong Liu
 */
public class MentionTypeFeatureExtractor extends UimaSentenceFeatureExtractor {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    List<StanfordCorenlpToken> sentenceTokens;
    List<SentenceFeatureWithFocus> featureFunctions = new ArrayList<>();


    public MentionTypeFeatureExtractor(Alphabet alphabet) {
        super(alphabet);
        featureFunctions.add(new WordFeatures());
    }

    @Override
    public void init(JCas context) {
        super.init(context);
    }

    @Override
    public void resetWorkspace(StanfordCorenlpSentence sentence) {
        super.resetWorkspace(sentence);
        sentenceTokens = JCasUtil.selectCovered(StanfordCorenlpToken.class, sentence);
    }

    @Override
    public void extract(int focus, TObjectDoubleMap<String> featuresNoState,
                        TObjectDoubleMap<String> featuresNeedForState) {
//                logger.info("Extracting features at focus : " + focus);
        featureFunctions.forEach(ff -> ff.extract(sentenceTokens, focus, featuresNoState,
                featuresNeedForState));
//                logger.info("Done extracting : " + focus);
    }
}
