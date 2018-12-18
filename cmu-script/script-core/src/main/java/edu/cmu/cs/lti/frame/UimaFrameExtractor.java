package edu.cmu.cs.lti.frame;

import edu.cmu.cs.lti.model.FrameNode;
import edu.cmu.cs.lti.script.type.*;
import edu.cmu.cs.lti.uima.util.UimaNlpUtils;
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
    private final boolean keepVerbs;

    private Map<String, FrameNode.FrameElement> feByName;

    private Set<String> targetFrames;

    public UimaFrameExtractor(Map<String, FrameNode.FrameElement> feByName, Set<String> targetFrames, boolean
            keepVerbs) {
        this.feByName = feByName;
        this.targetFrames = targetFrames;
        this.keepVerbs = keepVerbs;
    }

    public List<FrameStructure> getTargetFrames(ComponentAnnotation annotation) {
        List<FrameStructure> frameStructures = new ArrayList<>();
        for (FrameStructure frameStructure : getFrames(annotation)) {
            boolean keepFrame = false;

            if (keepVerbs) {
                SemaforLabel target = frameStructure.getTarget();
                StanfordCorenlpToken head = UimaNlpUtils.findHeadFromStanfordAnnotation(target);
                if (head.getPos().startsWith("V")) {
                    keepFrame = true;
                }
            }

            String frameName = frameStructure.getFrameName();
            if (targetFrames.contains(frameName)) {
                keepFrame = true;
            }

            if (keepFrame) {
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
