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
 * Date: 9/29/15
 * Time: 2:54 PM
 *
 * @author Zhengzhong Liu
 */
public class MentionTypeFeatures extends AbstractMentionPairFeatures {
    public MentionTypeFeatures(Configuration generalConfig, Configuration featureConfig) {
        super(generalConfig, featureConfig);
    }

    @Override
    public void initDocumentWorkspace(JCas context) {

    }

    @Override
    public void extract(JCas documentContext, TObjectDoubleMap<String> rawFeatures, EventMention firstAnno,
                        EventMention secondAnno) {
        String firstType = firstAnno.getEventType();
        String secondType = secondAnno.getEventType();

        if (firstType.equals(secondType)) {
            addBoolean(rawFeatures, "MentionTypeMatch");
        }

        String[] types = {firstType, secondType};
        Arrays.sort(types);

        addBoolean(rawFeatures, FeatureUtils.formatFeatureName("MentionTypePair", Joiner.on(":").join(types)));

        String firstSuperType = firstType.split("_")[0];
        String secondSuperType = secondType.split("_")[0];

        if (firstSuperType.equals(secondSuperType)) {
            addBoolean(rawFeatures, "MentionSuperTypeMatch");
        }

        String[] superTypes = {firstSuperType, secondSuperType};
        Arrays.sort(superTypes);

        addBoolean(rawFeatures, FeatureUtils.formatFeatureName("MentionSuperTypePair", Joiner.on(":").join
                (superTypes)));
    }

    @Override
    public void extract(JCas documentContext, TObjectDoubleMap<String> rawFeatures, EventMention secondAnno) {

    }
}
