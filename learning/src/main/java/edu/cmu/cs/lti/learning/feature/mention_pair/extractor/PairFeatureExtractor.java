package edu.cmu.cs.lti.learning.feature.mention_pair.extractor;

import edu.cmu.cs.lti.learning.feature.FeatureSpecParser;
import edu.cmu.cs.lti.learning.feature.mention_pair.functions.MentionPairFeatures;
import edu.cmu.cs.lti.learning.model.BinaryHashFeatureVector;
import edu.cmu.cs.lti.learning.model.FeatureAlphabet;
import edu.cmu.cs.lti.learning.model.FeatureVector;
import edu.cmu.cs.lti.learning.model.RealValueHashFeatureVector;
import edu.cmu.cs.lti.script.type.EventMention;
import edu.cmu.cs.lti.utils.Configuration;
import gnu.trove.map.TObjectDoubleMap;
import gnu.trove.map.hash.TObjectDoubleHashMap;
import org.apache.uima.jcas.JCas;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * Date: 9/27/15
 * Time: 4:31 PM
 *
 * @author Zhengzhong Liu
 */
public class PairFeatureExtractor {
    private List<MentionPairFeatures> featureFunctions = new ArrayList<>();

    private boolean useBinary;

    private FeatureAlphabet alphabet;

    public PairFeatureExtractor(FeatureAlphabet alphabet, boolean useBinary,
                                Configuration generalConfig, Configuration featureConfig)
            throws ClassNotFoundException, NoSuchMethodException, IllegalAccessException, InvocationTargetException,
            InstantiationException {
        this.useBinary = useBinary;
        this.alphabet = alphabet;
        String featureFunctionPackage = featureConfig.get(FeatureSpecParser.FEATURE_FUNCTION_PACKAGE_KEY);
        for (String featureFunctionName : featureConfig.getList(FeatureSpecParser.FEATURE_FUNCTION_NAME_KEY)) {
            String ffClassName = featureFunctionPackage + "." + featureFunctionName;
            Class<?> featureFunctionType = Class.forName(ffClassName);
            Constructor<?> constructor = featureFunctionType.getConstructor(Configuration.class, Configuration.class);
            featureFunctions.add((MentionPairFeatures) constructor.newInstance(generalConfig, featureConfig));
        }
    }

    public void initWorkspace(JCas context) {
        for (MentionPairFeatures ff : featureFunctions) {
            ff.initDocumentWorkspace(context);
        }
    }

    private FeatureVector newFeatureVector() {
        return useBinary ? new BinaryHashFeatureVector(alphabet) : new RealValueHashFeatureVector(alphabet);
    }

    public FeatureVector extract(JCas context, EventMention firstMention, EventMention secondMention) {
        FeatureVector featureVector = newFeatureVector();
        TObjectDoubleMap<String> rawFeatures = new TObjectDoubleHashMap<>();
        featureFunctions.forEach(ff -> ff.extract(context, rawFeatures, firstMention, secondMention));

        rawFeatures.forEachEntry((featureName, featureValue) -> {
            featureVector.addFeature(featureName, featureValue);
            return true;
        });

        return featureVector;
    }
}
