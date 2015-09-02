package edu.cmu.cs.lti.emd.annotators.crf;

import edu.cmu.cs.lti.emd.learn.feature.extractor.MentionTypeFeatureExtractor;
import edu.cmu.cs.lti.emd.learn.feature.extractor.UimaSentenceFeatureExtractor;
import edu.cmu.cs.lti.learning.decoding.ViterbiDecoder;
import edu.cmu.cs.lti.learning.model.Alphabet;
import edu.cmu.cs.lti.learning.model.AveragedWeightVector;
import edu.cmu.cs.lti.learning.model.ClassAlphabet;
import edu.cmu.cs.lti.learning.model.SequenceSolution;
import edu.cmu.cs.lti.learning.training.SequenceDecoder;
import edu.cmu.cs.lti.script.type.CandidateEventMention;
import edu.cmu.cs.lti.script.type.StanfordCorenlpSentence;
import edu.cmu.cs.lti.script.type.StanfordCorenlpToken;
import edu.cmu.cs.lti.uima.annotator.AbstractLoggingAnnotator;
import edu.cmu.cs.lti.uima.util.UimaAnnotationUtils;
import edu.cmu.cs.lti.uima.util.UimaConvenience;
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

    private UimaSentenceFeatureExtractor sentenceExtractor;
    private Alphabet alphabet;
    private ClassAlphabet classAlphabet;
    private AveragedWeightVector averagedWeightVector;
    private static SequenceDecoder decoder;

    public static final String PARAM_MODEL_DIRECTORY = "modelDirectory";

    @ConfigurationParameter(name = PARAM_MODEL_DIRECTORY)
    File modelDirectory;

    @Override
    public void initialize(UimaContext aContext) throws ResourceInitializationException {
        super.initialize(aContext);
        logger.info("Loading models ...");

        try {
            classAlphabet = SerializationUtils.deserialize(new FileInputStream(new File(modelDirectory,
                    MentionTypeCrfTrainer.CLASS_ALPHABET_NAME)));
            alphabet = SerializationUtils.deserialize(new FileInputStream(new File(modelDirectory,
                    MentionTypeCrfTrainer.ALPHABET_NAME)));
            averagedWeightVector = SerializationUtils.deserialize(new FileInputStream(new File
                    (modelDirectory, MentionTypeCrfTrainer.MODEL_NAME)));
        } catch (FileNotFoundException e) {
            throw new ResourceInitializationException(e);
        }

        logger.info("Model loaded");
        sentenceExtractor = new MentionTypeFeatureExtractor(alphabet);
        decoder = new ViterbiDecoder(alphabet, classAlphabet, null /**No caching here**/);
    }

    @Override
    public void process(JCas aJCas) throws AnalysisEngineProcessException {
        UimaConvenience.printProcessLog(aJCas, logger);
        sentenceExtractor.init(aJCas);

        for (StanfordCorenlpSentence sentence : JCasUtil.select(aJCas, StanfordCorenlpSentence.class)) {
            sentenceExtractor.resetWorkspace(sentence);

            List<StanfordCorenlpToken> tokens = JCasUtil.selectCovered(StanfordCorenlpToken.class, sentence);
            decoder.decode(sentenceExtractor, averagedWeightVector, tokens.size(), 0);

            SequenceSolution solution = decoder.getDecodedPrediction();

            for (Triplet<Integer, Integer, String> chunk : convertIOtagsToChunks(solution)) {
                CandidateEventMention candidateEventMention = new CandidateEventMention(aJCas);
                StanfordCorenlpToken firstToken = tokens.get(chunk.getValue0());
                StanfordCorenlpToken lastToken = tokens.get(chunk.getValue1());
                candidateEventMention.setPredictedType(chunk.getValue2());
                UimaAnnotationUtils.finishAnnotation(candidateEventMention, firstToken.getBegin(), lastToken.getEnd()
                        , COMPONENT_ID, 0, aJCas);
            }
        }
    }

    private List<Triplet<Integer, Integer, String>> convertIOtagsToChunks(SequenceSolution solution) {
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
