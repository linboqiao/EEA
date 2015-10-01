package edu.cmu.cs.lti.learning.feature.mention_pair.extractor;

import edu.cmu.cs.lti.learning.feature.FeatureSpecParser;
import edu.cmu.cs.lti.learning.feature.mention_pair.functions.AbstractMentionPairFeatures;
import edu.cmu.cs.lti.learning.model.*;
import edu.cmu.cs.lti.script.type.EventMention;
import edu.cmu.cs.lti.utils.Configuration;
import gnu.trove.map.TObjectDoubleMap;
import gnu.trove.map.hash.TObjectDoubleHashMap;
import org.apache.uima.fit.util.JCasUtil;
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
    private List<AbstractMentionPairFeatures> featureFunctions = new ArrayList<>();

    private boolean useBinary;

    private final ClassAlphabet classAlphabet;

    private final FeatureAlphabet featureAlphabet;

    private JCas context;

    private List<EventMention> mentions;

    public PairFeatureExtractor(FeatureAlphabet featureAlphabet, ClassAlphabet classAlphabet, boolean useBinary,
                                Configuration generalConfig, Configuration featureConfig)
            throws ClassNotFoundException, NoSuchMethodException, IllegalAccessException, InvocationTargetException,
            InstantiationException {
        this.useBinary = useBinary;
        this.featureAlphabet = featureAlphabet;
        this.classAlphabet = classAlphabet;
        String featureFunctionPackage = featureConfig.get(FeatureSpecParser.FEATURE_FUNCTION_PACKAGE_KEY);
        for (String featureFunctionName : featureConfig.getList(FeatureSpecParser.FEATURE_FUNCTION_NAME_KEY)) {
            String ffClassName = featureFunctionPackage + "." + featureFunctionName;
            Class<?> featureFunctionType = Class.forName(ffClassName);
            Constructor<?> constructor = featureFunctionType.getConstructor(Configuration.class, Configuration.class);
            featureFunctions.add((AbstractMentionPairFeatures) constructor.newInstance(generalConfig, featureConfig));
        }
    }

    public void initWorkspace(JCas context) {
        for (AbstractMentionPairFeatures ff : featureFunctions) {
            ff.initDocumentWorkspace(context);
        }
        this.context = context;
        this.mentions = new ArrayList<>(JCasUtil.select(context, EventMention.class));
    }

    public FeatureVector newFeatureVector() {
        return useBinary ? new BinaryHashFeatureVector(featureAlphabet) : new RealValueHashFeatureVector
                (featureAlphabet);
    }

    public GraphFeatureVector newGraphFeatureVector() {
        return new GraphFeatureVector(classAlphabet, featureAlphabet, useBinary);
    }

    /**
     * Extract features from one mention only when the other is deliberately omitted, for example, the other mention
     * is a virtual root.
     *
     * @param mentionId The mention id to extract from.
     * @return Feature vector of this mention against the other
     */
    public FeatureVector extract(int mentionId) {
        FeatureVector featureVector = newFeatureVector();
        TObjectDoubleMap<String> rawFeatures = new TObjectDoubleHashMap<>();
        featureFunctions.forEach(ff -> ff.extract(context, rawFeatures, mentions.get(mentionId)));

        rawFeatures.forEachEntry((featureName, featureValue) -> {
            featureVector.addFeature(featureName, featureValue);
            return true;
        });

        return featureVector;
    }

    /**
     * Extract features for the mention pair.
     *
     * @param firstId  The first mention to extract from.
     * @param secondId The second mention to extract from.
     * @return Feature vector of this mention against the other
     */
    public FeatureVector extract(int firstId, int secondId) {
        FeatureVector featureVector = newFeatureVector();
        TObjectDoubleMap<String> rawFeatures = new TObjectDoubleHashMap<>();
        featureFunctions.forEach(ff -> ff.extract(context, rawFeatures, mentions.get(firstId), mentions.get(secondId)));

        rawFeatures.forEachEntry((featureName, featureValue) -> {
            featureVector.addFeature(featureName, featureValue);
            return true;
        });

        return featureVector;
    }
}
