package edu.cmu.cs.lti.emd.annotators;

import com.google.common.base.Joiner;
import edu.cmu.cs.lti.model.UimaConst;
import edu.cmu.cs.lti.script.type.CandidateEventMention;
import edu.cmu.cs.lti.script.type.EventMention;
import edu.cmu.cs.lti.uima.annotator.AbstractLoggingAnnotator;
import edu.cmu.cs.lti.uima.io.reader.CustomCollectionReaderFactory;
import edu.cmu.cs.lti.uima.util.UimaConvenience;
import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.hash.TObjectIntHashMap;
import org.apache.commons.io.FileUtils;
import org.apache.uima.UIMAException;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.collection.CollectionReaderDescription;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.pipeline.SimplePipeline;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.uimafit.factory.TypeSystemDescriptionFactory;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Created with IntelliJ IDEA.
 * User: zhengzhongliu
 * Date: 1/31/15
 * Time: 10:09 PM
 */
public class EventMentionTypeClassPrinter extends AbstractLoggingAnnotator {

    public static final String CLASS_OUTPUT_PATH = "classOutputPath";

    @ConfigurationParameter(name = CLASS_OUTPUT_PATH)
    private File classOutputFile;

    TObjectIntMap<List<String>> goldClassesWithJoint = new TObjectIntHashMap<>();

    TObjectIntMap<String> candidateClassesSingle = new TObjectIntHashMap<>();

    static Set<String> targetClasses;

    static {
        targetClasses = new HashSet<>();
//        targetClasses.add("Life_Injure");
//        targetClasses.add("Business_Start-Org");
//        targetClasses.add("Personnel_Start-Position");
//        targetClasses.add("Contact_Meet");
//        targetClasses.add("Transaction_Transfer-Money");
    }

    @Override
    public void process(JCas aJCas) throws AnalysisEngineProcessException {
        JCas goldView = UimaConvenience.getView(aJCas, UimaConst.goldViewName);

        Set<String> joint_type = new HashSet<>();
        int previous_start = -1;
        int previous_end = -1;
        for (EventMention mention : JCasUtil.select(goldView, EventMention.class)) {
            String t = mention.getEventType();

            if (mention.getBegin() == previous_start && mention.getEnd() == previous_end) {
                joint_type.add(t);
            } else {
                if (!joint_type.isEmpty()) {
                    List<String> sorted_joint_type = new ArrayList<>(joint_type);
                    Collections.sort(sorted_joint_type);
                    goldClassesWithJoint.adjustOrPutValue(sorted_joint_type, 1, 1);
                    if (joint_type.size() > 1) {
                        System.out.println(aJCas.getDocumentText().substring(previous_start, previous_end) + "\t" +
                                Joiner.on(" ; ").join(joint_type));
                    }
                }
                joint_type = new HashSet<>();
                joint_type.add(t);
            }

            previous_start = mention.getBegin();
            previous_end = mention.getEnd();
        }

        if (!joint_type.isEmpty()) {
            List<String> sorted_joint_type = new ArrayList<>(joint_type);
            Collections.sort(sorted_joint_type);
            goldClassesWithJoint.adjustOrPutValue(sorted_joint_type, 1, 1);
        }

        for (CandidateEventMention mention : JCasUtil.select(aJCas, CandidateEventMention.class)) {
            String t = mention.getGoldStandardMentionType();

            if (t == null) {
                continue;
            }

            candidateClassesSingle.adjustOrPutValue(t, 1, 1);
        }
    }

    @Override
    public void collectionProcessComplete() throws AnalysisEngineProcessException {
        List<String> classesLines = new ArrayList<>();

        final int[] total_type_count = {0};
        final int[] joint_type_count = {0};

        goldClassesWithJoint.forEachEntry((t, c) -> {
            String joinedTypeName = Joiner.on(" ; ").join(t);
            if (t.size() > 1) {
                joint_type_count[0] += c;
                System.out.println(String.format("Joint type %s occurs %d times.", joinedTypeName, c));
            }
            total_type_count[0] += c;

            classesLines.add(joinedTypeName + "\t" + c);
            return true;
        });

        goldClassesWithJoint.forEachEntry((t, c) -> {
            String joinedTypeName = Joiner.on(" ; ").join(t);
            if (t.size() == 1) {
                System.out.println(String.format("Single type %s occurs %d times.", joinedTypeName, c));
            }
            return true;
        });


        System.out.println("Total types count : " + total_type_count[0]);
        System.out.println("Joint type count : " + joint_type_count[0]);


        Collections.sort(classesLines);
        try {
            FileUtils.writeLines(classOutputFile, classesLines);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String args[]) throws IOException, UIMAException {
//        String inputDir = args[0];
        String inputDir = "/Users/zhengzhongliu/Documents/projects/cmu-script/data/mention/kbp/LDC2015E73/";
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

        AnalysisEngineDescription runner = AnalysisEngineFactory.createEngineDescription(
                EventMentionTypeClassPrinter.class, typeSystemDescription,
                EventMentionTypeClassPrinter.CLASS_OUTPUT_PATH, inputDir + "mention_types.txt");

        SimplePipeline.runPipeline(allReader, runner);
//        SimplePipeline.runPipeline(testReader, runner);
//        SimplePipeline.runPipeline(devReader, runner);
//        SimplePipeline.runPipeline(trainReader, runner);
    }
}
