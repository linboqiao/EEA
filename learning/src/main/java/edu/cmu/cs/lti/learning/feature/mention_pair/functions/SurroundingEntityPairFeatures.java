package edu.cmu.cs.lti.learning.feature.mention_pair.functions;

import com.google.common.base.Joiner;
import com.google.common.collect.ArrayListMultimap;
import edu.cmu.cs.lti.learning.feature.sentence.FeatureUtils;
import edu.cmu.cs.lti.script.type.EventMention;
import edu.cmu.cs.lti.script.type.StanfordCorenlpSentence;
import edu.cmu.cs.lti.script.type.StanfordCorenlpToken;
import edu.cmu.cs.lti.script.type.StanfordEntityMention;
import edu.cmu.cs.lti.utils.Configuration;
import edu.cmu.cs.lti.utils.SimilarityUtils;
import gnu.trove.map.TObjectDoubleMap;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;

import java.util.Arrays;
import java.util.Collection;

/**
 * Created with IntelliJ IDEA.
 * Date: 9/29/15
 * Time: 3:45 PM
 *
 * @author Zhengzhong Liu
 */
public class SurroundingEntityPairFeatures extends AbstractMentionPairFeatures {
    private ArrayListMultimap<EventMention, StanfordEntityMention> mention2SurroundWnEntities;

    public SurroundingEntityPairFeatures(Configuration generalConfig, Configuration featureConfig) {
        super(generalConfig, featureConfig);
    }

    @Override
    public void initDocumentWorkspace(JCas context) {
        mention2SurroundWnEntities = ArrayListMultimap.create();
        for (StanfordCorenlpSentence sentence : JCasUtil.select(context, StanfordCorenlpSentence.class)) {
            Collection<StanfordEntityMention> entities = JCasUtil.selectCovered(StanfordEntityMention.class, sentence);
            for (EventMention mention : JCasUtil.selectCovered(EventMention.class, sentence)) {
                mention2SurroundWnEntities.putAll(mention, entities);
            }
        }
    }

    @Override
    public void extract(JCas documentContext, TObjectDoubleMap<String> rawFeatures, EventMention firstAnno,
                        EventMention secondAnno) {
        StanfordEntityMention firstCloestMention = closestEntityMention(firstAnno);
        StanfordEntityMention secondCloestMention = closestEntityMention(secondAnno);

        closestTypePairFeature(rawFeatures, firstCloestMention, secondCloestMention);
        closestSurfacePairFeature(rawFeatures, firstCloestMention, secondCloestMention);
    }

    @Override
    public void extract(JCas documentContext, TObjectDoubleMap<String> rawFeatures, EventMention secondAnno) {

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

    private StanfordEntityMention closestEntityMention(EventMention eventMention) {
        int minDistance = Integer.MAX_VALUE;
        StanfordEntityMention closestEntityMention = null;

        for (StanfordEntityMention entityMention : mention2SurroundWnEntities.get(eventMention)) {
            int distance = JCasUtil.selectBetween(StanfordCorenlpToken.class, entityMention, eventMention).size();
            if (distance < minDistance) {
                closestEntityMention = entityMention;
            }
        }
        return closestEntityMention;
    }
}
