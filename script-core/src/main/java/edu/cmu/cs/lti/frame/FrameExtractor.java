package edu.cmu.cs.lti.frame;

import com.google.common.collect.TreeTraverser;
import edu.cmu.cs.lti.model.FrameNode;
import edu.cmu.cs.lti.script.type.ComponentAnnotation;
import edu.cmu.cs.lti.script.type.SemaforAnnotationSet;
import edu.cmu.cs.lti.script.type.SemaforLabel;
import edu.cmu.cs.lti.script.type.SemaforLayer;
import org.apache.uima.fit.util.FSCollectionFactory;
import org.apache.uima.fit.util.JCasUtil;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.Namespace;
import org.jdom2.input.SAXBuilder;

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
    private Set<String> targetFrames;

    public FrameExtractor(String fnRelatonPath) throws JDOMException, IOException {
        frameByName = readFnRelations(fnRelatonPath);
    }

    public List<FrameStructure> getTargetFrames(ComponentAnnotation annotation) {
        List<FrameStructure> frameStructures = new ArrayList<>();
        for (FrameStructure frameStructure : getFrames(annotation)) {
            String frameName = frameStructure.getFrameName();
            if (targetFrames.contains(frameName)) {
                frameStructures.add(frameStructure);
            }
        }
        return frameStructures;
    }

    public FrameExtractor setTargetFrames(Set<String> targetFrames) {
        this.targetFrames = targetFrames;
        return this;
    }

    public FrameExtractor setSubframeAsTarget(String superFrameName) {
        this.targetFrames = getAllInHeritedFrameNames(superFrameName);
        return this;
    }

    private Set<String> getAllInHeritedFrameNames(String superFrameName) {
        Set<String> childFrameNames = new HashSet<>();
        for (FrameNode frameNode : getAllInherited(superFrameName)) {
            childFrameNames.add(frameNode.getFrameName());
        }
        return childFrameNames;
    }

    private List<FrameStructure> getFrames(ComponentAnnotation annotation) {
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

    private Iterable<FrameNode> getAllInherited(String superFrameName) {
        FrameNode superFrame = frameByName.get(superFrameName);
        TreeTraverser<FrameNode> traverser = TreeTraverser.using(FrameNode::getInheritedBy);
        return traverser.breadthFirstTraversal(superFrame);
    }

    private static Map<String, FrameNode> readFnRelations(String fnRelatonPath) throws JDOMException, IOException {
        Map<String, FrameNode> frameByName = new HashMap<>();

        SAXBuilder builder = new SAXBuilder();
        builder.setDTDHandler(null);
        Document doc = builder.build(fnRelatonPath);
        Element data = doc.getRootElement();
        Namespace ns = data.getNamespace();

        List<Element> relationTypeGroup = data.getChildren("frameRelationType", ns);

        for (Element relationsByType : relationTypeGroup) {
            String relationType = relationsByType.getAttributeValue("name");
            List<Element> frameRelations = relationsByType.getChildren("frameRelation", ns);
            for (Element frameRelation : frameRelations) {
                String subFrameName = frameRelation.getAttributeValue("subFrameName");
                String superFrameName = frameRelation.getAttributeValue("superFrameName");
                FrameNode subNode = getOrCreateNode(frameByName, subFrameName);
                FrameNode superNode = getOrCreateNode(frameByName, superFrameName);
                if (relationType.equals("Inheritance")) {
                    superNode.addInheritence(subNode);
                }
            }
        }

        return frameByName;
    }

    private static FrameNode getOrCreateNode(Map<String, FrameNode> frameByName, String frameName) {
        if (frameByName.containsKey(frameName)) {
            return frameByName.get(frameName);
        } else {
            FrameNode node = new FrameNode(frameName);
            frameByName.put(frameName, node);
            return node;
        }
    }
}
