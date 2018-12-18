<<<<<<< HEAD:cmu-script/cmu-script/event-mention-detection/src/main/java/edu/cmu/cs/lti/emd/annotators/SingleInstanceDebugger.java
package edu.cmu.cs.lti.emd.annotators;

import edu.cmu.cs.lti.emd.annotators.crf.MentionSequenceCrfTrainer;
import edu.cmu.cs.lti.emd.annotators.crf.MentionTypeCrfTrainer;
import edu.cmu.cs.lti.learning.decoding.ViterbiDecoder;
import edu.cmu.cs.lti.learning.feature.FeatureSpecParser;
import edu.cmu.cs.lti.learning.feature.extractor.MultiSentenceFeatureExtractor;
import edu.cmu.cs.lti.learning.feature.extractor.SentenceFeatureExtractor;
import edu.cmu.cs.lti.learning.feature.extractor.UimaSequenceFeatureExtractor;
import edu.cmu.cs.lti.learning.model.*;
import edu.cmu.cs.lti.learning.training.SequenceDecoder;
import edu.cmu.cs.lti.learning.utils.CubicLagrangian;
import edu.cmu.cs.lti.learning.utils.DummyCubicLagrangian;
import edu.cmu.cs.lti.pipeline.BasicPipeline;
import edu.cmu.cs.lti.pipeline.ProcessorWrapper;
import edu.cmu.cs.lti.script.type.CandidateEventMention;
import edu.cmu.cs.lti.script.type.EventMention;
import edu.cmu.cs.lti.script.type.StanfordCorenlpSentence;
import edu.cmu.cs.lti.script.type.StanfordCorenlpToken;
import edu.cmu.cs.lti.uima.annotator.AbstractLoggingAnnotator;
import edu.cmu.cs.lti.uima.io.reader.CustomCollectionReaderFactory;
import edu.cmu.cs.lti.uima.util.UimaAnnotationUtils;
import edu.cmu.cs.lti.uima.util.UimaConvenience;
import edu.cmu.cs.lti.utils.Configuration;
import org.apache.commons.lang3.SerializationUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.uima.UIMAException;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.collection.CollectionReaderDescription;
import org.apache.uima.collection.metadata.CpeDescriptorException;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.DocumentAnnotation;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.uimafit.factory.TypeSystemDescriptionFactory;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;

/**
 * Created with IntelliJ IDEA.
 * Date: 12/15/15
 * Time: 1:39 PM
 *
 * @author Zhengzhong Liu
 */
public class SingleInstanceDebugger extends AbstractLoggingAnnotator {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private UimaSequenceFeatureExtractor lv1Extractor;
    private MultiSentenceFeatureExtractor<CandidateEventMention> lv2Extractor;

    private GraphWeightVector lv1WeightVector;
    private GraphWeightVector lv2WeightVector;

    private File lv1ModelDirectory = new File("../models/mention_type_crf/split_0/");

    private File lv2ModelDirectory = new File("../models/mention_sequence_crf/split_0");

    public static final String PARAM_CONFIG = "configuration";
    @ConfigurationParameter(name = PARAM_CONFIG)
    private Configuration config;

    public static final String PARAM_TARGET_DOC_NAME = "docName";
    @ConfigurationParameter(name = PARAM_TARGET_DOC_NAME)
    private String targetDocName;

    public static final String PARAM_TARGET_WORD = "targetWord";
    @ConfigurationParameter(name = PARAM_TARGET_WORD)
    private String targetWord;

    //    private FeatureAlphabet lv1FeatureAlphabet;
    private ViterbiDecoder sentenceDecoder;
    private SequenceDecoder mentionDecoder;

    CubicLagrangian dummyLagrangian = new DummyCubicLagrangian();

    @Override
    public void initialize(UimaContext aContext) throws ResourceInitializationException {
        super.initialize(aContext);
        logger.info("Loading models ...");

        try {
            lv1WeightVector = SerializationUtils.deserialize(new FileInputStream(new File
                    (lv1ModelDirectory, MentionTypeCrfTrainer.MODEL_NAME)));
            lv2WeightVector = SerializationUtils.deserialize(new FileInputStream(new File
                    (lv2ModelDirectory, MentionSequenceCrfTrainer.MODEL_NAME)));
        } catch (IOException e) {
            throw new ResourceInitializationException(e);
        }

        logger.info("Model loaded");
        try {
            FeatureSpecParser wordFeatureSpecParser = new FeatureSpecParser(
                    config.get("edu.cmu.cs.lti.feature.sentence.package.name"));

            FeatureSpecParser mentionFeatureSpecParser = new FeatureSpecParser(
                    config.get("edu.cmu.cs.lti.feature.document.package.name")
            );

            String docFeatureSpec = config.get("edu.cmu.cs.lti.features.type.lv1.doc.spec");
            String wordFeatureSpec = config.get("edu.cmu.cs.lti.features.type.lv1.sentence.spec");

            Configuration lv1DocFeatureConfig = new FeatureSpecParser(
                    config.get("edu.cmu.cs.lti.feature.document.package.name")
            ).parseFeatureFunctionSpecs(docFeatureSpec);

            Configuration lv1SentFeatureConfig = new FeatureSpecParser(
                    config.get("edu.cmu.cs.lti.feature.sentence.package.name")
            ).parseFeatureFunctionSpecs(wordFeatureSpec);


            Configuration lv2typeFeatureConfig = wordFeatureSpecParser.parseFeatureFunctionSpecs(lv2WeightVector
                    .getFeatureSpec());
            Configuration mentionFeatureConfig = mentionFeatureSpecParser.parseFeatureFunctionSpecs(lv2WeightVector
                    .getFeatureSpec());

            logger.info("Feature specification for lv2 is " + lv2WeightVector.getFeatureSpec());

            lv1Extractor = new SentenceFeatureExtractor(lv1WeightVector.getFeatureAlphabet(), config,
                    lv1SentFeatureConfig, lv1DocFeatureConfig, false);
            lv2Extractor = new MultiSentenceFeatureExtractor<>(lv2WeightVector.getFeatureAlphabet(), config,
                    lv2typeFeatureConfig, mentionFeatureConfig, true, CandidateEventMention.class);
        } catch (ClassNotFoundException | NoSuchMethodException | InvocationTargetException | InstantiationException
                | IllegalAccessException e) {
            e.printStackTrace();
        }

        sentenceDecoder = new ViterbiDecoder(lv1WeightVector.getFeatureAlphabet(), lv1WeightVector.getClassAlphabet());
        mentionDecoder = new ViterbiDecoder(lv2WeightVector.getFeatureAlphabet(), lv2WeightVector.getClassAlphabet());

        logger.info("Target word is " + targetWord);
    }

    @Override
    public void process(JCas aJCas) throws AnalysisEngineProcessException {
        if (UimaConvenience.getShortDocumentName(aJCas).equals(targetDocName)) {
            logger.info("Found target document " + UimaConvenience.getShortDocumentName(aJCas));

            lv1Extractor.initWorkspace(aJCas);

            JCas goldView = JCasUtil.getView(aJCas, "GoldStandard", false);

            StringBuilder goldSolution = new StringBuilder();
            int index = 0;
            String sep = "";
            for (EventMention mention : JCasUtil.select(goldView, EventMention.class)) {
                CandidateEventMention candidate = new CandidateEventMention(aJCas, mention.getBegin(),
                        mention.getEnd());
                UimaAnnotationUtils.finishAnnotation(candidate, COMPONENT_ID, 0, aJCas);
                goldSolution.append(sep).append(index++).append(":").append(mention.getEventType());
                sep = ", ";
            }

            List<CandidateEventMention> candidateMentions = new ArrayList<>(JCasUtil.select(aJCas,
                    CandidateEventMention.class));

            logger.info("Number of candidates :  " + candidateMentions.size());

            lv2Extractor.initWorkspace(aJCas);

            DocumentAnnotation document = JCasUtil.selectSingle(aJCas, DocumentAnnotation.class);
            lv2Extractor.resetWorkspace(aJCas, document.getBegin(), document.getEnd());

            for (StanfordCorenlpSentence sentence : JCasUtil.select(aJCas, StanfordCorenlpSentence.class)) {
                lv1Extractor.resetWorkspace(aJCas, sentence.getBegin(), sentence.getEnd());

                boolean worthCheck = false;

                List<StanfordCorenlpToken> tokens = JCasUtil.selectCovered(StanfordCorenlpToken.class, sentence);

                for (int i = 0; i < tokens.size(); i++) {
                    StanfordCorenlpToken token = tokens.get(i);
                    if (token.getCoveredText().equals(targetWord)) {
                        logger.info("Checking features for target token at " + i);
                        inspectTopClasses(i, lv1Extractor, lv1WeightVector);
                        worthCheck = true;
                    }
                }

                if (worthCheck) {
                    // Check the viterbi sentence decoding.
                    sentenceDecoder.decode(lv1Extractor, lv1WeightVector, tokens.size(), dummyLagrangian,
                            dummyLagrangian, true);
                    SequenceSolution prediction = sentenceDecoder.getDecodedPrediction();
                    logger.info("Sentence decoding result is : ");
                    logger.info(prediction.toString());
//                    logger.info(prediction.showBestBackPointerMap());
                }
            }

            // Check the viterbi mention decoding
            mentionDecoder.decode(lv2Extractor, lv2WeightVector, candidateMentions.size(), dummyLagrangian,
                    dummyLagrangian, true);

            logger.info("Mention level output is: ");
            logger.info(mentionDecoder.getDecodedPrediction().toString());

            logger.info("Gold solution is : ");
            logger.info(goldSolution.toString());

            List<Integer> targetIndices = new ArrayList<>();
            for (int i = 0; i < candidateMentions.size(); i++) {
                CandidateEventMention cand = candidateMentions.get(i);
                if (cand.getCoveredText().equals(targetWord)) {
                    targetIndices.add(i);
                }
            }

            for (int targetIndex : targetIndices) {
                logger.info("Extracting target mention at " + targetIndex);
                inspectTopClasses(targetIndex, lv2Extractor, lv2WeightVector);
            }
        }
    }

    private void inspectTopClasses(int sequenceIndex, UimaSequenceFeatureExtractor extractor,
                                   GraphWeightVector weightVector) {
        FeatureVector nodeFeature = new RealValueHashFeatureVector(weightVector.getFeatureAlphabet());
        FeatureVector edgeFeature = new RealValueHashFeatureVector(weightVector.getFeatureAlphabet());

        extractor.extract(sequenceIndex, nodeFeature, edgeFeature);

        logger.info(nodeFeature.readableString());

        PriorityQueue<Pair<Double, Integer>> bestClasses = new PriorityQueue<>();

        weightVector.getClassAlphabet().getNormalClassesRange().forEach(
                classIndex -> {
                    double score = weightVector.dotProdAver(nodeFeature, classIndex);
                    bestClasses.add(Pair.of(-score, classIndex));
                }
        );

        int count = 0;
        while (!bestClasses.isEmpty() && count < 3) {
            Pair<Double, Integer> scoreAndClass = bestClasses.poll();
            double score = scoreAndClass.getKey();
            int classIndex = scoreAndClass.getRight();
            String className = weightVector.getClassAlphabet().getClassName(classIndex);

            logger.info("\n########NEXT TOP CLASS###########\n");
            logger.info(String.format("Top %d class : %s, score is %.4f", count + 1, className, -score));

            AveragedWeightVector weights = weightVector.getNodeWeights(classIndex);

            weights.dotProdAverDebug(nodeFeature, logger);

            count++;
        }
    }

    public static void main(String[] argv) throws UIMAException, IOException, CpeDescriptorException, SAXException {

        Configuration kbpConfig = new Configuration(argv[0]);
        String trainingWorkingDir = kbpConfig.get("edu.cmu.cs.lti.training.working.dir");

        Configuration commonConfig = new Configuration("settings/common.properties");
        String typeSystemName = commonConfig.get("edu.cmu.cs.lti.event.typesystem");

        String targetDocName = argv[1];
        String targetWord = argv[2];

        TypeSystemDescription typeSystemDescription = TypeSystemDescriptionFactory
                .createTypeSystemDescription(typeSystemName);

        new BasicPipeline(new ProcessorWrapper() {
            @Override
            public CollectionReaderDescription getCollectionReader() throws ResourceInitializationException {
                return CustomCollectionReaderFactory.createXmiReader(trainingWorkingDir, "preprocessed");
            }

            @Override
            public AnalysisEngineDescription[] getProcessors() throws ResourceInitializationException {
                AnalysisEngineDescription debugger = AnalysisEngineFactory.createEngineDescription(
                        SingleInstanceDebugger.class, typeSystemDescription,
                        SingleInstanceDebugger.PARAM_CONFIG, kbpConfig.getConfigFile(),
                        SingleInstanceDebugger.PARAM_TARGET_DOC_NAME, targetDocName,
                        SingleInstanceDebugger.PARAM_TARGET_WORD, targetWord);
                return new AnalysisEngineDescription[]{debugger};
            }
        }).run();
    }
}
=======
package edu.cmu.cs.lti.emd.annotators.misc;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import edu.cmu.cs.lti.emd.annotators.train.MentionLevelEventMentionCrfTrainer;
import edu.cmu.cs.lti.emd.annotators.train.TokenLevelEventMentionCrfTrainer;
import edu.cmu.cs.lti.learning.decoding.SequenceDecoder;
import edu.cmu.cs.lti.learning.decoding.ViterbiDecoder;
import edu.cmu.cs.lti.learning.feature.FeatureSpecParser;
import edu.cmu.cs.lti.learning.feature.extractor.MultiSentenceFeatureExtractor;
import edu.cmu.cs.lti.learning.feature.extractor.SentenceFeatureExtractor;
import edu.cmu.cs.lti.learning.feature.extractor.UimaSequenceFeatureExtractor;
import edu.cmu.cs.lti.learning.model.*;
import edu.cmu.cs.lti.learning.utils.CubicLagrangian;
import edu.cmu.cs.lti.learning.utils.DummyCubicLagrangian;
import edu.cmu.cs.lti.pipeline.BasicPipeline;
import edu.cmu.cs.lti.script.type.CandidateEventMention;
import edu.cmu.cs.lti.script.type.EventMention;
import edu.cmu.cs.lti.script.type.StanfordCorenlpSentence;
import edu.cmu.cs.lti.script.type.StanfordCorenlpToken;
import edu.cmu.cs.lti.uima.annotator.AbstractConfigAnnotator;
import edu.cmu.cs.lti.uima.io.reader.CustomCollectionReaderFactory;
import edu.cmu.cs.lti.uima.util.UimaAnnotationUtils;
import edu.cmu.cs.lti.uima.util.UimaConvenience;
import edu.cmu.cs.lti.utils.Configuration;
import org.apache.commons.lang3.SerializationUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.uima.UIMAException;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.collection.CollectionReaderDescription;
import org.apache.uima.collection.metadata.CpeDescriptorException;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.DocumentAnnotation;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.uimafit.factory.TypeSystemDescriptionFactory;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;

/**
 * Created with IntelliJ IDEA.
 * Date: 12/15/15
 * Time: 1:39 PM
 *
 * @author Zhengzhong Liu
 */
public class SingleInstanceDebugger extends AbstractConfigAnnotator {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private UimaSequenceFeatureExtractor lv1Extractor;
    private MultiSentenceFeatureExtractor<CandidateEventMention> lv2Extractor;

    private GraphWeightVector lv1WeightVector;
    private GraphWeightVector lv2WeightVector;

    private File lv1ModelDirectory = new File("../models/mention_type_crf/split_0/");

    private File lv2ModelDirectory = new File("../models/mention_sequence_crf/split_0");

    public static final String PARAM_CONFIG = "configuration";
    @ConfigurationParameter(name = PARAM_CONFIG)
    private Configuration config;

    public static final String PARAM_TARGET_DOC_NAME = "docName";
    @ConfigurationParameter(name = PARAM_TARGET_DOC_NAME)
    private String targetDocName;

    public static final String PARAM_TARGET_WORD = "targetWord";
    @ConfigurationParameter(name = PARAM_TARGET_WORD)
    private String targetWord;

    //    private FeatureAlphabet lv1FeatureAlphabet;
    private ViterbiDecoder sentenceDecoder;
    private SequenceDecoder mentionDecoder;

    CubicLagrangian dummyLagrangian = new DummyCubicLagrangian();

    @Override
    public void initialize(UimaContext aContext) throws ResourceInitializationException {
        super.initialize(aContext);
        logger.info("Loading models ...");

        try {
            lv1WeightVector = SerializationUtils.deserialize(new FileInputStream(new File
                    (lv1ModelDirectory, TokenLevelEventMentionCrfTrainer.MODEL_NAME)));
            lv2WeightVector = SerializationUtils.deserialize(new FileInputStream(new File
                    (lv2ModelDirectory, MentionLevelEventMentionCrfTrainer.MODEL_NAME)));
        } catch (IOException e) {
            throw new ResourceInitializationException(e);
        }

        logger.info("Model loaded");
        try {
            FeatureSpecParser wordFeatureSpecParser = new FeatureSpecParser(
                    config.get("edu.cmu.cs.lti.feature.sentence.package.name"));

            FeatureSpecParser mentionFeatureSpecParser = new FeatureSpecParser(
                    config.get("edu.cmu.cs.lti.feature.document.package.name")
            );

            String docFeatureSpec = config.get("edu.cmu.cs.lti.features.type.lv1.doc.spec");
            String wordFeatureSpec = config.get("edu.cmu.cs.lti.features.type.lv1.sentence.spec");

            Configuration lv1DocFeatureConfig = new FeatureSpecParser(
                    config.get("edu.cmu.cs.lti.feature.document.package.name")
            ).parseFeatureFunctionSpecs(docFeatureSpec);

            Configuration lv1SentFeatureConfig = new FeatureSpecParser(
                    config.get("edu.cmu.cs.lti.feature.sentence.package.name")
            ).parseFeatureFunctionSpecs(wordFeatureSpec);


            Configuration lv2typeFeatureConfig = wordFeatureSpecParser.parseFeatureFunctionSpecs(lv2WeightVector
                    .getFeatureSpec());
            Configuration mentionFeatureConfig = mentionFeatureSpecParser.parseFeatureFunctionSpecs(lv2WeightVector
                    .getFeatureSpec());

            logger.info("Feature specification for lv2 is " + lv2WeightVector.getFeatureSpec());

            lv1Extractor = new SentenceFeatureExtractor(lv1WeightVector.getFeatureAlphabet(), config,
                    lv1SentFeatureConfig, lv1DocFeatureConfig, false);
            lv2Extractor = new MultiSentenceFeatureExtractor<>(lv2WeightVector.getFeatureAlphabet(), config,
                    lv2typeFeatureConfig, mentionFeatureConfig, true, CandidateEventMention.class);
        } catch (ClassNotFoundException | NoSuchMethodException | InvocationTargetException | InstantiationException
                | IllegalAccessException e) {
            e.printStackTrace();
        }

        sentenceDecoder = new ViterbiDecoder(lv1WeightVector.getFeatureAlphabet(), lv1WeightVector.getClassAlphabet());
        mentionDecoder = new ViterbiDecoder(lv2WeightVector.getFeatureAlphabet(), lv2WeightVector.getClassAlphabet());

        logger.info("Target word is " + targetWord);
    }

    @Override
    public void process(JCas aJCas) throws AnalysisEngineProcessException {
        if (UimaConvenience.getShortDocumentName(aJCas).equals(targetDocName)) {
            logger.info("Found target document " + UimaConvenience.getShortDocumentName(aJCas));

            lv1Extractor.initWorkspace(aJCas);

            JCas goldView = JCasUtil.getView(aJCas, "GoldStandard", false);

            StringBuilder goldSolution = new StringBuilder();
            int index = 0;
            String sep = "";
            for (EventMention mention : JCasUtil.select(goldView, EventMention.class)) {
                CandidateEventMention candidate = new CandidateEventMention(aJCas, mention.getBegin(),
                        mention.getEnd());
                UimaAnnotationUtils.finishAnnotation(candidate, COMPONENT_ID, 0, aJCas);
                goldSolution.append(sep).append(index++).append(":").append(mention.getEventType());
                sep = ", ";
            }

            List<CandidateEventMention> candidateMentions = new ArrayList<>(JCasUtil.select(aJCas,
                    CandidateEventMention.class));

            logger.info("Number of candidates :  " + candidateMentions.size());

            lv2Extractor.initWorkspace(aJCas);

            DocumentAnnotation document = JCasUtil.selectSingle(aJCas, DocumentAnnotation.class);
            lv2Extractor.resetWorkspace(aJCas, document.getBegin(), document.getEnd());

            for (StanfordCorenlpSentence sentence : JCasUtil.select(aJCas, StanfordCorenlpSentence.class)) {
                lv1Extractor.resetWorkspace(aJCas, sentence.getBegin(), sentence.getEnd());

                boolean worthCheck = false;

                List<StanfordCorenlpToken> tokens = JCasUtil.selectCovered(StanfordCorenlpToken.class, sentence);

                for (int i = 0; i < tokens.size(); i++) {
                    StanfordCorenlpToken token = tokens.get(i);
                    if (token.getCoveredText().equals(targetWord)) {
                        logger.info("Checking features for target token at " + i);
                        inspectTopClasses(i, lv1Extractor, lv1WeightVector);
                        worthCheck = true;
                    }
                }

                if (worthCheck) {
                    // Check the viterbi sentence decoding.
                    sentenceDecoder.decode(lv1Extractor, lv1WeightVector, tokens.size(), dummyLagrangian,
                            dummyLagrangian, true);
                    SequenceSolution prediction = sentenceDecoder.getDecodedPrediction();
                    logger.info("Sentence decoding result is : ");
                    logger.info(prediction.toString());
//                    logger.info(prediction.showBestBackPointerMap());
                }
            }

            // Check the viterbi mention decoding
            mentionDecoder.decode(lv2Extractor, lv2WeightVector, candidateMentions.size(), dummyLagrangian,
                    dummyLagrangian, true);

            logger.info("Mention level output is: ");
            logger.info(mentionDecoder.getDecodedPrediction().toString());

            logger.info("Gold solution is : ");
            logger.info(goldSolution.toString());

            List<Integer> targetIndices = new ArrayList<>();
            for (int i = 0; i < candidateMentions.size(); i++) {
                CandidateEventMention cand = candidateMentions.get(i);
                if (cand.getCoveredText().equals(targetWord)) {
                    targetIndices.add(i);
                }
            }

            for (int targetIndex : targetIndices) {
                logger.info("Extracting target mention at " + targetIndex);
                inspectTopClasses(targetIndex, lv2Extractor, lv2WeightVector);
            }
        }
    }

    private void inspectTopClasses(int sequenceIndex, UimaSequenceFeatureExtractor extractor,
                                   GraphWeightVector weightVector) {
        FeatureVector nodeFeature = new RealValueHashFeatureVector(weightVector.getFeatureAlphabet());
        Table<Integer, Integer, FeatureVector> edgeFeature = HashBasedTable.create();

        extractor.extract(sequenceIndex, nodeFeature, edgeFeature);

        logger.info(nodeFeature.readableString());

        PriorityQueue<Pair<Double, Integer>> bestClasses = new PriorityQueue<>();

        weightVector.getClassAlphabet().getNormalClassesRange().forEach(
                classIndex -> {
                    double score = weightVector.dotProdAver(nodeFeature, classIndex);
                    bestClasses.add(Pair.of(-score, classIndex));
                }
        );

        int count = 0;
        while (!bestClasses.isEmpty() && count < 3) {
            Pair<Double, Integer> scoreAndClass = bestClasses.poll();
            double score = scoreAndClass.getKey();
            int classIndex = scoreAndClass.getRight();
            String className = weightVector.getClassAlphabet().getClassName(classIndex);

            logger.info("\n########NEXT TOP CLASS###########\n");
            logger.info(String.format("Top %d class : %s, score is %.4f", count + 1, className, -score));

            AveragedWeightVector weights = weightVector.getNodeWeights(classIndex);

            weights.dotProdAverDebug(nodeFeature, logger);

            count++;
        }
    }

    public static void main(String[] argv) throws UIMAException, IOException, CpeDescriptorException, SAXException {

        Configuration kbpConfig = new Configuration(argv[0]);
        String trainingWorkingDir = kbpConfig.get("edu.cmu.cs.lti.training.working.dir");

        Configuration commonConfig = new Configuration("settings/common.properties");
        String typeSystemName = commonConfig.get("edu.cmu.cs.lti.event.typesystem");

        String targetDocName = argv[1];
        String targetWord = argv[2];

        TypeSystemDescription typeSystemDescription = TypeSystemDescriptionFactory
                .createTypeSystemDescription(typeSystemName);

        CollectionReaderDescription reader = CustomCollectionReaderFactory.createXmiReader(trainingWorkingDir,
                "preprocessed");
        AnalysisEngineDescription debugger = AnalysisEngineFactory.createEngineDescription(
                SingleInstanceDebugger.class, typeSystemDescription,
                SingleInstanceDebugger.PARAM_TARGET_DOC_NAME, targetDocName,
                SingleInstanceDebugger.PARAM_TARGET_WORD, targetWord);

        SingleInstanceDebugger.setConfig(kbpConfig);
        new BasicPipeline(reader, debugger).run();
    }
}
>>>>>>> 0b1d50b9fb7eda2a0594d07b1bb39c417e65ceee:cmu-script/event-coref/src/main/java/edu/cmu/cs/lti/emd/annotators/misc/SingleInstanceDebugger.java
