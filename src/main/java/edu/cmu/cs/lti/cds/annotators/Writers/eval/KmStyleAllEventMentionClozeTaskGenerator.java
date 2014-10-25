package edu.cmu.cs.lti.cds.annotators.writers.eval;

import edu.cmu.cs.lti.cds.model.KmTargetConstants;
import edu.cmu.cs.lti.cds.model.MooneyEventRepre;
import edu.cmu.cs.lti.script.type.EventMention;
import edu.cmu.cs.lti.script.type.EventMentionArgumentLink;
import edu.cmu.cs.lti.uima.io.writer.AbstractCustomizedTextWriterAnalsysisEngine;
import edu.cmu.cs.lti.uima.util.UimaConvenience;
import edu.cmu.cs.lti.utils.TokenAlignmentHelper;
import edu.cmu.cs.lti.utils.Utils;
import gnu.trove.map.hash.TIntIntHashMap;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;
import org.apache.uima.UimaContext;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Created with IntelliJ IDEA.
 * User: zhengzhongliu
 * Date: 10/21/14
 * Time: 1:32 PM
 */
public class KmStyleAllEventMentionClozeTaskGenerator extends AbstractCustomizedTextWriterAnalsysisEngine {

    private TokenAlignmentHelper align = new TokenAlignmentHelper();

    private Random rand = new Random();


    @Override
    public void initialize(UimaContext aContext) throws ResourceInitializationException {
        super.initialize(aContext);
    }

    @Override
    public String getTextToPrint(JCas aJCas) {
        logger.info(progressInfo(aJCas));
        align.loadWord2Stanford(aJCas);
        StringBuilder sb = new StringBuilder();

        List<TIntIntHashMap> allSlots = new ArrayList<>();
        List<EventMention> allEvms = new ArrayList<>();

        for (EventMention mention : JCasUtil.select(aJCas, EventMention.class)) {
            TIntIntHashMap slots = new TIntIntHashMap();

            for (EventMentionArgumentLink aLink : UimaConvenience.convertFSListToList(mention.getArguments(), EventMentionArgumentLink.class)) {
                String argumentRole = aLink.getArgumentRole();
                if (KmTargetConstants.targetArguments.containsKey(argumentRole)) {
                    int slotId = KmTargetConstants.targetArguments.get(argumentRole);
                    int entityId = Utils.entityIdToInteger(aLink.getArgument().getReferingEntity().getId());
                    //substitution for the first event is always self
                    slots.put(slotId, entityId);
                }
            }

            allEvms.add(mention);
            allSlots.add(slots);
        }

        int heldOutIndex = rand.nextInt(allEvms.size());

        TIntIntHashMap rewriteMap = new TIntIntHashMap();

        TIntIntHashMap heldOutSlots = allSlots.get(heldOutIndex);

        TIntSet heldOutSlotMask = new TIntHashSet();

        for (int heldoutSlotId : heldOutSlots.keys()) {
            int entityId = heldOutSlots.get(heldoutSlotId);
            rewriteMap.put(entityId, heldoutSlotId);
        }

        MooneyEventRepre[] chain = new MooneyEventRepre[allEvms.size()];
        for (int i = 0; i < allEvms.size(); i++) {
            EventMention evm = allEvms.get(i);
            TIntIntHashMap slots = allSlots.get(i);

            String predicate = align.getLowercaseWordLemma(evm.getHeadWord());

            chain[i] = new MooneyEventRepre(
                    predicate,
                    rewrite(slots, KmTargetConstants.firstArg0Marker, rewriteMap, heldOutSlotMask),
                    rewrite(slots, KmTargetConstants.firstArg1Marker, rewriteMap, heldOutSlotMask),
                    rewrite(slots, KmTargetConstants.firstArg2Marker, rewriteMap, heldOutSlotMask));
        }

        for (int i = 0; i < chain.length; i++) {
            MooneyEventRepre evmRepre = chain[i];
            if (i == heldOutIndex) {
                sb.append(evmRepre.toStringWithEmptyIndicator(heldOutSlotMask)).append("\n");
            } else {
                sb.append(evmRepre.toString()).append("\n");
            }
        }

        return sb.toString();
    }

    private int rewrite(TIntIntHashMap slot2Id, int marker, TIntIntHashMap rewriteMap, TIntSet heldOutSlotAppearMarker) {
        if (slot2Id.containsKey(marker)) {
            int eid = slot2Id.get(marker);

            if (rewriteMap.containsKey(eid)) {
                heldOutSlotAppearMarker.add(marker);
                return rewriteMap.get(eid);
            } else {
                return KmTargetConstants.otherMarker;
            }
        } else {
            return KmTargetConstants.nullArgMarker;
        }
    }
}
