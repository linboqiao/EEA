package edu.cmu.lti.event_coref.ml;

import edu.cmu.cs.lti.script.type.EntityMention;
import edu.cmu.cs.lti.script.type.EventMention;
import edu.cmu.cs.lti.script.type.EventMentionArgumentLink;
import edu.cmu.cs.lti.script.type.StanfordCorenlpSentence;
import edu.cmu.lti.event_coref.model.graph.Edge;
import edu.cmu.lti.event_coref.model.graph.Edge.EdgeType;
import edu.cmu.lti.event_coref.model.graph.Node;
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

    public TObjectDoubleMap<String> getUnlabelledFeatures() {
        return unlabelledFeatures;
    }

    public TObjectDoubleMap<String> getFeaturesByType(Edge.EdgeType linkType) {
        return labelledFeatures.get(linkType);
    }


    public EnumMap<Edge.EdgeType, TObjectDoubleMap<String>> getLabelledFeatures() {
        return labelledFeatures;
    }


    public void computeFeatures(Edge edge, Node antecedent, Node anaphora) {
        unlabelledFeatures = new TObjectDoubleHashMap<>();
        labelledFeatures = new EnumMap<>(Edge.EdgeType.class);

        for (Edge.EdgeType type : Edge.EdgeType.values()) {
            labelledFeatures.put(type, new TObjectDoubleHashMap<String>());
        }

        for (Edge.EdgeType type : Edge.EdgeType.values()) {
            lexicalFeatures(type, antecedent, anaphora);
            argumentFeatures(type, antecedent, anaphora);
            distanceFeatures(type, antecedent, anaphora);
        }
    }

    private void lexicalFeatures(EdgeType type, Node antecedent, Node anaphora) {
        if (antecedent.isRoot()) {
            addLabelledFeature(type, "head_lemma_root_" + anaphora.getMention().getHeadWord().getLemma(), 1);
        } else {
            if (antecedent.getMention().getHeadWord().getLemma().toLowerCase().equals(anaphora.getMention().getHeadWord().getLemma())) {
                addUnlabelledFeature("headMatch");
                addLabelledFeature(type, "headMatch");
            }
        }
    }

    private void argumentFeatures(EdgeType type, Node antecedent, Node anaphora) {
        if (!antecedent.isRoot()) {
            EventMention govMention = antecedent.getMention();
            EventMention depMention = anaphora.getMention();

            Map<String, EntityMention> antecedentPropbankMentions = new HashMap<>();
            Map<String, EntityMention> mentionPropbankMentions = new HashMap<>();

            for (EventMentionArgumentLink antecedentArgLink : getArguments(govMention)) {
                antecedentPropbankMentions.put(antecedentArgLink.getPropbankRoleName(), antecedentArgLink.getArgument());
            }

            for (EventMentionArgumentLink mentionArgLink : getArguments(depMention)) {
                mentionPropbankMentions.put(mentionArgLink.getPropbankRoleName(), mentionArgLink.getArgument());
            }

            for (Map.Entry<String, EntityMention> antecedentPropbankMention : antecedentPropbankMentions.entrySet()) {
                String propbankRole = antecedentPropbankMention.getKey();
                EntityMention antMention = antecedentPropbankMention.getValue();
                if (mentionPropbankMentions.containsKey(propbankRole)) {
                    String featureTemplate = "argument_" + propbankRole;
                    EntityMention anaMention = mentionPropbankMentions.get(propbankRole);
                    if (antMention.getReferingEntity() == anaMention.getReferingEntity()) {
                        addLabelledFeature(type, featureTemplate + "_coref");
                        addUnlabelledFeature(featureTemplate + "_coref");
                    }
                    if (antMention.getHead().getLemma().toLowerCase().equals(anaMention.getHead().getLemma().toLowerCase())) {
                        addLabelledFeature(type, featureTemplate + "_headMatch");
                        addUnlabelledFeature(featureTemplate + "_headMatch");
                    }
                }
            }
        } else {
            //currently no features for root
        }
    }

    private void distanceFeatures(EdgeType type, Node antecedent, Node anaphora) {
        if (!antecedent.isRoot()) {
            EventMention govMention = antecedent.getMention();
            EventMention depMention = anaphora.getMention();

            int antecedentSentId = Integer.parseInt(evm2SentMap.get(govMention).getId());
            int mentionSentId = Integer.parseInt((evm2SentMap.get(depMention)).getId());

            int diff = mentionSentId - antecedentSentId;

            if (diff == 0) {
                addUnlabelledFeature("sameSentence");
                addLabelledFeature(type, "sameSentence");
            } else if (diff == 1) {
                addUnlabelledFeature("previousSentence");
                addLabelledFeature(type, "previousSentence");

                if (antecedentSentId == 0) {
                    addUnlabelledFeature("top2Sentence");
                    addLabelledFeature(type, "top2Sentence");
                }
            } else if (diff < 5) {
                addUnlabelledFeature("within5Sentence");
                addLabelledFeature(type, "within5Sentence");
            }
        }
    }

    private List<EventMentionArgumentLink> getArguments(EventMention mention) {
        if (mention.getArguments() != null) {
            return new ArrayList<>(FSCollectionFactory.create(mention.getArguments(), EventMentionArgumentLink.class));
        } else {
            return new ArrayList<>();
        }
    }

    private void addLabelledFeature(Edge.EdgeType type, String featureName, double val) {
        labelledFeatures.get(type).put(featureName, val);
    }

    private void addLabelledFeature(Edge.EdgeType type, String featureName) {
        addLabelledFeature(type, featureName, 1);
    }

    private void addUnlabelledFeature(String featureName, double val) {
        unlabelledFeatures.put(featureName, val);
    }

    private void addUnlabelledFeature(String featureName) {
        addUnlabelledFeature(featureName, 1);
    }
}
