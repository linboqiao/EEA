package edu.cmu.cs.lti.emd.annotators.demo;

import edu.cmu.cs.lti.model.UimaConst;
import edu.cmu.cs.lti.script.type.EventMention;
import edu.cmu.cs.lti.uima.annotator.AbstractLoggingAnnotator;
import edu.cmu.cs.lti.uima.io.reader.CustomCollectionReaderFactory;
import edu.cmu.cs.lti.uima.util.UimaConvenience;
import org.apache.uima.UIMAException;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.collection.CollectionReaderDescription;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.pipeline.SimplePipeline;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.uimafit.factory.TypeSystemDescriptionFactory;

import java.io.IOException;

/**
 * Created with IntelliJ IDEA.
 * Date: 1/21/16
 * Time: 2:36 PM
 *
 * @author Zhengzhong Liu
 */
public class MentionOffsetChecker extends AbstractLoggingAnnotator {

    private String compareViewName = UimaConst.inputViewName;

    @Override
    public void initialize(UimaContext aContext) throws ResourceInitializationException {
        super.initialize(aContext);
        logger.info("Checking between default view and " + compareViewName);
    }

    @Override
    public void process(JCas aJCas) throws AnalysisEngineProcessException {
        JCas inputView = JCasUtil.getView(aJCas, compareViewName, false);

        String docId = UimaConvenience.getDocId(aJCas, true);

        for (EventMention mention : JCasUtil.select(aJCas, EventMention.class)) {
            logger.info(mention.getCoveredText() + " " + inputView.getDocumentText().substring(mention.getBegin(),
                    mention.getEnd()));

            String newViewText = mention.getCoveredText();
            String inputText = inputView.getDocumentText().substring(mention.getBegin(), mention.getEnd());

            if (!newViewText.equals(inputText)) {
                logger.warn(String.format(
                        "Found discrepancies at doc [%s], text is [%s], input text is [%s] , at [%d:%d]",
                        docId, newViewText, inputText, mention.getBegin(), mention.getEnd())
                );
            }
        }
    }

    public static void main(String[] argv) throws UIMAException, IOException {
        TypeSystemDescription typeSystemDescription = TypeSystemDescriptionFactory.createTypeSystemDescription(
                "TaskEventMentionDetectionTypeSystem");

        CollectionReaderDescription reader = CustomCollectionReaderFactory.createXmiReader(typeSystemDescription,
                "data/mention/cold_start/LDC2015E77", "coref_lv1_mention_no_merge");

        AnalysisEngineDescription processor = AnalysisEngineFactory.createEngineDescription(
                MentionOffsetChecker.class, typeSystemDescription
        );

        SimplePipeline.runPipeline(reader, processor);
    }
}
