package edu.cmu.cs.lti.emd.annotators.crf;

import edu.cmu.cs.lti.learning.decoding.ViterbiDecoder;
import edu.cmu.cs.lti.learning.feature.FeatureSpecParser;
import edu.cmu.cs.lti.learning.feature.extractor.SentenceFeatureExtractor;
import edu.cmu.cs.lti.learning.feature.extractor.UimaSequenceFeatureExtractor;
import edu.cmu.cs.lti.learning.feature.sequence.FeatureUtils;
import edu.cmu.cs.lti.learning.model.ClassAlphabet;
import edu.cmu.cs.lti.learning.model.FeatureAlphabet;
import edu.cmu.cs.lti.learning.model.GraphWeightVector;
import edu.cmu.cs.lti.learning.model.SequenceSolution;
import edu.cmu.cs.lti.learning.training.SequenceDecoder;
import edu.cmu.cs.lti.learning.utils.DummyCubicLagrangian;
import edu.cmu.cs.lti.script.type.EventMention;
import edu.cmu.cs.lti.script.type.StanfordCorenlpSentence;
import edu.cmu.cs.lti.script.type.StanfordCorenlpToken;
import edu.cmu.cs.lti.uima.annotator.AbstractLoggingAnnotator;
import edu.cmu.cs.lti.uima.util.UimaAnnotationUtils;
import edu.cmu.cs.lti.uima.util.UimaConvenience;
import edu.cmu.cs.lti.utils.Configuration;
import edu.cmu.cs.lti.utils.DebugUtils;
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
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * Date: 8/31/15
 * Time: 4:03 PM
 *
 * @author Zhengzhong Liu
 */
public class CrfMentionTypeAnnotator extends AbstractLoggingAnnotator {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private UimaSequenceFeatureExtractor sentenceExtractor;
    private ClassAlphabet classAlphabet;
    private GraphWeightVector weightVector;
    private static SequenceDecoder decoder;

    public static final String PARAM_MODEL_DIRECTORY = "modelDirectory";
    @ConfigurationParameter(name = PARAM_MODEL_DIRECTORY)
    private File modelDirectory;

    public static final String PARAM_CONFIG = "configuration";
    @ConfigurationParameter(name = PARAM_CONFIG)
    private Configuration config;

    // A dummy lagrangian variable.
    private DummyCubicLagrangian lagrangian = new DummyCubicLagrangian();

    @Override
    public void initialize(UimaContext aContext) throws ResourceInitializationException {
        super.initialize(aContext);
        logger.info("Loading models ...");

        String featureSpec;
        FeatureAlphabet alphabet;
        try {
            weightVector = SerializationUtils.deserialize(new FileInputStream(new File
                    (modelDirectory, MentionTypeCrfTrainer.MODEL_NAME)));
            alphabet = weightVector.getFeatureAlphabet();
            classAlphabet = weightVector.getClassAlphabet();
            featureSpec = weightVector.getFeatureSpec();
        } catch (IOException e) {
            throw new ResourceInitializationException(e);
        }

        logger.info("Model loaded");
        try {
            FeatureSpecParser sentFeatureSpecParser = new FeatureSpecParser(
                    config.get("edu.cmu.cs.lti.feature.sentence.package.name"));

            FeatureSpecParser docFeatureSpecParser = new FeatureSpecParser(
                    config.get("edu.cmu.cs.lti.feature.document.package.name")
            );

            logger.debug(featureSpec);

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
        } catch (ClassNotFoundException | NoSuchMethodException | InvocationTargetException | InstantiationException
                | IllegalAccessException e) {
            e.printStackTrace();
        }
        decoder = new ViterbiDecoder(alphabet, classAlphabet);
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
        sentenceExtractor.initWorkspace(aJCas);

//        logger.info(UimaConvenience.getShortDocumentName(aJCas));

        for (StanfordCorenlpSentence sentence : JCasUtil.select(aJCas, StanfordCorenlpSentence.class)) {
            sentenceExtractor.resetWorkspace(aJCas, sentence.getBegin(), sentence.getEnd());

            logger.debug(sentence.getCoveredText());

            List<StanfordCorenlpToken> tokens = JCasUtil.selectCovered(StanfordCorenlpToken.class, sentence);

            // Extract features for this sentence and send in.
            decoder.decode(sentenceExtractor, weightVector, tokens.size(), lagrangian, lagrangian, true);

            SequenceSolution prediction = decoder.getDecodedPrediction();

            logger.debug(prediction.toString());

            List<Triplet<Integer, Integer, String>> mentionChunks = convertTypeTagsToChunks(prediction);

            for (Triplet<Integer, Integer, String> chunk : mentionChunks) {
                StanfordCorenlpToken firstToken = tokens.get(chunk.getValue0());
                StanfordCorenlpToken lastToken = tokens.get(chunk.getValue1());

                EventMention predictedMention = new EventMention(aJCas);
                predictedMention.setEventType(chunk.getValue2());
                UimaAnnotationUtils.finishAnnotation(predictedMention, firstToken.getBegin(), lastToken
                        .getEnd(), COMPONENT_ID, 0, aJCas);
            }
        }

        DebugUtils.pause(logger);
    }

    private List<Triplet<Integer, Integer, String>> convertTypeTagsToChunks(SequenceSolution solution) {
        List<Triplet<Integer, Integer, String>> chunkEndPoints = new ArrayList<>();

        for (int i = 0; i < solution.getSequenceLength(); i++) {
            int tag = solution.getClassAt(i);
            if (tag != classAlphabet.getNoneOfTheAboveClassIndex()) {
                String className = classAlphabet.getClassName(tag);
                if (chunkEndPoints.size() > 0) {
                    Triplet<Integer, Integer, String> lastChunk = chunkEndPoints.get(chunkEndPoints.size() - 1);
                    if (lastChunk.getValue1() == i - 1) {
                        if (lastChunk.getValue2().equals(className)) {
                            // Update endpoint.
                            lastChunk.setAt1(i);
                            continue;
                        }
                    }
                }
                // If not adjacent to previous chunks.
                chunkEndPoints.add(Triplet.with(i, i, className));
            }
        }
        return chunkEndPoints;
    }
}
