package edu.cmu.cs.lti.learning.feature.extractor;

import edu.cmu.cs.lti.learning.feature.FeatureSpecParser;
import edu.cmu.cs.lti.learning.feature.sequence.base.SequenceFeatureWithFocus;
import edu.cmu.cs.lti.learning.model.ChainFeatureExtractor;
import edu.cmu.cs.lti.learning.model.FeatureAlphabet;
import edu.cmu.cs.lti.learning.model.FeatureVector;
import edu.cmu.cs.lti.script.type.StanfordCorenlpToken;
import edu.cmu.cs.lti.utils.Configuration;
import gnu.trove.iterator.TObjectDoubleIterator;
import gnu.trove.map.TObjectDoubleMap;
import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.hash.TObjectDoubleHashMap;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * Date: 8/22/15
 * Time: 5:57 PM
 *
 * @author Zhengzhong Liu
 */
public abstract class UimaSequenceFeatureExtractor<T extends Annotation> extends ChainFeatureExtractor {
    protected final Logger logger = LoggerFactory.getLogger(getClass());

    protected JCas context;

    private boolean useStateFeatures;

    private List<SequenceFeatureWithFocus<StanfordCorenlpToken>> sentenceFeatureFunctions = new ArrayList<>();

    private List<SequenceFeatureWithFocus<T>> documentFeatureFunctions = new ArrayList<>();

    protected List<T> sequenceElements;

    protected TObjectIntMap<T> elementIndex;

    protected Class<T> clazz;

    public UimaSequenceFeatureExtractor(FeatureAlphabet alphabet, Configuration generalConfig,
                                        Configuration sentenceFeatureConfig, Configuration docFeatureConfig,
                                        boolean useStateFeatures, Class<T> elementClass)
            throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException, InstantiationException,
            IllegalAccessException {
        super(alphabet);
        clazz = elementClass;
        addFeatureFunctions(generalConfig, sentenceFeatureConfig, sentenceFeatureFunctions);
        addFeatureFunctions(generalConfig, docFeatureConfig, documentFeatureFunctions);
        this.useStateFeatures = useStateFeatures;
    }

    protected <A extends Annotation> void addFeatureFunctions(Configuration generalConfig, Configuration featureConfig,
                                                              List<SequenceFeatureWithFocus<A>> featureFunctions) throws
            ClassNotFoundException, NoSuchMethodException, IllegalAccessException, InvocationTargetException,
            InstantiationException {
        String featureFunctionPackage = featureConfig.get(FeatureSpecParser.FEATURE_FUNCTION_PACKAGE_KEY);
        for (String featureFunctionName : featureConfig.getList(FeatureSpecParser.FEATURE_FUNCTION_NAME_KEY)) {
            String ffClassName = featureFunctionPackage + "." + featureFunctionName;
            Class<?> featureFunctionType = Class.forName(ffClassName);
            Constructor<?> constructor = featureFunctionType.getConstructor(Configuration.class, Configuration.class);
            featureFunctions.add((SequenceFeatureWithFocus<A>) constructor.newInstance(generalConfig, featureConfig));
        }
    }

    /**
     * Called once per document.
     *
     * @param context
     */
    public void initWorkspace(JCas context) {
        this.context = context;

        sequenceElements = new ArrayList<>(JCasUtil.select(context, clazz));

        for (SequenceFeatureWithFocus ff : sentenceFeatureFunctions) {
            ff.initDocumentWorkspace(context);
        }

        for (SequenceFeatureWithFocus ff : documentFeatureFunctions) {
            ff.initDocumentWorkspace(context);
        }
    }

    /**
     * Called once per sequence.
     *
     * @param aJCas      The Cas containing the annotation.
     * @param annotation The annotation representing the sequence.
     */
    public void resetWorkspace(JCas aJCas, Annotation annotation) {
        resetWorkspace(aJCas, annotation.getBegin(), annotation.getEnd());
    }

    /**
     * Called once per sequence
     *
     * @param aJCas The Cas containing the annotation.
     * @param begin Sequence start.
     * @param end   Sequence end.
     */
    public void resetWorkspace(JCas aJCas, int begin, int end) {
        for (SequenceFeatureWithFocus ff : sentenceFeatureFunctions) {
            ff.resetWorkspace(aJCas);
        }
        for (SequenceFeatureWithFocus ff : documentFeatureFunctions) {
            ff.resetWorkspace(aJCas);
        }
    }

    @Override
    public void extract(int focus, FeatureVector featuresNoState, FeatureVector featuresNeedForState) {
        TObjectDoubleMap<String> rawFeaturesNoState = new TObjectDoubleHashMap<>();
        TObjectDoubleMap<String> rawFeaturesNeedForState = new TObjectDoubleHashMap<>();

        List<StanfordCorenlpToken> sentenceTokens = getSentenceTokens(focus);

        if (focus < sequenceElements.size() && focus >= 0) {
            sentenceFeatureFunctions.forEach(ff -> ff.extract(sentenceTokens, getSentenceFocus(focus),
                    rawFeaturesNoState, rawFeaturesNeedForState));
        }

        documentFeatureFunctions.forEach(ff -> ff.extract(sequenceElements, focus, rawFeaturesNoState,
                rawFeaturesNeedForState));

        for (TObjectDoubleIterator<String> iter = rawFeaturesNoState.iterator(); iter.hasNext(); ) {
            iter.advance();
            featuresNoState.addFeature(iter.key(), iter.value());
        }

        if (useStateFeatures) {
            logger.debug("Using state features.");
            for (TObjectDoubleIterator<String> iter = rawFeaturesNeedForState.iterator(); iter.hasNext(); ) {
                iter.advance();
                featuresNeedForState.addFeature(iter.key(), iter.value());
            }
        }

//        logger.debug("Extracted state features are : ");
//        logger.debug(featuresNeedForState.readableString());
//
//        DebugUtils.pause(logger);
    }

    public int getElementIndex(T token) {
        if (elementIndex.containsKey(token)) {
            return elementIndex.get(token);
        }
        return -1;
    }

    protected abstract int getSentenceFocus(int sequenceFocus);

    protected abstract List<StanfordCorenlpToken> getSentenceTokens(int elementFocus);
}
