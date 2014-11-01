package edu.cmu.cs.lti.cds.annotators.writers.eval;

import edu.cmu.cs.lti.cds.annotators.script.EventMentionHeadCounter;
import edu.cmu.cs.lti.cds.model.KmTargetConstants;
import edu.cmu.cs.lti.cds.model.MooneyEventRepre;
import edu.cmu.cs.lti.cds.runners.FullSystemRunner;
import edu.cmu.cs.lti.cds.utils.DbManager;
import edu.cmu.cs.lti.cds.utils.MultiMapUtils;
import edu.cmu.cs.lti.script.type.Article;
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
import org.mapdb.Fun;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Created with IntelliJ IDEA.
 * User: zhengzhongliu
 * Date: 10/21/14
 * Time: 1:32 PM
 */
public class KmStyleAllEventMentionClozeTaskGenerator extends AbstractCustomizedTextWriterAnalsysisEngine {

    public static final String PARAM_HEAD_COUNT_DB_NAMES = "headCountDbFileNames";

    public static final String PARAM_DB_DIR_PATH = "dbLocation";

    public static final String PARAM_IGNORE_LOW_FREQ = "ignoreLowFreq";

    private TokenAlignmentHelper align = new TokenAlignmentHelper();

    private Random rand = new Random();

    private Map<String, Fun.Tuple2<Integer, Integer>>[] headTfDfMaps;

    private boolean ignoreLowFreq;

    @Override
    public void initialize(UimaContext aContext) throws ResourceInitializationException {
        super.initialize(aContext);
        String[] countingDbFileNames = (String[]) aContext.getConfigParameterValue(PARAM_HEAD_COUNT_DB_NAMES);

        String dbPath = (String) aContext.getConfigParameterValue(PARAM_DB_DIR_PATH);

        if (aContext.getConfigParameterValue(PARAM_IGNORE_LOW_FREQ) != null) {
            ignoreLowFreq = (Boolean) aContext.getConfigParameterValue(PARAM_IGNORE_LOW_FREQ);
        } else {
            ignoreLowFreq = true;
        }

        if (ignoreLowFreq) {
            headTfDfMaps = DbManager.getMaps(dbPath, countingDbFileNames, EventMentionHeadCounter.defaultMentionHeadMapName);
        }
    }

    @Override
    public String getTextToPrint(JCas aJCas) {
        logger.info(progressInfo(aJCas));

        Article article = JCasUtil.selectSingle(aJCas, Article.class);

        if (FullSystemRunner.blackListedArticleId.contains(article.getArticleName())) {
            //ignore this blacklisted file;
            logger.info("Ignored black listed file");
            return "";
        }

        align.loadWord2Stanford(aJCas);
        StringBuilder sb = new StringBuilder();

        List<TIntIntHashMap> allSlots = new ArrayList<>();
        List<EventMention> allEvms = new ArrayList<>();

        for (EventMention mention : JCasUtil.select(aJCas, EventMention.class)) {
            if (ignoreLowFreq) {
                int evmTf = MultiMapUtils.getTf(headTfDfMaps, align.getLowercaseWordLemma(mention.getHeadWord()));
                //filter by low tf df counts
                if (Utils.termFrequencyFilter(evmTf)) {
                    logger.info("Mention filtered because of low frequency: " + mention.getCoveredText() + " " + evmTf);
                    continue;
                }
            }

            TIntIntHashMap slots = new TIntIntHashMap();

            for (EventMentionArgumentLink aLink : UimaConvenience.convertFSListToList(mention.getArguments(), EventMentionArgumentLink.class)) {
                String argumentRole = aLink.getArgumentRole();
                if (KmTargetConstants.targetArguments.containsKey(argumentRole)) {
                    int argumentId = KmTargetConstants.targetArguments.get(argumentRole);
                    int entityId = Utils.entityIdToInteger(aLink.getArgument().getReferingEntity().getId());
                    slots.put(argumentId, entityId);
                }
            }

            allEvms.add(mention);
            allSlots.add(slots);
        }

        if (allEvms.size() == 0) {
            //empty file
            return "";
        }

        int heldOutIndex = rand.nextInt(allEvms.size());

        TIntIntHashMap rewriteMap = new TIntIntHashMap();

        TIntIntHashMap heldOutSlots = allSlots.get(heldOutIndex);

        System.out.println("Held out slot is " + heldOutSlots);

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

            if (i == heldOutIndex) {
                chain[i] = new MooneyEventRepre();
                chain[i].setPredicate(predicate);

//                for (int argument : KmTargetConstants.targetArguments.values()) {
//                    if (slots.containsKey(argument)) {
//                        chain[i].setArgument(KmTargetConstants.argMarkerToSlotIndex(argument), argument);
//                        System.out.println("Setting slot id " + KmTargetConstants.argMarkerToSlotIndex(argument) + " into " + argument);
//                    } else {
//                        //TODO null doesn't work?
//                        chain[i].setArgument(argument, KmTargetConstants.nullArgMarker);
//                        System.out.println("Setting slot id " + KmTargetConstants.argMarkerToSlotIndex(argument) + " into null " + KmTargetConstants.nullArgMarker);
//                    }
//                }
                chain[i] = new MooneyEventRepre(
                        predicate,
                        rewriteHeldOut(slots, KmTargetConstants.anchorArg0Marker, rewriteMap),
                        rewriteHeldOut(slots, KmTargetConstants.anchorArg1Marker, rewriteMap),
                        rewriteHeldOut(slots, KmTargetConstants.anchorArg2Marker, rewriteMap)
                );
                System.out.println(chain[i]);

            } else {
                chain[i] = new MooneyEventRepre(
                        predicate,
                        rewrite(slots, KmTargetConstants.anchorArg0Marker, rewriteMap, heldOutSlotMask),
                        rewrite(slots, KmTargetConstants.anchorArg1Marker, rewriteMap, heldOutSlotMask),
                        rewrite(slots, KmTargetConstants.anchorArg2Marker, rewriteMap, heldOutSlotMask)
                );
            }
        }

        for (int i = 0; i < chain.length; i++) {
            MooneyEventRepre evmRepre = chain[i];
            if (i == heldOutIndex) {
                //applying the mask here;
                String heldOutLine = evmRepre.toStringWithEmptyIndicator(heldOutSlotMask);
                System.out.println(heldOutLine);
                sb.append(heldOutLine).append("\n");
            } else {
                sb.append(evmRepre.toString()).append("\n");
            }
        }

//        Utils.pause();

        return sb.toString();
    }

    private int rewriteHeldOut(TIntIntHashMap slot2Id, int argumentMarker, TIntIntHashMap rewriteMap) {
        if (slot2Id.containsKey(argumentMarker)) {
            int eid = slot2Id.get(argumentMarker);

            if (rewriteMap.containsKey(eid)) {
                return rewriteMap.get(eid);
            } else {
                return KmTargetConstants.otherMarker;
            }
        } else {
            return KmTargetConstants.nullArgMarker;
        }
    }

    private int rewrite(TIntIntHashMap slot2Id, int argumentMarker, TIntIntHashMap rewriteMap, TIntSet heldOutSlotAppearMarker) {
        if (slot2Id.containsKey(argumentMarker)) {
            int eid = slot2Id.get(argumentMarker);

            if (rewriteMap.containsKey(eid)) {
                int heldOutId = rewriteMap.get(eid);
                heldOutSlotAppearMarker.add(heldOutId);
                return heldOutId;
            } else {
                return KmTargetConstants.otherMarker;
            }
        } else {
            return KmTargetConstants.nullArgMarker;
        }
    }
}
