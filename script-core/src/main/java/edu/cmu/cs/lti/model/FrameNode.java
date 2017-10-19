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

    private FrameNode inheritedFrom;
    private List<FrameNode> inheritedBy;

    public FrameNode(String frameName) {
        this.frameName = frameName;
        inheritedBy = new ArrayList<>();
    }

    public void addInheritence(FrameNode childNode) {
        this.inheritedBy.add(childNode);
        childNode.inheritedFrom = this;
    }

    public List<FrameNode> getInheritedBy() {
        return inheritedBy;
    }

    public String getFrameName() {
        return frameName;
    }
}
