package edu.cmu.cs.lti.emd.learn.Feature.impl;

import edu.cmu.cs.lti.emd.learn.Feature.EventMentionFeatureGenerator;
import edu.cmu.cs.lti.script.type.CandidateEventMention;
import org.apache.uima.fit.util.FSCollectionFactory;
import org.apache.uima.jcas.cas.StringList;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Created with IntelliJ IDEA.
 * User: zhengzhongliu
 * Date: 3/4/15
 * Time: 4:53 PM
 */
public class FrameNameFeatureGenerator extends EventMentionFeatureGenerator {

    public FrameNameFeatureGenerator(Set<String> featureSubset) {
        super(featureSubset);
    }

    @Override
    public Map<String, Double> genFeatures(CandidateEventMention mention) {
        Map<String, Double> features = new HashMap<>();

        StringList potentialFrameFs = mention.getPotentialFrames();

        if (potentialFrameFs != null) {
            for (String potentialFrame : FSCollectionFactory.create(potentialFrameFs)) {
                addFeature("FrameName", potentialFrame, features);
            }
        }
        return features;
    }
}