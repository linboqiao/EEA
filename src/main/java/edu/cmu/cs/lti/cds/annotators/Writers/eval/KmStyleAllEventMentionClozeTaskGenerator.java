package edu.cmu.cs.lti.cds.annotators.writers.eval;

import edu.cmu.cs.lti.cds.annotators.script.EventMentionHeadCounter;
import edu.cmu.cs.lti.cds.model.KmTargetConstants;
import edu.cmu.cs.lti.cds.model.MooneyEventRepre;
import edu.cmu.cs.lti.cds.runners.FullSystemRunner;
import edu.cmu.cs.lti.cds.utils.DbManager;
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
import org.mapdb.DB;
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

    public static final String PARAM_HEAD_COUNT_DB_NAME = "headCountDbName";

    public static final String PARAM_DB_DIR_PATH = "dbLocation";

    private TokenAlignmentHelper align = new TokenAlignmentHelper();

    private Random rand = new Random();

    private Map<String, Fun.Tuple2<Integer, Integer>> headTfDfMap;

    @Override
    public void initialize(UimaContext aContext) throws ResourceInitializationException {
        super.initialize(aContext);
        String countingDbFileName = (String) aContext.getConfigParameterValue(PARAM_HEAD_COUNT_DB_NAME);

        String dbPath = (String) aContext.getConfigParameterValue(PARAM_DB_DIR_PATH);

        DB headCountDb = DbManager.getDB(dbPath, countingDbFileName);
        headTfDfMap = headCountDb.getHashMap(EventMentionHeadCounter.defaultMentionHeadMapName);
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
            Fun.Tuple2<Integer, Integer> evmTfDf = headTfDfMap.get(align.getLowercaseWordLemma(mention.getHeadWord()));

            //filter by low tf df counts
            if (Utils.tfDfFilter(evmTfDf.a, evmTfDf.b)) {
                logger.info("Mention filtered because of low frequency: " + mention.getCoveredText());
                continue;
            }

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

        if (allEvms.size() == 0) {
            //empty file
            return "";
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
