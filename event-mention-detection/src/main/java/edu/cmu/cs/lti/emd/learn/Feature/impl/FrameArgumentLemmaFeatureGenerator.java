package edu.cmu.cs.lti.emd.learn.Feature.impl;

import com.google.common.collect.ArrayListMultimap;
import edu.cmu.cs.lti.emd.learn.Feature.EventMentionFeatureGenerator;
import edu.cmu.cs.lti.emd.utils.WordNetSenseIdentifier;
import edu.cmu.cs.lti.script.type.CandidateEventMention;
import edu.cmu.cs.lti.script.type.CandidateEventMentionArgument;
import edu.cmu.cs.lti.script.type.StanfordCorenlpToken;
import org.apache.uima.fit.util.FSCollectionFactory;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.cas.FSList;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Created with IntelliJ IDEA.
 * User: zhengzhongliu
 * Date: 3/4/15
 * Time: 5:01 PM
 */
public class FrameArgumentLemmaFeatureGenerator extends EventMentionFeatureGenerator {
    private WordNetSenseIdentifier wnsi;
    private ArrayListMultimap<String, String> brownClusters;

    public FrameArgumentLemmaFeatureGenerator(ArrayListMultimap<String, String> brownClusters, WordNetSenseIdentifier wnsi, Set<String> featureSubset) {
        super(featureSubset);
        this.wnsi = wnsi;
        this.brownClusters = brownClusters;
    }

    @Override
    public Map<String, Double> genFeatures(CandidateEventMention mention) {
        Map<String, Double> features = new HashMap<>();

        FSList argumentFs = mention.getArguments();
        if (argumentFs != null) {
            for (CandidateEventMentionArgument argument : FSCollectionFactory.create(argumentFs, CandidateEventMentionArgument.class)) {
                StanfordCorenlpToken argumentHeadWord = argument.getHeadWord();
                addFeature("FrameArgument", argumentHeadWord.getLemma().toLowerCase(), features);
                addFeature("FrameArgumentSubPhrase", mention.getHeadWord().getLemma().toLowerCase() + "_" + argumentHeadWord.getLemma().toLowerCase(), features);
                addFeature("FrameArgumentRole", argument.getRoleName(), features);

                int objectStatus = wnsi.getPhysicalStatus(argumentHeadWord.getLemma().toLowerCase());

                if (objectStatus == 1) {
                    addFeature("FrameArgumentSense", "isPhysical", features);
                } else if (objectStatus == -1) {
                    addFeature("FrameArgumentSense", "isIntangible", features);
                }

                if (isHumanProunoun(argumentHeadWord)) {
                    addFeature("FrameArgumentSense", "isHuman", features);
                }

                if (brownClusters.containsKey(argumentHeadWord.getLemma())) {
                    for (String brownClusterLabel : brownClusters.get(argumentHeadWord.getLemma())) {
                        addFeature("FrameArgumentBrownCluster", brownClusterLabel, features);
                    }
                }

                if (argumentHeadWord.getNerTag() != null) {
                    addFeature("FrameArgumentHeadNer", argumentHeadWord.getNerTag(), features);
                }

                addFeature("FrameArgumentPOS", argumentHeadWord.getPos(), features);

                for (StanfordCorenlpToken token : JCasUtil.selectCovered(StanfordCorenlpToken.class, argument)) {
                    if (token.getNerTag() != null) {
                        addFeature("FrameArgumentSpanNer", token.getNerTag(), features);
                    }
                }
            }
        }
        return features;
    }

    public boolean isHumanProunoun(StanfordCorenlpToken token) {
        return token.getPos().equals("PPS") && !token.getLemma().equals("it");
    }


}