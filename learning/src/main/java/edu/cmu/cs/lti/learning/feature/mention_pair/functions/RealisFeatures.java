package edu.cmu.cs.lti.learning.feature.mention_pair.functions;

import com.google.common.base.Joiner;
import edu.cmu.cs.lti.learning.feature.sequence.FeatureUtils;
import edu.cmu.cs.lti.script.type.EventMention;
import edu.cmu.cs.lti.utils.Configuration;
import gnu.trove.map.TObjectDoubleMap;
import org.apache.uima.jcas.JCas;

import java.util.Arrays;

/**
 * Created with IntelliJ IDEA.
 * Date: 9/30/15
 * Time: 4:57 AM
 *
 * @author Zhengzhong Liu
 */
public class RealisFeatures extends AbstractMentionPairFeatures {
    public RealisFeatures(Configuration generalConfig, Configuration featureConfig) {
        super(generalConfig, featureConfig);
    }

    @Override
    public void initDocumentWorkspace(JCas context) {

    }

    @Override
    public void extract(JCas documentContext, TObjectDoubleMap<String> rawFeatures, EventMention firstAnno,
                        EventMention secondAnno) {
        String[] realisTypes = {firstAnno.getRealisType(), secondAnno.getRealisType()};
        Arrays.sort(realisTypes);
        addBoolean(rawFeatures, FeatureUtils.formatFeatureName("RealisPair", Joiner.on(":").join(realisTypes)));
    }

    @Override
    public void extract(JCas documentContext, TObjectDoubleMap<String> rawFeatures, EventMention secondAnno) {
        addBoolean(rawFeatures, FeatureUtils.formatFeatureName("SingleRealis", secondAnno.getRealisType()));
    }
}
