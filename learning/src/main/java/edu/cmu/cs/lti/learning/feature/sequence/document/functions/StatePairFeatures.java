package edu.cmu.cs.lti.learning.feature.sequence.document.functions;

import com.google.common.collect.Table;
import edu.cmu.cs.lti.learning.feature.sequence.base.SequenceFeatureWithFocus;
import edu.cmu.cs.lti.learning.model.MultiNodeKey;
import edu.cmu.cs.lti.script.type.EventMention;
import edu.cmu.cs.lti.utils.Configuration;
import gnu.trove.map.TObjectDoubleMap;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.uima.jcas.JCas;

import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * Date: 12/21/15
 * Time: 4:49 PM
 *
 * @author Zhengzhong Liu
 */
public class StatePairFeatures extends SequenceFeatureWithFocus<EventMention> {
    public StatePairFeatures(Configuration generalConfig, Configuration featureConfig) {
        super(generalConfig, featureConfig);
    }

    @Override
    public void initDocumentWorkspace(JCas context) {

    }

    @Override
    public void resetWorkspace(JCas aJCas) {

    }

    @Override
    public void extract(List<EventMention> sequence, int focus, TObjectDoubleMap<String> nodeFeatures,
                        Table<Pair<Integer, Integer>, String, Double> edgeFeatures) {
        addToFeatures(edgeFeatures, focus - 1, focus, "StatePair", 1);
    }

    @Override
    public void extractGlobal(List<EventMention> sequence, int focus,
                              TObjectDoubleMap<String> globalFeatures, List<MultiNodeKey> knownStates) {

    }
}
