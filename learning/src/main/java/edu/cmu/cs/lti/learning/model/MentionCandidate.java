package edu.cmu.cs.lti.learning.model;

import edu.cmu.cs.lti.learning.utils.MentionTypeUtils;
import edu.cmu.cs.lti.script.type.Sentence;
import edu.cmu.cs.lti.script.type.Word;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.EqualsBuilder;

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

    public static final String REALIS_ROOT = "ROOT";

    public static final String TYPE_ROOT = "ROOT";

    public static final List<DecodingResult> rootKey;

    static {
        rootKey = new ArrayList<>();
        rootKey.add(new DecodingResult(0, 0, TYPE_ROOT, REALIS_ROOT));
    }

//    public final DecodingResult selfKey;

    public MentionCandidate(int begin, int end, Sentence containedSentence, Word headWord) {
        this.begin = begin;
        this.containedSentence = containedSentence;
        this.end = end;
        this.headWord = headWord;
//        this.selfKey = new DecodingResult(begin, end, realis, mentionType);
    }

    public String toString() {
        return String.format("%s,[%s]_[%s],[%d,%d]", headWord.getCoveredText(), mentionType, realis, begin, end);
    }

    public static boolean isRootKey(List<DecodingResult> key) {
        return key.size() > 1 && key.get(0).equals(rootKey.get(0));
    }

    /**
     * This is basically a hashable, serializable representation of the candidate.
     */
    public static class DecodingResult {
        private int begin;
        private int end;
        private String realis;
        private String mentionType;

        public DecodingResult(int begin, int end, String mentionType, String realis) {
            this.begin = begin;
            this.end = end;
            this.mentionType = mentionType;
            this.realis = realis;
        }

        public int hashCode() {
            return new HashCodeBuilder(17, 37).append(begin).append(end).append(realis).append(mentionType)
                    .toHashCode();
        }

        public boolean equals(Object o) {
            if (!(o instanceof DecodingResult)) {
                return false;
            }
            DecodingResult otherKey = (DecodingResult) o;

            return new EqualsBuilder().append(begin, otherKey.begin).append(end, otherKey.end)
                    .append(realis, otherKey.realis).append(mentionType, otherKey.mentionType).build();
        }

        public String getRealis() {
            return realis;
        }

        public int getBegin() {
            return begin;
        }

        public int getEnd() {
            return end;
        }

        public String getMentionType() {
            return mentionType;
        }

        public String toString() {
            return String.format("[Result]_[%d:%d]_[%s,%s]", begin, end, realis, mentionType);
        }
    }

    public static List<DecodingResult> getRootKey() {
        return rootKey;
    }

    public List<DecodingResult> asKey() {
        // Make sure the key is up to date.

        MentionTypeUtils.splitToTmultipleTypes(mentionType);

        List<DecodingResult> keys = new ArrayList<>();

        for (String t : MentionTypeUtils.splitToTmultipleTypes(mentionType)) {
            keys.add(new DecodingResult(begin, end, t, realis));
        }

        return keys;
    }

    public int getBegin() {
        return begin;
    }

    public void setBegin(int begin) {
        this.begin = begin;
    }

    public int getEnd() {
        return end;
    }

    public void setEnd(int end) {
        this.end = end;
    }

    public Word getHeadWord() {
        return headWord;
    }

    public void setHeadWord(Word headWord) {
        this.headWord = headWord;
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
    }

    public Sentence getContainedSentence() {
        return containedSentence;
    }

    public void setContainedSentence(Sentence containedSentence) {
        this.containedSentence = containedSentence;
    }
}
