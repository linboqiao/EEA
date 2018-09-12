package edu.cmu.cs.lti.cds.ml.features.impl;

import edu.cmu.cs.lti.cds.ml.features.GlobalFeature;
import edu.cmu.cs.lti.script.model.ContextElement;
import edu.cmu.cs.lti.script.model.KmTargetConstants;
import edu.cmu.cs.lti.script.model.LocalArgumentRepre;
import edu.cmu.cs.lti.script.type.EntityMention;
import edu.cmu.cs.lti.script.type.EventMention;
import edu.cmu.cs.lti.script.type.EventMentionArgumentLink;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.map.hash.TObjectIntHashMap;
import org.apache.uima.fit.util.FSCollectionFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: zhengzhongliu
 * Date: 2/22/15
 * Time: 6:14 PM
 */
public class ArgumentTypeFeature extends GlobalFeature {

    Map<String, EntityMention>[] allArgumentsByRole;

    public void preprocessChain(List<ContextElement> elements) {
        super.preprocessChain(elements);
        allArgumentsByRole = new Map[elements.size()];
        for (int i = 0; i < elements.size(); i++) {
            Map<String, EntityMention> arguments = getArguments(elements.get(i));
            allArgumentsByRole[i] = arguments;
        }
    }

    @Override
    public Map<String, Double> getFeature(ContextElement targetElement, int targetIndex) {
        Map<String, Double> features = new HashMap<>();

        TIntObjectMap<String> targetArgumentIds = new TIntObjectHashMap<>();

        LocalArgumentRepre[] targetArgs = targetElement.getMention().getArgs();
        for (int slotId = 0; slotId < targetArgs.length; slotId++) {
            LocalArgumentRepre arg = targetArgs[slotId];
            if (arg != null && !arg.isOther()) {
                int rid = arg.isConcrete() ? arg.getEntityId() : arg.getRewrittenId();
                targetArgumentIds.put(rid, KmTargetConstants.argumentSlotName[slotId]);
            }
        }

        Map<String, TObjectIntMap<String>> argumentEntityTypeCounts = new HashMap<>();
        Map<String, TObjectIntMap<String>> argumentWordTypeCounts = new HashMap<>();

        for (int i = 0; i < elements.size(); i++) {
            if (i == targetIndex) {
                continue;
            }
            ContextElement element = elements.get(i);
            LocalArgumentRepre[] args = element.getMention().getArgs();
            for (LocalArgumentRepre arg : args) {
                if (arg != null && !arg.isOther()) {
                    int rid = arg.isConcrete() ? arg.getEntityId() : arg.getRewrittenId();
                    if (targetArgumentIds.containsKey(rid)) {
                        String slotName = targetArgumentIds.get(rid);
                        EntityMention targetArgument = allArgumentsByRole[i].get(slotName);
                        if (targetArgument != null) {
                            if (!argumentEntityTypeCounts.containsKey(slotName)) {
                                argumentEntityTypeCounts.put(slotName, new TObjectIntHashMap<String>());
                            }
                            if (!argumentWordTypeCounts.containsKey(slotName)) {
                                argumentWordTypeCounts.put(slotName, new TObjectIntHashMap<String>());
                            }
                            String entityType = targetArgument.getEntityType();
                            String wordType = targetArgument.getHead().getLemma().toLowerCase();
                            if (entityType != null) {
                                argumentEntityTypeCounts.get(slotName).adjustOrPutValue(entityType, 1, 1);
                            }

                            argumentWordTypeCounts.get(slotName).adjustOrPutValue(wordType, 1, 1);
                        }
                    }
                }
            }
        }

        for (Map.Entry<String, TObjectIntMap<String>> argumentEntityCountEntry : argumentEntityTypeCounts.entrySet()) {
            String slotName = argumentEntityCountEntry.getKey();
            TObjectIntMap<String> entityTypeCounts = argumentEntityCountEntry.getValue();

            double totalCount = 0.0;
            for (String type : entityTypeCounts.keySet()) {
                int typeCount = entityTypeCounts.get(type);
                totalCount += typeCount;
            }

            for (String type : entityTypeCounts.keySet()) {
                features.put("entity_type_for_" + slotName + "_" + type, entityTypeCounts.get(type) / totalCount);
            }
        }

        for (Map.Entry<String, TObjectIntMap<String>> argumentWordCountEntry : argumentWordTypeCounts.entrySet()) {
            String slotName = argumentWordCountEntry.getKey();
            TObjectIntMap<String> wordTypeCount = argumentWordCountEntry.getValue();

            double totalCount = 0.0;
            for (String type : wordTypeCount.keySet()) {
                int typeCount = wordTypeCount.get(type);
                totalCount += typeCount;
            }

            for (String type : wordTypeCount.keySet()) {
                features.put("word_type_for_" + slotName + "_" + type, wordTypeCount.get(type) / totalCount);
            }
        }
        return features;
    }

    private Map<String, EntityMention> getArguments(ContextElement element) {
        Map<String, EntityMention> argumentsByRoleName = new HashMap<>();
        EventMention mention = element.getOriginalMention();
        if (mention.getArguments() != null) {
            for (EventMentionArgumentLink link : FSCollectionFactory.create(mention.getArguments(), EventMentionArgumentLink.class)) {
                argumentsByRoleName.put(link.getArgumentRole(), link.getArgument());
            }
        }

        return argumentsByRoleName;
    }

//    private EntityMention getTargetArgument(ContextElement element, String targetArgument) {
//        EventMention mention = element.getOriginalMention();
//        if (mention.getArguments() != null) {
//            for (EventMentionArgumentLink link : FSCollectionFactory.create(mention.getArguments(), EventMentionArgumentLink.class)) {
//                if (link.getArgumentRole().equals(targetArgument)) {
//                    return link.getArgument();
//                }
//            }
//        }
//        return null;
//    }
}
