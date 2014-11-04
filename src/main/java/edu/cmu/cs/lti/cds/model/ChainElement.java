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

//    private boolean isBeginningOfDocument = false;
//    private boolean isEndOfDocument = false;
//
//    public static ChainElement getBeginOfDoc() {
//        ChainElement e = new ChainElement();
//        e.isBeginningOfDocument = true;
//        return e;
//    }
//
//    public static ChainElement getEndOfDoc() {
//        ChainElement e = new ChainElement();
//        e.isEndOfDocument = true;
//        return e;
//    }

//    public ChainElement() {
//        isBeginningOfDocument = true;
//        isEndOfDocument = true;
//    }

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

//    public boolean isBeginningOfDocument() {
//        return isBeginningOfDocument;
//    }
//
//    public boolean isEndOfDocument() {
//        return isEndOfDocument;
//    }
}
