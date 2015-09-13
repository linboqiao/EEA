package edu.cmu.cs.lti.learning.feature.sentence.generator.impl;

import com.google.common.collect.ArrayListMultimap;
import edu.cmu.cs.lti.learning.feature.sentence.generator.EventMentionFeatureGenerator;
import edu.cmu.cs.lti.emd.utils.WordNetSenseIdentifier;
import edu.cmu.cs.lti.script.type.CandidateEventMention;
import edu.cmu.cs.lti.script.type.Dependency;
import edu.cmu.cs.lti.script.type.StanfordCorenlpToken;
import org.apache.uima.fit.util.FSCollectionFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Created with IntelliJ IDEA.
 * User: zhengzhongliu
 * Date: 3/5/15
 * Time: 2:19 PM
 */
public class HeadWordFeatureGenerator extends EventMentionFeatureGenerator {

    ArrayListMultimap<String, String> brownClusters;
    WordNetSenseIdentifier wnsi;

    public HeadWordFeatureGenerator(ArrayListMultimap<String, String> brownClusters, WordNetSenseIdentifier wnsi,
                                    Set<String> featureSubset) {
        super(featureSubset);
        this.brownClusters = brownClusters;
        this.wnsi = wnsi;
    }

    @Override
    public Map<String, Double> genFeatures(CandidateEventMention mention) {
        Map<String, Double> features = new HashMap<>();
        StanfordCorenlpToken headWord = mention.getHeadWord();
        addHeadWordSurfaceFeatures(headWord, features);
        addHeadWordChildrenDepedencyFeatures(headWord, features);
        addHeadWordGovDepedencyFeatures(headWord, features);
        return features;
    }

    private void addHeadWordSurfaceFeatures(StanfordCorenlpToken headWord, Map<String, Double> features) {
        String lemma = headWord.getLemma().toLowerCase();
        addFeature("TriggerHeadLemma", lemma, features);
        addFeature("HeadPOS", headWord.getPos(), features);
        if (brownClusters.containsKey(lemma)) {
            for (String brownClusterLabel : brownClusters.get(lemma)) {
                addFeature("HeadWordLemmaBrownCluster", brownClusterLabel, features);
            }
        }
        if (brownClusters.containsKey(headWord.getCoveredText())) {
            for (String brownClusterLabel : brownClusters.get(lemma)) {
                addFeature("HeadWordSurfaceBrownCluster", brownClusterLabel, features);
            }
        }
        if (headWord.getNerTag() != null) {
            addFeature("HeadNer", headWord.getNerTag(), features);
        }
    }

    private void addHeadWordGovDepedencyFeatures(StanfordCorenlpToken triggerWord, Map<String, Double> features) {
        if (triggerWord.getHeadDependencyRelations() == null) {
            return;
        }

        for (Dependency dep : FSCollectionFactory.create(triggerWord.getHeadDependencyRelations(), Dependency.class)) {
            String headLemma = dep.getHead().getLemma();
            String headSurface = dep.getHead().getCoveredText();

            addFeature("HeadDepLemma", dep.getDependencyType() + "_" + headLemma, features);
            if (dep.getHead().getNerTag() != null) {
                addFeature("HeadDepNer", dep.getDependencyType() + "_" + dep.getHead().getNerTag(), features);
            }

            addFeature("HeadDepPos", dep.getDependencyType() + "_" + dep.getHead().getPos(), features);

            if (brownClusters.containsKey(headLemma)) {
                for (String brownClusterLabel : brownClusters.get(headLemma)) {
                    addFeature("HeadDepLemmaBrownCluster", brownClusterLabel, features);
                }
            }

            if (brownClusters.containsKey(headSurface)) {
                for (String brownClusterLabel : brownClusters.get(headLemma)) {
                    addFeature("HeadDepSurfaceBrownCluster", brownClusterLabel, features);
                }
            }

            for (String superType : wnsi.getInterestingSupertype(headLemma.toLowerCase())) {
                addFeature("HeadDepLemmaSuperType", superType, features);
                addFeature("HeadDepLemmaSuperTypeWithLemma", superType + "_" +
                        dep.getDependencyType() + "_" + triggerWord.getLemma().toLowerCase(), features);
                addFeature("HeadDepLemmaSuperTypeWithDepLemma,", superType +
                        "_get_" + triggerWord.getLemma().toLowerCase(), features);
            }
        }
    }

    private void addHeadWordChildrenDepedencyFeatures(StanfordCorenlpToken triggerWord, Map<String, Double> features) {
        if (triggerWord.getChildDependencyRelations() == null) {
            return;
        }

        for (Dependency dep : FSCollectionFactory.create(triggerWord.getChildDependencyRelations(), Dependency.class)) {
            String childLemma = dep.getChild().getLemma();
            String childSurface = dep.getChild().getCoveredText();

            addFeature("ChildDepLemma", dep.getDependencyType() + "_" + childLemma, features);
            if (dep.getChild().getNerTag() != null) {
                addFeature("ChildDepNer", dep.getDependencyType() + "_" + dep.getChild().getNerTag(), features);
            }

            addFeature("ChildDepPos", dep.getDependencyType() + "_" + dep.getChild().getPos(), features);

            if (brownClusters.containsKey(childLemma)) {
                for (String brownClusterLabel : brownClusters.get(childLemma)) {
                    addFeature("ChildDepLemmaBrownCluster", brownClusterLabel, features);
                }
            }

            if (brownClusters.containsKey(childSurface)) {
                for (String brownClusterLabel : brownClusters.get(childLemma)) {
                    addFeature("ChildDepSurfaceBrownCluster", brownClusterLabel, features);
                }
            }

            for (String superType : wnsi.getInterestingSupertype(childLemma.toLowerCase())) {
                addFeature("ChildDepLemmaSuperType", superType, features);
                addFeature("ChildDepLemmaSuperTypeWithLemma", superType + "_get_" + triggerWord.getLemma()
                        .toLowerCase(), features);
                addFeature("ChildDepLemmaSuperTypeWithDepLemma", superType + "_" +
                        dep.getDependencyType() + "_" + triggerWord.getLemma().toLowerCase(), features);
            }
        }

    }

//    private void addFeature(String featureName, Map<String, Double> features) {
//        features.put(featureName, 1.0);
//    }
}