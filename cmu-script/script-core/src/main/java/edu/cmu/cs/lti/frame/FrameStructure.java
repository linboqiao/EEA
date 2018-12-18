package edu.cmu.cs.lti.frame;

import edu.cmu.cs.lti.script.type.SemaforLabel;

import java.util.ArrayList;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * Date: 10/20/17
 * Time: 7:49 PM
 *
 * @author Zhengzhong Liu
 */
public class FrameStructure {
    private String frameName;

    private List<SemaforLabel> frameElements;

    private List<String> superFeNames;

    private SemaforLabel target;

    public FrameStructure(String frameName) {
        this.frameName = frameName;
        this.frameElements = new ArrayList<>();
        this.superFeNames = new ArrayList<>();
    }

    public void setTarget(SemaforLabel label) {
        this.target = label;
    }

    public void addFrameElement(SemaforLabel label, String superFeName) {
        this.frameElements.add(label);
        this.superFeNames.add(superFeName);
    }

    public String getFrameName() {
        return frameName;
    }

    public List<SemaforLabel> getFrameElements() {
        return frameElements;
    }

    public List<String> getSuperFeNames() {
        return superFeNames;
    }

    public SemaforLabel getTarget() {
        return target;
    }
}
