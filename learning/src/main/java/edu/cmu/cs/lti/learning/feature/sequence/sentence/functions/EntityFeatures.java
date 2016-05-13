package edu.cmu.cs.lti.learning.feature.sequence.sentence.functions;

import com.google.common.collect.Table;
import edu.cmu.cs.lti.learning.feature.sequence.FeatureUtils;
import edu.cmu.cs.lti.learning.feature.sequence.base.SequenceFeatureWithFocus;
import edu.cmu.cs.lti.learning.model.MultiNodeKey;
import edu.cmu.cs.lti.script.type.StanfordCorenlpToken;
import edu.cmu.cs.lti.script.type.StanfordEntityMention;
import edu.cmu.cs.lti.utils.Configuration;
import gnu.trove.map.TObjectDoubleMap;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;

import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * Date: 9/15/15
 * Time: 1:35 AM
 *
 * @author Zhengzhong Liu
 */
public class EntityFeatures extends SequenceFeatureWithFocus<StanfordCorenlpToken> {

    public EntityFeatures(Configuration generalConfig, Configuration featureConfig) {
        super(generalConfig, featureConfig);
    }

    @Override
    public void initDocumentWorkspace(JCas context) {
        for (StanfordEntityMention mention : JCasUtil.select(context, StanfordEntityMention.class)) {
            String entityType = mention.getEntityType();
            for (StanfordCorenlpToken token : JCasUtil.selectCovered(StanfordCorenlpToken.class, mention)) {
                token.setNerTag(entityType);
            }
        }
    }

    @Override
    public void resetWorkspace(JCas aJCas) {

    }

    @Override
    public void extract(List<StanfordCorenlpToken> sequence, int focus, TObjectDoubleMap<String> nodeFeatures,
                        Table<Pair<Integer, Integer>, String, Double> edgeFeatures) {
        if (focus < sequence.size() && focus > 0) {
            closestEntityType(sequence, focus, nodeFeatures);
        }
    }

    private void closestEntityType(List<StanfordCorenlpToken> sequence, int focus, TObjectDoubleMap<String> features) {
        int minDistance = Integer.MAX_VALUE;
        String closestMentionType = null;

        for (int i = 0; i < sequence.size(); i++) {
            StanfordCorenlpToken token = sequence.get(i);
            if (token.getNerTag() != null) {
                int distance = Math.abs(focus - i);
                if (distance < minDistance) {
                    minDistance = distance;
                    closestMentionType = token.getNerTag();
                }
            }
        }

        if (closestMentionType != null) {
            addToFeatures(features, FeatureUtils.formatFeatureName("ClosestEntityType", closestMentionType), 1);
        }
    }

    @Override
    public void extractGlobal(List<StanfordCorenlpToken> sequence, int focus, TObjectDoubleMap<String>
            globalFeatures, List<MultiNodeKey> knownStates) {
    }

}
