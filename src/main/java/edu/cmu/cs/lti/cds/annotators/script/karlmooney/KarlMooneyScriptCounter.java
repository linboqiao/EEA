package edu.cmu.cs.lti.cds.annotators.script.karlmooney;

import edu.cmu.cs.lti.cds.model.KmTargetConstants;
import edu.cmu.cs.lti.script.type.EventMention;
import edu.cmu.cs.lti.script.type.EventMentionArgumentLink;
import edu.cmu.cs.lti.uima.annotator.AbstractLoggingAnnotator;
import edu.cmu.cs.lti.uima.util.UimaConvenience;
import edu.cmu.cs.lti.utils.CollectionUtils;
import edu.cmu.cs.lti.utils.TokenAlignmentHelper;
import edu.cmu.cs.lti.utils.Utils;
import gnu.trove.map.hash.TIntIntHashMap;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.mapdb.Fun;

import java.io.File;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentNavigableMap;

/**
 * Created with IntelliJ IDEA.
 * User: zhengzhongliu
 * Date: 10/20/14
 * Time: 3:47 PM
 */
public class KarlMooneyScriptCounter extends AbstractLoggingAnnotator {

    public static final String PARAM_DB_DIR_PATH = "dbLocation";

    public static final String PARAM_SKIP_BIGRAM_N = "skippedBigramN";

    private ConcurrentNavigableMap<Fun.Tuple2<Fun.Tuple4<String, Integer, Integer, Integer>, Fun.Tuple4<String, Integer, Integer, Integer>>, Integer> cooccCounts;

    private ConcurrentNavigableMap<Fun.Tuple4<String, Integer, Integer, Integer>, Integer> occCounts;

    private int skippedBigramN;

    private DB db;

    private int counter = 0;

//    private LinkedHashMap<String, Integer> targetArguments;

    public static final String defaultDBName = "tuple_counts";

    public static final String defaultCooccMapName = "subsituted_event_cooccurrences";

    public static final String defaultOccMapName = "subsituted_event_occurrences";

    private TokenAlignmentHelper align = new TokenAlignmentHelper();

    @Override
    public void initialize(UimaContext aContext) throws ResourceInitializationException {
        super.initialize(aContext);

        String dbPath = (String) aContext.getConfigParameterValue(PARAM_DB_DIR_PATH);
        skippedBigramN = (Integer) aContext.getConfigParameterValue(PARAM_SKIP_BIGRAM_N);

        File dbParentPath = new File(dbPath);

        if (!dbParentPath.isDirectory()) {
            dbParentPath.mkdirs();
        }

        db = DBMaker.newFileDB(new File(dbPath, defaultDBName)).transactionDisable().closeOnJvmShutdown().make();
        cooccCounts = db.getTreeMap(defaultCooccMapName);
        occCounts = db.getTreeMap(defaultOccMapName);
    }

    @Override
    public void process(JCas aJCas) throws AnalysisEngineProcessException {
        logger.info(progressInfo(aJCas));

        align.loadWord2Stanford(aJCas);

        Collection<EventMention> allMentions = JCasUtil.select(aJCas, EventMention.class);
        List<Pair<EventMention, EventMention>> mentionBigrams = CollectionUtils.nSkippedBigrams(allMentions, skippedBigramN);

        //TODO we probably also want to have a EOF indicator here
        for (Pair<EventMention, EventMention> bigram : mentionBigrams) {
            Fun.Tuple2<Fun.Tuple4<String, Integer, Integer, Integer>, Fun.Tuple4<String, Integer, Integer, Integer>> subsitutedBigram =
                    firstBasedSubstitution(bigram.getLeft(), bigram.getRight());

            Integer oldCooccCount = cooccCounts.get(subsitutedBigram);
            if (oldCooccCount == null) {
                cooccCounts.put(subsitutedBigram, 1);
            } else {
                cooccCounts.put(subsitutedBigram, oldCooccCount + 1);
            }

            Integer occCount = occCounts.get(subsitutedBigram.a);
            if (occCount == null) {
                occCounts.put(subsitutedBigram.a, 1);
            } else {
                occCounts.put(subsitutedBigram.a, occCount + 1);
            }

        }

        //defrag from time to time
        //debug compact
        counter++;
        if (counter % 1000 == 0) {
            logger.info("Commit and compacting after " + counter);
            db.commit();
            db.compact();
        }
    }

    private void store() {

    }


    private Fun.Tuple2<Fun.Tuple4<String, Integer, Integer, Integer>, Fun.Tuple4<String, Integer, Integer, Integer>>
    firstBasedSubstitution(EventMention evm1, EventMention evm2) {
        TIntIntHashMap evm1Args = new TIntIntHashMap();

        TIntIntHashMap evm1Slots = new TIntIntHashMap();

        for (EventMentionArgumentLink aLink : UimaConvenience.convertFSListToList(evm1.getArguments(), EventMentionArgumentLink.class)) {
            String argumentRole = aLink.getArgumentRole();
            if (KmTargetConstants.targetArguments.containsKey(argumentRole)) {
                int slotId = KmTargetConstants.targetArguments.get(argumentRole);
                evm1Args.put(Utils.entityIdToInteger(aLink.getArgument().getReferingEntity().getId()), slotId);
                //initialize with other
                evm1Slots.put(slotId, KmTargetConstants.otherMarker);
            }
        }

        TIntIntHashMap evm2Slots = new TIntIntHashMap();
        for (EventMentionArgumentLink aLink : UimaConvenience.convertFSListToList(evm2.getArguments(), EventMentionArgumentLink.class)) {
            String argumentRole = aLink.getArgumentRole();

            if (KmTargetConstants.targetArguments.containsKey(argumentRole)) {
                int entityId = Utils.entityIdToInteger(aLink.getArgument().getReferingEntity().getId());
                int slotId = KmTargetConstants.targetArguments.get(argumentRole);

                //substitution for the second event is based on the first event mention
                int substituteId;
                if (evm1Args.containsKey(entityId)) {
                    substituteId = evm1Args.get(entityId);
                    //we apply the mask to the former slot here
                    //former event based, so event slot id is the same as subsituted id
                    evm1Slots.put(substituteId, substituteId);
                } else {
                    substituteId = KmTargetConstants.otherMarker;
                }

                evm2Slots.put(slotId, substituteId);
            }
        }
//
//        MooneyEventRepre evmRepre1 = new MooneyEventRepre(align.getLowercaseWordLemma(evm1.getHeadWord()),
//                evm1Slots.containsKey(KmTargetConstants.firstArg0Marker) ? evm1Slots.get(KmTargetConstants.firstArg0Marker) : KmTargetConstants.nullArgMarker,
//                evm1Slots.containsKey(KmTargetConstants.firstArg1Marker) ? evm1Slots.get(KmTargetConstants.firstArg1Marker) : KmTargetConstants.nullArgMarker,
//                evm1Slots.containsKey(KmTargetConstants.firstArg2Marker) ? evm1Slots.get(KmTargetConstants.firstArg2Marker) : KmTargetConstants.nullArgMarker
//        );
//
//
//        MooneyEventRepre evmRepre2 = new MooneyEventRepre(align.getLowercaseWordLemma(evm2.getHeadWord()),
//                evm2Slots.containsKey(KmTargetConstants.firstArg0Marker) ? evm2Slots.get(KmTargetConstants.firstArg0Marker) : KmTargetConstants.nullArgMarker,
//                evm2Slots.containsKey(KmTargetConstants.firstArg1Marker) ? evm2Slots.get(KmTargetConstants.firstArg1Marker) : KmTargetConstants.nullArgMarker,
//                evm2Slots.containsKey(KmTargetConstants.firstArg2Marker) ? evm2Slots.get(KmTargetConstants.firstArg2Marker) : KmTargetConstants.nullArgMarker
//        );


        Fun.Tuple4<String, Integer, Integer, Integer> eventTuple1 = new Fun.Tuple4<>(align.getLowercaseWordLemma(evm1.getHeadWord()),
                evm1Slots.containsKey(KmTargetConstants.firstArg0Marker) ? evm1Slots.get(KmTargetConstants.firstArg0Marker) : KmTargetConstants.nullArgMarker,
                evm1Slots.containsKey(KmTargetConstants.firstArg1Marker) ? evm1Slots.get(KmTargetConstants.firstArg1Marker) : KmTargetConstants.nullArgMarker,
                evm1Slots.containsKey(KmTargetConstants.firstArg2Marker) ? evm1Slots.get(KmTargetConstants.firstArg2Marker) : KmTargetConstants.nullArgMarker
        );

        Fun.Tuple4<String, Integer, Integer, Integer> eventTuple2 = new Fun.Tuple4<>(align.getLowercaseWordLemma(evm2.getHeadWord()),
                evm2Slots.containsKey(KmTargetConstants.firstArg0Marker) ? evm2Slots.get(KmTargetConstants.firstArg0Marker) : KmTargetConstants.nullArgMarker,
                evm2Slots.containsKey(KmTargetConstants.firstArg1Marker) ? evm2Slots.get(KmTargetConstants.firstArg1Marker) : KmTargetConstants.nullArgMarker,
                evm2Slots.containsKey(KmTargetConstants.firstArg2Marker) ? evm2Slots.get(KmTargetConstants.firstArg2Marker) : KmTargetConstants.nullArgMarker
        );

        return new Fun.Tuple2<>(eventTuple1, eventTuple2);
    }

    @Override
    public void collectionProcessComplete() throws AnalysisEngineProcessException {
        db.commit();
        db.compact();
        db.close();
    }

}
