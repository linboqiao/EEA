package edu.cmu.cs.lti.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * Date: 10/19/17
 * Time: 3:51 PM
 *
 * @author Zhengzhong Liu
 */
public class FrameNode {
    private String frameName;

    private FrameNode superFrame;
    private List<FrameNode> subFrames;

    private List<FrameElement> frameElements;

    public FrameNode(String frameName) {
        this.frameName = frameName;
        subFrames = new ArrayList<>();
        frameElements = new ArrayList<>();
    }

    public FrameElement addFrameElement(String feName) {
        FrameElement fe = new FrameElement(this, feName);
        this.frameElements.add(fe);
        return fe;
    }

    public void addSubFrame(FrameNode childNode) {
        this.subFrames.add(childNode);
        childNode.superFrame = this;
    }

    public List<FrameNode> getSubFrames() {
        return subFrames;
    }

    public String getFrameName() {
        return frameName;
    }


    public class FrameElement {
        private String feName;
        private FrameElement superFrameElement;
        private List<FrameElement> subFrameElements;
        private FrameNode frameNode;

        public FrameElement(FrameNode frameNode, String feName) {
            this.frameNode = frameNode;
            this.feName = feName;
            this.subFrameElements = new ArrayList<>();
            this.superFrameElement = null;
        }

        public List<FrameElement> getSubFrameElements() {
            return this.subFrameElements;
        }

        public FrameElement getSuperFrameElement() {
            return superFrameElement;
        }

        public String getFeName() {
            return feName;
        }

        public String getFullFeName() {
            return frameNode.getFrameName() + "." + feName;
        }

        public void addSubFrameElement(FrameElement fe) {
            subFrameElements.add(fe);
            fe.superFrameElement = this;
        }
    }
}
