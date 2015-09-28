package edu.cmu.cs.lti.event_coref.decoding;

import edu.cmu.cs.lti.event_coref.model.graph.MentionGraph;
import edu.cmu.cs.lti.event_coref.model.graph.MentionSubGraph;
import edu.cmu.cs.lti.learning.feature.mention_pair.extractor.PairFeatureExtractor;
import edu.cmu.cs.lti.learning.model.ClassAlphabet;
import edu.cmu.cs.lti.learning.model.FeatureAlphabet;
import edu.cmu.cs.lti.learning.model.GraphWeightVector;
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

    /**
     * Decode the graph to produce a best tree structure.
     *
     * @param mentionGraph    The base graph to be decode.
     * @param featureAlphabet The feature alphabet that map from string to feature index.
     * @param classAlphabet   The class alphabet that map from class to class index.
     * @param weights         The weight vector used to decode.
     * @return The subgraph containing only the selected weights.
     */
    public abstract MentionSubGraph decode(MentionGraph mentionGraph, FeatureAlphabet featureAlphabet, ClassAlphabet
            classAlphabet, GraphWeightVector weights, PairFeatureExtractor extractor);
}
