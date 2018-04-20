package edu.cmu.cs.lti.script.utils;

import edu.cmu.cs.lti.script.type.Entity;
import edu.cmu.cs.lti.script.type.EntityMention;
import edu.cmu.cs.lti.script.type.StanfordCorenlpToken;
import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.hash.TObjectIntHashMap;
import org.apache.uima.fit.util.FSCollectionFactory;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;

import java.util.HashMap;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * Date: 4/19/18
 * Time: 9:19 PM
 *
 * @author Zhengzhong Liu
 */
public class ImplicitFeaturesExtractor {
    public static Map<EntityMention, Map<String, Double>> getSalienceFeatures(JCas aJCas) {

        Map<EntityMention, Map<String, Double>> allFeatures = new HashMap<>();

        TObjectIntMap<String> tokenCount = new TObjectIntHashMap<>();
        for (StanfordCorenlpToken token : JCasUtil.select(aJCas, StanfordCorenlpToken.class)) {
            tokenCount.adjustOrPutValue(token.getLemma().toLowerCase(), 1, 1);
        }

        for (EntityMention entityMention : JCasUtil.select(aJCas, EntityMention.class)) {
            Entity entity = entityMention.getReferingEntity();
            String representHead = entity.getRepresentativeMention().getHead().getLemma().toLowerCase();
            int count = tokenCount.get(representHead);

            Map<String, Double> features = new HashMap<>();
            features.put("HeadCount", 1.0 * count);
            features.put("TotalMentionCount", 1.0 * entity.getEntityMentions().size());

            int nominalCount = 0;
            int namedCount = 0;
            int pronominalCount = 0;

            for (EntityMention mention : FSCollectionFactory.create(entity.getEntityMentions(), EntityMention.class)) {
            }

            allFeatures.put(entityMention, features);
        }

        return allFeatures;
    }
}
