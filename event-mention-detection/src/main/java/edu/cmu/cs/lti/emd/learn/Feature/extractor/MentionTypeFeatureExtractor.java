package edu.cmu.cs.lti.emd.learn.feature.extractor;

import edu.cmu.cs.lti.emd.learn.feature.sentence.*;
import edu.cmu.cs.lti.learning.model.Alphabet;
import edu.cmu.cs.lti.script.type.StanfordCorenlpSentence;
import edu.cmu.cs.lti.script.type.StanfordCorenlpToken;
import edu.cmu.cs.lti.utils.Configuration;
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

    private List<StanfordCorenlpToken> sentenceTokens;
    private List<SentenceFeatureWithFocus> featureFunctions = new ArrayList<>();


    public MentionTypeFeatureExtractor(Alphabet alphabet, Configuration kbpConfig) {
        super(alphabet);
        featureFunctions.add(new WindowWordFeatures(kbpConfig));
        featureFunctions.add(new BrownClusterFeatures(kbpConfig));
        featureFunctions.add(new FrameFeatures(kbpConfig));
        featureFunctions.add(new DependentWordFeatures(kbpConfig));
    }

    @Override
    public void initWorkspace(JCas context) {
        super.initWorkspace(context);
        for (SentenceFeatureWithFocus ff : featureFunctions) {
            ff.initWorkspace(context);
        }
    }

    @Override
    public void resetWorkspace(StanfordCorenlpSentence sentence) {
        super.resetWorkspace(sentence);
        sentenceTokens = JCasUtil.selectCovered(StanfordCorenlpToken.class, sentence);

        for (SentenceFeatureWithFocus ff : featureFunctions) {
            ff.resetWorkspace(sentence);
        }
    }

    @Override
    public void extract(int focus, TObjectDoubleMap<String> featuresNoState,
                        TObjectDoubleMap<String> featuresNeedForState) {
//                logger.info("Extracting features at focus : " + focus);
        featureFunctions.forEach(ff -> ff.extract(sentenceTokens, focus, featuresNoState, featuresNeedForState));
    }

}
