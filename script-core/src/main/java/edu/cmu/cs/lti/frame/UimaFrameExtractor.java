package edu.cmu.cs.lti.frame;

import edu.cmu.cs.lti.model.FrameNode;
import edu.cmu.cs.lti.script.type.ComponentAnnotation;
import edu.cmu.cs.lti.script.type.SemaforAnnotationSet;
import edu.cmu.cs.lti.script.type.SemaforLabel;
import edu.cmu.cs.lti.script.type.SemaforLayer;
import org.apache.uima.fit.util.FSCollectionFactory;
import org.apache.uima.fit.util.JCasUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created with IntelliJ IDEA.
 * Date: 11/14/17
 * Time: 4:21 PM
 *
 * @author Zhengzhong Liu
 */
public class UimaFrameExtractor {
    private Map<String, FrameNode.FrameElement> feByName;

    private Set<String> targetFrames;

    public UimaFrameExtractor(Map<String, FrameNode.FrameElement> feByName, Set<String> targetFrames) {
        this.feByName = feByName;
        this.targetFrames = targetFrames;
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

    private String getFullFeName(String frameName, String feName) {
        return frameName + "." + feName;
    }

    private FrameNode.FrameElement getSuperFrameName(FrameNode.FrameElement fe) {
        FrameNode.FrameElement root = fe;

        while (root.getSuperFrameElement() != null) {
            root = root.getSuperFrameElement();
        }
        return root;
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


}
