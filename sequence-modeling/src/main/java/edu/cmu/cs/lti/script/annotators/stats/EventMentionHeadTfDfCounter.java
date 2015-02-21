package edu.cmu.cs.lti.script.annotators.stats;

import edu.cmu.cs.lti.script.annotators.learn.train.KarlMooneyScriptCounter;
import edu.cmu.cs.lti.script.type.Article;
import edu.cmu.cs.lti.script.type.EventMention;
import edu.cmu.cs.lti.script.utils.DataPool;
import edu.cmu.cs.lti.uima.annotator.AbstractLoggingAnnotator;
import edu.cmu.cs.lti.uima.io.reader.CustomCollectionReaderFactory;
import edu.cmu.cs.lti.uima.io.writer.CustomAnalysisEngineFactory;
import edu.cmu.cs.lti.uima.util.BasicConvenience;
import edu.cmu.cs.lti.utils.Configuration;
import edu.cmu.cs.lti.uima.util.TokenAlignmentHelper;
import gnu.trove.iterator.TObjectIntIterator;
import gnu.trove.map.TIntIntMap;
import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.hash.TIntIntHashMap;
import gnu.trove.map.hash.TObjectIntHashMap;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.collection.CollectionReaderDescription;
import org.apache.uima.fit.pipeline.SimplePipeline;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.uimafit.factory.TypeSystemDescriptionFactory;
import weka.core.SerializationHelper;

import java.io.File;

/**
 * Created with IntelliJ IDEA.
 * User: zhengzhongliu
 * Date: 1/11/15
 * Time: 9:06 PM
 */
public class EventMentionHeadTfDfCounter extends AbstractLoggingAnnotator {
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
            BasicConvenience.printMemInfo(logger);
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


    /**
     * @param args
     * @throws java.io.IOException
     * @throws org.apache.uima.UIMAException
     */
    public static void main(String[] args) throws Exception {
        String className = EventMentionHeadTfDfCounter.class.getSimpleName();

        System.out.println(className + " started...");
        Configuration config = new Configuration(new File(args[0]));

        String inputDir = config.get("edu.cmu.cs.lti.cds.event_tuple.path"); //"data/02_event_tuples";
        String dbPath = config.get("edu.cmu.cs.lti.cds.dbpath");
        String blackListFile = config.get("edu.cmu.cs.lti.cds.blacklist"); //"duplicate.count.tail"
        String[] dbNames = config.getList("edu.cmu.cs.lti.cds.db.basenames"); //db names;

        String paramTypeSystemDescriptor = "TypeSystem";

        DataPool.readBlackList(new File(blackListFile));
        DataPool.loadHeadIds(dbPath, dbNames[0], KarlMooneyScriptCounter.defaltHeadIdMapName);

        // Instantiate the analysis engine.
        TypeSystemDescription typeSystemDescription = TypeSystemDescriptionFactory
                .createTypeSystemDescription(paramTypeSystemDescriptor);

        CollectionReaderDescription reader =
                CustomCollectionReaderFactory.createRecursiveGzippedXmiReader(typeSystemDescription, inputDir, false);

        // The Tf Df counter
        AnalysisEngineDescription headTfDfCounter = CustomAnalysisEngineFactory.createAnalysisEngine(
                EventMentionHeadTfDfCounter.class, typeSystemDescription,
                EventMentionHeadTfDfCounter.PARAM_DB_DIR_PATH, dbPath,
                EventMentionHeadTfDfCounter.PARAM_KEEP_QUIET, false);

        SimplePipeline.runPipeline(reader, headTfDfCounter);
        System.out.println(className + " completed.");
    }
}
