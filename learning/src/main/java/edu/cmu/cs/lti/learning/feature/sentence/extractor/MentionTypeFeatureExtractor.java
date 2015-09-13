package edu.cmu.cs.lti.learning.feature.sentence.extractor;

import edu.cmu.cs.lti.learning.feature.sentence.functions.*;
import edu.cmu.cs.lti.learning.model.FeatureAlphabet;
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
public class MentionTypeFeatureExtractor extends UimaSequenceFeatureExtractor {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private List<StanfordCorenlpToken> sentenceTokens;
    private List<SequenceFeatureWithFocus> featureFunctions = new ArrayList<>();


    public MentionTypeFeatureExtractor(FeatureAlphabet alphabet, Configuration kbpConfig) {
        super(alphabet);
        featureFunctions.add(new WindowWordFeatures(kbpConfig));
        featureFunctions.add(new BrownClusterFeatures(kbpConfig));
        featureFunctions.add(new FrameFeatures(kbpConfig));
        featureFunctions.add(new DependentWordFeatures(kbpConfig));
    }

    @Override
    public void initWorkspace(JCas context) {
        super.initWorkspace(context);
        for (SequenceFeatureWithFocus ff : featureFunctions) {
            ff.initDocumentWorkspace(context);
        }
    }

    @Override
    public void resetWorkspace(JCas aJCas, int begin, int end) {
        sentenceTokens = JCasUtil.selectCovered(aJCas, StanfordCorenlpToken.class, begin, end);

        for (SequenceFeatureWithFocus ff : featureFunctions) {
            ff.resetWorkspace(aJCas, begin, end);
        }
    }

    @Override
    public void extract(int focus, TObjectDoubleMap<String> featuresNoState,
                        TObjectDoubleMap<String> featuresNeedForState) {
//                logger.info("Extracting features at focus : " + focus);
        featureFunctions.forEach(ff -> ff.extract(sentenceTokens, focus, featuresNoState, featuresNeedForState));
    }

}
