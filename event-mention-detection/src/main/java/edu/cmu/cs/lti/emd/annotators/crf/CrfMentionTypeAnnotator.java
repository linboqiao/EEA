package edu.cmu.cs.lti.emd.annotators.crf;

import com.google.common.io.Files;
import edu.cmu.cs.lti.emd.annotators.EventMentionTypeClassPrinter;
import edu.cmu.cs.lti.learning.decoding.ViterbiDecoder;
import edu.cmu.cs.lti.learning.feature.FeatureSpecParser;
import edu.cmu.cs.lti.learning.feature.sentence.extractor.SentenceFeatureExtractor;
import edu.cmu.cs.lti.learning.feature.sentence.extractor.UimaSequenceFeatureExtractor;
import edu.cmu.cs.lti.learning.model.ClassAlphabet;
import edu.cmu.cs.lti.learning.model.FeatureAlphabet;
import edu.cmu.cs.lti.learning.model.GraphWeightVector;
import edu.cmu.cs.lti.learning.model.SequenceSolution;
import edu.cmu.cs.lti.learning.training.SequenceDecoder;
import edu.cmu.cs.lti.script.type.CandidateEventMention;
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
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.Charset;
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
    File modelDirectory;

    public static final String PARAM_VERBOSE = "verbose";
    @ConfigurationParameter(name = PARAM_VERBOSE, defaultValue = "false")
    boolean verbose;

    public static Configuration config;

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
            featureSpec = Files.readFirstLine(new File(modelDirectory, MentionTypeCrfTrainer.FEATURE_SPEC_FILE),
                    Charset.defaultCharset());
        } catch (IOException e) {
            throw new ResourceInitializationException(e);
        }

        logger.info("Model loaded");
        try {
            FeatureSpecParser specParser = new FeatureSpecParser(config.get("edu.cmu.cs.lti.feature.sentence.package" +
                    ".name"));

            String currentFeatureSpec = config.get("edu.cmu.cs.lti.features.type.lv1.spec");

            if (!currentFeatureSpec.equals(featureSpec)) {
                logger.warn("Current feature specification is not the same with the trained model.");
                logger.warn("Will use the stored specification, it might create unexpected errors");
                logger.warn("Using Spec:" + featureSpec);
            }
            Configuration typeFeatureConfig = specParser.parseFeatureFunctionSpecs(featureSpec);

            sentenceExtractor = new SentenceFeatureExtractor(alphabet, config, typeFeatureConfig);
        } catch (ClassNotFoundException | NoSuchMethodException | InvocationTargetException | InstantiationException
                | IllegalAccessException e) {
            e.printStackTrace();
        }
        decoder = new ViterbiDecoder(alphabet, classAlphabet, null /**No caching here**/);
    }

    @Override
    public void process(JCas aJCas) throws AnalysisEngineProcessException {
        UimaConvenience.printProcessLog(aJCas, logger);
        sentenceExtractor.initWorkspace(aJCas);

        for (StanfordCorenlpSentence sentence : JCasUtil.select(aJCas, StanfordCorenlpSentence.class)) {

            sentenceExtractor.resetWorkspace(aJCas, sentence.getBegin(), sentence.getEnd());

            List<StanfordCorenlpToken> tokens = JCasUtil.selectCovered(StanfordCorenlpToken.class, sentence);
            decoder.decode(sentenceExtractor, weightVector, tokens.size(), 0, true);

            SequenceSolution solution = decoder.getDecodedPrediction();

            if (verbose) {
                logger.info(sentence.getCoveredText());
                logger.info(solution.toString());
            }

            for (Triplet<Integer, Integer, String> chunk : convertTypeTagsToChunks(solution)) {
                StanfordCorenlpToken firstToken = tokens.get(chunk.getValue0());
                StanfordCorenlpToken lastToken = tokens.get(chunk.getValue1());
                String[] predictedTypes = EventMentionTypeClassPrinter.splitToTmultipleTypes(chunk.getValue2());

                for (String t : predictedTypes) {
                    CandidateEventMention candidateEventMention = new CandidateEventMention(aJCas);
                    candidateEventMention.setPredictedType(t);
                    UimaAnnotationUtils.finishAnnotation(candidateEventMention, firstToken.getBegin(), lastToken
                            .getEnd(), COMPONENT_ID, 0, aJCas);
                    if (verbose) {
                        logger.info(String.format("%s : [%d, %d]",
                                candidateEventMention.getPredictedType(),
                                candidateEventMention.getBegin(),
                                candidateEventMention.getEnd()));
                    }
                }
            }
        }
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
