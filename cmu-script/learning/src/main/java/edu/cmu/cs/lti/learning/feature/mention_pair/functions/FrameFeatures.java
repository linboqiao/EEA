package edu.cmu.cs.lti.learning.feature.mention_pair.functions;

import edu.cmu.cs.lti.learning.feature.sequence.FeatureUtils;
import edu.cmu.cs.lti.learning.model.MentionCandidate;
import edu.cmu.cs.lti.learning.model.NodeKey;
import edu.cmu.cs.lti.script.type.Word;
import edu.cmu.cs.lti.utils.Configuration;
import gnu.trove.map.TObjectDoubleMap;
import org.apache.uima.jcas.JCas;

import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * Date: 9/30/15
 * Time: 5:16 AM
 *
 * @author Zhengzhong Liu
 */
public class FrameFeatures extends AbstractMentionPairFeatures {
//    private Table<String, String, String> frameRelations;

    public FrameFeatures(Configuration generalConfig, Configuration featureConfig) {
        super(generalConfig, featureConfig);

//        String fnRelationPath = FileUtils.joinPaths(
//                generalConfig.get("edu.cmu.cs.lti.resource.dir"),
//                generalConfig.get("edu.cmu.cs.lti.fn_relation.path")
//        );

//        frameRelations = FrameDataReader.getFrameOnlyRelations(fnRelationPath);
    }

    @Override
    public void initDocumentWorkspace(JCas context) {

    }

    @Override
    public void extract(JCas documentContext, TObjectDoubleMap<String> featuresNoLabel, List<MentionCandidate>
            candidates, int firstCandidateId, int secondCandidateId) {
        MentionCandidate firstCandidate = candidates.get(firstCandidateId);
        MentionCandidate secondCandidate = candidates.get(secondCandidateId);

        Word firstHead = firstCandidate.getHeadWord();
        Word secondHead = secondCandidate.getHeadWord();

        String firstFrame = firstHead.getFrameName();
        String secondFrame = secondHead.getFrameName();

        if (firstFrame != null && secondFrame != null) {
            addBoolean(featuresNoLabel, FeatureUtils.formatFeatureName("FramePair", FeatureUtils.sortedJoin(
                    firstFrame, secondFrame)));

//            String relation = frameRelations.get(firstFrame, secondFrame);
//            if (relation == null) {
//                relation = frameRelations.get(secondFrame, firstFrame);
//            }
//
//            if (relation != null) {
//                addBoolean(featuresNoLabel, FeatureUtils.formatFeatureName("FrameRelation", relation));
//            }
        }
    }

    @Override
    public void extractNodeRelated(JCas documentContext, TObjectDoubleMap<String> featuresNeedLabel,
                                   List<MentionCandidate> candidates, NodeKey firstNodeKey, NodeKey
                                           secondNodeKey) {

    }

    @Override
    public void extract(JCas documentContext, TObjectDoubleMap<String> featuresNoLabel, MentionCandidate
            candidate) {

    }

    @Override
    public void extractNodeRelated(JCas documentContext, TObjectDoubleMap<String> featureNoLabel, MentionCandidate
            secondCandidate, NodeKey secondNodeKey) {

    }
}
