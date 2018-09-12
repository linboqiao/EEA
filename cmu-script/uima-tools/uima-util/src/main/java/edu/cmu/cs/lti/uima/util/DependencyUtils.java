package edu.cmu.cs.lti.uima.util;

import edu.cmu.cs.lti.script.type.Dependency;
import edu.cmu.cs.lti.script.type.Word;
import org.apache.uima.fit.util.FSCollectionFactory;
import org.apache.uima.jcas.cas.FSList;

/**
 * Created with IntelliJ IDEA.
 * Date: 8/23/15
 * Time: 12:05 AM
 *
 * @author Zhengzhong Liu
 */
public class DependencyUtils {
    public static String getTokenParentDependency(Word token) {
        FSList headDependencies = token.getHeadDependencyRelations();
        if (headDependencies == null) {
            return "<ROOT>";
        }

        for (Dependency relation : FSCollectionFactory.create(headDependencies, Dependency.class)) {
            // Effective return the first.
            return relation.getDependencyType();
        }

        return "<ROOT>";
    }
}
