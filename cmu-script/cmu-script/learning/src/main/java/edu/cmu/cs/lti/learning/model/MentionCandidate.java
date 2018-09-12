package edu.cmu.cs.lti.learning.model;

import edu.cmu.cs.lti.learning.utils.MentionTypeUtils;
import edu.cmu.cs.lti.script.type.Sentence;
import edu.cmu.cs.lti.script.type.Word;

import java.util.ArrayList;
import java.util.List;

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
    private int index;
    private boolean isEvent;

    public static final String REALIS_ROOT = "ROOT";

    public static final String TYPE_ROOT = "ROOT";

    public static final List<NodeKey> rootKey;

    static {
        rootKey = new ArrayList<>();
        rootKey.add(new NodeKey(0, 0, TYPE_ROOT, REALIS_ROOT, -1));
    }

    public MentionCandidate(int begin, int end, Sentence containedSentence, Word headWord, int index) {
        this.begin = begin;
        this.containedSentence = containedSentence;
        this.end = end;
        this.headWord = headWord;
        this.index = index;
        isEvent = false;
    }

    public String toString() {
        return String.format("%s,[%s]_[%s],[%d,%d]", headWord.getCoveredText(), mentionType, realis, begin, end);
    }

    public static boolean isRootKey(List<NodeKey> key) {
        return key.size() > 0 && key.get(0).equals(rootKey.get(0));
    }

    public static List<NodeKey> getRootKey() {
        return rootKey;
    }

    public List<NodeKey> asKey() {
        // Make sure the key is up to date.

        MentionTypeUtils.splitToTmultipleTypes(mentionType);

        List<NodeKey> keys = new ArrayList<>();

        for (String t : MentionTypeUtils.splitToTmultipleTypes(mentionType)) {
            keys.add(new NodeKey(begin, end, t, realis, index));
        }

        return keys;
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
}
