package edu.cmu.cs.lti.cds.model;

import edu.cmu.cs.lti.script.type.Sentence;
import edu.cmu.cs.lti.script.type.Word;
import org.apache.uima.jcas.JCas;

/**
 * Created with IntelliJ IDEA.
 * User: zhengzhongliu
 * Date: 11/2/14
 * Time: 5:11 PM
 */
public class ContextElement {
    //some uima types
    private Sentence sent;
    private Word head;
    private JCas jcas;

    private LocalEventMentionRepre mention;

    public ContextElement(JCas jcas, Sentence sent, Word headWord, LocalEventMentionRepre mention) {
        this.sent = sent;
        this.mention = mention;
        this.jcas = jcas;
        this.head = headWord;
    }

    public static ContextElement fromMooney(JCas aJCas, Sentence sent, Word headWord, MooneyEventRepre mooneyEventRepre) {
        return new ContextElement(aJCas, sent, headWord, LocalEventMentionRepre.fromMooneyMention(mooneyEventRepre));
    }

    public Sentence getSent() {
        return sent;
    }

    public Word getHead() {
        return head;
    }

    public JCas getJcas() {
        return jcas;
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
        return mention + "@[sent:" + (sent != null ? sent.getId() : "not_assigned") + "]";
    }
}
