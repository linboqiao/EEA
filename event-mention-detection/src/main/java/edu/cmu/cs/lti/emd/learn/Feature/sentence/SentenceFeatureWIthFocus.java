package edu.cmu.cs.lti.emd.learn.feature.sentence;

import edu.cmu.cs.lti.script.type.StanfordCorenlpSentence;
import edu.cmu.cs.lti.script.type.StanfordCorenlpToken;
import edu.cmu.cs.lti.utils.Configuration;
import gnu.trove.map.TObjectDoubleMap;
import org.apache.uima.jcas.JCas;

import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * Date: 8/22/15
 * Time: 10:09 PM
 *
 * @author Zhengzhong Liu
 */
public abstract class SentenceFeatureWithFocus {
    Configuration config;

    public SentenceFeatureWithFocus(Configuration config) {
        this.config = config;
    }

    public String featureName() {
        return this.getClass().getName();
    }

    public abstract void initWorkspace(JCas context);

    public abstract void resetWorkspace(StanfordCorenlpSentence sentence);

    public abstract void extract(List<StanfordCorenlpToken> sentence, int focus,
                                 TObjectDoubleMap<String> features,
                                 TObjectDoubleMap<String> featuresNeedForState);
}
