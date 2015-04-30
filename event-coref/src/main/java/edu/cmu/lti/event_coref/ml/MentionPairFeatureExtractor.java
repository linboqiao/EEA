package edu.cmu.lti.event_coref.ml;

import edu.cmu.cs.lti.script.type.EntityMention;
import edu.cmu.cs.lti.script.type.EventMention;
import edu.cmu.cs.lti.script.type.EventMentionArgumentLink;
import edu.cmu.cs.lti.script.type.StanfordCorenlpSentence;
import gnu.trove.map.TObjectFloatMap;
import gnu.trove.map.hash.TObjectFloatHashMap;
import org.apache.uima.fit.util.FSCollectionFactory;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * Date: 4/29/15
 * Time: 7:42 PM
 *
 * @author Zhengzhong Liu
 */
public class MentionPairFeatureExtractor {
    //TODO speed up using hash kernel

    Map<EventMention, StanfordCorenlpSentence> evm2SentMap = new HashMap<>();

    public MentionPairFeatureExtractor(JCas aJCas) {
        for (StanfordCorenlpSentence sent : JCasUtil.select(aJCas, StanfordCorenlpSentence.class)) {
            for (EventMention mention : JCasUtil.selectCovered(EventMention.class, sent)) {
                evm2SentMap.put(mention, sent);
            }
        }
    }

    public TObjectFloatMap<String> getFeatures(EventMention antecedent, EventMention anaphora) {
        TObjectFloatMap<String> features = new TObjectFloatHashMap<>();
        features.putAll(lexicalFeatures(antecedent, anaphora));
        features.putAll(argumentFeatures(antecedent, anaphora));
        features.putAll(distanceFeatures(antecedent, anaphora));
        return features;
    }

    public TObjectFloatMap<String> lexicalFeatures(EventMention antecedent, EventMention anaphora) {
        TObjectFloatMap<String> features = new TObjectFloatHashMap<>();
        if (antecedent.getHeadWord().getLemma().toLowerCase().equals(anaphora.getHeadWord().getLemma())) {
            features.put("headMatch", 1);
        }
        return features;
    }


    public TObjectFloatMap<String> argumentFeatures(EventMention antecedent, EventMention anaphora) {
        TObjectFloatMap<String> features = new TObjectFloatHashMap<>();

        Map<String, EntityMention> antecedentPropbankMentions = new HashMap<>();
        Map<String, EntityMention> anaphoraPropbankMentions = new HashMap<>();

        for (EventMentionArgumentLink antecedentArgLink : getArguments(antecedent)) {
            antecedentPropbankMentions.put(antecedentArgLink.getPropbankRoleName(), antecedentArgLink.getArgument());
        }

        for (EventMentionArgumentLink anaphoraArgLink : getArguments(anaphora)) {
            anaphoraPropbankMentions.put(anaphoraArgLink.getPropbankRoleName(), anaphoraArgLink.getArgument());
        }

        for (Map.Entry<String, EntityMention> antecedentPropbankMention : antecedentPropbankMentions.entrySet()) {
            String propbankRole = antecedentPropbankMention.getKey();
            EntityMention antMention = antecedentPropbankMention.getValue();
            if (anaphoraPropbankMentions.containsKey(propbankRole)) {
                String featureTemplate = "argument_" + propbankRole;
                EntityMention anaMention = anaphoraPropbankMentions.get(propbankRole);
                if (antMention.getReferingEntity() == anaMention.getReferingEntity()) {
                    features.put("argument_" + propbankRole + "_coref", 1);
                }
                if (antMention.getHead().getLemma().toLowerCase().equals(anaMention.getHead().getLemma().toLowerCase())) {
                    features.put("argument_" + propbankRole + "_headMatch", 1);
                }
            }
        }

        return features;
    }

    private TObjectFloatMap<String> distanceFeatures(EventMention antecedent, EventMention anaphora) {
        TObjectFloatMap<String> features = new TObjectFloatHashMap<>();

        int antecedentSentId = Integer.parseInt(evm2SentMap.get(antecedent).getId());
        int anaphoraSentId = Integer.parseInt((evm2SentMap.get(anaphora)).getId());

        int diff = anaphoraSentId - antecedentSentId;

        if (diff == 0) {
            features.put("sameSentence", 1);
        } else if (diff == 1) {
            features.put("previousSentence", 1);
            if (antecedentSentId == 0) {
                features.put("top2Sentence", 1);
            }
        } else if (diff < 5) {
            features.put("within5Sentence", 1);
        }

        return features;
    }

    private List<EventMentionArgumentLink> getArguments(EventMention mention) {
        if (mention.getArguments() != null) {
            return new ArrayList<>(FSCollectionFactory.create(mention.getArguments(), EventMentionArgumentLink.class));
        } else {
            return new ArrayList<>();
        }
    }
}
