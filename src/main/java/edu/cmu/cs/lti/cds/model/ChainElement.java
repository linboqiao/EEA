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

    private final boolean isConcrete;

    public ChainElement(Sentence sent, LocalEventMentionRepre mention, boolean isConcrete) {
        this.sent = sent;
        this.mention = mention;
        this.isConcrete = isConcrete;
    }


    public boolean isConcrete() {
        return isConcrete;
    }

    public ChainElement(Sentence sent, LocalEventMentionRepre mention) {
        this(sent, mention, true);
    }

    public static ChainElement fromMooney(MooneyEventRepre mooneyEventRepre) {
        return new ChainElement(null, LocalEventMentionRepre.fromMooneyMention(mooneyEventRepre), false);
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
}
