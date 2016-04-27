package edu.cmu.cs.lti.emd.annotators.train;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.SetMultimap;
import edu.cmu.cs.lti.emd.decoding.BeamCrfDecoder;
import edu.cmu.cs.lti.emd.utils.MentionUtils;
import edu.cmu.cs.lti.learning.feature.FeatureSpecParser;
import edu.cmu.cs.lti.learning.feature.extractor.SentenceFeatureExtractor;
import edu.cmu.cs.lti.learning.feature.sequence.FeatureUtils;
import edu.cmu.cs.lti.learning.model.*;
import edu.cmu.cs.lti.learning.update.DiscriminativeUpdater;
import edu.cmu.cs.lti.learning.utils.MentionTypeUtils;
import edu.cmu.cs.lti.script.type.EventMention;
import edu.cmu.cs.lti.script.type.StanfordCorenlpToken;
import edu.cmu.cs.lti.script.type.Word;
import edu.cmu.cs.lti.uima.annotator.AbstractLoggingAnnotator;
import edu.cmu.cs.lti.utils.Configuration;
import org.apache.commons.io.FileUtils;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Created with IntelliJ IDEA.
 * Date: 4/18/16
 * Time: 4:56 PM
 *
 * @author Zhengzhong Liu
 */
public class BeamBasedMentionTypeTrainer extends AbstractLoggingAnnotator {
    private static DiscriminativeUpdater updater;

    public static final String PARAM_CONFIGURATION_FILE = "configPath";
    @ConfigurationParameter(name = PARAM_CONFIGURATION_FILE)
    private Configuration config;

    public static final String PARAM_DELAYED_LASO = "delayedLaso";
    @ConfigurationParameter(name = PARAM_DELAYED_LASO)
    private boolean delayedLaso;

    public static final String PARAM_USE_PA_UPDATE = "usePaUpdate";
    @ConfigurationParameter(name = PARAM_USE_PA_UPDATE)
    private boolean usePaUpdate;

    public static final String PARAM_LOSS_TYPE = "lossType";
    @ConfigurationParameter(name = PARAM_LOSS_TYPE)
    private String lossType;

    private SentenceFeatureExtractor sentExtractor;
    private BeamCrfDecoder decoder;

    @Override
    public void initialize(UimaContext context) throws ResourceInitializationException {
        logger.info("Preparing the Beam based Trainer for mention type detection...");
        super.initialize(context);

        logger.info(String.format("Beam Trainer using PA update : %s, Delayed LaSO : %s, Loss Type : %s",
                usePaUpdate, delayedLaso, lossType));

        updater = new DiscriminativeUpdater(true, false);
        updater.addWeightVector(ModelConstants.TYPE_MODEL_NAME, prepareCrfWeights());

        try {
            this.sentExtractor = initializeCrfExtractor(config);
        } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException | InvocationTargetException
                | InstantiationException e) {
            e.printStackTrace();
        }

        decoder = new BeamCrfDecoder(updater.getWeightVector(ModelConstants.TYPE_MODEL_NAME), sentExtractor, updater,
                usePaUpdate, delayedLaso, lossType);
    }

    private SentenceFeatureExtractor initializeCrfExtractor(Configuration config) throws ClassNotFoundException,
            NoSuchMethodException, InstantiationException, IllegalAccessException, InvocationTargetException {
        String sentFeatureSpec = config.get("edu.cmu.cs.lti.features.type.lv1.sentence.spec");
        String docFeatureSpec = config.get("edu.cmu.cs.lti.features.type.beam.doc.spec");

        Configuration sentFeatureConfig = new FeatureSpecParser(
                config.get("edu.cmu.cs.lti.feature.sentence.package.name")
        ).parseFeatureFunctionSpecs(sentFeatureSpec);

        Configuration docFeatureConfig = new FeatureSpecParser(
                config.get("edu.cmu.cs.lti.feature.document.package.name")
        ).parseFeatureFunctionSpecs(docFeatureSpec);

        return new SentenceFeatureExtractor(updater.getWeightVector(ModelConstants.TYPE_MODEL_NAME)
                .getFeatureAlphabet(), config, sentFeatureConfig, docFeatureConfig, false /**use state feature?**/);
    }

    private GraphWeightVector prepareCrfWeights() throws ResourceInitializationException {
        logger.info("Initializing labeling weights.");

        int alphabetBits = config.getInt("edu.cmu.cs.lti.mention.feature.alphabet_bits", 24);
        boolean readableModel = config.getBoolean("edu.cmu.cs.lti.mention.readableModel", false);
        String sentFeatureSpec = config.get("edu.cmu.cs.lti.features.type.lv1.sentence.spec");
        String docFeatureSpec = config.get("edu.cmu.cs.lti.features.type.beam.doc.spec");
        File classFile = config.getFile("edu.cmu.cs.lti.mention.classes.path");

        String[] classes;
        if (classFile != null) {
            try {
                classes = FileUtils.readLines(classFile).stream().map(l -> l.split("\t"))
                        .filter(p -> p.length >= 1).map(p -> p[0]).toArray(String[]::new);
                logger.info(String.format("Registered %d classes.", classes.length));
            } catch (IOException e) {
                throw new ResourceInitializationException(e);
            }
        } else {
            throw new ResourceInitializationException(new Throwable("No classes provided for training"));
        }

        ClassAlphabet classAlphabet = new ClassAlphabet(classes, false, true);
        HashAlphabet featureAlphabet = new HashAlphabet(alphabetBits, readableModel);

        return new GraphWeightVector(classAlphabet, featureAlphabet,
                FeatureUtils.joinFeatureSpec(sentFeatureSpec, docFeatureSpec));
    }


    @Override
    public void process(JCas aJCas) throws AnalysisEngineProcessException {
        List<StanfordCorenlpToken> allTokens = new ArrayList<>(JCasUtil.select(aJCas, StanfordCorenlpToken.class));
        List<MentionCandidate> systemCandidates = MentionUtils.createCandidatesFromTokens(aJCas, allTokens);
        List<MentionCandidate> goldCandidates = MentionUtils.createCandidatesFromTokens(aJCas, allTokens);

        annotateGoldCandidates(new ArrayList<>(JCasUtil.select(aJCas, EventMention.class)), goldCandidates);

        decoder.decode(aJCas, systemCandidates, goldCandidates, false);
    }

    private void annotateGoldCandidates(List<EventMention> mentions, List<MentionCandidate> goldCandidates) {
        SetMultimap<Word, Integer> head2Mentions = HashMultimap.create();

        for (int i = 0; i < mentions.size(); i++) {
            head2Mentions.put(mentions.get(i).getHeadWord(), i);
        }

        for (int candidateIndex = 0; candidateIndex < goldCandidates.size(); candidateIndex++) {
            MentionCandidate candidate = goldCandidates.get(candidateIndex);
            Word candidateHead = candidate.getHeadWord();
            if (head2Mentions.containsKey(candidateHead)) {

                Set<Integer> correspondingMentions = head2Mentions.get(candidateHead);
                String mentionType = MentionTypeUtils.joinMultipleTypes(correspondingMentions.stream()
                        .map(mentions::get).map(EventMention::getEventType).collect(Collectors.toList()));
                candidate.setMentionType(mentionType);

                for (Integer mentionId : head2Mentions.get(candidateHead)) {
                    EventMention mention = mentions.get(mentionId);
                    candidate.setRealis(mention.getRealisType());
                }
            } else {
                candidate.setMentionType(ClassAlphabet.noneOfTheAboveClass);
                candidate.setRealis(ClassAlphabet.noneOfTheAboveClass);
            }
        }
    }

    public static void saveModels(File modelOutputDirectory) throws IOException {
        edu.cmu.cs.lti.utils.FileUtils.ensureDirectory(modelOutputDirectory);
        updater.getWeightVector(ModelConstants.TYPE_MODEL_NAME).write(
                new File(modelOutputDirectory, ModelConstants.TYPE_MODEL_NAME)
        );
    }

}
