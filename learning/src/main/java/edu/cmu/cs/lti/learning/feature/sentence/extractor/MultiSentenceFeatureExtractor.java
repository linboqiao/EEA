package edu.cmu.cs.lti.learning.feature.sentence.extractor;

import edu.cmu.cs.lti.learning.feature.FeatureSpecParser;
import edu.cmu.cs.lti.learning.feature.sentence.functions.SequenceFeatureWithFocus;
import edu.cmu.cs.lti.learning.model.FeatureAlphabet;
import edu.cmu.cs.lti.learning.model.FeatureVector;
import edu.cmu.cs.lti.script.type.StanfordCorenlpSentence;
import edu.cmu.cs.lti.script.type.StanfordCorenlpToken;
import edu.cmu.cs.lti.script.type.Word;
import edu.cmu.cs.lti.uima.util.UimaNlpUtils;
import edu.cmu.cs.lti.utils.Configuration;
import gnu.trove.iterator.TObjectDoubleIterator;
import gnu.trove.map.TObjectDoubleMap;
import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.hash.TObjectDoubleHashMap;
import gnu.trove.map.hash.TObjectIntHashMap;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * While modeling mention features above the sentence level, we need to deal with the offset a little different.
 *
 * @author Zhengzhong Liu
 */
public class MultiSentenceFeatureExtractor<T extends Annotation> extends UimaSequenceFeatureExtractor {
    protected final Logger logger = LoggerFactory.getLogger(getClass());

    protected List<SequenceFeatureWithFocus> featureFunctions = new ArrayList<>();

    private Map<T, List<StanfordCorenlpToken>> mention2SentenceTokens;

    private TObjectIntMap<T> headWordOffsets;

    private List<T> mentions;

    private Class<T> clazz;

    public MultiSentenceFeatureExtractor(
            FeatureAlphabet alphabet, Configuration generalConfig, Configuration featureConfig, Class<T> clazz
    ) throws ClassNotFoundException, NoSuchMethodException, IllegalAccessException, InvocationTargetException,
            InstantiationException {
        super(alphabet);
        String featureFunctionPackage = featureConfig.get(FeatureSpecParser.FEATURE_FUNCTION_PACKAGE_KEY);
        for (String featureFunctionName : featureConfig.getList(FeatureSpecParser.FEATURE_FUNCTION_NAME_KEY)) {
            String ffClassName = featureFunctionPackage + "." + featureFunctionName;
            Class<?> featureFunctionType = Class.forName(ffClassName);
            Constructor<?> constructor = featureFunctionType.getConstructor(Configuration.class, Configuration.class);
            featureFunctions.add((SequenceFeatureWithFocus) constructor.newInstance(generalConfig, featureConfig));
        }
        this.clazz = clazz;
    }

    @Override
    public void initWorkspace(JCas context) {
        super.initWorkspace(context);

        for (SequenceFeatureWithFocus ff : featureFunctions) {
            ff.initDocumentWorkspace(context);
        }

        mention2SentenceTokens = new HashMap<>();
        mentions = new ArrayList<>();
        headWordOffsets = new TObjectIntHashMap<>();

        for (StanfordCorenlpSentence sentence : JCasUtil.select(context, StanfordCorenlpSentence.class)) {
            List<StanfordCorenlpToken> sentenceTokens = JCasUtil.selectCovered(StanfordCorenlpToken.class, sentence);
            for (T mention : JCasUtil.selectCovered(clazz, sentence)) {
                int tokenIndex = 0;
                Word headWord = UimaNlpUtils.findHeadFromAnnotation(mention);
                for (StanfordCorenlpToken token : sentenceTokens) {
                    if (headWord.equals(token)) {
                        headWordOffsets.put(mention, tokenIndex);
                    }
                    tokenIndex++;
                }
                mention2SentenceTokens.put(mention, sentenceTokens);
            }
        }
        mentions = new ArrayList<>(JCasUtil.select(context, clazz));

//        logger.info(String.format("Initialized the document level extractor with %d %ss", mentions.size(),
//                clazz.getSimpleName()));
    }

    @Override
    public void resetWorkspace(JCas aJCas, int begin, int end) {
        // nothing to do
        for (SequenceFeatureWithFocus ff : featureFunctions) {
            ff.resetWorkspace(aJCas, begin, end);
        }
    }

    @Override
    public void extract(int focus, FeatureVector featuresNoState, FeatureVector featuresNeedForState) {
        // TODO should change the extractor to work on mention sequence.

        TObjectDoubleMap<String> rawFeaturesNoState = new TObjectDoubleHashMap<>();
        TObjectDoubleMap<String> rawFeaturesNeedForState = new TObjectDoubleHashMap<>();

        if (focus > 0 && focus < mentions.size()) {
            T mention = mentions.get(focus);
            List<StanfordCorenlpToken> sentenceTokens = mention2SentenceTokens.get(mention);

            int headWordFocus = headWordOffsets.get(mention);

            featureFunctions.forEach(ff ->
                    ff.extract(sentenceTokens, headWordFocus, rawFeaturesNoState, rawFeaturesNeedForState)
            );
        } else {
            // TODO currently do not handle the <START> <STOP> mention features.
        }

        for (TObjectDoubleIterator<String> iter = rawFeaturesNoState.iterator(); iter.hasNext(); ) {
            iter.advance();
            featuresNoState.addFeature(iter.key(), iter.value());
        }

        for (TObjectDoubleIterator<String> iter = rawFeaturesNeedForState.iterator(); iter.hasNext(); ) {
            iter.advance();
            featuresNeedForState.addFeature(iter.key(), iter.value());
        }
    }
}
