package edu.cmu.cs.lti.emd.annotators;

import com.google.common.base.Joiner;
import edu.cmu.cs.lti.model.UimaConst;
import edu.cmu.cs.lti.script.type.CandidateEventMention;
import edu.cmu.cs.lti.script.type.EventMention;
import edu.cmu.cs.lti.script.type.StanfordCorenlpSentence;
import edu.cmu.cs.lti.uima.annotator.AbstractLoggingAnnotator;
import edu.cmu.cs.lti.uima.io.reader.CustomCollectionReaderFactory;
import edu.cmu.cs.lti.uima.util.UimaConvenience;
import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.hash.TObjectIntHashMap;
import org.apache.uima.UIMAException;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.collection.CollectionReaderDescription;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.pipeline.SimplePipeline;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.uimafit.factory.TypeSystemDescriptionFactory;

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

    TObjectIntMap<Set<String>> jointClassesGold = new TObjectIntHashMap<>();

    TObjectIntMap<String> singleClassesGold = new TObjectIntHashMap<>();

    TObjectIntHashMap<String> jointClassesCandidate = new TObjectIntHashMap<>();

    TObjectIntMap<String> singleClassesGoldCandidate = new TObjectIntHashMap<>();

    static Set<String> targetClasses;

    static {
        targetClasses = new HashSet<>();
//        targetClasses.add("Life_Injure");
//        targetClasses.add("Business_Start-Org");
//        targetClasses.add("Personnel_Start-Position");
//        targetClasses.add("Contact_Meet");
        targetClasses.add("Transaction_Transfer-Money");
    }

    @Override
    public void process(JCas aJCas) throws AnalysisEngineProcessException {
        JCas goldView = UimaConvenience.getView(aJCas, UimaConst.goldViewName);

//        String joint_type = null;
        Set<String> joint_type = new HashSet<>();
        int previous_start = -1;
        int previous_end = -1;
        for (EventMention mention : JCasUtil.select(goldView, EventMention.class)) {
            String t = mention.getEventType();

            singleClassesGold.adjustOrPutValue(t, 1, 1);

            if (mention.getBegin() == previous_start && mention.getEnd() == previous_end) {
                joint_type.add(t);
            } else {
                if (!joint_type.isEmpty()) {
                    jointClassesGold.adjustOrPutValue(joint_type, 1, 1);
                }
                joint_type = new HashSet<>();
                joint_type.add(t);
            }

            previous_start = mention.getBegin();
            previous_end = mention.getEnd();
        }

        if (!joint_type.isEmpty()) {
            jointClassesGold.adjustOrPutValue(joint_type, 1, 1);
        }

        Map<CandidateEventMention, Collection<StanfordCorenlpSentence>> mentionBySent =
                JCasUtil.indexCovering(aJCas, CandidateEventMention.class, StanfordCorenlpSentence.class);

        for (CandidateEventMention mention : JCasUtil.select(aJCas, CandidateEventMention.class)) {
            String t = mention.getGoldStandardMentionType();

            if (t == null) {
                continue;
            }

            singleClassesGoldCandidate.adjustOrPutValue(t, 1, 1);

//            if (targetClasses.contains(t)) {
//                System.out.println("================");
//                System.out.println(JCasUtil.selectSingle(aJCas, Article.class).getArticleName());
//                System.out.println(t);
//                System.out.println(mention.getCoveredText());
//                System.out.println("===Sentence:====");
//                System.out.println(Iterables.get(mentionBySent.get(mention), 0).getCoveredText());
//                System.out.println("================");
//            }
        }
    }

    @Override
    public void collectionProcessComplete() throws AnalysisEngineProcessException {
        TObjectIntMap<String> joint_types = new TObjectIntHashMap<>();

        final int[] total_type_count = {0};
        final int[] joint_type_count = {0};
        jointClassesGold.forEachEntry((t, c) -> {
            System.out.println(String.format("Type %s occurs %d times.", t, c));
            if (t.size() > 1) {
                joint_types.adjustOrPutValue(Joiner.on(" ; ").join(t), 1, c);
                joint_type_count[0] += c;
            }
            total_type_count[0] += c;
            return true;
        });

        System.out.println("#Possible joint types : " + joint_types.size());
        joint_types.forEachEntry((t, c) -> {
//            if (c > 5) {
                System.out.println(String.format("Joint type %s occurs %d times.", t, c));
//            }
            return true;
        });

        System.out.println("Total types count : " + total_type_count[0]);
        System.out.println("Joint type count : " + joint_type_count[0]);
    }

    public static void main(String args[]) throws IOException, UIMAException {
//        String inputDir = args[0];
        String inputDir = "/Users/zhengzhongliu/Documents/projects/cmu-script/data/mention/kbp/";
        String inputAll = inputDir + "preprocessed";
        String inputTest = inputDir + "01_test_data";
        String inputDev = inputDir + "01_dev_data";
        String inputTrain = inputDir + "01_train_data";

        String paramTypeSystemDescriptor = "TaskEventMentionDetectionTypeSystem";
        TypeSystemDescription typeSystemDescription = TypeSystemDescriptionFactory
                .createTypeSystemDescription(paramTypeSystemDescriptor);

        CollectionReaderDescription allReader = CustomCollectionReaderFactory.createXmiReader(typeSystemDescription,
                inputAll, false);
//        CollectionReaderDescription testReader = CustomCollectionReaderFactory.createXmiReader(typeSystemDescription,
//                inputTest, false);
//        CollectionReaderDescription devReader = CustomCollectionReaderFactory.createXmiReader(typeSystemDescription,
//                inputDev, false);
//        CollectionReaderDescription trainReader = CustomCollectionReaderFactory.createXmiReader
//                (typeSystemDescription, inputTrain, false);

        AnalysisEngineDescription runner = AnalysisEngineFactory.createEngineDescription(EventMentionTypeClassPrinter
                .class, typeSystemDescription);

        SimplePipeline.runPipeline(allReader, runner);
//        SimplePipeline.runPipeline(testReader, runner);
//        SimplePipeline.runPipeline(devReader, runner);
//        SimplePipeline.runPipeline(trainReader, runner);
    }
}
