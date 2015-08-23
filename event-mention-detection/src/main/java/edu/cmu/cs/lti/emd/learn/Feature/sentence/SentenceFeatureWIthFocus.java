package edu.cmu.cs.lti.emd.learn.feature.sentence;

import edu.cmu.cs.lti.learning.model.HashedFeatureVector;
import edu.cmu.cs.lti.script.type.StanfordCorenlpToken;

import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * Date: 8/22/15
 * Time: 10:09 PM
 *
 * @author Zhengzhong Liu
 */
public abstract class SentenceFeatureWithFocus {
    public abstract void extract(HashedFeatureVector fv, List<StanfordCorenlpToken> sentence, int focus, int
            previousStateValue);
}
