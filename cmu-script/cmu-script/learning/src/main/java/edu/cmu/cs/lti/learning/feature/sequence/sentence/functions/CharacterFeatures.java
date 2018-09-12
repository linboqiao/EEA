package edu.cmu.cs.lti.learning.feature.sequence.sentence.functions;

import edu.cmu.cs.lti.learning.feature.sequence.base.SequenceFeatureWithFocus;
import edu.cmu.cs.lti.script.type.StanfordCorenlpToken;
import edu.cmu.cs.lti.utils.Configuration;
import gnu.trove.map.TObjectDoubleMap;
import org.apache.uima.jcas.JCas;

import java.util.List;

/**
 * Extract character related features from the token. This is mainly used for language such as Chinese, where
 * characters can carry significant semantic.
 * Date: 2/9/16
 * Time: 6:38 PM
 *
 * @author Zhengzhong Liu
 */
public class CharacterFeatures extends SequenceFeatureWithFocus<StanfordCorenlpToken> {
    public CharacterFeatures(Configuration generalConfig, Configuration featureConfig) {
        super(generalConfig, featureConfig);
    }

    @Override
    public void initDocumentWorkspace(JCas context) {

    }

    @Override
    public void resetWorkspace(JCas aJCas) {

    }

    @Override
    public void extract(List<StanfordCorenlpToken> sequence, int focus, TObjectDoubleMap<String> features,
                        TObjectDoubleMap<String> featuresNeedForState) {
        if (focus < 0 || focus >= sequence.size()) {
            return;
        }

        StanfordCorenlpToken targetToken = sequence.get(focus);

        char[] characters = targetToken.getCoveredText().toCharArray();
        for (int index = 0; index < characters.length; index++) {
            char character = characters[index];
            addToFeatures(features, String.format("ContainsChar:%s", character), 1);
            addToFeatures(features, String.format("ContainsChar:%sWithPos:%s", character, targetToken.getPos()), 1);

            if (index == 0) {
                addToFeatures(features, String.format("BeginChar:%s", character), 1);
            }

            if (index == characters.length - 1) {
                addToFeatures(features, String.format("EndChar:%s", character), 1);
            }
        }
    }
}
