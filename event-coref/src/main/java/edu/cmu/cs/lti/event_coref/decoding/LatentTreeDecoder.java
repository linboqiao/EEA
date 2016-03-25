package edu.cmu.cs.lti.event_coref.decoding;

import edu.cmu.cs.lti.event_coref.model.graph.MentionGraph;
import edu.cmu.cs.lti.event_coref.model.graph.MentionSubGraph;
import edu.cmu.cs.lti.learning.feature.mention_pair.extractor.PairFeatureExtractor;
import edu.cmu.cs.lti.learning.model.GraphWeightVector;
import edu.cmu.cs.lti.learning.model.MentionCandidate;
import edu.cmu.cs.lti.learning.utils.CubicLagrangian;
import edu.cmu.cs.lti.learning.utils.DummyCubicLagrangian;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * Date: 9/26/15
 * Time: 6:20 PM
 *
 * @author Zhengzhong Liu
 */
public abstract class LatentTreeDecoder {
    protected final Logger logger = LoggerFactory.getLogger(getClass());

    private DummyCubicLagrangian dummyLagrangian = new DummyCubicLagrangian();

    public LatentTreeDecoder() {
        logger.info("Decoder is " + this.getClass().getSimpleName());
    }

    /**
     * Decode the graph to produce a best tree structure.
     *
     * @param mentionGraph The base graph to be decode.
     * @param weights      The weight vector used to decode.
     * @param u            The Lagrangian multiplier for decoding.
     * @param v
     * @return The subgraph containing only the selected weights.
     */
    public abstract MentionSubGraph decode(MentionGraph mentionGraph, List<MentionCandidate> mentionCandidates,
                                           GraphWeightVector weights, PairFeatureExtractor extractor,
                                           CubicLagrangian u, CubicLagrangian v);


    /**
     * Decode the graph to produce a best tree structure.
     *
     * @param mentionGraph The base graph to be decode.
     * @param weights      The weight vector used to decode.
     * @return The subgraph containing only the selected weights.
     */
    public MentionSubGraph decode(MentionGraph mentionGraph, List<MentionCandidate> mentionCandidates,
                                           GraphWeightVector weights, PairFeatureExtractor extractor){
        return decode(mentionGraph, mentionCandidates, weights, extractor, dummyLagrangian, dummyLagrangian);
    }

}
