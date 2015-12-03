package edu.cmu.cs.lti.learning.feature.sentence.functions;

import edu.cmu.cs.lti.learning.feature.sentence.FeatureUtils;
import edu.cmu.cs.lti.script.type.StanfordCorenlpToken;
import edu.cmu.cs.lti.script.type.StanfordEntityMention;
import edu.cmu.cs.lti.utils.Configuration;
import gnu.trove.map.TObjectDoubleMap;
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
public class EntityFeatures extends SequenceFeatureWithFocus {

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
    public void resetWorkspace(JCas aJCas, int begin, int end) {

    }

    @Override
    public void extract(List<StanfordCorenlpToken> sequence, int focus, TObjectDoubleMap<String> features,
                        TObjectDoubleMap<String> featuresNeedForState) {
        if (focus < sequence.size() && focus > 0) {
            closestEntityType(sequence, focus, features);
        }
    }

    private void closestEntityType(List<StanfordCorenlpToken> sequence, int focus, TObjectDoubleMap<String> features) {
        int closest = Integer.MAX_VALUE;
        String closestMentionType = null;

        for (int i = 0; i < sequence.size(); i++) {
            StanfordCorenlpToken token = sequence.get(i);
            if (token.getNerTag() != null) {
                int distance = Math.abs(focus - i);
                if (distance < closest) {
                    closest = distance;
                    closestMentionType = token.getNerTag();
                }
            }
        }

        if (closestMentionType != null) {
            features.put(FeatureUtils.formatFeatureName("ClosestEntityType", closestMentionType), 1);
        }
    }

}
