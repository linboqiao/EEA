package edu.cmu.cs.lti.emd.utils;

import edu.cmu.cs.lti.ling.WordNetSearcher;
import edu.cmu.cs.lti.script.type.StanfordCorenlpToken;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Created with IntelliJ IDEA.
 * User: zhengzhongliu
 * Date: 3/5/15
 * Time: 2:46 PM
 */
public class WordNetSenseIdentifier {
    private WordNetSearcher wns;

    private String[] injuryRelatedSenses = {"body_part"};
    private String[] physicalSense = {"artifact", "whole", "component"};
    private String[] intangibleAssets = {"possession", "transferred_property", "liabilities", "assets"};

    public WordNetSenseIdentifier(WordNetSearcher wns) {
        this.wns = wns;
    }

    public boolean isHumanProunoun(StanfordCorenlpToken token) {
        return token.getPos().equals("PPS") && !token.getLemma().equals("it");
    }

    public int getPhysicalStatus(String word) {
        for (Set<String> hypernyms : wns.getAllHypernymsForAllSense(word)) {
            for (String intangible : intangibleAssets) {
                if (hypernyms.contains(intangible)) {
                    return -1;
                }
            }

            for (String physical : physicalSense) {
                if (hypernyms.contains(physical)) {
                    return 1;
                }
            }
        }
        return 0;
    }

    public List<String> getInterestingSupertype(String word) {
        List<String> interestTypes = new ArrayList<>();
        for (String interestingWordType : injuryRelatedSenses) {
            for (Set<String> hypernyms : wns.getAllHypernymsForAllSense(word)) {
                if (hypernyms.contains(interestingWordType)) {
                    interestTypes.add(interestingWordType);
                }
            }
        }
        return interestTypes;
    }

}
