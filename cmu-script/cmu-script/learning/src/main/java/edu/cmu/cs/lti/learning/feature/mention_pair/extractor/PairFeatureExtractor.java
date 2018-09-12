package edu.cmu.cs.lti.learning.feature.mention_pair.extractor;

import edu.cmu.cs.lti.learning.feature.FeatureSpecParser;
import edu.cmu.cs.lti.learning.feature.mention_pair.functions.AbstractMentionPairFeatures;
import edu.cmu.cs.lti.learning.model.*;
import edu.cmu.cs.lti.utils.Configuration;
import gnu.trove.map.TObjectDoubleMap;
import org.apache.uima.jcas.JCas;

import java.io.Serializable;
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
public class PairFeatureExtractor implements Serializable {
    private static final long serialVersionUID = -1278823150356335817L;

    private List<AbstractMentionPairFeatures> featureFunctions = new ArrayList<>();

    private final ClassAlphabet classAlphabet;

    private final FeatureAlphabet featureAlphabet;

    private JCas context;

    // TODO Extractor is highly depended on event mentions now. let's what can be done
    public PairFeatureExtractor(FeatureAlphabet featureAlphabet, ClassAlphabet classAlphabet,
                                Configuration generalConfig, Configuration featureConfig)
            throws ClassNotFoundException, NoSuchMethodException, IllegalAccessException, InvocationTargetException,
            InstantiationException {
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
    }

    public FeatureVector newFeatureVector() {
        return new RealValueHashFeatureVector(featureAlphabet);
    }

    public GraphFeatureVector newGraphFeatureVector() {
        return new GraphFeatureVector(classAlphabet, featureAlphabet);
    }

    /**
     * Extract label agnostic features from one mention only when the other is deliberately omitted, for example, the
     * other mention is a virtual root.
     *
     * @param mentionCandidate The mention to extract from.
     * @param depKey           The node key for the dependent.
     * @return Feature vector of this mention against the other
     */
    public void extract(MentionCandidate mentionCandidate, TObjectDoubleMap<String> rawFeaturesNoLabel,
                        NodeKey depKey) {
        featureFunctions.forEach(ff -> ff.extract(context, rawFeaturesNoLabel, mentionCandidate, depKey));
    }

    /**
     * Extract label agnostic features from one mention only when the other is deliberately omitted, for example, the
     * other mention is a virtual root.
     *
     * @param mentionCandidate The mention to extract from.
     * @param depKey           The node key for the dependent
     * @return Feature vector of this mention against the other
     */
    public void extractCandidateRelated(MentionCandidate mentionCandidate, TObjectDoubleMap<String>
            rawFeaturesNeedLabel, NodeKey depKey) {
        featureFunctions.forEach(ff -> {
            ff.extractCandidateRelated(context, rawFeaturesNeedLabel, mentionCandidate, depKey);
        });
    }


    /**
     * Extract label agnostic features for the mention pair.
     *
     * @param candidates
     * @param firstNode
     * @param secondNode @return Feature vector of this mention against the other
     */
    public void extract(List<MentionCandidate> candidates, NodeKey firstNode, NodeKey secondNode,
                        TObjectDoubleMap<String>
            rawFeaturesNoLabel) {
        featureFunctions.forEach(ff -> {
            ff.extract(context, rawFeaturesNoLabel, candidates, firstNode, secondNode);
        });
    }

    /**
     * Extract label dependent features for the mention pair.
     *
     * @param candidates
     * @param firstKey
     * @param secondKey  @return Feature vector of this mention against the other
     */
    public void extractCandidateRelated(List<MentionCandidate> candidates, NodeKey firstKey, NodeKey secondKey,
                                        TObjectDoubleMap<String> rawFeaturesNeedLabel) {
        featureFunctions.forEach(ff -> {
            ff.extractCandidateRelated(context, rawFeaturesNeedLabel, candidates, firstKey, secondKey);
        });
    }
}
