package edu.cmu.cs.lti.learning.feature.sentence.extractor;

import edu.cmu.cs.lti.learning.feature.FeatureSpecParser;
import edu.cmu.cs.lti.learning.feature.sentence.functions.SequenceFeatureWithFocus;
import edu.cmu.cs.lti.learning.model.FeatureAlphabet;
import edu.cmu.cs.lti.script.type.StanfordCorenlpToken;
import edu.cmu.cs.lti.utils.Configuration;
import gnu.trove.map.TObjectDoubleMap;
import gnu.trove.map.TObjectIntMap;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * Date: 9/12/15
 * Time: 3:39 PM
 *
 * @author Zhengzhong Liu
 */
public class SentenceFeatureExtractor extends UimaSequenceFeatureExtractor {

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    protected List<StanfordCorenlpToken> sentenceTokens;
    protected TObjectIntMap<StanfordCorenlpToken> tokenIndex;
    protected List<SequenceFeatureWithFocus> featureFunctions = new ArrayList<>();

    public SentenceFeatureExtractor(FeatureAlphabet alphabet, Configuration featureConfig) throws
            ClassNotFoundException, NoSuchMethodException, IllegalAccessException, InvocationTargetException,
            InstantiationException {
        super(alphabet);

        String featureFunctionPackage = featureConfig.get(FeatureSpecParser.FEATURE_FUNCTION_PACKAGE_KEY);

        for (String featureFunctionName : featureConfig.getList(FeatureSpecParser.FEAUTRE_FUNCTION_NAME_KEY)) {
            String ffClassName = featureFunctionPackage + "." + featureFunctionName;
            Class<?> featureFunctionType = Class.forName(ffClassName);
            Constructor<?> constructor = featureFunctionType.getConstructor(Configuration.class);
            featureFunctions.add((SequenceFeatureWithFocus) constructor.newInstance(featureConfig));
        }

    }

    @Override
    public void initWorkspace(JCas context) {
        super.initWorkspace(context);
        for (SequenceFeatureWithFocus ff : featureFunctions) {
            ff.initDocumentWorkspace(context);
        }
    }

    @Override
    public void resetWorkspace(JCas aJCas, int begin, int end) {
        sentenceTokens = JCasUtil.selectCovered(aJCas, StanfordCorenlpToken.class, begin, end);

        int i = 0;
        for (StanfordCorenlpToken token : sentenceTokens) {
            tokenIndex.put(token, i++);
        }

        for (SequenceFeatureWithFocus ff : featureFunctions) {
            ff.resetWorkspace(aJCas, begin, end);
        }
    }

    public int getTokenIndex(StanfordCorenlpToken token) {
        if (tokenIndex.containsKey(token)) {
            return tokenIndex.get(token);
        }

        return -1;
    }

    @Override
    public void extract(int focus, TObjectDoubleMap<String> featuresNoState, TObjectDoubleMap<String>
            featuresNeedForState) {
        featureFunctions.forEach(ff -> ff.extract(sentenceTokens, focus, featuresNoState, featuresNeedForState));
    }
}
