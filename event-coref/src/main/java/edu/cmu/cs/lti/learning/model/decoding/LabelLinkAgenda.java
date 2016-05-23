package edu.cmu.cs.lti.learning.model.decoding;

import edu.cmu.cs.lti.learning.model.FeatureVector;
import edu.cmu.cs.lti.learning.model.GraphFeatureVector;
import edu.cmu.cs.lti.learning.model.graph.EdgeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * Date: 3/1/16
 * Time: 3:59 PM
 *
 * @author Zhengzhong Liu
 */
public abstract class LabelLinkAgenda {
    protected transient final Logger logger = LoggerFactory.getLogger(getClass());

    public LabelLinkAgenda() {
    }

    public abstract Collection<NodeLinkingState> getBeamStates();

    public abstract NodeLinkingState getBestBeamState();

    /**
     * Take the states out in order as a list. This requires one full creation of a Queue and a list.
     *
     * @return The ordered states in a list.
     */
    public abstract List<NodeLinkingState> getOrderedStates();

    public abstract void prepareExpand();

    public abstract void clearFeatures();

    public abstract void expand(StateDelta delta);

    public abstract Map<EdgeType, FeatureVector> getBestDeltaCorefVectors();

    public abstract GraphFeatureVector getBestDeltaLabelFv();

    /**
     * Update the beam states, by converting the deltas to new states, and remove the old ones.
     */
    public abstract void updateStates();

    public abstract boolean contains(LabelLinkAgenda anotherAgenda);

    public abstract void copyFrom(LabelLinkAgenda anotherAgenda);
}
