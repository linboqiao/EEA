package edu.cmu.cs.lti.learning.feature.sequence.document.functions;

import edu.cmu.cs.lti.learning.feature.sequence.base.SequenceFeatureWithFocus;
import edu.cmu.cs.lti.script.type.CandidateEventMention;
import edu.cmu.cs.lti.utils.Configuration;
import gnu.trove.map.TObjectDoubleMap;
import org.apache.uima.jcas.JCas;

import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * Date: 12/21/15
 * Time: 4:49 PM
 *
 * @author Zhengzhong Liu
 */
public class StatePairFeatures extends SequenceFeatureWithFocus<CandidateEventMention> {
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
    public void extract(List<CandidateEventMention> sequence, int focus, TObjectDoubleMap<String> features,
                        TObjectDoubleMap<String> featuresNeedForState) {
        addToFeatures(featuresNeedForState, "StatePair", 1);
    }
}
