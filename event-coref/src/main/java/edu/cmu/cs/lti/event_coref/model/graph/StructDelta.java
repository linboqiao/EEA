package edu.cmu.cs.lti.event_coref.model.graph;

import edu.cmu.cs.lti.event_coref.ml.StructWeights;
import edu.cmu.cs.lti.feature.MapBasedFeatureContainer;
import edu.cmu.cs.lti.feature.VectorUtils;
import gnu.trove.map.TObjectDoubleMap;

import java.util.EnumMap;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * Date: 5/4/15
 * Time: 1:10 AM
 *
 * @author Zhengzhong Liu
 */
public class StructDelta {
    public final TObjectDoubleMap<String> unlabelledFeatures;
    public final EnumMap<Edge.EdgeType, TObjectDoubleMap<String>> typedFeatures;

    public StructDelta(TObjectDoubleMap<String> unlabelledFeatures, EnumMap<Edge.EdgeType, TObjectDoubleMap<String>> typedFeatures) {
        this.unlabelledFeatures = unlabelledFeatures;
        this.typedFeatures = typedFeatures;
    }

    public double getL2() {
        double l2Sq = VectorUtils.vectorL2Sq(unlabelledFeatures);
        for (TObjectDoubleMap<String> featureValues : typedFeatures.values()) {
            l2Sq += VectorUtils.vectorL2Sq(featureValues);
        }
        return Math.sqrt(l2Sq);
    }

    public double getScore(StructWeights weights) {
        double score = weights.unlabelledWeights.score(unlabelledFeatures);
        for (Map.Entry<Edge.EdgeType, MapBasedFeatureContainer> typedWeights : weights.labelledWeights.entrySet()) {
            TObjectDoubleMap<String> typedFeature = typedFeatures.get(typedWeights.getKey());
            if (typedFeature != null) {
                score += typedWeights.getValue().score(typedFeature);
            }
        }
        return score;
    }

    @Override
    public String toString() {
        return String.format("[StructDelta]:\n - Unlabelled Features : [%s]\n - Labelled Features: [%s]", unlabelledFeatures.toString(), typedFeatures.toString());
    }
}
