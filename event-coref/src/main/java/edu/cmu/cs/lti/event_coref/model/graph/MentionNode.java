package edu.cmu.cs.lti.event_coref.model.graph;

import java.io.Serializable;

/**
 * Created with IntelliJ IDEA.
 * Date: 5/1/15
 * Time: 11:28 PM
 * <p>
 * Each mention node contains an event mention and a couple
 *
 * @author Zhengzhong Liu
 */
public class MentionNode implements Serializable {
    private static final long serialVersionUID = 5437802273911493639L;
    private int id;
    private int mentionIndex;

    public MentionNode(int id) {
        this.id = id;
        this.mentionIndex = id - 1;
    }

    public boolean isRoot() {
        return id == 0;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String toString() {
        return String.format("%s:%d", "MentionNode", id);
    }

    public int getMentionIndex() {
        return mentionIndex;
    }
}
