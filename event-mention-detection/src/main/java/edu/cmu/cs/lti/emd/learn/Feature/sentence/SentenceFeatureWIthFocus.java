package edu.cmu.cs.lti.emd.learn.feature.sentence;

import edu.cmu.cs.lti.script.type.StanfordCorenlpToken;
import gnu.trove.map.TObjectDoubleMap;

import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * Date: 8/22/15
 * Time: 10:09 PM
 *
 * @author Zhengzhong Liu
 */
public abstract class SentenceFeatureWithFocus {
//    /**
//     * Extract features from the sentence. The implementer should cache the features to the cacher. The choice of
// using
//     * the which cache (with state or not) is delegate to the feature implementation. The implemented features can
//     * also cache two versions (ie. with and without state), as long as the cache logic and reading logic match (i.e.
//     * read in the same way that they are stored.)
//     *
//     * @param cacher   A cacher that can cache feature vector.
//     * @param key      The current state of the training, contains enough information for caching and locating the
//     *                 focus.
//     * @param fv       The feature vector to be filled.
//     * @param sentence The sentence tokens.
//     */
//    public abstract void extract(CrfFeatureCacher cacher, CrfState key, HashedFeatureVector fv,
//                                 List<StanfordCorenlpToken> sentence);

    public String featureName() {
        return this.getClass().getName();
    }

    public abstract void extract(List<StanfordCorenlpToken> sentence, int focus, TObjectDoubleMap<String> features,
                                 TObjectDoubleMap<String> featuresNeedForState);
}
