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

    private SemaforLabel target;

    public FrameStructure(String frameName) {
        this.frameName = frameName;
        this.frameElements = new ArrayList<>();
    }

    public void setTarget(SemaforLabel label) {
        this.target = label;
    }

    public void addFrameElement(SemaforLabel label) {
        this.frameElements.add(label);
    }

    public String getFrameName() {
        return frameName;
    }

    public List<SemaforLabel> getFrameElements() {
        return frameElements;
    }

    public SemaforLabel getTarget() {
        return target;
    }

    //    private class FrameElement {
//        private String name;
//        private Span span;
//
//        public FrameElement(String name, int begin, int end) {
//            this.name = name;
//            this.span = Span.of(begin, end);
//        }
//
//        public String getName() {
//            return name;
//        }
//
//        public Span getSpan() {
//            return span;
//        }
//    }
}
