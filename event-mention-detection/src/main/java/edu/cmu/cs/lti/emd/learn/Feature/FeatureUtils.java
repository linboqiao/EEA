package edu.cmu.cs.lti.emd.learn.feature;

import edu.cmu.cs.lti.script.type.StanfordCorenlpToken;
import edu.cmu.cs.lti.script.type.StanfordDependencyRelation;
import org.apache.uima.fit.util.FSCollectionFactory;
import org.apache.uima.jcas.cas.FSList;
import org.javatuples.Pair;

/**
 * Created with IntelliJ IDEA.
 * Date: 8/22/15
 * Time: 11:49 PM
 *
 * @author Zhengzhong Liu
 */
public class FeatureUtils {
    public static String formatFeatureName(Pair<String, String> featureTypeAndName) {
        return formatFeatureName(featureTypeAndName.getValue0(), featureTypeAndName.getValue1());
    }

    public static String formatFeatureName(String featureType, String featureName) {
        return String.format("%s::%s", featureType, featureName);
    }
}
