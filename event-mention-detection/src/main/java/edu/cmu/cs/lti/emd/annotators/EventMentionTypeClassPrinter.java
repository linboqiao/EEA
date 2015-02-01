package edu.cmu.cs.lti.emd.annotators;

import edu.cmu.cs.lti.script.type.EventMention;
import edu.cmu.cs.lti.uima.annotator.AbstractLoggingAnnotator;
import edu.cmu.cs.lti.uima.io.reader.CustomCollectionReaderFactory;
import edu.cmu.cs.lti.uima.io.writer.CustomAnalysisEngineFactory;
import edu.cmu.cs.lti.uima.util.UimaConvenience;
import org.apache.uima.UIMAException;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.collection.CollectionReaderDescription;
import org.apache.uima.fit.pipeline.SimplePipeline;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.uimafit.factory.TypeSystemDescriptionFactory;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

/**
 * Created with IntelliJ IDEA.
 * User: zhengzhongliu
 * Date: 1/31/15
 * Time: 10:09 PM
 */
public class EventMentionTypeClassPrinter extends AbstractLoggingAnnotator {
    Set<String> allClasses = new HashSet<>();

    @Override
    public void process(JCas aJCas) throws AnalysisEngineProcessException {
        JCas goldView = UimaConvenience.getView(aJCas, "goldStandard");

        for (EventMention mention : JCasUtil.select(goldView, EventMention.class)) {
            String t = mention.getEventType();

            allClasses.add(t);

            if (mention.getEventType().equals("Movement_Transport")) {
                System.out.println(t);
                System.out.println(mention.getMentionContext().getCoveredText());
            } else if (t.equals("Contact_Phone-Write")) {
                System.out.println(t);
                System.out.println(mention.getMentionContext().getCoveredText());
            }

        }
    }

    @Override
    public void collectionProcessComplete() throws AnalysisEngineProcessException {
        System.out.println(allClasses.size());
        for (String c : allClasses) {
            System.out.println(c);
        }
    }

    public static void main(String args[]) throws IOException, UIMAException {
        String inputDir = args[0];
        String paramTypeSystemDescriptor = "TypeSystem";
        TypeSystemDescription typeSystemDescription = TypeSystemDescriptionFactory
                .createTypeSystemDescription(paramTypeSystemDescriptor);
        CollectionReaderDescription reader = CustomCollectionReaderFactory.createXmiReader(typeSystemDescription, new File(inputDir).getCanonicalPath(), false);
        AnalysisEngineDescription runner = CustomAnalysisEngineFactory.createAnalysisEngine(EventMentionTypeClassPrinter.class, typeSystemDescription);
        SimplePipeline.runPipeline(reader, runner);
    }
}
