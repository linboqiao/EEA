package edu.cmu.cs.lti.script.annotators.learn.train;

import edu.cmu.cs.lti.script.model.KmTargetConstants;
import edu.cmu.cs.lti.script.type.Article;
import edu.cmu.cs.lti.script.type.EventMention;
import edu.cmu.cs.lti.script.type.EventMentionArgumentLink;
import edu.cmu.cs.lti.script.utils.DataPool;
import edu.cmu.cs.lti.uima.annotator.AbstractLoggingAnnotator;
import edu.cmu.cs.lti.uima.io.reader.CustomCollectionReaderFactory;
import edu.cmu.cs.lti.uima.util.BasicConvenience;
import edu.cmu.cs.lti.uima.util.TokenAlignmentHelper;
import edu.cmu.cs.lti.uima.util.UimaConvenience;
import edu.cmu.cs.lti.utils.Configuration;
import gnu.trove.list.TIntList;
import gnu.trove.list.linked.TIntLinkedList;
import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.hash.TObjectIntHashMap;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.collection.CollectionReaderDescription;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.factory.TypeSystemDescriptionFactory;
import org.apache.uima.fit.pipeline.SimplePipeline;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import weka.core.SerializationHelper;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: zhengzhongliu
 * Date: 11/20/14
 * Time: 5:04 PM
 */
public class UnigramScriptCounter extends AbstractLoggingAnnotator {
    public static final String PARAM_DB_NAME = "dbName";

    public static final String PARAM_DB_DIR_PATH = "dbLocation";

    //TODO consider sentence or event distance

    private TObjectIntMap<TIntList> unigramEventCounts = new TObjectIntHashMap<>();

    public static final String defaultDBName = "tuple_counts";

    public static final String defaultUnigramMapName = "substituted_event_unigrams";

    private String tupleCountDbFileName;

    private TokenAlignmentHelper align = new TokenAlignmentHelper();

    private String dbPath;

    private int counter = 0;

    @Override
    public void initialize(UimaContext aContext) throws ResourceInitializationException {
        super.initialize(aContext);

        String dbName = (String) aContext.getConfigParameterValue(PARAM_DB_NAME);
        tupleCountDbFileName = dbName == null ? defaultDBName : dbName;

        dbPath = (String) aContext.getConfigParameterValue(PARAM_DB_DIR_PATH);

        File dbParentPath = new File(dbPath);

        if (!dbParentPath.isDirectory()) {
            dbParentPath.mkdirs();
        }

        BasicConvenience.printMemInfo(logger, "Initial memory information ");
    }

    @Override
    public void process(JCas aJCas) throws AnalysisEngineProcessException {
        startProcessInfo(aJCas);

        Article article = JCasUtil.selectSingle(aJCas, Article.class);

        if (DataPool.blackListedArticleId.contains(article.getArticleName())) {
            //ignore this blacklisted file;
            logger.info("Ignored black listed file");
            return;
        }

        align.loadWord2Stanford(aJCas);
        align.loadFanse2Stanford(aJCas);

        Collection<EventMention> allMentions = JCasUtil.select(aJCas, EventMention.class);

        for (EventMention mention : allMentions) {
            for (TIntList evmRepre : getAllPossibleRewrite(mention)) {
                unigramEventCounts.adjustOrPutValue(evmRepre, 1, 1);
            }
        }

        counter++;
        if (counter % 4000 == 0) {
            BasicConvenience.printMemInfo(logger, "Memory info after loaded " + counter + " files");
        }
    }

    private List<TIntList> getAllPossibleRewrite(EventMention mention) {
        List<TIntList> previousPossibleRewrites = new ArrayList<>();
        TIntList rewritedEvents = new TIntLinkedList();
        rewritedEvents.add(DataPool.headIdMap.get(align.getLowercaseWordLemma(mention.getHeadWord())));
        previousPossibleRewrites.add(rewritedEvents);

        boolean[] slotsHasArgument = new boolean[KmTargetConstants.numSlots];

        List<EventMentionArgumentLink> argumentLinks = UimaConvenience.convertFSListToList(mention.getArguments(), EventMentionArgumentLink.class);
        for (EventMentionArgumentLink aLink : argumentLinks) {
            String argumentRole = aLink.getArgumentRole();
            if (KmTargetConstants.targetArguments.containsKey(argumentRole)) {
                int slotIndex = KmTargetConstants.argMarkerToSlotIndex(KmTargetConstants.targetArguments.get(argumentRole));
                slotsHasArgument[slotIndex] = true;
            }
        }

        List<TIntList> nextPossibleRewrites = new ArrayList<>();
        for (boolean hasArg : slotsHasArgument) {
            nextPossibleRewrites.clear();
            if (hasArg) {
                for (int rewrite : KmTargetConstants.notNullArguments) {
                    for (TIntList rewritedEvent : previousPossibleRewrites) {
                        TIntList appendedRewritedEvent = new TIntLinkedList();
                        appendedRewritedEvent.addAll(rewritedEvent);
                        appendedRewritedEvent.add(rewrite);
                        nextPossibleRewrites.add(appendedRewritedEvent);
                    }
                }
            } else {
                for (TIntList rewritedEvent : previousPossibleRewrites) {
                    rewritedEvent.add(KmTargetConstants.nullArgMarker);
                    nextPossibleRewrites.add(rewritedEvent);
                }
            }

            previousPossibleRewrites.clear();
            for (TIntList r : nextPossibleRewrites) {
                TIntList c = new TIntLinkedList();
                c.addAll(r);
                previousPossibleRewrites.add(c);
            }
        }

        return nextPossibleRewrites;
    }

    @Override
    public void collectionProcessComplete() throws AnalysisEngineProcessException {
        logger.info("Totally events stored " + unigramEventCounts.size());
        try {
            SerializationHelper.write(new File(dbPath, tupleCountDbFileName + "_" + defaultUnigramMapName).getAbsolutePath(), unigramEventCounts);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }


    /**
     * @param args
     * @throws java.io.IOException
     * @throws org.apache.uima.UIMAException
     */
    public static void main(String[] args) throws Exception {
        String className = UnigramScriptCounter.class.getSimpleName();

        System.out.println(className + " started...");

        Configuration config = new Configuration(new File(args[0]));
        String inputDir = config.get("edu.cmu.cs.lti.cds.event_tuple.path"); //"data/02_event_tuples";
        String blackListFile = config.get("edu.cmu.cs.lti.cds.blacklist"); //"duplicate.count.tail"
        String dbPath = config.get("edu.cmu.cs.lti.cds.dbpath"); //data/_db

        String[] dbNames = config.getList("edu.cmu.cs.lti.cds.db.basenames"); //db names;
        String occSuffix = dbNames[0]; //e.g. 00-02, full

        String headIdMapName = KarlMooneyScriptCounter.defaltHeadIdMapName;

        // ////////////////////////////////////////////////////////////////

        DataPool.readBlackList(new File(blackListFile));
        DataPool.loadHeadIds(dbPath, dbNames[0], headIdMapName);

        String paramTypeSystemDescriptor = "TypeSystem";

        // Instantiate the analysis engine.
        TypeSystemDescription typeSystemDescription = TypeSystemDescriptionFactory
                .createTypeSystemDescription(paramTypeSystemDescriptor);

        CollectionReaderDescription reader =
                CustomCollectionReaderFactory.createRecursiveGzippedXmiReader(typeSystemDescription, inputDir, false);

        AnalysisEngineDescription unigramCounter = AnalysisEngineFactory.createEngineDescription(
                UnigramScriptCounter.class, typeSystemDescription,
                UnigramScriptCounter.PARAM_DB_DIR_PATH, dbPath,
                UnigramScriptCounter.PARAM_DB_NAME, occSuffix,
                UnigramScriptCounter.PARAM_KEEP_QUIET, false);

        SimplePipeline.runPipeline(reader, unigramCounter);

        System.out.println(className + " completed.");
    }
}
