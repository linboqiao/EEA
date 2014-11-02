package edu.cmu.cs.lti.cds.annotators.script;

import edu.cmu.cs.lti.cds.runners.FullSystemRunner;
import edu.cmu.cs.lti.script.type.Article;
import edu.cmu.cs.lti.script.type.EventMention;
import edu.cmu.cs.lti.uima.annotator.AbstractLoggingAnnotator;
import edu.cmu.cs.lti.utils.TokenAlignmentHelper;
import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.hash.TObjectIntHashMap;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.mapdb.Fun;

import java.io.File;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: zhengzhongliu
 * Date: 10/25/14
 * Time: 4:46 PM
 */
public class EventMentionHeadCounter extends AbstractLoggingAnnotator {

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

        if (FullSystemRunner.blackListedArticleId.contains(article.getArticleName())) {
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
        db.commit();
        db.compact();
        db.close();
    }
}
