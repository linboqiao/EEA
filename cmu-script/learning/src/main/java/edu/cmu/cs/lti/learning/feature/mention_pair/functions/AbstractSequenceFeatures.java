package edu.cmu.cs.lti.learning.feature.mention_pair.functions;

import edu.cmu.cs.lti.learning.feature.mention_pair.functions.sequence.SequenceFeatureUtils;
import edu.cmu.cs.lti.utils.Configuration;

import java.io.IOException;

/**
 * Created with IntelliJ IDEA.
 * Date: 2/4/17
 * Time: 3:17 PM
 *
 * @author Zhengzhong Liu
 */
public abstract class AbstractSequenceFeatures extends AbstractMentionPairFeatures {
    SequenceFeatureUtils utils;

    public AbstractSequenceFeatures(Configuration generalConfig, Configuration featureConfig) {
        super(generalConfig, featureConfig);
        try {
            utils = new SequenceFeatureUtils(generalConfig);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
