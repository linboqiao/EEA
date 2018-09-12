package edu.cmu.cs.lti.script.annotators.writers;

import edu.cmu.cs.lti.script.model.KmTargetConstants;
import edu.cmu.cs.lti.script.type.Article;
import edu.cmu.cs.lti.script.utils.DataPool;
import edu.cmu.cs.lti.uima.io.reader.CustomCollectionReaderFactory;
import edu.cmu.cs.lti.uima.io.writer.AbstractCustomizedTextWriterAnalysisEngine;
import edu.cmu.cs.lti.uima.util.UimaConvenience;
import edu.cmu.cs.lti.utils.Configuration;
import gnu.trove.map.hash.TIntIntHashMap;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.collection.CollectionReaderDescription;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.factory.TypeSystemDescriptionFactory;
import org.apache.uima.fit.pipeline.SimplePipeline;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.metadata.TypeSystemDescription;

import java.io.File;
import java.util.logging.Logger;

/**
 * Created with IntelliJ IDEA.
 * User: zhengzhongliu
 * Date: 10/21/14
 * Time: 1:32 PM
 */
public class NonDuplicateFileNamePrinter extends AbstractCustomizedTextWriterAnalysisEngine {

    @Override
    public String getTextToPrint(JCas aJCas) {
        logger.info(progressInfo(aJCas));

        Article article = JCasUtil.selectSingle(aJCas, Article.class);

        if (DataPool.blackListedArticleId.contains(article.getArticleName())) {
            //ignore this blacklisted file;
            logger.info("Ignored black listed file");
            return "";
        }


        return UimaConvenience.getShortDocumentNameWithOffset(aJCas);
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

    /**
     * @param args
     * @throws java.io.IOException
     * @throws org.apache.uima.UIMAException
     */
    public static void main(String[] args) throws Exception {
        String className = NonDuplicateFileNamePrinter.class.getSimpleName();
        Logger logger = Logger.getLogger(className);

        System.out.println(className + " started...");

        Configuration config = new Configuration(new File(args[0]));

        String inputDir = config.get("edu.cmu.cs.lti.cds.event_tuple.heldout.path"); //"data/02_event_tuples/dev";
        String paramParentOutputDir = config.get("edu.cmu.cs.lti.cds.parent.output"); // "data";
        String blackListFile = config.get("edu.cmu.cs.lti.cds.blacklist"); //"duplicate.count.tail"

        String outputDir = args[1];

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
                NonDuplicateFileNamePrinter.class, typeSystemDescription,
                NonDuplicateFileNamePrinter.PARAM_BASE_OUTPUT_DIR_NAME, outputDir,
                NonDuplicateFileNamePrinter.PARAM_OUTPUT_FILE_SUFFIX, paramOutputFileSuffix,
                NonDuplicateFileNamePrinter.PARAM_PARENT_OUTPUT_DIR_PATH, paramParentOutputDir,
                NonDuplicateFileNamePrinter.PARAM_OUTPUT_STEP_NUMBER, stepNum
        );

        SimplePipeline.runPipeline(reader, writer);

        logger.info("Completed.");
    }
}