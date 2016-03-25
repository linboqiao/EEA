package edu.cmu.cs.lti.learning.feature.mention_pair.functions;

import com.google.common.base.Joiner;
import edu.cmu.cs.lti.learning.feature.sequence.FeatureUtils;
import edu.cmu.cs.lti.learning.model.MentionCandidate;
import edu.cmu.cs.lti.utils.Configuration;
import gnu.trove.map.TObjectDoubleMap;
import org.apache.uima.jcas.JCas;

import java.util.Arrays;
import java.util.List;

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
    public void extract(JCas documentContext, TObjectDoubleMap<String> featuresNoLabel, List<MentionCandidate>
            candidates, int firstIndex, int secondIndex) {

    }

    @Override
    public void extractCandidateRelated(JCas documentContext, TObjectDoubleMap<String> featuresNeedLabel,
                                        List<MentionCandidate> candidates, int firstIndex, int secondIndex) {
        MentionCandidate firstCandidate = candidates.get(firstIndex);
        MentionCandidate secondCandidate = candidates.get(secondIndex);

        String firstType = firstCandidate.getMentionType();
        String secondType = secondCandidate.getMentionType();
//
//        logger.info(firstCandidate.toString());
//
//        logger.info(secondCandidate.toString());

        if (firstType.equals(secondType)) {
            addBoolean(featuresNeedLabel, "MentionTypeMatch");
        }

        String[] types = {firstType, secondType};
        Arrays.sort(types);

        addBoolean(featuresNeedLabel, FeatureUtils.formatFeatureName("MentionTypePair", Joiner.on(":").join(types)));

        String firstSuperType = firstType.split("_")[0];
        String secondSuperType = secondType.split("_")[0];

        if (firstSuperType.equals(secondSuperType)) {
            addBoolean(featuresNeedLabel, "MentionSuperTypeMatch");
        }

        String[] superTypes = {firstSuperType, secondSuperType};
        Arrays.sort(superTypes);

        addBoolean(featuresNeedLabel, FeatureUtils.formatFeatureName("MentionSuperTypePair", Joiner.on(":").join
                (superTypes)));
    }

    @Override
    public void extract(JCas documentContext, TObjectDoubleMap<String> featuresNoLabel, MentionCandidate
            secondCandidate) {
        addBoolean(featuresNoLabel, FeatureUtils.formatFeatureName("SingleType", secondCandidate.getMentionType()));
    }

    @Override
    public void extractCandidateRelated(JCas documentContext, TObjectDoubleMap<String> featureNoLabel, MentionCandidate
            secondCandidate) {

    }
}
