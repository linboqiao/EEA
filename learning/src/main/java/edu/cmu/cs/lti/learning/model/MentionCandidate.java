package edu.cmu.cs.lti.learning.model;

import edu.cmu.cs.lti.learning.utils.MentionTypeUtils;
import edu.cmu.cs.lti.script.type.Sentence;
import edu.cmu.cs.lti.script.type.Word;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Contains information useful to extract features about a mention candidate.
 *
 * @author Zhengzhong Liu
 */
public class MentionCandidate {
    protected transient final Logger logger = LoggerFactory.getLogger(getClass());

    private int begin;
    private int end;
    private Word headWord;
    private Sentence containedSentence;
    private String realis;
    private String mentionType;
    private int candidateIndex;
    private boolean isEvent;

    private MentionKey key;

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

    public MentionKey asKey() {
        if (key == null) {
            makeKey();
        }
        return key;
    }

    private void makeKey() {
        String[] types = MentionTypeUtils.splitToMultipleTypes(mentionType);
        NodeKey[] singleKeys = new NodeKey[types.length];
//        logger.info("Making key realis is " + realis);
        for (int i = 0; i < types.length; i++) {
            singleKeys[i] = new NodeKey(begin, end, types[i], realis, candidateIndex);
        }
        key = new MentionKey(headWord, mentionType, singleKeys);
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
        if (mentionType != null) {
            makeKey();
        }
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
