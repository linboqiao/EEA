package edu.cmu.cs.lti.script.model;

import edu.cmu.cs.lti.script.type.EventMention;
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
    private boolean isTarget;
    private EventMention originalMention;
    private LocalEventMentionRepre mention;

    public ContextElement(JCas jcas, Sentence sent, EventMention originMention, LocalEventMentionRepre mention) {
        this.sent = sent;
        this.mention = mention;
        this.originalMention = originMention;
        this.jcas = jcas;
        this.head = originMention.getHeadWord();
        this.isTarget = false;
    }

    /**
     * Convert the real element to the candidate event mention, removing the gold standard
     * information that will needed to be inferred, which include the headword and argument setting,
     * however, this will tell the candidate event mention some other information, includes:
     * 1. Sentence id
     * 2. Concrete Entity Id in document for the proposed candidate argument
     *
     * @param realElement
     * @param candidateEvm
     * @return
     */
    public static ContextElement eraseGoldStandard(ContextElement realElement, MooneyEventRepre candidateEvm) {
        return new ContextElement(realElement.getJcas(), realElement.getSent(), realElement.getOriginalMention(),
                LocalEventMentionRepre.rewriteUsingCandidateMention(realElement.getMention(), candidateEvm));
    }

//
//    public static ContextElement fromMooney(JCas aJCas, Sentence sent, Word headWord, MooneyEventRepre mooneyEventRepre) {
//        return new ContextElement(aJCas, sent, headWord, LocalEventMentionRepre.fromMooneyMention(mooneyEventRepre));
//    }

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

    public void setIsTarget(boolean isTarget) {
        this.isTarget = isTarget;
    }

    public boolean isTarget() {
        return isTarget;
    }

    public String toString() {
        return mention + "@[sent:" + (sent != null ? sent.getId() : "not_assigned") + "]";
    }

    public EventMention getOriginalMention() {
        return originalMention;
    }
}
