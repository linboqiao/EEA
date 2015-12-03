package edu.cmu.cs.lti.learning.feature.sentence.extractor;

import edu.cmu.cs.lti.learning.feature.FeatureSpecParser;
import edu.cmu.cs.lti.learning.feature.sentence.functions.SequenceFeatureWithFocus;
import edu.cmu.cs.lti.learning.model.FeatureAlphabet;
import edu.cmu.cs.lti.learning.model.FeatureVector;
import edu.cmu.cs.lti.script.type.EventMention;
import edu.cmu.cs.lti.script.type.StanfordCorenlpToken;
import edu.cmu.cs.lti.utils.Configuration;
import org.apache.uima.jcas.JCas;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * While modeling mention features above the sentence level, we need to deal with the offset a little different.
 *
 * @author Zhengzhong Liu
 */
public class MultiSentenceFeatureExtractor extends UimaSequenceFeatureExtractor {
    protected final Logger logger = LoggerFactory.getLogger(getClass());

    protected List<SequenceFeatureWithFocus> featureFunctions = new ArrayList<>();

    private Map<EventMention, List<StanfordCorenlpToken>> mention2SentenceTokens;

    public MultiSentenceFeatureExtractor(FeatureAlphabet alphabet, Configuration generalConfig, Configuration featureConfig)
            throws ClassNotFoundException, NoSuchMethodException, IllegalAccessException, InvocationTargetException,
            InstantiationException {
        super(alphabet);

        String featureFunctionPackage = featureConfig.get(FeatureSpecParser.FEATURE_FUNCTION_PACKAGE_KEY);
        for (String featureFunctionName : featureConfig.getList(FeatureSpecParser.FEATURE_FUNCTION_NAME_KEY)) {
            String ffClassName = featureFunctionPackage + "." + featureFunctionName;
            Class<?> featureFunctionType = Class.forName(ffClassName);
            Constructor<?> constructor = featureFunctionType.getConstructor(Configuration.class, Configuration.class);
            featureFunctions.add((SequenceFeatureWithFocus) constructor.newInstance(generalConfig, featureConfig));
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

    }

    @Override
    public void extract(int focus, FeatureVector features, FeatureVector featuresNeedForState) {

    }
}
