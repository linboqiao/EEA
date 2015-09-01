package edu.cmu.cs.lti.script.annotators.writers.eval;

import edu.cmu.cs.lti.script.model.KmTargetConstants;
import edu.cmu.cs.lti.script.model.MooneyEventRepre;
import edu.cmu.cs.lti.script.type.Article;
import edu.cmu.cs.lti.script.type.EventMention;
import edu.cmu.cs.lti.script.type.EventMentionArgumentLink;
import edu.cmu.cs.lti.script.utils.DataPool;
import edu.cmu.cs.lti.uima.io.reader.CustomCollectionReaderFactory;
import edu.cmu.cs.lti.uima.io.writer.AbstractCustomizedTextWriterAnalysisEngine;
import edu.cmu.cs.lti.uima.util.TokenAlignmentHelper;
import edu.cmu.cs.lti.uima.util.UimaAnnotationUtils;
import edu.cmu.cs.lti.uima.util.UimaConvenience;
import edu.cmu.cs.lti.utils.Configuration;
import edu.cmu.cs.lti.utils.Utils;
import gnu.trove.map.hash.TIntIntHashMap;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.collection.CollectionReaderDescription;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.factory.TypeSystemDescriptionFactory;
import org.apache.uima.fit.pipeline.SimplePipeline;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.metadata.TypeSystemDescription;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.logging.Logger;

/**
 * Created with IntelliJ IDEA.
 * User: zhengzhongliu
 * Date: 10/21/14
 * Time: 1:32 PM
 */
public class KmStyleAllEventMentionClozeTaskGenerator extends AbstractCustomizedTextWriterAnalysisEngine {
//    public static final String PARAM_HEAD_COUNT_DB_NAMES = "headCountDbFileNames";

    public static final String PARAM_DB_DIR_PATH = "dbLocation";

    public static final String PARAM_IGNORE_LOW_FREQ = "ignoreLowFreq";

    public static final String PARAM_CLOZE_MIN_SIZE = "clozeMinSize";

    private TokenAlignmentHelper align = new TokenAlignmentHelper();

    private Random rand = new Random();

//    private Map<String, Fun.Tuple2<Integer, Integer>>[] headTfDfMaps;

    private boolean ignoreLowFreq;

    private int clozeMinSize;

    @Override
    public void initialize(UimaContext aContext) throws ResourceInitializationException {
        super.initialize(aContext);
        clozeMinSize = (Integer) aContext.getConfigParameterValue(PARAM_CLOZE_MIN_SIZE);
        if (aContext.getConfigParameterValue(PARAM_IGNORE_LOW_FREQ) != null) {
            ignoreLowFreq = (Boolean) aContext.getConfigParameterValue(PARAM_IGNORE_LOW_FREQ);
        } else {
            ignoreLowFreq = true;
        }
    }

    @Override
    public String getTextToPrint(JCas aJCas) {
        logger.info(progressInfo(aJCas));

        Article article = JCasUtil.selectSingle(aJCas, Article.class);

        if (DataPool.blackListedArticleId.contains(article.getArticleName())) {
            //ignore this blacklisted file;
            logger.info("Ignored black listed file");
            return "";
        }

        align.loadWord2Stanford(aJCas);
        align.loadFanse2Stanford(aJCas);
        StringBuilder sb = new StringBuilder();

        List<TIntIntHashMap> allSlots = new ArrayList<>();
        List<EventMention> allEvms = new ArrayList<>();

        for (EventMention mention : JCasUtil.select(aJCas, EventMention.class)) {
            if (ignoreLowFreq) {
                long evmTf = DataPool.getPredicateFreq(align.getLowercaseWordLemma(mention.getHeadWord()));
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
                    int entityId = UimaAnnotationUtils.entityIdToInteger(aLink.getArgument().getReferingEntity().getId());
                    slots.put(argumentId, entityId);
                }
            }

            allEvms.add(mention);
            allSlots.add(slots);
        }

        if (allEvms.size() < clozeMinSize) {
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
            //here, if multiple entity id has been mapped to the same held out slot?
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


    /**
     * @param args
     * @throws java.io.IOException
     * @throws org.apache.uima.UIMAException
     */
    public static void main(String[] args) throws Exception {
        String className = KmStyleAllEventMentionClozeTaskGenerator.class.getSimpleName();
        Logger logger = Logger.getLogger(className);

        System.out.println(className + " started...");

        Configuration config = new Configuration(new File(args[0]));

        String inputDir = config.get("edu.cmu.cs.lti.cds.event_tuple.heldout.path"); //"data/02_event_tuples/dev";
        String paramParentOutputDir = config.get("edu.cmu.cs.lti.cds.parent.output"); // "data";
        String clozePath = config.get("edu.cmu.cs.lti.cds.cloze.base.path"); // "cloze_dev"
        String blackListFile = config.get("edu.cmu.cs.lti.cds.blacklist"); //"duplicate.count.tail"
        boolean ignoreLowFreq = config.getBoolean("edu.cmu.cs.lti.cds.filter.lowfreq", false);
        int clozeMinSize = config.getInt("edu.cmu.cs.lti.cds.cloze.minsize", 5);

        String paramOutputFileSuffix = ".txt";

        int stepNum = 3;

        DataPool.readBlackList(new File(blackListFile));
        DataPool.loadHeadStatistics(config, false);

        String paramTypeSystemDescriptor = "TypeSystem";

        // Instantiate the analysis engine.
        TypeSystemDescription typeSystemDescription = TypeSystemDescriptionFactory
                .createTypeSystemDescription(paramTypeSystemDescriptor);

        // Instantiate a collection reader to get XMI as input.
        // Note that you should change the following parameters for your setting.
        CollectionReaderDescription reader =
                CustomCollectionReaderFactory.createGzippedXmiReader(typeSystemDescription, inputDir, false);

        AnalysisEngineDescription writer = AnalysisEngineFactory.createEngineDescription(
                KmStyleAllEventMentionClozeTaskGenerator.class, typeSystemDescription,
                KmStyleAllEventMentionClozeTaskGenerator.PARAM_BASE_OUTPUT_DIR_NAME, clozePath,
                KmStyleAllEventMentionClozeTaskGenerator.PARAM_OUTPUT_FILE_SUFFIX, paramOutputFileSuffix,
                KmStyleAllEventMentionClozeTaskGenerator.PARAM_PARENT_OUTPUT_DIR_PATH, paramParentOutputDir,
                KmStyleAllEventMentionClozeTaskGenerator.PARAM_OUTPUT_STEP_NUMBER, stepNum,
                KmStyleAllEventMentionClozeTaskGenerator.PARAM_DB_DIR_PATH, "data/_db/",
                KmStyleAllEventMentionClozeTaskGenerator.PARAM_IGNORE_LOW_FREQ, ignoreLowFreq,
                KmStyleAllEventMentionClozeTaskGenerator.PARAM_CLOZE_MIN_SIZE, clozeMinSize
        );

        SimplePipeline.runPipeline(reader, writer);

        logger.info("Completed.");
        logger.info("Remeber to clean out empty cloze using the shell scripts");
    }
}
