package edu.cmu.cs.lti.learning.feature.mention_pair.functions;

import edu.cmu.cs.lti.learning.feature.sentence.FeatureUtils;
import edu.cmu.cs.lti.script.type.EventMention;
import edu.cmu.cs.lti.utils.Configuration;
import gnu.trove.map.TObjectDoubleMap;
import org.apache.uima.jcas.JCas;

/**
 * Created with IntelliJ IDEA.
 * Date: 9/30/15
 * Time: 5:16 AM
 *
 * @author Zhengzhong Liu
 */
public class FrameFeatures extends AbstractMentionPairFeatures {
    public FrameFeatures(Configuration generalConfig, Configuration featureConfig) {
        super(generalConfig, featureConfig);
    }

    @Override
    public void initDocumentWorkspace(JCas context) {

    }

    @Override
    public void extract(JCas documentContext, TObjectDoubleMap<String> rawFeatures, EventMention firstAnno,
                        EventMention secondAnno) {
        if (firstAnno.getFrameName() != null && secondAnno.getFrameName() != null) {
            addBoolean(rawFeatures, FeatureUtils.formatFeatureName("FramePair", FeatureUtils.sortedJoin(firstAnno
                    .getFrameName(), secondAnno.getFrameName())));
        }
    }

    @Override
    public void extract(JCas documentContext, TObjectDoubleMap<String> rawFeatures, EventMention secondAnno) {

    }
}
