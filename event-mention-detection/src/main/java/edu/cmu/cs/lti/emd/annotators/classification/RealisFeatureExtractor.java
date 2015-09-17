package edu.cmu.cs.lti.emd.annotators.classification;

import edu.cmu.cs.lti.collection_reader.TbfEventDataReader;
import edu.cmu.cs.lti.learning.cache.CrfState;
import edu.cmu.cs.lti.learning.feature.FeatureSpecParser;
import edu.cmu.cs.lti.learning.feature.sentence.extractor.SentenceFeatureExtractor;
import edu.cmu.cs.lti.learning.model.ClassAlphabet;
import edu.cmu.cs.lti.learning.model.FeatureAlphabet;
import edu.cmu.cs.lti.learning.model.FeatureVector;
import edu.cmu.cs.lti.learning.model.RealValueHashFeatureVector;
import edu.cmu.cs.lti.script.type.*;
import edu.cmu.cs.lti.uima.annotator.AbstractLoggingAnnotator;
import edu.cmu.cs.lti.uima.util.TokenAlignmentHelper;
import edu.cmu.cs.lti.uima.util.UimaNlpUtils;
import edu.cmu.cs.lti.utils.Configuration;
import gnu.trove.map.TIntDoubleMap;
import gnu.trove.map.hash.TIntDoubleHashMap;
import org.apache.uima.UIMAException;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.collection.CollectionReaderDescription;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.pipeline.SimplePipeline;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
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

    @Override
    public void process(JCas aJCas) throws AnalysisEngineProcessException {
//        UimaConvenience.printProcessLog(aJCas);
        extractor.initWorkspace(aJCas);
        alignmentHelper.loadWord2Stanford(aJCas, goldTokenComponentId);

        JCas goldView = JCasUtil.getView(aJCas, goldStandardViewName, aJCas);

        String documentKey = JCasUtil.selectSingle(aJCas, Article.class).getArticleName();
        CrfState key = new CrfState();
        key.setDocumentKey(documentKey);

        int sentenceId = 0;

        FeatureVector dummy = new RealValueHashFeatureVector(alphabet);

        for (StanfordCorenlpSentence sentence : JCasUtil.select(aJCas, StanfordCorenlpSentence.class)) {
            extractor.resetWorkspace(aJCas, sentence);
            key.setSequenceId(sentenceId);

            List<EventMention> mentions = JCasUtil.selectCovered(goldView, EventMention.class, sentence.getBegin(),
                    sentence.getEnd());

            for (EventMention mention : mentions) {
//                TObjectDoubleMap<String> rawFeatures = new TObjectDoubleHashMap<>();
                FeatureVector rawFeatures = new RealValueHashFeatureVector(alphabet);
//                logger.info("Extracting from mention " + mention.getCoveredText());
                int head = extractor.getTokenIndex(getHead(aJCas, mention.getBegin(), mention.getEnd()));
                extractor.extract(head, rawFeatures, dummy);
                classAlphabet.addClass(mention.getRealisType());
                TIntDoubleMap indexedFeatures = new TIntDoubleHashMap();
                for (FeatureVector.FeatureIterator iter = rawFeatures.featureIterator(); iter.hasNext(); ) {
                    iter.next();
                    indexedFeatures.put(iter.featureIndex(), iter.featureValue());
                }
                features.add(Pair.with(indexedFeatures, mention.getRealisType()));
            }
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
            Configuration kbpConfig) throws UIMAException, IOException, ClassNotFoundException,
            NoSuchMethodException, InstantiationException, IllegalAccessException, InvocationTargetException {
        // Prepare feature functions.
        setupFeatureExtractor(features, alphabet, classAlphabet, kbpConfig);
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
        FeatureSpecParser parser = new FeatureSpecParser(config.get("edu.cmu.cs.lti.feature.package.name"));
        Configuration realisSpec = parser.parseFeatureFunctionSpecs(config.get("edu.cmu.cs.lti.features.realis.spec"));
        extractor = new SentenceFeatureExtractor(alphabet, config, realisSpec);
    }
}
