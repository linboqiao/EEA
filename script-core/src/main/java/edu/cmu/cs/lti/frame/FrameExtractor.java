package edu.cmu.cs.lti.frame;

import com.google.common.collect.TreeTraverser;
import edu.cmu.cs.lti.model.FrameNode;
import edu.cmu.cs.lti.script.type.ComponentAnnotation;
import edu.cmu.cs.lti.script.type.SemaforAnnotationSet;
import edu.cmu.cs.lti.script.type.SemaforLabel;
import edu.cmu.cs.lti.script.type.SemaforLayer;
import org.apache.uima.fit.util.FSCollectionFactory;
import org.apache.uima.fit.util.JCasUtil;
import org.jdom2.JDOMException;

import java.io.IOException;
import java.util.*;

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

    public List<FrameStructure> getFrames(ComponentAnnotation annotation) {
        List<FrameStructure> frameStructures = new ArrayList<>();

        for (SemaforAnnotationSet annoSet : JCasUtil.selectCovered(SemaforAnnotationSet.class, annotation)) {
            FrameStructure fs = new FrameStructure(annoSet.getFrameName());

            int numLayers = annoSet.getLayers().size();
            for (int i = 0; i < numLayers; i++) {
                SemaforLayer layer = annoSet.getLayers(i);
                if (layer.getName().equals("Target")) {
                    for (SemaforLabel label : FSCollectionFactory.create(layer.getLabels(), SemaforLabel.class)) {
                        fs.setTarget(label);
                    }
                } else {
                    for (SemaforLabel label : FSCollectionFactory.create(layer.getLabels(), SemaforLabel.class)) {
                        fs.addFrameElement(label);
                    }
                }
            }
            frameStructures.add(fs);
        }
        return frameStructures;
    }

    public List<FrameStructure> getFramesOfType(ComponentAnnotation annotation, Set<String> frameTypes) {
        List<FrameStructure> frameStructures = new ArrayList<>();
        for (FrameStructure frameStructure : getFrames(annotation)) {
            String frameName = frameStructure.getFrameName();
            if (frameTypes.contains(frameName)) {
                frameStructures.add(frameStructure);
            }
        }
        return frameStructures;
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
