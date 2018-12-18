package edu.cmu.cs.lti.cds.ml.features.impl;

import edu.cmu.cs.lti.cds.ml.features.GlobalFeature;
import edu.cmu.cs.lti.script.model.ContextElement;
import edu.cmu.cs.lti.script.model.KmTargetConstants;
import edu.cmu.cs.lti.script.model.LocalArgumentRepre;
import edu.cmu.cs.lti.script.type.FanseToken;
import edu.cmu.cs.lti.script.utils.DataPool;
import gnu.trove.iterator.TObjectIntIterator;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.map.hash.TObjectIntHashMap;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: zhengzhongliu
 * Date: 2/22/15
 * Time: 6:15 PM
 */
public class FrameFeature extends GlobalFeature {
    String[] frameNames;
    Map<String, String>[] frameRoleNames;

    @Override
    public void preprocessChain(List<ContextElement> elements) {
        super.preprocessChain(elements);

        frameNames = new String[elements.size()];
        frameRoleNames = new HashMap[elements.size()];

        for (int i = 0; i < elements.size(); i++) {
            ContextElement element = elements.get(i);
            FanseToken mentionHead = (FanseToken) element.getOriginalMention().getHeadWord();
            String pbRoleset = mentionHead.getLexicalSense();

            String frame = DataPool.pb2FnFrameMapping.get(pbRoleset);

            if (frame != null) {
                frameNames[i] = frame;
            }

            LocalArgumentRepre[] args = element.getMention().getArgs();

            Map<String, String> pb2FrameRoleNames = new HashMap<>();
            for (int argIdx = 0; argIdx < args.length; argIdx++) {
                LocalArgumentRepre arg = args[argIdx];
                if (arg != null && !arg.isOther()) {
                    int rid = arg.isConcrete() ? arg.getEntityId() : arg.getRewrittenId();
                    String roleName = KmTargetConstants.argumentSlotName[argIdx];
                    pb2FrameRoleNames.put(roleName, DataPool.pb2FnFrameRoleMapping.get(pbRoleset + "_" + roleName));
                }
            }
            frameRoleNames[i] = pb2FrameRoleNames;
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

        TObjectIntMap<String> frameCounts = new TObjectIntHashMap();
        Map<String, TObjectIntMap<String>> frameRoleCounts = new HashMap<>();

        for (int i = 0; i < elements.size(); i++) {
            if (i == targetIndex) {
                continue;
            }
            ContextElement element = elements.get(i);
            LocalArgumentRepre[] args = element.getMention().getArgs();

            boolean hasCoref = false;

            for (LocalArgumentRepre arg : args) {
                if (arg != null && !arg.isOther()) {
                    int rid = arg.isConcrete() ? arg.getEntityId() : arg.getRewrittenId();
                    if (targetArgumentIds.containsKey(rid)) {
                        hasCoref = true;
                        String slotName = targetArgumentIds.get(rid);
                        String frameRoleName = frameRoleNames[i].get(slotName);

                        if (frameRoleName != null) {
                            if (!frameRoleCounts.containsKey(slotName)) {
                                frameRoleCounts.put(slotName, new TObjectIntHashMap<String>());
                            }
                            frameRoleCounts.get(slotName).adjustOrPutValue(frameRoleName, 1, 1);
                        }
                    }
                }
            }

            if (hasCoref) {
                String frameName = frameNames[i];
                if (frameName != null) {
                    frameCounts.adjustOrPutValue(frameName, 1, 1);
                }
            }
        }


        for (Map.Entry<String, TObjectIntMap<String>> frameRoleCount : frameRoleCounts.entrySet()) {
            String slotName = frameRoleCount.getKey();
            TObjectIntMap<String> roleCount = frameRoleCount.getValue();

            double totalCount = 0.0;
            for (String type : roleCount.keySet()) {
                int typeCount = roleCount.get(type);
                totalCount += typeCount;
            }

            for (String type : roleCount.keySet()) {
                features.put("coref_frame_role_" + slotName + "_" + type, roleCount.get(type) / totalCount);
            }
        }


        double totalCount = 0.0;

        for (TObjectIntIterator<String> iter = frameCounts.iterator(); iter.hasNext(); ) {
            iter.advance();
            int count = iter.value();
            totalCount += count;
        }

        for (TObjectIntIterator<String> iter = frameCounts.iterator(); iter.hasNext(); ) {
            iter.advance();
            String frameName = iter.key();
            int count = iter.value();
            features.put("corefer_frame_" + frameName, count / totalCount);
        }

        return features;
    }
}
