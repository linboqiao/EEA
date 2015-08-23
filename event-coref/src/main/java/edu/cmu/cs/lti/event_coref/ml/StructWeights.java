package edu.cmu.cs.lti.event_coref.ml;

import edu.cmu.cs.lti.feature.MapBasedFeatureContainer;
import edu.cmu.cs.lti.event_coref.model.graph.Edge;
import edu.cmu.cs.lti.event_coref.model.graph.StructDelta;
import gnu.trove.map.TObjectDoubleMap;

import java.io.Serializable;
import java.util.EnumMap;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * Date: 5/3/15
 * Time: 10:49 PM
 *
 * @author Zhengzhong Liu
 */
public class StructWeights implements Serializable {
    private static final long serialVersionUID = -4869316622108717167L;
    public final EnumMap<Edge.EdgeType, MapBasedFeatureContainer> labelledWeights;
    public final MapBasedFeatureContainer unlabelledWeights;

    public StructWeights() {
        labelledWeights = new EnumMap<>(Edge.EdgeType.class);
        unlabelledWeights = new MapBasedFeatureContainer();
        for (Edge.EdgeType type : Edge.EdgeType.values()) {
            labelledWeights.put(type, new MapBasedFeatureContainer());
            labelledWeights.put(type, new MapBasedFeatureContainer());
        }
    }

    public void update(StructDelta delta, double tau) {
        unlabelledWeights.update(delta.unlabelledFeatures, tau);
        for (Map.Entry<Edge.EdgeType, TObjectDoubleMap<String>> entry : delta.typedFeatures.entrySet()) {
            Edge.EdgeType edgeType = entry.getKey();
            labelledWeights.get(edgeType).update(entry.getValue(), tau);
        }
    }
}
