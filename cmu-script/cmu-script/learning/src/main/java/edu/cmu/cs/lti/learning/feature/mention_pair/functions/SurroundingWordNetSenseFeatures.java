package edu.cmu.cs.lti.learning.feature.mention_pair.functions;

import com.google.common.base.Joiner;
import com.google.common.collect.ArrayListMultimap;
import edu.cmu.cs.lti.learning.feature.sequence.FeatureUtils;
import edu.cmu.cs.lti.learning.model.MentionCandidate;
import edu.cmu.cs.lti.learning.model.NodeKey;
import edu.cmu.cs.lti.script.type.Sentence;
import edu.cmu.cs.lti.script.type.StanfordCorenlpSentence;
import edu.cmu.cs.lti.script.type.StanfordCorenlpToken;
import edu.cmu.cs.lti.script.type.WordNetBasedEntity;
import edu.cmu.cs.lti.uima.util.UimaConvenience;
import edu.cmu.cs.lti.utils.Configuration;
import gnu.trove.map.TObjectDoubleMap;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;

import java.util.Arrays;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * Date: 9/29/15
 * Time: 4:31 PM
 *
 * @author Zhengzhong Liu
 */
public class SurroundingWordNetSenseFeatures extends AbstractMentionPairFeatures {
    ArrayListMultimap<Sentence, WordNetBasedEntity> wnEntitiesBySentence;

    public SurroundingWordNetSenseFeatures(Configuration generalConfig, Configuration featureConfig) {
        super(generalConfig, featureConfig);
    }

    @Override
    public void initDocumentWorkspace(JCas context) {
        wnEntitiesBySentence = ArrayListMultimap.create();
        for (StanfordCorenlpSentence sentence : JCasUtil.select(context, StanfordCorenlpSentence.class)) {
            for (WordNetBasedEntity wordNetEntity : JCasUtil.selectCovered(WordNetBasedEntity.class, sentence)) {
                wnEntitiesBySentence.put(sentence, wordNetEntity);
            }
        }

        int tokenIndex = 0;
        for (StanfordCorenlpToken token : JCasUtil.select(context, StanfordCorenlpToken.class)) {
            token.setIndex(tokenIndex++);
        }
    }

    @Override
    public void extract(JCas documentContext, TObjectDoubleMap<String> featuresNoLabel, List<MentionCandidate>
            candidates, NodeKey firstNode, NodeKey secondNode) {
        MentionCandidate firstCandidate = candidates.get(firstNode.getIndex());
        MentionCandidate secondCandidate = candidates.get(secondNode.getIndex());

        WordNetBasedEntity firstClosestEn = closestWordNetEntity(firstCandidate);
        WordNetBasedEntity secondClosestEn = closestWordNetEntity(secondCandidate);

        if (firstClosestEn != null && secondClosestEn != null) {
            // Add type pair.
            String firstClosestEnType = getWordNetEntityType(firstClosestEn);
            String secondClosestEnType = getWordNetEntityType(secondClosestEn);
            String[] enPair = {firstClosestEnType, secondClosestEnType};
            Arrays.sort(enPair);
            addBoolean(featuresNoLabel, FeatureUtils.formatFeatureName("ClosestWordNetSenseType",
                    Joiner.on(":").join(enPair)));

            // Add surface pair.
            String firstClosestEnSurface = getWordNetEntitySurface(firstClosestEn);
            String secondClosestEnSurface = getWordNetEntitySurface(secondClosestEn);
            String[] enSurfacePair = {firstClosestEnSurface, secondClosestEnSurface};

            Arrays.sort(enSurfacePair);
            addBoolean(featuresNoLabel, FeatureUtils.formatFeatureName("ClosestWordNetSenseSurface",
                    Joiner.on(":").join(enSurfacePair)));

            if (firstClosestEnSurface.equals(secondClosestEnSurface)) {
                addBoolean(featuresNoLabel, "ClosestWordNetSenseSurfaceMatch");
            }
        }
    }

    @Override
    public void extractCandidateRelated(JCas documentContext, TObjectDoubleMap<String> featuresNeedLabel,
                                        List
                                                <MentionCandidate> candidates, NodeKey firstNode, NodeKey secondNode) {

    }

    @Override
    public void extract(JCas documentContext, TObjectDoubleMap<String> featuresNoLabel, MentionCandidate
            secondCandidate, NodeKey secondNode) {

    }

    @Override
    public void extractCandidateRelated(JCas documentContext, TObjectDoubleMap<String> featureNoLabel, MentionCandidate
            secondCandidate, NodeKey secondNode) {

    }

    private String getWordNetEntityType(WordNetBasedEntity wnEntity) {
        if (wnEntity == null) {
            return "<NONE>";
        }
        return wnEntity.getSense();
    }


    private String getWordNetEntitySurface(WordNetBasedEntity wnEntity) {
        if (wnEntity == null) {
            return "<NONE>";
        }
        return wnEntity.getCoveredText().toLowerCase();
    }

    private WordNetBasedEntity closestWordNetEntity(MentionCandidate candidateMention) {
        List<WordNetBasedEntity> wnEntities = wnEntitiesBySentence.get(candidateMention.getContainedSentence());

        int minDistance = Integer.MAX_VALUE;
        WordNetBasedEntity closestEntity = null;
        for (WordNetBasedEntity en : wnEntities) {
            StanfordCorenlpToken wnToken = UimaConvenience.selectCoveredFirst(en, StanfordCorenlpToken.class);
            int distance = Math.abs(wnToken.getIndex() - candidateMention.getHeadWord().getIndex());
            if (distance < minDistance) {
                closestEntity = en;
            }
        }

        return closestEntity;
    }

}
