package edu.cmu.cs.lti.emd.annotators.classification;

import edu.cmu.cs.lti.learning.cache.CrfState;
import edu.cmu.cs.lti.learning.feature.FeatureSpecParser;
import edu.cmu.cs.lti.learning.feature.sentence.extractor.SentenceFeatureExtractor;
import edu.cmu.cs.lti.learning.model.ClassAlphabet;
import edu.cmu.cs.lti.learning.model.FeatureAlphabet;
import edu.cmu.cs.lti.script.type.Article;
import edu.cmu.cs.lti.script.type.EventMention;
import edu.cmu.cs.lti.script.type.StanfordCorenlpSentence;
import edu.cmu.cs.lti.uima.annotator.AbstractLoggingAnnotator;
import edu.cmu.cs.lti.uima.util.UimaNlpUtils;
import edu.cmu.cs.lti.utils.Configuration;
import gnu.trove.iterator.TObjectDoubleIterator;
import gnu.trove.map.TIntDoubleMap;
import gnu.trove.map.TObjectDoubleMap;
import gnu.trove.map.hash.TIntDoubleHashMap;
import gnu.trove.map.hash.TObjectDoubleHashMap;
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

    @Override
    public void process(JCas aJCas) throws AnalysisEngineProcessException {
        extractor.initWorkspace(aJCas);

        JCas goldView = JCasUtil.getView(aJCas, goldStandardViewName, aJCas);

        String documentKey = JCasUtil.selectSingle(aJCas, Article.class).getArticleName();
        CrfState key = new CrfState();
        key.setDocumentKey(documentKey);

        int sentenceId = 0;

        TObjectDoubleMap<String> dummy = new TObjectDoubleHashMap<>();

        for (StanfordCorenlpSentence sentence : JCasUtil.select(aJCas, StanfordCorenlpSentence.class)) {
            extractor.resetWorkspace(aJCas, sentence);
            key.setSequenceId(sentenceId);

            List<EventMention> mentions = JCasUtil.selectCovered(goldView, EventMention.class, sentence.getBegin(),
                    sentence.getEnd());

            for (EventMention mention : mentions) {
                TObjectDoubleMap<String> rawFeatures = new TObjectDoubleHashMap<>();
                int head = extractor.getTokenIndex(UimaNlpUtils.findHeadFromRange(aJCas, mention
                        .getBegin(), mention.getEnd()));
                extractor.extract(head, rawFeatures, dummy);
                classAlphabet.addClass(mention.getRealisType());
                TIntDoubleMap indexedFeatures = new TIntDoubleHashMap();
                for (TObjectDoubleIterator<String> iter = rawFeatures.iterator(); iter.hasNext(); ) {
                    iter.advance();
                    String featureName = iter.key();
                    double featureValue = iter.value();
                    indexedFeatures.put(alphabet.getFeatureId(featureName), featureValue);
                }
                features.add(Pair.with(indexedFeatures, mention.getRealisType()));
            }
        }
    }

    public static void getFeatures(
            CollectionReaderDescription reader,
            TypeSystemDescription typeSystemDescription,
            List<Pair<TIntDoubleMap, String>> features,
            FeatureAlphabet alphabet,
            ClassAlphabet classAlphabet,
            Configuration kbpConfig) throws UIMAException, IOException, ClassNotFoundException,
            NoSuchMethodException, InstantiationException, IllegalAccessException, InvocationTargetException {
        RealisFeatureExtractor.features = features;
        RealisFeatureExtractor.alphabet = alphabet;
        RealisFeatureExtractor.classAlphabet = classAlphabet;

        // Prepare feature functions.
        setupFeatureExtractor(alphabet, kbpConfig);
        SimplePipeline.runPipeline(reader, AnalysisEngineFactory.createEngineDescription(RealisFeatureExtractor
                .class, typeSystemDescription));
    }

    private static void setupFeatureExtractor(FeatureAlphabet alphabet, Configuration config) throws
            ClassNotFoundException, NoSuchMethodException, InvocationTargetException, InstantiationException,
            IllegalAccessException {
        // Parse feature configs from config.
        FeatureSpecParser parser = new FeatureSpecParser(config.get("edu.cmu.cs.lti.feature.package.name"));
        Configuration realisSpec = parser.parseFeatureFunctionSpecs(config.get("edu.cmu.cs.lti.features.realis.spec"));

        realisSpec.getAllEntries().forEach(a -> {
            System.out.println(a.getKey()+"="+a.getValue());
        });

        extractor = new SentenceFeatureExtractor(alphabet, realisSpec);
    }

    public static void main(String[] args) throws IOException, ClassNotFoundException, NoSuchMethodException,
            InstantiationException, IllegalAccessException, InvocationTargetException {
        setupFeatureExtractor(null, new Configuration
                ("/Users/zhengzhongliu/Documents/projects/cmu-script/settings/kbp.properties"));
    }
}
