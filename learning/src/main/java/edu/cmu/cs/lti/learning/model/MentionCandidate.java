package edu.cmu.cs.lti.learning.model;

import edu.cmu.cs.lti.learning.utils.MentionTypeUtils;
import edu.cmu.cs.lti.script.type.Sentence;
import edu.cmu.cs.lti.script.type.Word;

/**
 * Contains information useful to extract features about a mention candidate.
 *
 * @author Zhengzhong Liu
 */
public class MentionCandidate {
    private int begin;
    private int end;
    private Word headWord;
    private Sentence containedSentence;
    private String realis;
    private String mentionType;
    private int candidateIndex;
    private boolean isEvent;

    private MultiNodeKey key;

    // TODO check whether the index is used correctly.
    public MentionCandidate(int begin, int end, Sentence containedSentence, Word headWord, int candidateIndex) {
        this.begin = begin;
        this.containedSentence = containedSentence;
        this.end = end;
        this.headWord = headWord;
        this.candidateIndex = candidateIndex;
        isEvent = false;
    }

    public String toString() {
        return String.format("%s,[%s]_[%s],[%d,%d]", headWord.getCoveredText(), mentionType, realis, begin, end);
    }

    public MultiNodeKey asKey() {
        if (key == null) {
            makeKey();
        }
        return key;
    }

    private void makeKey() {
        String[] types = MentionTypeUtils.splitToMultipleTypes(mentionType);
        NodeKey[] singleKeys = new NodeKey[types.length];
        for (int i = 0; i < types.length; i++) {
            singleKeys[i] = new NodeKey(begin, end, types[i], realis, candidateIndex);
        }
        key = new MultiNodeKey(mentionType, singleKeys);
    }

    public int getBegin() {
        return begin;
    }

    public int getEnd() {
        return end;
    }

    public Word getHeadWord() {
        return headWord;
    }

    public String getRealis() {
        return realis;
    }

    public void setRealis(String realis) {
        this.realis = realis;
    }

    public String getMentionType() {
        return mentionType;
    }

    public void setMentionType(String mentionType) {
        this.mentionType = mentionType;
        makeKey();
        if (!mentionType.equals(ClassAlphabet.noneOfTheAboveClass)) {
            setEvent(true);
        }
    }

    public Sentence getContainedSentence() {
        return containedSentence;
    }

    public boolean isEvent() {
        return isEvent;
    }

    public void setEvent(boolean event) {
        isEvent = event;
    }

//    public int getMentionIndex() {
//        return mentionIndex;
//    }
}
