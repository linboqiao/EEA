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
    private Map<String, FrameNode.FrameElement> feByName;

    private Set<String> targetFrames;

    public FrameExtractor(String fnRelatonPath) throws JDOMException, IOException {
        frameByName = new HashMap<>();
        feByName = new HashMap<>();
        readFnRelations(fnRelatonPath, "Inheritance");
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
        this.targetFrames = getAllSubFrameNames(superFrameName);
        System.out.println("Number of target frames " + targetFrames.size());
        return this;
    }

    private Set<String> getAllSubFrameNames(String superFrameName) {
        Set<String> childFrameNames = new HashSet<>();
        for (FrameNode frameNode : getAllSubFrames(superFrameName)) {
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
                        String fullFeName = getFullFeName(fs.getFrameName(), label.getName());

                        if (feByName.containsKey(fullFeName)) {
                            FrameNode.FrameElement fe = feByName.get(fullFeName);
                            FrameNode.FrameElement superFe = getSuperFrameName(fe);
                            fullFeName = superFe.getFullFeName();
                        }
                        fs.addFrameElement(label, fullFeName);
                    }
                }
            }
            frameStructures.add(fs);
        }
        return frameStructures;
    }

    private FrameNode.FrameElement getSuperFrameName(FrameNode.FrameElement fe) {
        FrameNode.FrameElement root = fe;

        while (root.getSuperFrameElement() != null) {
            root = root.getSuperFrameElement();
        }
        return root;
    }

    private Iterable<FrameNode> getAllSubFrames(String superFrameName) {
        FrameNode superFrame = frameByName.get(superFrameName);
        TreeTraverser<FrameNode> traverser = TreeTraverser.using(FrameNode::getSubFrames);
        return traverser.breadthFirstTraversal(superFrame);
    }

    private Map<String, FrameNode> readFnRelations(String fnRelatonPath, String targetRelationType)
            throws JDOMException, IOException {
        SAXBuilder builder = new SAXBuilder();
        builder.setDTDHandler(null);
        Document doc = builder.build(fnRelatonPath);
        Element data = doc.getRootElement();
        Namespace ns = data.getNamespace();

        List<Element> relationTypeGroup = data.getChildren("frameRelationType", ns);

        for (Element relationsByType : relationTypeGroup) {
            String relationType = relationsByType.getAttributeValue("name");

            if (relationType.equals(targetRelationType)) {
                List<Element> frameRelations = relationsByType.getChildren("frameRelation", ns);
                for (Element frameRelation : frameRelations) {
                    String subFrameName = frameRelation.getAttributeValue("subFrameName");
                    String superFrameName = frameRelation.getAttributeValue("superFrameName");

                    List<Element> feRelations = frameRelation.getChildren("FERelation", ns);

                    FrameNode subNode = getOrCreateNode(frameByName, subFrameName);
                    FrameNode superNode = getOrCreateNode(frameByName, superFrameName);

                    for (Element feRelation : feRelations) {
                        String subFeName = feRelation.getAttributeValue("subFEName");
                        FrameNode.FrameElement subFe = getOrCreateFe(feByName, subNode, subFeName);

                        String superFeName = feRelation.getAttributeValue("superFEName");
                        FrameNode.FrameElement superFe = getOrCreateFe(feByName, superNode, superFeName);

                        superFe.addSubFrameElement(subFe);
                    }

                    superNode.addSubFrame(subNode);
                }
            }
        }
        return frameByName;
    }

    private FrameNode.FrameElement getOrCreateFe(Map<String, FrameNode.FrameElement> feByName, FrameNode frameNode,
                                                 String feName) {
        String fullFeName = getFullFeName(frameNode.getFrameName(), feName);
        if (feByName.containsKey(fullFeName)) {
            return feByName.get(fullFeName);
        } else {
            FrameNode.FrameElement fe = frameNode.addFrameElement(feName);
            feByName.put(fullFeName, fe);
            return fe;
        }
    }

    private FrameNode getOrCreateNode(Map<String, FrameNode> frameByName, String frameName) {
        if (frameByName.containsKey(frameName)) {
            return frameByName.get(frameName);
        } else {
            FrameNode node = new FrameNode(frameName);
            frameByName.put(frameName, node);
            return node;
        }
    }

    private String getFullFeName(String frameName, String feName) {
        return frameName + "." + feName;
    }
}
