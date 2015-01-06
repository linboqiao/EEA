package edu.cmu.cs.lti.cds.annotators.stats;

import edu.cmu.cs.lti.cds.utils.DataPool;
import edu.cmu.cs.lti.script.type.EventMention;
import edu.cmu.cs.lti.uima.annotator.AbstractLoggingAnnotator;
import edu.cmu.cs.lti.utils.BitUtils;
import edu.cmu.cs.lti.utils.TokenAlignmentHelper;
import edu.cmu.cs.lti.utils.Utils;
import gnu.trove.iterator.TIntIntIterator;
import gnu.trove.iterator.TLongIntIterator;
import gnu.trove.map.TIntIntMap;
import gnu.trove.map.TIntLongMap;
import gnu.trove.map.TLongIntMap;
import gnu.trove.map.TLongLongMap;
import gnu.trove.map.hash.TIntIntHashMap;
import gnu.trove.map.hash.TIntLongHashMap;
import gnu.trove.map.hash.TLongIntHashMap;
import gnu.trove.map.hash.TLongLongHashMap;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import weka.core.SerializationHelper;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: zhengzhongliu
 * Date: 11/28/14
 * Time: 3:44 PM
 */
public class FastEventMentionHeadCounter extends AbstractLoggingAnnotator {

    public static final String PARAM_DB_NAME = "dbName";

    private TIntLongMap eventHeadTfDf = new TIntLongHashMap();

    private TLongLongMap eventPairCount = new TLongLongHashMap();

    public static final String PARAM_DB_DIR_PATH = "dbLocation";

    public static final String defaultDBName = "predicate";

    //the name is tf df, it is actually only tf
    public static final String defaultMentionHeadCountMapName = "tfdf";

    public static final String defaultMentionPairCountName = "coocc";

    private int counter = 0;

    private TokenAlignmentHelper align = new TokenAlignmentHelper();

    private String dbPath;

    private String dbFileName;

    @Override
    public void initialize(UimaContext aContext) throws ResourceInitializationException {
        super.initialize(aContext);

        String dbName = (String) aContext.getConfigParameterValue(PARAM_DB_NAME);
        dbFileName = dbName == null ? defaultDBName : dbName;

        dbPath = (String) aContext.getConfigParameterValue(PARAM_DB_DIR_PATH);

        File dbParentPath = new File(dbPath);

        if (!dbParentPath.isDirectory()) {
            dbParentPath.mkdirs();
        }
    }

    @Override
    public void process(JCas aJCas) throws AnalysisEngineProcessException {
        logger.info(progressInfo(aJCas));

        if (DataPool.isBlackList(aJCas, logger)) {
            return;
        }

        TIntIntMap tfCounts = new TIntIntHashMap();
        TLongIntMap pairCounts = new TLongIntHashMap();

        align.loadWord2Stanford(aJCas);

        List<String> allPredicates = new ArrayList<>();

        for (EventMention mention : JCasUtil.select(aJCas, EventMention.class)) {
            String head = align.getLowercaseWordLemma(mention.getHeadWord());
            allPredicates.add(head);
            int headId = DataPool.headIdMap.get(head);
            tfCounts.adjustOrPutValue(headId, 1, 1);
        }

        for (int i = 0; i < allPredicates.size() - 1; i++) {
            for (int j = i + 1; j < allPredicates.size(); j++) {
                int headIdI = DataPool.headIdMap.get(allPredicates.get(i));
                int headIdJ = DataPool.headIdMap.get(allPredicates.get(j));
                pairCounts.adjustOrPutValue(BitUtils.store2Int(headIdI, headIdJ), 1, 1);
            }
        }

        for (TIntIntIterator iter = tfCounts.iterator(); iter.hasNext(); ) {
            iter.advance();
            eventHeadTfDf.adjustOrPutValue(iter.key(), iter.value(), iter.value());
        }

        for (TLongIntIterator iter = pairCounts.iterator(); iter.hasNext(); ) {
            iter.advance();
            eventPairCount.adjustOrPutValue(iter.key(), iter.value(), iter.value());
        }

        counter++;
        if (counter % 4000 == 0) {
            logger.info("Processed " + counter + " documents");
            Utils.printMemInfo(logger);
        }
    }

    @Override
    public void collectionProcessComplete() throws AnalysisEngineProcessException {
        logger.info("Total head words: " + eventHeadTfDf.size());
        logger.info("Total pairs " + eventPairCount.size());
        try {
            SerializationHelper.write(new File(dbPath, dbFileName + "_" + defaultMentionHeadCountMapName).getAbsolutePath(), eventHeadTfDf);
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            SerializationHelper.write(new File(dbPath, dbFileName + "_" + defaultMentionPairCountName).getAbsolutePath(), eventPairCount);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}