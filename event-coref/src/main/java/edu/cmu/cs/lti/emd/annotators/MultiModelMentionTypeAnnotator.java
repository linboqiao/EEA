package edu.cmu.cs.lti.emd.annotators;

import edu.cmu.cs.lti.emd.annotators.train.TokenLevelEventMentionCrfTrainer;
import edu.cmu.cs.lti.exceptions.NotImplementedException;
import edu.cmu.cs.lti.learning.decoding.SequenceDecoder;
import edu.cmu.cs.lti.learning.decoding.ViterbiDecoder;
import edu.cmu.cs.lti.learning.feature.FeatureSpecParser;
import edu.cmu.cs.lti.learning.feature.extractor.SentenceFeatureExtractor;
import edu.cmu.cs.lti.learning.feature.extractor.UimaSequenceFeatureExtractor;
import edu.cmu.cs.lti.learning.feature.sequence.FeatureUtils;
import edu.cmu.cs.lti.learning.model.ClassAlphabet;
import edu.cmu.cs.lti.learning.model.FeatureAlphabet;
import edu.cmu.cs.lti.learning.model.GraphWeightVector;
import edu.cmu.cs.lti.learning.model.SequenceSolution;
import edu.cmu.cs.lti.learning.utils.DummyCubicLagrangian;
import edu.cmu.cs.lti.learning.utils.MentionTypeUtils;
import edu.cmu.cs.lti.learning.utils.ModelUtils;
import edu.cmu.cs.lti.script.type.EventMention;
import edu.cmu.cs.lti.script.type.StanfordCorenlpSentence;
import edu.cmu.cs.lti.script.type.StanfordCorenlpToken;
import edu.cmu.cs.lti.uima.annotator.AbstractLoggingAnnotator;
import edu.cmu.cs.lti.uima.util.UimaAnnotationUtils;
import edu.cmu.cs.lti.uima.util.UimaConvenience;
import edu.cmu.cs.lti.utils.Configuration;
import org.apache.commons.lang3.SerializationUtils;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.javatuples.Triplet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.*;

/**
 * Load multiple models and produce the output
 *
 * @author Zhengzhong Liu
 */
public class MultiModelMentionTypeAnnotator extends AbstractLoggingAnnotator {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    public static final String PARAM_MAIN_MODEL_DIRECTORY = "mainModelDirectory";
    @ConfigurationParameter(name = PARAM_MAIN_MODEL_DIRECTORY)
    private String mainModelDirectory;

    public static final String PARAM_CONFIGS = "configurations";
    @ConfigurationParameter(name = PARAM_CONFIGS)
    private String[] configPaths;

    public static final String PARAM_STRATEGY = "strategy";
    @ConfigurationParameter(name = PARAM_STRATEGY)
    private String strategy;

    // A dummy lagrangian variable.
    private DummyCubicLagrangian lagrangian = new DummyCubicLagrangian();

    private Map<String, Model> models;

    class Model {
        private UimaSequenceFeatureExtractor sentenceExtractor;
        private ClassAlphabet classAlphabet;
        private GraphWeightVector weightVector;
        private SequenceDecoder decoder;
        private String modelName;

        private Model(GraphWeightVector weightVector, Configuration config) throws
                ClassNotFoundException, NoSuchMethodException, InstantiationException, IllegalAccessException,
                InvocationTargetException {
            this.weightVector = weightVector;
            this.classAlphabet = weightVector.getClassAlphabet();

            FeatureAlphabet alphabet = weightVector.getFeatureAlphabet();
            decoder = new ViterbiDecoder(alphabet, classAlphabet);
            String featureSpec = this.weightVector.getFeatureSpec();

            FeatureSpecParser sentFeatureSpecParser = new FeatureSpecParser(
                    config.get("edu.cmu.cs.lti.feature.sentence.package.name"));

            FeatureSpecParser docFeatureSpecParser = new FeatureSpecParser(
                    config.get("edu.cmu.cs.lti.feature.document.package.name")
            );

            String[] savedFeatureSpecs = FeatureUtils.splitFeatureSpec(featureSpec);
            String savedSentFeatureSpec = savedFeatureSpecs[0];
            String savedDocFeatureSpec = (savedFeatureSpecs.length == 2) ? savedFeatureSpecs[1] : "";

            String currentSentFeatureSpec = config.get("edu.cmu.cs.lti.features.type.lv1.sentence.spec");
            String currentDocFeatureSpepc = config.get("edu.cmu.cs.lti.features.type.lv1.doc.spec");

            warning(savedSentFeatureSpec, currentSentFeatureSpec);
            warning(savedDocFeatureSpec, currentDocFeatureSpepc);

            logger.info("Sent feature spec : " + savedSentFeatureSpec);
            logger.info("Doc feature spec : " + savedDocFeatureSpec);

            Configuration sentFeatureConfig = sentFeatureSpecParser.parseFeatureFunctionSpecs(savedSentFeatureSpec);
            Configuration docFeatureConfig = docFeatureSpecParser.parseFeatureFunctionSpecs(savedDocFeatureSpec);

            sentenceExtractor = new SentenceFeatureExtractor(alphabet, config, sentFeatureConfig, docFeatureConfig,
                    false);

            modelName = config.get("edu.cmu.cs.lti.model.name");
        }

        private void init(JCas aJCas) {
            sentenceExtractor.initWorkspace(aJCas);
        }

        private SequenceSolution decode(JCas aJCas, StanfordCorenlpSentence sentence) {
            sentenceExtractor.resetWorkspace(aJCas, sentence.getBegin(), sentence.getEnd());
            List<StanfordCorenlpToken> tokens = JCasUtil.selectCovered(StanfordCorenlpToken.class, sentence);
            decoder.decode(sentenceExtractor, weightVector, tokens.size(), lagrangian, lagrangian, true);
            return decoder.getDecodedPrediction();
        }
    }

    @Override
    public void initialize(UimaContext aContext) throws ResourceInitializationException {
        super.initialize(aContext);
        models = new HashMap<>();

        for (int i = 0; i < configPaths.length; i++) {
            try {
                Configuration config = new Configuration(configPaths[i]);
                String modelDirectory = ModelUtils.getTestModelFile(mainModelDirectory, config);
                Model model = loadModel(new File(modelDirectory), config);
                models.put(model.modelName, model);
            } catch (ClassNotFoundException | NoSuchMethodException | InstantiationException |
                    InvocationTargetException | IllegalAccessException | IOException e) {
                e.printStackTrace();
            }
        }
    }

    private Model loadModel(File modelDirectory, Configuration config) throws FileNotFoundException,
            ClassNotFoundException, NoSuchMethodException, InvocationTargetException, InstantiationException,
            IllegalAccessException {
        logger.info("Loading models ...");
        GraphWeightVector weightVector = SerializationUtils.deserialize(new FileInputStream(new File
                (modelDirectory, TokenLevelEventMentionCrfTrainer.MODEL_NAME)));
        Model model = new Model(weightVector, config);
        logger.info("Loading done...");
        return model;
    }

    private void warning(String savedSpec, String oldSpec) {
        if (!oldSpec.equals(savedSpec)) {
            logger.warn("Current feature specification is not the same with the trained model.");
            logger.warn("Will use the stored specification, it might create unexpected errors");
            logger.warn("Using Spec:" + savedSpec);
        }
    }

    @Override
    public void process(JCas aJCas) throws AnalysisEngineProcessException {
        UimaConvenience.printProcessLog(aJCas, logger, true);

        for (Model model : models.values()) {
            model.init(aJCas);
        }

        for (StanfordCorenlpSentence sentence : JCasUtil.select(aJCas, StanfordCorenlpSentence.class)) {
            Map<String, SequenceSolution> predictions = new HashMap<>();

            List<StanfordCorenlpToken> tokens = JCasUtil.selectCovered(StanfordCorenlpToken.class, sentence);

            for (Map.Entry<String, Model> modelWithName : models.entrySet()) {
                String modelName = modelWithName.getKey();
                Model model = modelWithName.getValue();
                SequenceSolution prediction = model.decode(aJCas, sentence);
                predictions.put(modelName, prediction);
            }

            String[] mergedTags = mergePredictions(predictions, tokens.size());

            List<Triplet<Integer, Integer, String>> mentionChunks = convertTypeTagsToChunks(mergedTags);

            for (Triplet<Integer, Integer, String> chunk : mentionChunks) {
                StanfordCorenlpToken firstToken = tokens.get(chunk.getValue0());
                StanfordCorenlpToken lastToken = tokens.get(chunk.getValue1());

                EventMention predictedMention = new EventMention(aJCas);
                predictedMention.setEventType(chunk.getValue2());
                UimaAnnotationUtils.finishAnnotation(predictedMention, firstToken.getBegin(), lastToken
                        .getEnd(), COMPONENT_ID, 0, aJCas);
            }
        }
    }

    private String[] mergePredictions(Map<String, SequenceSolution> predictions, int predictionLength) {
        if (strategy.equals("union")) {
            return unionMerge(predictions, predictionLength);
        } else {
            throw new NotImplementedException(String.format("Merge strategy %s is not implemented.", strategy));
        }
    }

    private String[] unionMerge(Map<String, SequenceSolution> predictions, int predictionLength) {
        String[] mergedResult = new String[predictionLength];

        Set<String>[] typeCollects = new Set[predictionLength];

        for (Map.Entry<String, SequenceSolution> nameAndSolution : predictions.entrySet()) {
            SequenceSolution solution = nameAndSolution.getValue();
            String modelName = nameAndSolution.getKey();

            for (int i = 0; i < solution.getSequenceLength(); i++) {
                int c = solution.getClassAt(i);

                String tag = models.get(modelName).classAlphabet.getClassName(c);

                if (!tag.equals(ClassAlphabet.noneOfTheAboveClass)) {
                    if (typeCollects[i] == null) {
                        typeCollects[i] = new HashSet<>();
                    }
                    typeCollects[i].add(tag);
                }
            }
        }

        for (int i = 0; i < typeCollects.length; i++) {
            Set<String> collectedType = typeCollects[i];
            if (collectedType == null) {
                mergedResult[i] = ClassAlphabet.noneOfTheAboveClass;
            } else {
                mergedResult[i] = unionTypes(collectedType);

//                logger.info("Original types");
//                for (String type : collectedType) {
//                    logger.info(type);
//                }
//                logger.info("Merged types");
//                logger.info(mergedResult[i]);
//                DebugUtils.pause();
            }
        }
        return mergedResult;
    }

    private String unionTypes(Set<String> types) {
        // Turn it into separated types and then join to ensure.
        Set<String> splitTypes = new HashSet<>();

        for (String type : types) {
            for (String s : MentionTypeUtils.splitToMultipleTypes(type)) {
                splitTypes.add(MentionTypeUtils.canonicalize(s));
            }
        }
        return MentionTypeUtils.joinMultipleTypes(splitTypes);
    }

    private List<Triplet<Integer, Integer, String>> convertTypeTagsToChunks(String[] tags) {
        List<Triplet<Integer, Integer, String>> chunkEndPoints = new ArrayList<>();

        for (int i = 0; i < tags.length; i++) {
            String tag = tags[i];
            if (!tag.equals(ClassAlphabet.noneOfTheAboveClass)) {
                if (chunkEndPoints.size() > 0) {
                    Triplet<Integer, Integer, String> lastChunk = chunkEndPoints.get(chunkEndPoints.size() - 1);
                    if (lastChunk.getValue1() == i - 1) {
                        if (lastChunk.getValue2().equals(tag)) {
                            // Update endpoint.
                            lastChunk.setAt1(i);
                            continue;
                        }
                    }
                }
                // If not adjacent to previous chunks.
                chunkEndPoints.add(Triplet.with(i, i, tag));
            }
        }
        return chunkEndPoints;
    }
}
