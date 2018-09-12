package edu.cmu.cs.lti.cds.ml.features.impl;

import edu.cmu.cs.lti.cds.ml.features.PairwiseFeature;
import edu.cmu.cs.lti.script.model.ContextElement;
import edu.cmu.cs.lti.script.model.KmTargetConstants;
import edu.cmu.cs.lti.script.model.LocalArgumentRepre;
import edu.cmu.cs.lti.script.type.EventMentionArgumentLink;
import org.apache.uima.fit.util.FSCollectionFactory;
import org.apache.uima.jcas.cas.FSList;

import java.util.HashMap;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: zhengzhongliu
 * Date: 11/30/14
 * Time: 5:55 PM
 */
public class ArgumentCorefFeature extends PairwiseFeature {
    String prefix = "r_arg_";

    @Override
    public Map<String, Double> getFeature(ContextElement elementLeft, ContextElement elementRight, int skip) {
        Map<String, Double> features = new HashMap<>();

        LocalArgumentRepre[] leftArgs = elementLeft.getMention().getArgs();
        LocalArgumentRepre[] rightArgs = elementRight.getMention().getArgs();


        StringBuilder sb = new StringBuilder();

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

                int leftEntityId = leftArg.isConcrete() ? leftArg.getEntityId() : leftArg.getRewrittenId();
                int rightEntityId = rightArg.isConcrete() ? rightArg.getEntityId() : rightArg.getRewrittenId();

                if (leftEntityId == rightEntityId) {
//                    String type = getCoreferredEntityType(elementLeft, slotLeft);
//                    if (type != null) {
////                        System.err.println(leftEntityId + " " + rightEntityId);
//                        features.put(prefix + slotLeft + "_" + slotRight + "_type_" + type, 1.0);
////                        System.err.println(prefix + slotLeft + "_" + slotRight + "_type_" + type);
////                        Utils.pause();
//                    }
                    features.put(prefix + slotLeft + "_" + slotRight, 1.0);
                }
            }
        }
        return features;
    }

    private String getCoreferredEntityType(ContextElement element, int slotId) {
        String argumentRoleName = KmTargetConstants.argumentSlotName[slotId];

        FSList arguments = element.getOriginalMention().getArguments();
        if (arguments != null) {
            for (EventMentionArgumentLink argumentLink : FSCollectionFactory.create(arguments, EventMentionArgumentLink.class)) {
                if (argumentLink.getArgumentRole().equals(argumentRoleName)) {
                    return argumentLink.getArgument().getHead().getLemma().toLowerCase();
                }
            }
        }

        return null;
    }

}
