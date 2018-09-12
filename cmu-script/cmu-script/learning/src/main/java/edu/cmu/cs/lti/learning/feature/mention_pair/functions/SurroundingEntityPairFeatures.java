package edu.cmu.cs.lti.learning.feature.mention_pair.functions;

import com.google.common.base.Joiner;
import com.google.common.collect.ArrayListMultimap;
import edu.cmu.cs.lti.learning.feature.sequence.FeatureUtils;
import edu.cmu.cs.lti.learning.model.MentionCandidate;
import edu.cmu.cs.lti.learning.model.NodeKey;
import edu.cmu.cs.lti.script.type.Sentence;
import edu.cmu.cs.lti.script.type.StanfordCorenlpSentence;
import edu.cmu.cs.lti.script.type.StanfordEntityMention;
import edu.cmu.cs.lti.utils.Configuration;
import edu.cmu.cs.lti.utils.SimilarityUtils;
import gnu.trove.map.TObjectDoubleMap;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * Date: 9/29/15
 * Time: 3:45 PM
 *
 * @author Zhengzhong Liu
 */
public class SurroundingEntityPairFeatures extends AbstractMentionPairFeatures {
    private ArrayListMultimap<Sentence, StanfordEntityMention> mentionsBySentence;

    public SurroundingEntityPairFeatures(Configuration generalConfig, Configuration featureConfig) {
        super(generalConfig, featureConfig);
    }

    @Override
    public void initDocumentWorkspace(JCas context) {
        mentionsBySentence = ArrayListMultimap.create();
        for (StanfordCorenlpSentence sentence : JCasUtil.select(context, StanfordCorenlpSentence.class)) {
            Collection<StanfordEntityMention> entities = JCasUtil.selectCovered(StanfordEntityMention.class, sentence);
            mentionsBySentence.putAll(sentence, entities);
        }
    }

    @Override
    public void extract(JCas documentContext, TObjectDoubleMap<String> featuresNoLabel, List<MentionCandidate>
            candidates, NodeKey firstNode, NodeKey secondNode) {
        MentionCandidate firstCandidate = candidates.get(firstNode.getIndex());
        MentionCandidate secondCandidate = candidates.get(secondNode.getIndex());

        StanfordEntityMention firstCloestMention = closestEntityMention(firstCandidate);
        StanfordEntityMention secondCloestMention = closestEntityMention(secondCandidate);

        closestTypePairFeature(featuresNoLabel, firstCloestMention, secondCloestMention);
        closestSurfacePairFeature(featuresNoLabel, firstCloestMention, secondCloestMention);

    }

    @Override
    public void extractCandidateRelated(JCas documentContext, TObjectDoubleMap<String> featuresNeedLabel,
                                        List<MentionCandidate> candidates, NodeKey firstNode, NodeKey

                                                secondNode) {

    }

    @Override
    public void extract(JCas documentContext, TObjectDoubleMap<String> featuresNoLabel, MentionCandidate
            secondCandidate, NodeKey secondNode) {

    }

    @Override
    public void extractCandidateRelated(JCas documentContext, TObjectDoubleMap<String> featureNoLabel,
                                        MentionCandidate secondCandidate, NodeKey secondNode) {

    }

    private void closestTypePairFeature(TObjectDoubleMap<String> rawFeatures,
                                        StanfordEntityMention firstMention, StanfordEntityMention secondMention) {
        if (firstMention != null && secondMention != null) {
            String firstClosest = getEntityType(firstMention);
            String secondClosest = getEntityType(secondMention);
            String[] closestTypes = {firstClosest, secondClosest};
            Arrays.sort(closestTypes);
            addBoolean(rawFeatures, FeatureUtils.formatFeatureName("ClosestEntityTypePair",
                    Joiner.on(":").join(closestTypes)));
        }
    }

    private void closestSurfacePairFeature(TObjectDoubleMap<String> rawFeatures, StanfordEntityMention firstMention,
                                           StanfordEntityMention secondMention) {
        if (firstMention != null && secondMention != null) {
            String firstSurface = firstMention.getCoveredText();
            String secondSurface = secondMention.getCoveredText();
            if (firstSurface.contains(secondSurface) || secondSurface.contains(firstSurface)) {
                addBoolean(rawFeatures, "ClosestEntitySurfaceSubstring");
            }
            double rDice = SimilarityUtils.relaxedDiceTest(firstSurface, secondSurface);
            rawFeatures.put("ClosestEntitySurfaceDice", rDice);
        }
    }


    private String getEntityType(StanfordEntityMention mention) {
        if (mention == null) {
            return "<NONE>";
        }
        String type = mention.getEntityType();
        if (type == null) {
            mention.getReferingEntity().getRepresentativeMention().getEntityType();
        }
        if (type == null) {
            type = "<OTHER>";
        }

        return type;
    }

    private StanfordEntityMention closestEntityMention(MentionCandidate candidate) {
        int minDistance = Integer.MAX_VALUE;
        StanfordEntityMention closestEntityMention = null;

        for (StanfordEntityMention entityMention : mentionsBySentence.get(candidate.getContainedSentence())) {
            int distance = Math.abs(entityMention.getHead().getIndex() - candidate.getHeadWord().getIndex());
            if (distance < minDistance) {
                closestEntityMention = entityMention;
            }
        }
        return closestEntityMention;
    }
}
