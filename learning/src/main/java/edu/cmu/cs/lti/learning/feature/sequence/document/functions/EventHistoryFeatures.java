package edu.cmu.cs.lti.learning.feature.sequence.document.functions;

import com.google.common.collect.Table;
import edu.cmu.cs.lti.learning.feature.sequence.FeatureUtils;
import edu.cmu.cs.lti.learning.feature.sequence.base.SequenceFeatureWithFocus;
import edu.cmu.cs.lti.learning.model.MultiNodeKey;
import edu.cmu.cs.lti.learning.model.NodeKey;
import edu.cmu.cs.lti.script.type.EventMention;
import edu.cmu.cs.lti.utils.Configuration;
import gnu.trove.map.TObjectDoubleMap;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.uima.jcas.JCas;

import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * Date: 4/24/16
 * Time: 2:41 PM
 *
 * @author Zhengzhong Liu
 */
public class EventHistoryFeatures extends SequenceFeatureWithFocus<EventMention> {
    public EventHistoryFeatures(Configuration generalConfig, Configuration featureConfig) {
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

    }

    @Override
    public void extractGlobal(List<EventMention> sequence, int focus, TObjectDoubleMap<String> globalFeatures,
                              List<MultiNodeKey> knownStates) {
        //TODO extract this feature carefully, do not include all pairs.
        for (int i = 0; i < focus; i++) {
            MultiNodeKey historyType = knownStates.get(i);
            for (NodeKey s : historyType.getKeys()) {
                String type = s.getMentionType();
                addToFeatures(globalFeatures, FeatureUtils.formatFeatureName("TypeHistory", type), 1);
            }
        }
    }
}
