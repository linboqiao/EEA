package edu.cmu.cs.lti.cds.ml.features.impl;

import edu.cmu.cs.lti.cds.ml.features.Feature;
import edu.cmu.cs.lti.cds.model.ContextElement;
import edu.cmu.cs.lti.script.type.Sentence;
import org.apache.uima.fit.util.JCasUtil;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: zhengzhongliu
 * Date: 12/2/14
 * Time: 1:02 AM
 */
public class SentDistFeature extends Feature {
    @Override
    public Map<String, Double> getFeature(ContextElement elementLeft, ContextElement elementRight, int skip) {
        Map<String, Double> features = new HashMap<>();
        if (skip > 10) {
            return features;
        }

        Collection<Sentence> sents = JCasUtil.select(elementLeft.getJcas(), Sentence.class);
        int sentSize = sents.size();

        int leftSid = getSentId(elementLeft.getSent());
        int rightSid = getSentId(elementRight.getSent());
        int dist = Math.abs(leftSid - rightSid);
        features.put("sent_dist", 1.0 - (dist / sentSize));

        return features;
    }

    @Override
    public boolean isLexicalized() {
        return true;
    }

    private int getSentId(Sentence sent) {
        return Integer.parseInt(sent.getId());
    }

}
