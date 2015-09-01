package edu.cmu.cs.lti.emd.pipeline;

import edu.cmu.cs.lti.learning.decoding.ViterbiDecoder;
import edu.cmu.cs.lti.learning.model.*;
import edu.cmu.cs.lti.learning.training.AveragePerceptronTrainer;
import edu.cmu.cs.lti.script.type.EventMention;
import edu.cmu.cs.lti.script.type.StanfordCorenlpSentence;
import edu.cmu.cs.lti.script.type.StanfordCorenlpToken;
import edu.cmu.cs.lti.uima.annotator.AbstractLoggingAnnotator;
import edu.cmu.cs.lti.uima.pipeline.LoopPipeline;
import edu.cmu.cs.lti.uima.util.UimaConvenience;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.collection.CollectionReaderDescription;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * Date: 8/23/15
 * Time: 3:05 PM
 *
 * @author Zhengzhong Liu
 */
public class CrfMentionTrainingRunner extends LoopPipeline {
    protected CrfMentionTrainingRunner(CollectionReaderDescription readerDescription, AnalysisEngineDescription...
            descs) throws ResourceInitializationException {
        super(readerDescription, descs);
    }

    @Override
    protected boolean checkStopCriteria() {
        return false;
    }

    @Override
    protected void stopActions() {

    }


    public class MentionTypeCrfTrainer extends AbstractLoggingAnnotator {
        private static AveragePerceptronTrainer trainer;

        private static UimaSentenceFeatureExtractor sentenceExtractor;

        private static ClassAlphabet classAlphabet;

        private static Alphabet alphabet;

        public static final String MODEL_NAME = "crfWeights";

        public static final String CLASS_ALPHABET_NAME = "classAlphabet";

        public static final String ALPHABET_NAME = "alphabet";

        @Override
        public void process(JCas aJCas) throws AnalysisEngineProcessException {
            UimaConvenience.printProcessLog(aJCas, logger);

            for (StanfordCorenlpSentence sentence : JCasUtil.select(aJCas, StanfordCorenlpSentence.class)) {
                System.out.println(sentence.getCoveredText());

                sentenceExtractor.resetWorkspace(sentence);

                Map<StanfordCorenlpToken, String> tokenTypes = new HashMap<>();

                for (EventMention mention : JCasUtil.selectCovered(EventMention.class, sentence)) {
                    for (StanfordCorenlpToken token : JCasUtil.selectCovered(StanfordCorenlpToken.class, mention)) {
                        tokenTypes.put(token, mention.getEventType());
                    }
                }

                List<StanfordCorenlpToken> sentenceTokens = JCasUtil.selectCovered(StanfordCorenlpToken.class,
                        sentence);

                int sequenceLength = sentenceTokens.size();

                int[] sequence = new int[sequenceLength];

                HashedFeatureVector goldFv = new RealValueFeatureVector(alphabet);

                int seqIndex = 0;
                for (StanfordCorenlpToken token : sentenceTokens) {
                    if (tokenTypes.containsKey(token)) {
                        sequence[seqIndex] = classAlphabet.getClassIndex(tokenTypes.get(token));
                    } else {
                        sequence[seqIndex] = classAlphabet.getNoneOfTheAboveClassIndex();
                    }
                    sentenceExtractor.extract(seqIndex, sequence[seqIndex - 1]);
                    seqIndex++;
                }

                Solution goldSolution = new SequenceSolution(classAlphabet, sequence);

                trainer.trainNext(goldSolution, goldFv, sentenceExtractor, sequenceLength);
            }
        }

        private void saveModels(File outputDirectory, AveragePerceptronTrainer trainer, ClassAlphabet classAlphabet,
                                Alphabet alphabet) throws FileNotFoundException {
            trainer.write(new File(outputDirectory, MODEL_NAME));
            classAlphabet.write(new File(outputDirectory, CLASS_ALPHABET_NAME));
            alphabet.write(new File(outputDirectory, ALPHABET_NAME));
        }

        public void run() {
            int featureDimension = 1000000;
            double stepSize = 0.01;

            classAlphabet = new ClassAlphabet(true);
            alphabet = new Alphabet(featureDimension);

            trainer = new AveragePerceptronTrainer(new ViterbiDecoder(alphabet, classAlphabet, false), stepSize,
                    featureDimension);

            List<SentenceFeatureWithFocus> featureFunctions = new ArrayList<>();
            featureFunctions.add(new WordFeatures());

            sentenceExtractor = new UimaSentenceFeatureExtractor(new Alphabet(featureDimension)) {
                List<StanfordCorenlpToken> sentenceTokens;

                public void init(JCas context) {
                    super.init(context);
                    sentenceTokens = JCasUtil.selectCovered(StanfordCorenlpToken.class, sentence);
                }

                @Override
                public HashedFeatureVector extract(int focus, int previousStateValue) {
                    HashedFeatureVector featureVector = new RealValueFeatureVector(alphabet);
                    featureFunctions.forEach(ff -> {
                        ff.extract(featureVector, sentenceTokens, focus, previousStateValue);
                    });
                    return featureVector;
                }
            };
        }
    }
}
