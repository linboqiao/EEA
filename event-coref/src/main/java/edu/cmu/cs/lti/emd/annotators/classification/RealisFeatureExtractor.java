package edu.cmu.cs.lti.emd.annotators.classification;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import edu.cmu.cs.lti.collection_reader.TbfEventDataReader;
import edu.cmu.cs.lti.learning.feature.FeatureSpecParser;
import edu.cmu.cs.lti.learning.feature.extractor.SentenceFeatureExtractor;
import edu.cmu.cs.lti.learning.model.ClassAlphabet;
import edu.cmu.cs.lti.learning.model.FeatureAlphabet;
import edu.cmu.cs.lti.learning.model.FeatureVector;
import edu.cmu.cs.lti.learning.model.RealValueHashFeatureVector;
import edu.cmu.cs.lti.script.type.EventMention;
import edu.cmu.cs.lti.script.type.StanfordCorenlpSentence;
import edu.cmu.cs.lti.script.type.StanfordCorenlpToken;
import edu.cmu.cs.lti.script.type.Word;
import edu.cmu.cs.lti.uima.annotator.AbstractLoggingAnnotator;
import edu.cmu.cs.lti.uima.util.TokenAlignmentHelper;
import edu.cmu.cs.lti.uima.util.UimaConvenience;
import edu.cmu.cs.lti.uima.util.UimaNlpUtils;
import edu.cmu.cs.lti.utils.Configuration;
import gnu.trove.map.TIntDoubleMap;
import gnu.trove.map.hash.TIntDoubleHashMap;
import org.apache.uima.UIMAException;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.collection.CollectionReaderDescription;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.pipeline.SimplePipeline;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.javatuples.Pair;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * Date: 9/11/15
 * Time: 1:09 PM
 *
 * @author Zhengzhong Liu
 */
public class RealisFeatureExtractor extends AbstractLoggingAnnotator {
    private static List<Pair<TIntDoubleMap, String>> features;
    private static FeatureAlphabet alphabet;
    private static ClassAlphabet classAlphabet;

    private static SentenceFeatureExtractor extractor;

    private TokenAlignmentHelper alignmentHelper = new TokenAlignmentHelper();

    private String goldTokenComponentId = TbfEventDataReader.class.getSimpleName();

    private Table<Integer, Integer, FeatureVector> dummy = HashBasedTable.create();

    @Override
    public void initialize(UimaContext aContext) throws ResourceInitializationException {
        super.initialize(aContext);
        logger.info("Preparing to extract realis features.");
    }

    @Override
    public void process(JCas aJCas) throws AnalysisEngineProcessException {
        UimaConvenience.printProcessLog(aJCas, logger);
        logger.info("Extracting feature");

//        logger.info(JCasUtil.select(aJCas, StanfordCorenlpSentence.class).size() + " sentences found.");

        extractor.initWorkspace(aJCas);
        alignmentHelper.loadWord2Stanford(aJCas, goldTokenComponentId);

        boolean isLower = false;

        for (StanfordCorenlpSentence sentence : JCasUtil.select(aJCas, StanfordCorenlpSentence.class)) {
            extractor.resetWorkspace(aJCas, sentence);

            List<EventMention> mentions = JCasUtil.selectCovered(aJCas, EventMention.class, sentence);

//            logger.info("Number of mentions is " + mentions.size());

            for (EventMention mention : mentions) {
                FeatureVector rawFeatures = new RealValueHashFeatureVector(alphabet);

                StanfordCorenlpToken mentionHead = UimaNlpUtils.findHeadFromStanfordAnnotation(mention);
                int head = extractor.getElementIndex(mentionHead);
                extractor.extract(head, rawFeatures, dummy);
                classAlphabet.addClass(mention.getRealisType());
                TIntDoubleMap indexedFeatures = new TIntDoubleHashMap();

                if (mention.getRealisType().equals("actual")){
                    isLower = true;
                }

                if (mentionHead == null || head == -1) {
                    logger.info("Will not be able to find features for mention " + mention.getCoveredText() +
                            ", since the mention head is not found.");
                }

                for (FeatureVector.FeatureIterator iter = rawFeatures.featureIterator(); iter.hasNext(); ) {
                    iter.next();
                    indexedFeatures.put(iter.featureIndex(), iter.featureValue());
//                    logger.info("Feature is " + iter.featureIndex() + " " + iter.featureValue());
                }
                features.add(Pair.with(indexedFeatures, mention.getRealisType()));

            }
        }

        if (isLower){
            logger.info("Realis type is lower");
        }else{
            logger.info("Realis type is upper");
        }
    }

    private StanfordCorenlpToken getHead(JCas aJCas, int begin, int end) {
        StanfordCorenlpToken head = UimaNlpUtils.findHeadFromRange(aJCas, begin, end);
        if (head == null) {
            head = UimaNlpUtils.findFirstToken(aJCas, begin, end);
        }
        if (head == null) {
            Word word = UimaNlpUtils.findFirstWord(aJCas, begin, end, goldTokenComponentId);
            if (word != null) {
                head = alignmentHelper.getStanfordToken(word);
            }
        }
        return head;
    }

    public static void getFeatures(
            CollectionReaderDescription reader, TypeSystemDescription typeSystemDescription,
            List<Pair<TIntDoubleMap, String>> features, FeatureAlphabet alphabet, ClassAlphabet classAlphabet,
            Configuration config) throws UIMAException, IOException, ClassNotFoundException,
            NoSuchMethodException, InstantiationException, IllegalAccessException, InvocationTargetException {
        // Prepare feature functions.
        setupFeatureExtractor(features, alphabet, classAlphabet, config);
        SimplePipeline.runPipeline(reader, AnalysisEngineFactory.createEngineDescription(RealisFeatureExtractor
                .class, typeSystemDescription));
    }

    private static void setupFeatureExtractor(List<Pair<TIntDoubleMap, String>> features, FeatureAlphabet alphabet,
                                              ClassAlphabet classAlphabet, Configuration config) throws
            ClassNotFoundException, NoSuchMethodException, InvocationTargetException, InstantiationException,
            IllegalAccessException {
        RealisFeatureExtractor.features = features;
        RealisFeatureExtractor.alphabet = alphabet;
        RealisFeatureExtractor.classAlphabet = classAlphabet;

        // Parse feature configs from featureConfig.
        FeatureSpecParser parser = new FeatureSpecParser(config.get("edu.cmu.cs.lti.feature.sentence.package.name"));
        Configuration realisSpec = parser.parseFeatureFunctionSpecs(config.get("edu.cmu.cs.lti.features.realis.spec"));

        // Currently no document level realis features.
        Configuration placeHolderSpec = new Configuration();

        extractor = new SentenceFeatureExtractor(alphabet, config, realisSpec, placeHolderSpec, false);
    }
}
