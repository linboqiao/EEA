package edu.cmu.cs.lti.cds.ml.features.impl;

import edu.cmu.cs.lti.cds.ml.features.Feature;
import edu.cmu.cs.lti.script.model.ContextElement;
import edu.cmu.cs.lti.script.model.LocalArgumentRepre;

import java.util.HashMap;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: zhengzhongliu
 * Date: 11/30/14
 * Time: 5:55 PM
 */
public class ArgumentCorefFeature extends Feature {
    String prefix = "r_arg_";

    @Override
    public Map<String, Double> getFeature(ContextElement elementLeft, ContextElement elementRight, int skip) {
        Map<String, Double> features = new HashMap<>();

        if (skip > 3) {
            return features;
        }

        LocalArgumentRepre[] leftArgs = elementLeft.getMention().getArgs();
        LocalArgumentRepre[] rightArgs = elementRight.getMention().getArgs();

        for (int slotLeft = 0; slotLeft < leftArgs.length; slotLeft++) {
            LocalArgumentRepre leftArg = leftArgs[slotLeft];
            if (leftArg == null) {
                continue;
            }
            for (int slotRight = 0; slotRight < rightArgs.length; slotRight++) {
                LocalArgumentRepre rightArg = rightArgs[slotRight];
                if (rightArg == null) {
                    continue;
                }

                if (leftArg.getRewritedId() == rightArg.getRewritedId()) {
                    features.put(prefix + slotLeft + "_" + slotRight, 1.0);
                }
            }
        }
        return features;
    }

    @Override
    public boolean isLexicalized() {
        return true;
    }
}
