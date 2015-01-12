package edu.cmu.cs.lti.script.annotators.stats;

import edu.cmu.cs.lti.script.type.Article;
import edu.cmu.cs.lti.script.type.EventMention;
import edu.cmu.cs.lti.script.utils.DataPool;
import edu.cmu.cs.lti.uima.annotator.AbstractLoggingAnnotator;
import edu.cmu.cs.lti.utils.TokenAlignmentHelper;
import edu.cmu.cs.lti.utils.Utils;
import gnu.trove.iterator.TObjectIntIterator;
import gnu.trove.map.TIntIntMap;
import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.hash.TIntIntHashMap;
import gnu.trove.map.hash.TObjectIntHashMap;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import weka.core.SerializationHelper;

import java.io.File;

/**
 * Created with IntelliJ IDEA.
 * User: zhengzhongliu
 * Date: 1/11/15
 * Time: 9:06 PM
 */
public class EventMentoinHeadTfDfCounter extends AbstractLoggingAnnotator {
    public static final String PARAM_DB_NAME = "dbName";

    public static final String PARAM_DB_DIR_PATH = "dbLocation";

    TIntIntMap tfCounts = new TIntIntHashMap();

    TIntIntMap dfCounts = new TIntIntHashMap();

    private TokenAlignmentHelper align = new TokenAlignmentHelper();

    private int counter = 0;

    public static final String defaultDBName = "predicate";

    public static final String defaultMentionHeadTfMapName = "tf";

    public static final String defaultMentionHeadDfMapName = "df";

    private File tfOut;

    private File dfOut;

    @Override
    public void initialize(UimaContext aContext) throws ResourceInitializationException {
        super.initialize(aContext);

        String dbName = (String) aContext.getConfigParameterValue(PARAM_DB_NAME);
        String dbFileName = dbName == null ? defaultDBName : dbName;
        String dbPath = (String) aContext.getConfigParameterValue(PARAM_DB_DIR_PATH);

        File dbDir = new File(dbPath);
        if (!dbDir.isDirectory()) {
            dbDir.mkdirs();
        }

        tfOut = new File(dbPath, dbFileName + "_" + defaultMentionHeadTfMapName);
        dfOut = new File(dbPath, dbFileName + "_" + defaultMentionHeadDfMapName);

        logger.info("Term frequencies will be saved at : " + tfOut.getAbsolutePath());
        logger.info("Document frequencies will be saved at : " + dfOut.getAbsolutePath());
    }

    @Override
    public void process(JCas aJCas) throws AnalysisEngineProcessException {
        logger.info(progressInfo(aJCas));
        Article article = JCasUtil.selectSingle(aJCas, Article.class);

        if (DataPool.blackListedArticleId.contains(article.getArticleName())) {
            logger.info("Ignored black listed file");
            return;
        }

        align.loadWord2Stanford(aJCas);

        TObjectIntMap<String> localTfCounts = new TObjectIntHashMap<>();
        TObjectIntMap<String> localDfCounts = new TObjectIntHashMap<>();

        for (EventMention mention : JCasUtil.select(aJCas, EventMention.class)) {
            String headLemma = align.getLowercaseWordLemma(mention.getHeadWord());
            localTfCounts.adjustOrPutValue(headLemma, 1, 1);
            localDfCounts.adjustOrPutValue(headLemma, 0, 1);
        }

        for (TObjectIntIterator<String> localTfIter = localTfCounts.iterator(); localTfIter.hasNext(); ) {
            localTfIter.advance();
            tfCounts.adjustOrPutValue(DataPool.headIdMap.get(localTfIter.key()), localTfIter.value(), localTfIter.value());
        }

        for (TObjectIntIterator<String> localDfIter = localDfCounts.iterator(); localDfIter.hasNext(); ) {
            localDfIter.advance();
            dfCounts.adjustOrPutValue(DataPool.headIdMap.get(localDfIter.key()), localDfIter.value(), localDfIter.value());
        }

        counter++;
        if (counter % 4000 == 0) {
            logger.info("Processed " + counter + " documents");
            Utils.printMemInfo(logger);
        }
    }


    @Override
    public void collectionProcessComplete() throws AnalysisEngineProcessException {
        logger.info("Total head words: " + tfCounts.size() + " == " + dfCounts.size() + ". Writing counts to disk.");
        try {
            SerializationHelper.write(tfOut.getAbsolutePath(), tfCounts);
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            SerializationHelper.write(dfOut.getAbsolutePath(), dfCounts);
        } catch (Exception e) {
            e.printStackTrace();
        }
        logger.info("Done.");
    }
}
