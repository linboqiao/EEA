package edu.cmu.cs.lti.emd.learn.Feature.impl;

import edu.cmu.cs.lti.emd.learn.Feature.EventMentionFeature;
import edu.cmu.cs.lti.script.type.CandidateEventMention;
import edu.cmu.cs.lti.script.type.CandidateEventMentionArgument;
import edu.cmu.cs.lti.script.type.StanfordCorenlpToken;
import org.apache.uima.fit.util.FSCollectionFactory;
import org.apache.uima.jcas.cas.FSList;

import java.util.HashMap;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: zhengzhongliu
 * Date: 3/4/15
 * Time: 5:01 PM
 */
public class FrameArgumentLemmaFeature extends EventMentionFeature {
    @Override
    public Map<String, Double> genFeatures(CandidateEventMention mention) {
        Map<String, Double> features = new HashMap<>();

        FSList argumentFs = mention.getArguments();
        if (argumentFs != null) {
            for (CandidateEventMentionArgument argument : FSCollectionFactory.create(argumentFs, CandidateEventMentionArgument.class)) {
                StanfordCorenlpToken argumentHeadWord = argument.getHeadWord();
                features.put("FrameArgument_" + argumentHeadWord.getLemma().toLowerCase(), 1.0);
            }
        }
        return features;
    }
}
