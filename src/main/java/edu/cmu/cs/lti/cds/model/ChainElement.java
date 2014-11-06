package edu.cmu.cs.lti.cds.model;

import edu.cmu.cs.lti.script.type.Sentence;

/**
 * Created with IntelliJ IDEA.
 * User: zhengzhongliu
 * Date: 11/2/14
 * Time: 5:11 PM
 */
public class ChainElement {

    private Sentence sent;

    private LocalEventMentionRepre mention;

    public ChainElement(Sentence sent, LocalEventMentionRepre mention) {
        this.sent = sent;
        this.mention = mention;
    }

    public Sentence getSent() {
        return sent;
    }

    public void setSent(Sentence sent) {
        this.sent = sent;
    }


    public LocalEventMentionRepre getMention() {
        return mention;
    }

    public void setMention(LocalEventMentionRepre mention) {
        this.mention = mention;
    }

    public String toString() {
        return mention + "@" + sent.getId();
    }


//    public boolean isBeginningOfDocument() {
//        return isBeginningOfDocument;
//    }
//
//    public boolean isEndOfDocument() {
//        return isEndOfDocument;
//    }
}
