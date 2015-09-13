package edu.cmu.cs.lti.learning.feature.sentence.generator;

import edu.cmu.cs.lti.script.type.CandidateEventMention;

import java.util.Map;
import java.util.Set;

/**
 * Created with IntelliJ IDEA.
 * User: zhengzhongliu
 * Date: 3/4/15
 * Time: 4:51 PM
 */
public abstract class EventMentionFeatureGenerator {

    private Set<String> featureSubSet;

    public EventMentionFeatureGenerator() {

    }

    public EventMentionFeatureGenerator(Set<String> featureSubset) {
        this.featureSubSet = featureSubset;
    }

    public abstract Map<String, Double> genFeatures(CandidateEventMention mention);

    protected void addFeature(String featureBaseName, String featureSpecificName, Map<String, Double> features) {
        if (featureSubSet.isEmpty() || featureSubSet.contains(featureBaseName)) {
            features.put(featureBaseName + "_" + featureSpecificName, 1.0);
        }
    }

}