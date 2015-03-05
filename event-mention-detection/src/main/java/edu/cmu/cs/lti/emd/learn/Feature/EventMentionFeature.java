package edu.cmu.cs.lti.emd.learn.Feature;

import edu.cmu.cs.lti.script.type.CandidateEventMention;

import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: zhengzhongliu
 * Date: 3/4/15
 * Time: 4:51 PM
 */
public abstract class EventMentionFeature {
    public abstract Map<String, Double> genFeatures(CandidateEventMention mention);
}