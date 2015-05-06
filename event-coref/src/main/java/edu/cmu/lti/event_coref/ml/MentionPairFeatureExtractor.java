package edu.cmu.lti.event_coref.ml;

import edu.cmu.cs.lti.script.type.EntityMention;
import edu.cmu.cs.lti.script.type.EventMention;
import edu.cmu.cs.lti.script.type.EventMentionArgumentLink;
import edu.cmu.cs.lti.script.type.StanfordCorenlpSentence;
import edu.cmu.lti.event_coref.model.graph.Edge;
import gnu.trove.map.TObjectDoubleMap;
import gnu.trove.map.hash.TObjectDoubleHashMap;
import org.apache.uima.fit.util.FSCollectionFactory;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;

import java.io.Serializable;
import java.util.*;

/**
 * Created with IntelliJ IDEA.
 * Date: 4/29/15
 * Time: 7:42 PM
 *
 * @author Zhengzhong Liu
 */
public class MentionPairFeatureExtractor implements Serializable {
    private static final long serialVersionUID = 2717024521605026684L;
    //TODO speed up using hash kernel

    private Map<EventMention, StanfordCorenlpSentence> evm2SentMap = new HashMap<>();

    private TObjectDoubleMap<String> unlabelledFeatures;

    private EnumMap<Edge.EdgeType, TObjectDoubleMap<String>> labelledFeatures;

    public MentionPairFeatureExtractor(JCas aJCas) {
        for (StanfordCorenlpSentence sent : JCasUtil.select(aJCas, StanfordCorenlpSentence.class)) {
            for (EventMention mention : JCasUtil.selectCovered(EventMention.class, sent)) {
                evm2SentMap.put(mention, sent);
            }
        }
    }

    public TObjectDoubleMap<String> getUnlablledFeatures() {
        return unlabelledFeatures;
    }

    public TObjectDoubleMap<String> getFeaturesByType(Edge.EdgeType linkType) {
        return labelledFeatures.get(linkType);
    }


    public EnumMap<Edge.EdgeType, TObjectDoubleMap<String>> getLabelledFeatures() {
        return labelledFeatures;
    }


    public void computeFeatures(EventMention antecedent, EventMention mention) {
        unlabelledFeatures = new TObjectDoubleHashMap<>();
        labelledFeatures = new EnumMap<>(Edge.EdgeType.class);
        lexicalFeatures(antecedent, mention);
        argumentFeatures(antecedent, mention);
        distanceFeatures(antecedent, mention);
    }

    private void lexicalFeatures(EventMention antecedent, EventMention mention) {
        if (antecedent.getHeadWord().getLemma().toLowerCase().equals(mention.getHeadWord().getLemma())) {
            unlabelledFeatures.put("headMatch", 1);
        }
    }

    private void argumentFeatures(EventMention antecedent, EventMention mention) {
        Map<String, EntityMention> antecedentPropbankMentions = new HashMap<>();
        Map<String, EntityMention> mentionPropbankMentions = new HashMap<>();

        for (EventMentionArgumentLink antecedentArgLink : getArguments(antecedent)) {
            antecedentPropbankMentions.put(antecedentArgLink.getPropbankRoleName(), antecedentArgLink.getArgument());
        }

        for (EventMentionArgumentLink mentionArgLink : getArguments(mention)) {
            mentionPropbankMentions.put(mentionArgLink.getPropbankRoleName(), mentionArgLink.getArgument());
        }

        for (Map.Entry<String, EntityMention> antecedentPropbankMention : antecedentPropbankMentions.entrySet()) {
            String propbankRole = antecedentPropbankMention.getKey();
            EntityMention antMention = antecedentPropbankMention.getValue();
            if (mentionPropbankMentions.containsKey(propbankRole)) {
                String featureTemplate = "argument_" + propbankRole;
                EntityMention anaMention = mentionPropbankMentions.get(propbankRole);
                if (antMention.getReferingEntity() == anaMention.getReferingEntity()) {
                    unlabelledFeatures.put(featureTemplate + "_coref", 1);
                }
                if (antMention.getHead().getLemma().toLowerCase().equals(anaMention.getHead().getLemma().toLowerCase())) {
                    unlabelledFeatures.put(featureTemplate + "_headMatch", 1);
                }
            }
        }
    }

    private void distanceFeatures(EventMention antecedent, EventMention mention) {
        int antecedentSentId = Integer.parseInt(evm2SentMap.get(antecedent).getId());
        int mentionSentId = Integer.parseInt((evm2SentMap.get(mention)).getId());

        int diff = mentionSentId - antecedentSentId;

        if (diff == 0) {
            unlabelledFeatures.put("sameSentence", 1);
        } else if (diff == 1) {
            unlabelledFeatures.put("previousSentence", 1);
            if (antecedentSentId == 0) {
                unlabelledFeatures.put("top2Sentence", 1);
            }
        } else if (diff < 5) {
            unlabelledFeatures.put("within5Sentence", 1);
        }
    }

    private List<EventMentionArgumentLink> getArguments(EventMention mention) {
        if (mention.getArguments() != null) {
            return new ArrayList<>(FSCollectionFactory.create(mention.getArguments(), EventMentionArgumentLink.class));
        } else {
            return new ArrayList<>();
        }
    }
}
