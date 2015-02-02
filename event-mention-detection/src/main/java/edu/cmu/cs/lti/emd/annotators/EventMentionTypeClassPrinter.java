package edu.cmu.cs.lti.emd.annotators;

import com.google.common.collect.Iterables;
import edu.cmu.cs.lti.script.type.Article;
import edu.cmu.cs.lti.script.type.CandidateEventMention;
import edu.cmu.cs.lti.script.type.StanfordCorenlpSentence;
import edu.cmu.cs.lti.uima.annotator.AbstractLoggingAnnotator;
import edu.cmu.cs.lti.uima.io.reader.CustomCollectionReaderFactory;
import edu.cmu.cs.lti.uima.io.writer.CustomAnalysisEngineFactory;
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
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Created with IntelliJ IDEA.
 * User: zhengzhongliu
 * Date: 1/31/15
 * Time: 10:09 PM
 */
public class EventMentionTypeClassPrinter extends AbstractLoggingAnnotator {
    Set<String> allClasses = new HashSet<>();

    static Set<String> targetClasses;

    static {
        targetClasses = new HashSet<>();
//        targetClasses.add("Life_Injure");
//        targetClasses.add("Business_Start-Org");
        targetClasses.add("Personnel_Start-Position");
    }

    @Override
    public void process(JCas aJCas) throws AnalysisEngineProcessException {
//        JCas goldView = UimaConvenience.getView(aJCas, "goldStandard");
//        for (EventMention mention : JCasUtil.select(goldView, EventMention.class)) {
//            String t = mention.getEventType();
//
//            allClasses.add(t);
//
//            if (mention.getEventType().equals("Movement_Transport")) {
//                System.out.println(t);
//                System.out.println(mention.getMentionContext().getCoveredText());
//            } else if (t.equals("Contact_Phone-Write")) {
//                System.out.println(t);
//                System.out.println(mention.getMentionContext().getCoveredText());
//            }
//        }

        Map<CandidateEventMention, Collection<StanfordCorenlpSentence>> mentionBySent =
                JCasUtil.indexCovering(aJCas, CandidateEventMention.class, StanfordCorenlpSentence.class);

        for (CandidateEventMention mention : JCasUtil.select(aJCas, CandidateEventMention.class)) {
            String t = mention.getGoldStandardMentionType();

            if (t == null) {
                continue;
            }

            allClasses.add(t);

            if (targetClasses.contains(t)) {
                System.out.println("================");
                System.out.println(JCasUtil.selectSingle(aJCas, Article.class).getArticleName());
                System.out.println(t);
                System.out.println(mention.getCoveredText());
                System.out.println("===Sentence:====");
                System.out.println(Iterables.get(mentionBySent.get(mention), 0).getCoveredText());
                System.out.println("================");
            }
        }
    }

    @Override
    public void collectionProcessComplete() throws AnalysisEngineProcessException {
//        System.out.println(allClasses.size());
//        for (String c : allClasses) {
//            System.out.println(c);
//        }
    }

    public static void main(String args[]) throws IOException, UIMAException {
//        String inputDir = args[0];
        String inputDir = "/Users/zhengzhongliu/Documents/projects/cmu-script" +
                "/event-mention-detection/data/Event-mention-detection-2014/" +
                "01_dev_data";
        String paramTypeSystemDescriptor = "TypeSystem";
        TypeSystemDescription typeSystemDescription = TypeSystemDescriptionFactory
                .createTypeSystemDescription(paramTypeSystemDescriptor);
        CollectionReaderDescription reader = CustomCollectionReaderFactory.createXmiReader(typeSystemDescription, new File(inputDir).getCanonicalPath(), false);
        AnalysisEngineDescription runner = CustomAnalysisEngineFactory.createAnalysisEngine(EventMentionTypeClassPrinter.class, typeSystemDescription);
        SimplePipeline.runPipeline(reader, runner);
    }
}
