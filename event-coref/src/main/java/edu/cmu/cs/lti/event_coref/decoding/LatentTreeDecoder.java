package edu.cmu.cs.lti.event_coref.decoding;

import edu.cmu.cs.lti.event_coref.model.graph.MentionGraph;
import edu.cmu.cs.lti.event_coref.model.graph.MentionSubGraph;
import edu.cmu.cs.lti.learning.feature.mention_pair.extractor.PairFeatureExtractor;
import edu.cmu.cs.lti.learning.model.GraphWeightVector;
import edu.cmu.cs.lti.learning.utils.CubicLagrangian;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created with IntelliJ IDEA.
 * Date: 9/26/15
 * Time: 6:20 PM
 *
 * @author Zhengzhong Liu
 */
public abstract class LatentTreeDecoder {
    protected final Logger logger = LoggerFactory.getLogger(getClass());

    public LatentTreeDecoder() {
        logger.info("Decoder is " + this.getClass().getSimpleName());
    }

    /**
     * Decode the graph to produce a best tree structure.
     *
     * @param mentionGraph The base graph to be decode.
     * @param weights      The weight vector used to decode.
     * @param u   The Lagrangian multiplier for decoding.
     * @param v
     * @return The subgraph containing only the selected weights.
     */
    public abstract MentionSubGraph decode(MentionGraph mentionGraph, GraphWeightVector weights, PairFeatureExtractor
            extractor, CubicLagrangian u, CubicLagrangian v);
}
