package edu.cmu.cs.lti.script.annotators.stats;

import edu.cmu.cs.lti.script.type.Article;
import edu.cmu.cs.lti.script.type.EventMention;
import edu.cmu.cs.lti.script.utils.DataPool;
import edu.cmu.cs.lti.uima.annotator.AbstractLoggingAnnotator;
import edu.cmu.cs.lti.uima.io.reader.CustomCollectionReaderFactory;
import edu.cmu.cs.lti.uima.io.writer.CustomAnalysisEngineFactory;
import edu.cmu.cs.lti.utils.Configuration;
import edu.cmu.cs.lti.utils.TokenAlignmentHelper;
import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.hash.TObjectIntHashMap;
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
import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.mapdb.Fun;
import org.uimafit.factory.TypeSystemDescriptionFactory;

import java.io.File;
import java.io.IOException;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: zhengzhongliu
 * Date: 10/25/14
 * Time: 4:46 PM
 */
public class MapDbBasedEventMentionHeadCounter extends AbstractLoggingAnnotator {

    public static final String PARAM_DB_NAME = "dbName";

    private Map<String, Fun.Tuple2<Integer, Integer>> eventHeadTfDf;

    public static final String PARAM_DB_DIR_PATH = "dbLocation";

    public static final String defaultDBName = "headcounts";

    public static final String defaultMentionHeadMapName = "mention_head_counts";

    private int counter = 0;

    private DB db;

    private TokenAlignmentHelper align = new TokenAlignmentHelper();

    @Override
    public void initialize(UimaContext aContext) throws ResourceInitializationException {
        super.initialize(aContext);

        String dbName = (String) aContext.getConfigParameterValue(PARAM_DB_NAME);
        String dbFileName = dbName == null ? defaultDBName : dbName;

        String dbPath = (String) aContext.getConfigParameterValue(PARAM_DB_DIR_PATH);

        File dbParentPath = new File(dbPath);

        if (!dbParentPath.isDirectory()) {
            dbParentPath.mkdirs();
        }

        db = DBMaker.newFileDB(new File(dbPath, dbFileName)).transactionDisable().closeOnJvmShutdown().make();
        eventHeadTfDf = db.getHashMap(defaultMentionHeadMapName);
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

        TObjectIntMap<String> tfCounts = new TObjectIntHashMap<>();

        align.loadWord2Stanford(aJCas);

        for (EventMention mention : JCasUtil.select(aJCas, EventMention.class)) {
            String head = align.getLowercaseWordLemma(mention.getHeadWord());
            tfCounts.adjustOrPutValue(head, 1, 1);
        }

        for (String head : tfCounts.keySet()) {
            int localCount = tfCounts.get(head);
            Fun.Tuple2<Integer, Integer> counts = eventHeadTfDf.get(head);

            if (counts == null) {
                eventHeadTfDf.put(head, new Fun.Tuple2<>(localCount, 1));
            } else {
                eventHeadTfDf.put(head, new Fun.Tuple2<>(counts.a + localCount, counts.b + 1));
            }
        }

        //defrag from time to time
        //debug compact
        counter++;
        if (counter % 4000 == 0) {
            logger.info("Commit and compacting after " + counter);
            db.commit();
            db.compact();
        }
    }

    @Override
    public void collectionProcessComplete() throws AnalysisEngineProcessException {
        logger.info("Total head words: " + eventHeadTfDf.size());
        db.commit();
        db.compact();
        db.close();
    }

    /**
     * @param args
     * @throws java.io.IOException
     * @throws org.apache.uima.UIMAException
     */
    public static void main(String[] args) throws UIMAException, IOException {
        String className = MapDbBasedEventMentionHeadCounter.class.getSimpleName();
        System.out.println(className + " started...");

        Configuration config = new Configuration(new File(args[0]));

        String inputDir = config.get("edu.cmu.cs.lti.cds.event_tuple.path"); //"data/02_event_tuples";
        String dbPath = config.get("edu.cmu.cs.lti.cds.dbpath");
        String blackListFile = config.get("edu.cmu.cs.lti.cds.blacklist"); //"duplicate.count.tail"

        String paramTypeSystemDescriptor = "TypeSystem";

        DataPool.readBlackList(new File(blackListFile));

        // Instantiate the analysis engine.
        TypeSystemDescription typeSystemDescription = TypeSystemDescriptionFactory
                .createTypeSystemDescription(paramTypeSystemDescriptor);

        CollectionReaderDescription reader =
                CustomCollectionReaderFactory.createRecursiveGzippedXmiReader(typeSystemDescription, inputDir, false);

        AnalysisEngineDescription kmScriptCounter = CustomAnalysisEngineFactory.createAnalysisEngine(
                MapDbBasedEventMentionHeadCounter.class, typeSystemDescription,
                MapDbBasedEventMentionHeadCounter.PARAM_DB_DIR_PATH, dbPath,
                MapDbBasedEventMentionHeadCounter.PARAM_KEEP_QUIET, false);

        SimplePipeline.runPipeline(reader, kmScriptCounter);

        System.out.println(className + " completed.");
    }

}