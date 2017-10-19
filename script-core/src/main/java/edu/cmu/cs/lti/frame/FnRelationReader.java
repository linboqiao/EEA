package edu.cmu.cs.lti.frame;

import edu.cmu.cs.lti.model.FrameNode;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.Namespace;
import org.jdom2.input.SAXBuilder;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * Date: 10/19/17
 * Time: 5:07 PM
 *
 * @author Zhengzhong Liu
 */
public class FnRelationReader {
    public static Map<String, FrameNode> readFnRelations(String fnRelatonPath) throws JDOMException, IOException {
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
