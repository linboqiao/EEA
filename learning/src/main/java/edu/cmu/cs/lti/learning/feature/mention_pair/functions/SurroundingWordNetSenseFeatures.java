package edu.cmu.cs.lti.learning.feature.mention_pair.functions;

import com.google.common.base.Joiner;
import com.google.common.collect.ArrayListMultimap;
import edu.cmu.cs.lti.learning.feature.sentence.FeatureUtils;
import edu.cmu.cs.lti.script.type.*;
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
    ArrayListMultimap<EventMention, WordNetBasedEntity> mention2SurroundWnEntities;

    public SurroundingWordNetSenseFeatures(Configuration generalConfig, Configuration featureConfig) {
        super(generalConfig, featureConfig);
    }

    @Override
    public void initDocumentWorkspace(JCas context) {
        mention2SurroundWnEntities = ArrayListMultimap.create();
        for (StanfordCorenlpSentence sentence : JCasUtil.select(context, StanfordCorenlpSentence.class)) {
            for (EventMention mention : JCasUtil.selectCovered(EventMention.class, sentence)) {
                for (WordNetBasedEntity wordNetEntity : JCasUtil.selectCovered(WordNetBasedEntity.class, sentence)) {
                    mention2SurroundWnEntities.put(mention, wordNetEntity);
                }
            }
        }

        int tokenIndex = 0;
        for (StanfordCorenlpToken token : JCasUtil.select(context, StanfordCorenlpToken.class)) {
            token.setIndex(tokenIndex++);
        }
    }

    @Override
    public void extract(JCas documentContext, TObjectDoubleMap<String> rawFeatures, EventMention firstAnno,
                        EventMention secondAnno) {
        WordNetBasedEntity firstClosestEn = closestWordNetEntity(firstAnno);
        WordNetBasedEntity secondClosestEn = closestWordNetEntity(secondAnno);

        if (firstClosestEn != null && secondClosestEn != null) {
            // Add type pair.
            String firstClosestEnType = getWordNetEntityType(firstClosestEn);
            String secondClosestEnType = getWordNetEntityType(secondClosestEn);
            String[] enPair = {firstClosestEnType, secondClosestEnType};
            Arrays.sort(enPair);
            addBoolean(rawFeatures, FeatureUtils.formatFeatureName("ClosestWordNetSenseType",
                    Joiner.on(":").join(enPair)));

            // Add surface pair.
            String firstClosestEnSurface = getWordNetEntitySurface(firstClosestEn);
            String secondClosestEnSurface = getWordNetEntitySurface(secondClosestEn);
            String[] enSurfacePair = {firstClosestEnSurface, secondClosestEnSurface};

            Arrays.sort(enSurfacePair);
            addBoolean(rawFeatures, FeatureUtils.formatFeatureName("ClosestWordNetSenseSurface",
                    Joiner.on(":").join(enSurfacePair)));

            if (firstClosestEnSurface.equals(secondClosestEnSurface)) {
                addBoolean(rawFeatures, "ClosestWordNetSenseSurfaceMatch");
            }
        }

    }

    @Override
    public void extract(JCas documentContext, TObjectDoubleMap<String> rawFeatures, EventMention secondAnno) {

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

    private WordNetBasedEntity closestWordNetEntity(EventMention mention) {
        List<WordNetBasedEntity> wnEntities = mention2SurroundWnEntities.get(mention);

        Word mentionHead = mention.getHeadWord();

        int minDistance = Integer.MAX_VALUE;
        WordNetBasedEntity closestEntity = null;
        for (WordNetBasedEntity en : wnEntities) {
            StanfordCorenlpToken wnToken = UimaConvenience.selectCoveredFirst(en, StanfordCorenlpToken.class);

            int distance = Math.abs(wnToken.getIndex() - mentionHead.getIndex());
            if (distance < minDistance) {
                closestEntity = en;
            }
        }

        return closestEntity;
    }

}
