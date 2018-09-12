package edu.cmu.cs.lti.emd.annotators;

import edu.cmu.cs.lti.collection_reader.TbfEventDataReader;
import edu.cmu.cs.lti.learning.utils.MentionTypeUtils;
import edu.cmu.cs.lti.model.UimaConst;
import edu.cmu.cs.lti.script.type.CandidateEventMention;
import edu.cmu.cs.lti.script.type.EventMention;
import edu.cmu.cs.lti.uima.annotator.AbstractLoggingAnnotator;
import edu.cmu.cs.lti.uima.util.UimaConvenience;
import edu.cmu.cs.lti.utils.Configuration;
import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.hash.TObjectIntHashMap;
import org.apache.commons.io.FileUtils;
import org.apache.uima.UIMAException;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.collection.CollectionReaderDescription;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.factory.CollectionReaderFactory;
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

    TObjectIntMap<String> goldClassesWithJoint = new TObjectIntHashMap<>();

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
                    goldClassesWithJoint.adjustOrPutValue(MentionTypeUtils.joinMultipleTypes(joint_type), 1, 1);
                }
                joint_type = new HashSet<>();
                joint_type.add(t);
            }

            previous_start = mention.getBegin();
            previous_end = mention.getEnd();
        }

        if (!joint_type.isEmpty()) {
            goldClassesWithJoint.adjustOrPutValue(MentionTypeUtils.joinMultipleTypes(joint_type), 1, 1);
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

        goldClassesWithJoint.forEachEntry((t, c) -> {
//            logger.debug(String.format("Joint type [%s] occurs %d times.", t, c));
            classesLines.add(t + "\t" + c);
            return true;
        });

        Collections.sort(classesLines);
        try {
            FileUtils.writeLines(classOutputFile, classesLines);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String args[]) throws IOException, UIMAException {
        Configuration taskConfig = new Configuration(args[0]);

        String paramTypeSystemDescriptor = "TaskEventMentionDetectionTypeSystem";
        TypeSystemDescription typeSystemDescription = TypeSystemDescriptionFactory
                .createTypeSystemDescription(paramTypeSystemDescriptor);

        CollectionReaderDescription reader = CollectionReaderFactory.createReaderDescription(
                TbfEventDataReader.class, typeSystemDescription,
                TbfEventDataReader.PARAM_GOLD_STANDARD_FILE, taskConfig.get("edu.cmu.cs.lti.training.gold.tbf"),
                TbfEventDataReader.PARAM_SOURCE_EXT, ".txt",
                TbfEventDataReader.PARAM_SOURCE_TEXT_DIRECTORY,
                taskConfig.get("edu.cmu.cs.lti.training.source_text.dir"),
                TbfEventDataReader.PARAM_TOKEN_DIRECTORY, taskConfig.get("edu.cmu.cs.lti.training.token_map.dir"),
                TbfEventDataReader.PARAM_TOKEN_EXT, ".tab",
                TbfEventDataReader.PARAM_INPUT_VIEW_NAME, UimaConst.inputViewName
        );

        AnalysisEngineDescription runner = AnalysisEngineFactory.createEngineDescription(
                EventMentionTypeClassPrinter.class, typeSystemDescription,
                EventMentionTypeClassPrinter.CLASS_OUTPUT_PATH,
                edu.cmu.cs.lti.utils.FileUtils.joinPaths(
                        taskConfig.get("edu.cmu.cs.lti.training.working.dir"), "mention_types.txt")
        );

        SimplePipeline.runPipeline(reader, runner);
    }
}
