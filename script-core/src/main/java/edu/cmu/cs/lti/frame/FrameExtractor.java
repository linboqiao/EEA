package edu.cmu.cs.lti.frame;

import com.google.common.collect.TreeTraverser;
import edu.cmu.cs.lti.model.FrameNode;
import org.jdom2.JDOMException;

import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Created with IntelliJ IDEA.
 * Date: 10/19/17
 * Time: 3:35 PM
 *
 * @author Zhengzhong Liu
 */
public class FrameExtractor {
    private Map<String, FrameNode> frameByName;

    public FrameExtractor(String fnRelatonPath) throws JDOMException, IOException {
        frameByName = FnRelationReader.readFnRelations(fnRelatonPath);
    }

    public Set<String> getAllInHeritedFrameNames(String superFrameName) {
        Set<String> childFrameNames = new HashSet<>();
        for (FrameNode frameNode : getAllInherited(superFrameName)) {
            childFrameNames.add(frameNode.getFrameName());
        }
        return childFrameNames;
    }

    public Iterable<FrameNode> getAllInherited(String superFrameName) {
        FrameNode superFrame = frameByName.get(superFrameName);
        TreeTraverser<FrameNode> traverser = TreeTraverser.using(FrameNode::getInheritedBy);
        return traverser.breadthFirstTraversal(superFrame);
    }
}
