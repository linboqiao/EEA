package edu.cmu.cs.lti.learning.feature.extractor;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import edu.cmu.cs.lti.learning.model.FeatureAlphabet;
import edu.cmu.cs.lti.learning.model.FeatureVector;
import edu.cmu.cs.lti.learning.model.MentionKey;

import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * Date: 8/21/15
 * Time: 1:47 PM
 *
 * @author Zhengzhong Liu
 */
public abstract class ChainFeatureExtractor {
    protected FeatureAlphabet alphabet;

    public ChainFeatureExtractor(FeatureAlphabet alphabet) {
        this.alphabet = alphabet;
    }

    public void extract(int focus, FeatureVector nodeFeatures) {
        extract(focus, nodeFeatures, HashBasedTable.create());
    }

    public abstract void extract(int focus, FeatureVector nodeFeatures,
                                 Table<Integer, Integer, FeatureVector> edgeFeatures);

    public abstract void extractGlobal(int focus, FeatureVector globalFeatures, List<MentionKey> knownStates,
                                       MentionKey currentState);

}
