package edu.cmu.cs.lti.cds.annotators.script.karlmooney;

import edu.cmu.cs.lti.cds.annotators.script.EventMentionHeadCounter;
import edu.cmu.cs.lti.cds.model.KmTargetConstants;
import edu.cmu.cs.lti.cds.runners.FullSystemRunner;
import edu.cmu.cs.lti.cds.utils.DbManager;
import edu.cmu.cs.lti.cds.utils.MultiMapUtils;
import edu.cmu.cs.lti.script.type.Article;
import edu.cmu.cs.lti.script.type.EventMention;
import edu.cmu.cs.lti.script.type.EventMentionArgumentLink;
import edu.cmu.cs.lti.uima.annotator.AbstractLoggingAnnotator;
import edu.cmu.cs.lti.uima.util.UimaConvenience;
import edu.cmu.cs.lti.utils.CollectionUtils;
import edu.cmu.cs.lti.utils.TokenAlignmentHelper;
import edu.cmu.cs.lti.utils.Utils;
import gnu.trove.list.TIntList;
import gnu.trove.list.linked.TIntLinkedList;
import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.hash.TIntIntHashMap;
import gnu.trove.map.hash.TObjectIntHashMap;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.mapdb.Fun;
import weka.core.SerializationHelper;

import java.io.File;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: zhengzhongliu
 * Date: 10/20/14
 * Time: 3:47 PM
 */
public class KarlMooneyScriptCounter extends AbstractLoggingAnnotator {
    //TODO use word sense instead of predicate head word

    public static final String PARAM_DB_NAME = "dbName";

    public static final String PARAM_DB_DIR_PATH = "dbLocation";

    public static final String PARAM_SKIP_BIGRAM_N = "skippedBigramN";

    public static final String PARAM_HEAD_COUNT_DB_NAMES = "headCountDbName";

    public static final String PARAM_IGNORE_LOW_FREQ = "ignoreLowFreq";

    //TODO consider sentence or event distance

    private TObjectIntMap<String> headIdMap = new TObjectIntHashMap<>();

    private TObjectIntMap<TIntList> cooccCounts = new TObjectIntHashMap<>();

    private TObjectIntMap<TIntList> occCounts = new TObjectIntHashMap<>();

//    private Map<String, Fun.Tuple2<Integer, Integer>> headTfDfMap;

    private Map<String, Fun.Tuple2<Integer, Integer>>[] headTfDfMaps;

    private int skippedBigramN;

    private int counter = 0;

    public static final String defaultDBName = "tuple_counts";

    public static final String defaultCooccMapName = "substituted_event_cooccurrences";

    public static final String defaultOccMapName = "substituted_event_occurrences";

    public static final String defaltHeadIdMapName = "head_id_map";

    private TokenAlignmentHelper align = new TokenAlignmentHelper();

    private String tupleCountDbFileName;

    private String dbPath;

    private boolean ignoreLowFreq;

    @Override
    public void initialize(UimaContext aContext) throws ResourceInitializationException {
        super.initialize(aContext);

        String dbName = (String) aContext.getConfigParameterValue(PARAM_DB_NAME);
        tupleCountDbFileName = dbName == null ? defaultDBName : dbName;

        dbPath = (String) aContext.getConfigParameterValue(PARAM_DB_DIR_PATH);
        skippedBigramN = (Integer) aContext.getConfigParameterValue(PARAM_SKIP_BIGRAM_N);

        if (aContext.getConfigParameterValue(PARAM_IGNORE_LOW_FREQ) != null) {
            ignoreLowFreq = (Boolean) aContext.getConfigParameterValue(PARAM_IGNORE_LOW_FREQ);
        } else {
            ignoreLowFreq = true;
        }

        File dbParentPath = new File(dbPath);

        if (!dbParentPath.isDirectory()) {
            dbParentPath.mkdirs();
        }

        if (ignoreLowFreq) {
//            String countingDbFileName = (String) aContext.getConfigParameterValue(PARAM_HEAD_COUNT_DB_NAME);
//            if (countingDbFileName != null) {
//                DB headCountDb = DbManager.getDB(dbPath, countingDbFileName);
//                headTfDfMap = headCountDb.getHashMap(EventMentionHeadCounter.defaultMentionHeadMapName);
//            }

            String[] countingDbFileNames = (String[]) aContext.getConfigParameterValue(PARAM_HEAD_COUNT_DB_NAMES);
            headTfDfMaps = DbManager.getMaps(dbPath, countingDbFileNames, EventMentionHeadCounter.defaultMentionHeadMapName);
        }

        Utils.printMemInfo(logger, "Initial memory information ");
    }

    @Override
    public void process(JCas aJCas) throws AnalysisEngineProcessException {
        logger.info(progressInfo(aJCas));

        Article article = JCasUtil.selectSingle(aJCas, Article.class);

        if (FullSystemRunner.blackListedArticleId.contains(article.getArticleName())) {
            //ignore this blacklisted file;
            logger.info("Ignored black listed file");
            return;
        }

        align.loadWord2Stanford(aJCas);

        Collection<EventMention> allMentions = JCasUtil.select(aJCas, EventMention.class);
        List<Pair<EventMention, EventMention>> mentionBigrams = CollectionUtils.nSkippedBigrams(allMentions, skippedBigramN);

        //TODO we probably also want to have a EOF indicator here
        for (Pair<EventMention, EventMention> bigram : mentionBigrams) {
            Fun.Tuple2<Fun.Tuple4<String, Integer, Integer, Integer>, Fun.Tuple4<String, Integer, Integer, Integer>> subsitutedBigram =
                    firstBasedSubstitution(bigram.getLeft(), bigram.getRight());

            if (ignoreLowFreq) {
                int evm1Tf = MultiMapUtils.getTf(headTfDfMaps, align.getLowercaseWordLemma(bigram.getLeft().getHeadWord()));
                int evm2Tf = MultiMapUtils.getTf(headTfDfMaps, align.getLowercaseWordLemma(bigram.getRight().getHeadWord()));

                if (Utils.termFrequencyFilter(evm1Tf) || Utils.termFrequencyFilter(evm2Tf)) {
                    logger.info("Filtered because of low frequency: " + bigram.getLeft().getCoveredText() + " " + evm1Tf + " " + bigram.getRight().getCoveredText() + " " + evm2Tf);
                    continue;
                }
            }

//            if (subsitutedBigram.a.a.equals("have")) {
//                System.out.println(subsitutedBigram);
//                System.out.println(compactEvmPairSubstituiton(subsitutedBigram, headIdMap));
//            }

            cooccCounts.adjustOrPutValue(compactEvmPairSubstituiton(subsitutedBigram, headIdMap), 1, 1);
            occCounts.adjustOrPutValue(compactEvmSubstituiton(subsitutedBigram.a, headIdMap), 1, 1);
        }

        //defrag from time to time
        //debug compact
        counter++;
        if (counter % 4000 == 0) {
            Utils.printMemInfo(logger, "Memory info after loaded " + counter + " files");
        }
    }


    public TIntLinkedList compactEvmSubstituiton(Fun.Tuple4<String, Integer, Integer, Integer> evm, TObjectIntMap<String> headMap) {
        TIntLinkedList compactRep = new TIntLinkedList();
        compactRep.add(getHeadId(headMap, evm.a));
        compactRep.add(evm.b);
        compactRep.add(evm.c);
        compactRep.add(evm.d);
        return compactRep;
    }

    public TIntLinkedList compactEvmPairSubstituiton(Fun.Tuple2<Fun.Tuple4<String, Integer, Integer, Integer>, Fun.Tuple4<String, Integer, Integer, Integer>> evmPair,
                                                     TObjectIntMap<String> headMap) {
        TIntLinkedList compactRep = new TIntLinkedList();

        compactRep.add(getHeadId(headMap, evmPair.a.a));
        compactRep.add(evmPair.a.b);
        compactRep.add(evmPair.a.c);
        compactRep.add(evmPair.a.d);

        compactRep.add(getHeadId(headMap, evmPair.b.a));
        compactRep.add(evmPair.b.b);
        compactRep.add(evmPair.b.c);
        compactRep.add(evmPair.b.d);

        return compactRep;
    }


    private int getHeadId(TObjectIntMap<String> headMap, String head) {
        int id;
        if (headMap.containsKey(head)) {
            id = headMap.get(head);
        } else {
            id = headMap.size();
            headMap.put(head, id);
        }
        return id;
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

        Fun.Tuple4<String, Integer, Integer, Integer> eventTuple1 = new Fun.Tuple4<>(align.getLowercaseWordLemma(evm1.getHeadWord()),
                evm1Slots.containsKey(KmTargetConstants.anchorArg0Marker) ? evm1Slots.get(KmTargetConstants.anchorArg0Marker) : KmTargetConstants.nullArgMarker,
                evm1Slots.containsKey(KmTargetConstants.anchorArg1Marker) ? evm1Slots.get(KmTargetConstants.anchorArg1Marker) : KmTargetConstants.nullArgMarker,
                evm1Slots.containsKey(KmTargetConstants.anchorArg2Marker) ? evm1Slots.get(KmTargetConstants.anchorArg2Marker) : KmTargetConstants.nullArgMarker
        );

        Fun.Tuple4<String, Integer, Integer, Integer> eventTuple2 = new Fun.Tuple4<>(align.getLowercaseWordLemma(evm2.getHeadWord()),
                evm2Slots.containsKey(KmTargetConstants.anchorArg0Marker) ? evm2Slots.get(KmTargetConstants.anchorArg0Marker) : KmTargetConstants.nullArgMarker,
                evm2Slots.containsKey(KmTargetConstants.anchorArg1Marker) ? evm2Slots.get(KmTargetConstants.anchorArg1Marker) : KmTargetConstants.nullArgMarker,
                evm2Slots.containsKey(KmTargetConstants.anchorArg2Marker) ? evm2Slots.get(KmTargetConstants.anchorArg2Marker) : KmTargetConstants.nullArgMarker
        );

        return new Fun.Tuple2<>(eventTuple1, eventTuple2);
    }

    @Override
    public void collectionProcessComplete() throws AnalysisEngineProcessException {
//        tupleCountDb.commit();
//        tupleCountDb.compact();
//        tupleCountDb.close();
        try {
            SerializationHelper.write(new File(dbPath, tupleCountDbFileName + "_" + defaultCooccMapName).getAbsolutePath(), cooccCounts);
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            SerializationHelper.write(new File(dbPath, tupleCountDbFileName + "_" + defaultOccMapName).getAbsolutePath(), occCounts);
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            SerializationHelper.write(new File(dbPath, tupleCountDbFileName + "_" + defaltHeadIdMapName).getAbsolutePath(), headIdMap);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
