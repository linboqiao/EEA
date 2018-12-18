package edu.cmu.cs.lti.learning.utils;

import edu.cmu.cs.lti.learning.feature.FeatureSpecParser;
import edu.cmu.cs.lti.learning.feature.extractor.SentenceFeatureExtractor;
import edu.cmu.cs.lti.learning.feature.mention_pair.extractor.PairFeatureExtractor;
import edu.cmu.cs.lti.learning.model.ClassAlphabet;
import edu.cmu.cs.lti.learning.model.FeatureAlphabet;
import edu.cmu.cs.lti.learning.model.GraphWeightVector;
import edu.cmu.cs.lti.learning.model.HashAlphabet;
import edu.cmu.cs.lti.learning.model.graph.EdgeType;
import edu.cmu.cs.lti.utils.Configuration;
import org.apache.uima.resource.ResourceInitializationException;

import java.lang.reflect.InvocationTargetException;

/**
 * Created with IntelliJ IDEA.
 * Date: 1/1/17
 * Time: 11:44 AM
 *
 * @author Zhengzhong Liu
 */
public class LearningUtils {
    public static PairFeatureExtractor initializeMentionPairExtractor(Configuration config, String featureSetName,
                                                                      FeatureAlphabet featureAlphabet) throws
            ResourceInitializationException {
        String featureSpec = config.get(featureSetName);

        Configuration featureConfig = new FeatureSpecParser(
                config.get("edu.cmu.cs.lti.feature.pair.package.name")
        ).parseFeatureFunctionSpecs(featureSpec);

        ClassAlphabet classAlphabet = new ClassAlphabet();
        for (EdgeType edgeType : EdgeType.values()) {
            classAlphabet.addClass(edgeType.name());
        }

        try {
            return new PairFeatureExtractor(featureAlphabet, classAlphabet, config, featureConfig);
        } catch (ClassNotFoundException | NoSuchMethodException | InstantiationException | IllegalAccessException |
                InvocationTargetException e) {
            throw new ResourceInitializationException(e);
        }
    }

    public static SentenceFeatureExtractor initializeCrfExtractor(Configuration config,
                                                                  String sentFeatureSetName,
                                                                  String docFeatureSetName,
                                                                  FeatureAlphabet featureAlphabet) throws
            ResourceInitializationException {
        String sentFeatureSpec = config.get(sentFeatureSetName);
        String docFeatureSpec = config.getOrElse(docFeatureSetName, "");

        Configuration sentFeatureConfig = new FeatureSpecParser(
                config.get("edu.cmu.cs.lti.feature.sentence.package.name")
        ).parseFeatureFunctionSpecs(sentFeatureSpec);

        Configuration docFeatureConfig = new FeatureSpecParser(
                config.get("edu.cmu.cs.lti.feature.document.package.name")
        ).parseFeatureFunctionSpecs(docFeatureSpec);

        try {
            return new SentenceFeatureExtractor(featureAlphabet, config,
                    sentFeatureConfig, docFeatureConfig, false);
        } catch (ClassNotFoundException | NoSuchMethodException | InstantiationException | IllegalAccessException |
                InvocationTargetException e) {
            throw new ResourceInitializationException(e);
        }
    }


    public static SentenceFeatureExtractor initializeRealisExtractor(Configuration config, String featureSetName,
                                                                     FeatureAlphabet featureAlphabet) throws
            ResourceInitializationException {
        String featurePackageName = config.get("edu.cmu.cs.lti.feature.sentence.package.name");
        String featureSpec = config.get(featureSetName);

        FeatureSpecParser parser = new FeatureSpecParser(featurePackageName);
        Configuration realisSpec = parser.parseFeatureFunctionSpecs(featureSpec);

        // Currently no document level realis features.
        Configuration placeHolderSpec = new Configuration();

        try {
            return new SentenceFeatureExtractor(featureAlphabet, config, realisSpec, placeHolderSpec, false);
        } catch (ClassNotFoundException | NoSuchMethodException | InstantiationException | IllegalAccessException |
                InvocationTargetException e) {
            throw new ResourceInitializationException(e);
        }
    }

    public static GraphWeightVector preareGraphWeights(Configuration config, String featureSetName) {
        ClassAlphabet classAlphabet = new ClassAlphabet();
        for (EdgeType edgeType : EdgeType.values()) {
            classAlphabet.addClass(edgeType.name());
        }

        boolean readable = config.getBoolean("edu.cmu.cs.lti.readableModel", false);
        int featureBits = config.getInt("edu.cmu.cs.lti.feature.alphabet_bits", 22);

        HashAlphabet featureAlphabet = new HashAlphabet(featureBits, readable);
        String featureSpec = config.get(featureSetName);

        return new GraphWeightVector(classAlphabet, featureAlphabet, featureSpec);
    }

}
