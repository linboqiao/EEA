package edu.cmu.cs.lti.cds.ml.features.impl;

import com.google.common.base.Joiner;
import edu.cmu.cs.lti.cds.ml.features.Feature;
import edu.cmu.cs.lti.script.model.ContextElement;
import edu.cmu.cs.lti.script.type.Dependency;
import edu.cmu.cs.lti.script.type.Sentence;
import edu.cmu.cs.lti.script.type.Word;
import org.apache.uima.fit.util.FSCollectionFactory;
import org.apache.uima.jcas.cas.FSList;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: zhengzhongliu
 * Date: 12/1/14
 * Time: 2:57 PM
 */
public class NeighbourDependencyPath extends Feature {
    @Override
    public Map<String, Double> getFeature(ContextElement elementLeft, ContextElement elementRight, int skip) {
        Map<String, Double> features = new HashMap<>();

        if (skip > 3) {
            return features;
        }

        Sentence leftSent = elementLeft.getSent();
        Sentence rightSent = elementRight.getSent();

        int leftSentId = getId(leftSent);
        int rightSentId = getId(rightSent);

        if (leftSentId == rightSentId) {
            String depPath = getDepdencyPath(elementLeft.getHead(), elementRight.getHead(), 2);
            if (depPath != null) {
                features.put(depPath, 1.0);
            }
        }
        return features;
    }

    private String getDepdencyPath(Word leftWord, Word rightWord, int k) {
        List<String> path = getDependencyPath(leftWord, rightWord, k);
        if (path.size() > 0) {
            return Joiner.on("_").join(path);
        }
        path = getDependencyPath(rightWord, leftWord, k);
        if (path.size() > 0) {
            return Joiner.on("_").join(path) + "_inv";
        }
        return null;
    }

    private List<String> getDependencyPath(Word ancenstor, Word descendant, int k) {
        List<String> path = new LinkedList<>();
        if (k == 0) {
            return path;
        }
        FSList childDependencyFS = ancenstor.getChildDependencyRelations();
        if (childDependencyFS != null) {
            for (Dependency childDependency : FSCollectionFactory.create(childDependencyFS, Dependency.class)) {
                if (childDependency.getChild().equals(descendant)) {
                    path.add(childDependency.getDependencyType());
                    break;
                } else {
                    List<String> nextLayerPath = getDependencyPath(childDependency.getChild(), descendant, k - 1);
                    if (nextLayerPath.size() > 0) {
                        path.add(childDependency.getDependencyType());
                        path.addAll(nextLayerPath);
                        break;
                    }
                }
            }
        }
        return path;
    }

    private int getId(Sentence sent) {
        return Integer.parseInt(sent.getId());
    }


    @Override
    public boolean isLexicalized() {
        return true;
    }
}
