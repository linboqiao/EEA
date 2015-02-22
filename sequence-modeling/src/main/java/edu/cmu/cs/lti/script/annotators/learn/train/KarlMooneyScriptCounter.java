package edu.cmu.cs.lti.script.annotators.learn.train;

import edu.cmu.cs.lti.script.model.KmTargetConstants;
import edu.cmu.cs.lti.script.model.LocalArgumentRepre;
import edu.cmu.cs.lti.script.model.LocalEventMentionRepre;
import edu.cmu.cs.lti.script.type.Article;
import edu.cmu.cs.lti.script.type.EventMention;
import edu.cmu.cs.lti.script.type.EventMentionArgumentLink;
import edu.cmu.cs.lti.script.utils.DataPool;
import edu.cmu.cs.lti.uima.annotator.AbstractLoggingAnnotator;
import edu.cmu.cs.lti.uima.io.reader.CustomCollectionReaderFactory;
import edu.cmu.cs.lti.uima.io.writer.CustomAnalysisEngineFactory;
import edu.cmu.cs.lti.uima.util.BasicConvenience;
import edu.cmu.cs.lti.uima.util.TokenAlignmentHelper;
import edu.cmu.cs.lti.uima.util.UimaAnnotationUtils;
import edu.cmu.cs.lti.uima.util.UimaConvenience;
import edu.cmu.cs.lti.utils.CollectionUtils;
import edu.cmu.cs.lti.utils.Configuration;
import edu.cmu.cs.lti.utils.Utils;
import gnu.trove.list.TIntList;
import gnu.trove.list.linked.TIntLinkedList;
import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.hash.TIntIntHashMap;
import gnu.trove.map.hash.TObjectIntHashMap;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.uima.UIMAException;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.collection.CollectionReaderDescription;
import org.apache.uima.fit.pipeline.SimplePipeline;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.mapdb.Fun;
import org.uimafit.factory.TypeSystemDescriptionFactory;
import weka.core.SerializationHelper;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: zhengzhongliu
 * Date: 10/20/14
 * Time: 3:47 PM
 */
public class KarlMooneyScriptCounter extends AbstractLoggingAnnotator {
    public static final String PARAM_DB_NAME = "dbName";

    public static final String PARAM_DB_DIR_PATH = "dbLocation";

    public static final String PARAM_SKIP_BIGRAM_N = "skippedBigramN";

//    public static final String PARAM_HEAD_COUNT_DB_NAMES = "headCountDbName";

    public static final String PARAM_IGNORE_LOW_FREQ = "ignoreLowFreq";

    private TObjectIntMap<String> headIdMap = new TObjectIntHashMap<>();

    private TObjectIntMap<TIntList> cooccCounts = new TObjectIntHashMap<>();

    private TObjectIntMap<TIntList> occCounts = new TObjectIntHashMap<>();

//    private Map<String, Fun.Tuple2<Integer, Integer>> headTfDfMap;

//    private Map<String, Fun.Tuple2<Integer, Integer>>[] headTfDfMaps;

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
        BasicConvenience.printMemInfo(logger, "Initial memory information ");
    }

    @Override
    public void process(JCas aJCas) throws AnalysisEngineProcessException {
        logger.info(progressInfo(aJCas));
        Article article = JCasUtil.selectSingle(aJCas, Article.class);

        if (DataPool.blackListedArticleId.contains(article.getArticleName())) {
            //ignore this blacklisted file;
            logger.info("Ignored black listed file");
            return;
        }

        align.loadWord2Stanford(aJCas);
        align.loadFanse2Stanford(aJCas);

        Collection<EventMention> allMentions = JCasUtil.select(aJCas, EventMention.class);
        List<Pair<EventMention, EventMention>> mentionBigrams = CollectionUtils.nSkippedBigrams(allMentions, skippedBigramN);

        //TODO we probably also want to have a EOF indicator here
        for (Pair<EventMention, EventMention> bigram : mentionBigrams) {
            Fun.Tuple2<Fun.Tuple4<String, Integer, Integer, Integer>, Fun.Tuple4<String, Integer, Integer, Integer>> subsitutedBigram =
                    firstBasedSubstitution(align, bigram.getLeft(), bigram.getRight());

            if (ignoreLowFreq) {
//                int evm1Tf = MultiMapUtils.getTf(headTfDfMaps, align.getLowercaseWordLemma(bigram.getLeft().getHeadWord()));
//                int evm2Tf = MultiMapUtils.getTf(headTfDfMaps, align.getLowercaseWordLemma(bigram.getRight().getHeadWord()));

                long evm1Tf = DataPool.getPredicateFreq(align.getLowercaseWordLemma(bigram.getLeft().getHeadWord()));
                long evm2Tf = DataPool.getPredicateFreq(align.getLowercaseWordLemma(bigram.getRight().getHeadWord()));

                if (Utils.termFrequencyFilter(evm1Tf) || Utils.termFrequencyFilter(evm2Tf)) {
                    logger.info("Filtered because of low frequency: " + bigram.getLeft().getCoveredText() + " " + evm1Tf + " " + bigram.getRight().getCoveredText() + " " + evm2Tf);
                    continue;
                }
            }


//            System.err.println(subsitutedBigram);
//
//            if (checkMatch(subsitutedBigram, "understand", "say", 0, 0, -1, -1, 0, -1)) {
//                System.err.println("Found this pair");
//                System.err.println(compactEvmPairSubstituiton(subsitutedBigram, headIdMap));
//            }


            cooccCounts.adjustOrPutValue(compactEvmPairSubstituiton(subsitutedBigram, headIdMap), 1, 1);
            occCounts.adjustOrPutValue(compactEvmSubstituiton(subsitutedBigram.a, headIdMap), 1, 1);
        }

        counter++;
        if (counter % 4000 == 0) {
            BasicConvenience.printMemInfo(logger, "Memory info after loaded " + counter + " files");
        }
    }

    private boolean checkMatch(Fun.Tuple2<Fun.Tuple4<String, Integer, Integer, Integer>, Fun.Tuple4<String, Integer, Integer, Integer>> sb,
                               String word1, String word2, int id11, int id12, int id13, int id21, int id22, int id23) {
        return sb.a.a.equals(word1) && sb.b.a.equals(word2)
                && sb.a.b == id11 && sb.b.b == id21
                && sb.a.c == id12 && sb.b.c == id22
                && sb.a.d == id13 && sb.b.d == id23;
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
//            System.err.println("head: " + head);
        }
        return id;
    }

    public static Fun.Tuple2<Fun.Tuple4<String, Integer, Integer, Integer>, Fun.Tuple4<String, Integer, Integer, Integer>> firstBasedSubstitution(
            LocalEventMentionRepre evm1, LocalEventMentionRepre evm2) {
        TIntIntHashMap evm1BasedRewriteMap = new TIntIntHashMap();

        TIntIntHashMap evm1Slots = new TIntIntHashMap();
        TIntIntHashMap evm2Slots = new TIntIntHashMap();

        for (int slotId = 0; slotId < evm1.getNumArgs(); slotId++) {
            int argMarker = KmTargetConstants.slotIndexToArgMarker(slotId);
            LocalArgumentRepre argi = evm1.getArg(slotId);
            if (argi != null) {
                //set default evm1 slots when we haven't check for overlap in evm2
                evm1Slots.put(argMarker, KmTargetConstants.otherMarker);

                if (!argi.isOther()) {
                    if (argi.isConcrete()) {
                        evm1BasedRewriteMap.put(argi.getEntityId(), argMarker);
                    } else {
                        evm1BasedRewriteMap.put(argi.getRewrittenId(), argMarker);
                    }
                }
            }
        }

        for (int slotIndex = 0; slotIndex < evm2.getNumArgs(); slotIndex++) {
            LocalArgumentRepre argi = evm2.getArg(slotIndex);
            int argMarker = KmTargetConstants.slotIndexToArgMarker(slotIndex);

            if (argi != null) {
                int entityId = argi.isConcrete() ? argi.getEntityId() : argi.getRewrittenId();
                int substitutedArgMarker;
                if (evm1BasedRewriteMap.containsKey(entityId)) {
                    substitutedArgMarker = evm1BasedRewriteMap.get(entityId);
                    evm1Slots.put(substitutedArgMarker, substitutedArgMarker);
                } else {
                    substitutedArgMarker = KmTargetConstants.otherMarker;
                }
                evm2Slots.put(argMarker, substitutedArgMarker);
            }
        }

        Fun.Tuple4<String, Integer, Integer, Integer> eventTuple1 = new Fun.Tuple4<>(evm1.getMentionHead(),
                evm1Slots.containsKey(KmTargetConstants.anchorArg0Marker) ? evm1Slots.get(KmTargetConstants.anchorArg0Marker) : KmTargetConstants.nullArgMarker,
                evm1Slots.containsKey(KmTargetConstants.anchorArg1Marker) ? evm1Slots.get(KmTargetConstants.anchorArg1Marker) : KmTargetConstants.nullArgMarker,
                evm1Slots.containsKey(KmTargetConstants.anchorArg2Marker) ? evm1Slots.get(KmTargetConstants.anchorArg2Marker) : KmTargetConstants.nullArgMarker
        );

        Fun.Tuple4<String, Integer, Integer, Integer> eventTuple2 = new Fun.Tuple4<>(evm2.getMentionHead(),
                evm2Slots.containsKey(KmTargetConstants.anchorArg0Marker) ? evm2Slots.get(KmTargetConstants.anchorArg0Marker) : KmTargetConstants.nullArgMarker,
                evm2Slots.containsKey(KmTargetConstants.anchorArg1Marker) ? evm2Slots.get(KmTargetConstants.anchorArg1Marker) : KmTargetConstants.nullArgMarker,
                evm2Slots.containsKey(KmTargetConstants.anchorArg2Marker) ? evm2Slots.get(KmTargetConstants.anchorArg2Marker) : KmTargetConstants.nullArgMarker
        );

        return new Fun.Tuple2<>(eventTuple1, eventTuple2);
    }

    public static Fun.Tuple2<Fun.Tuple4<String, Integer, Integer, Integer>, Fun.Tuple4<String, Integer, Integer, Integer>> firstBasedSubstitution(
            TokenAlignmentHelper align, EventMention evm1, EventMention evm2) {
        TIntIntHashMap evm1Args = new TIntIntHashMap();
        TIntIntHashMap evm1Slots = new TIntIntHashMap();

        for (EventMentionArgumentLink aLink : UimaConvenience.convertFSListToList(evm1.getArguments(), EventMentionArgumentLink.class)) {
            String argumentRole = aLink.getArgumentRole();

            if (KmTargetConstants.targetArguments.containsKey(argumentRole)) {
                //only when this role is one of the targets arguments we record it
                int slotId = KmTargetConstants.targetArguments.get(argumentRole);
                evm1Args.put(UimaAnnotationUtils.entityIdToInteger(aLink.getArgument().getReferingEntity().getId()), slotId);
                //if evm1 has an argument here, we mark it as other; otherwise it will be null
                evm1Slots.put(slotId, KmTargetConstants.otherMarker);
            }
        }

        TIntIntHashMap evm2Slots = new TIntIntHashMap();
        for (EventMentionArgumentLink aLink : UimaConvenience.convertFSListToList(evm2.getArguments(), EventMentionArgumentLink.class)) {
            String argumentRole = aLink.getArgumentRole();

            if (KmTargetConstants.targetArguments.containsKey(argumentRole)) {
                int entityId = UimaAnnotationUtils.entityIdToInteger(aLink.getArgument().getReferingEntity().getId());
                int slotId = KmTargetConstants.targetArguments.get(argumentRole);

                //substitution for the second event is based on the first event mention
                int substituteId;
                if (evm1Args.containsKey(entityId)) {
                    substituteId = evm1Args.get(entityId);
                    //if evm2 has an overlap with evm1, we activate both
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

        logger.info("Coocc counts: " + cooccCounts.size());
        logger.info("Occ counts: " + occCounts.size());
        logger.info("Head word counts : " + headIdMap.size());

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


    /**
     * @param args
     * @throws java.io.IOException
     * @throws org.apache.uima.UIMAException
     */
    public static void main(String[] args) throws UIMAException, IOException {
        String className = KarlMooneyScriptCounter.class.getSimpleName();

        System.out.println(className + " started...");

        Configuration config = new Configuration(new File(args[0]));
        String occSuffix = args.length > 1 ? args[1] : "db"; //e.g. 00-02, full

        String inputDir = config.get("edu.cmu.cs.lti.cds.event_tuple.path"); //"data/02_event_tuples";
        String blackListFile = config.get("edu.cmu.cs.lti.cds.blacklist"); //"duplicate.count.tail"
        String dbPath = config.get("edu.cmu.cs.lti.cds.dbpath"); //data/_db
        boolean ignoreLowFreq = config.getBoolean("edu.cmu.cs.lti.cds.filter.lowfreq");
        int skipGramN = config.getInt("edu.cmu.cs.lti.cds.mooney.skipgram.n");


        DataPool.readBlackList(new File(blackListFile));

        String paramTypeSystemDescriptor = "TypeSystem";

        // Instantiate the analysis engine.
        TypeSystemDescription typeSystemDescription = TypeSystemDescriptionFactory
                .createTypeSystemDescription(paramTypeSystemDescriptor);

        CollectionReaderDescription reader =
                CustomCollectionReaderFactory.createRecursiveGzippedXmiReader(typeSystemDescription, inputDir, true);

        AnalysisEngineDescription kmScriptCounter = CustomAnalysisEngineFactory.createAnalysisEngine(
                KarlMooneyScriptCounter.class, typeSystemDescription,
                KarlMooneyScriptCounter.PARAM_DB_DIR_PATH, dbPath,
                KarlMooneyScriptCounter.PARAM_SKIP_BIGRAM_N, skipGramN,
                KarlMooneyScriptCounter.PARAM_DB_NAME, "occs_" + occSuffix,
                KarlMooneyScriptCounter.PARAM_IGNORE_LOW_FREQ, ignoreLowFreq,
                AbstractLoggingAnnotator.PARAM_KEEP_QUIET, false);

        SimplePipeline.runPipeline(reader, kmScriptCounter);

        System.out.println(className + " completed.");
    }
}
