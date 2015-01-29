package edu.cmu.cs.lti.emd.learn;

/**
 * Created with IntelliJ IDEA.
 * User: zhengzhongliu
 * Date: 1/28/15
 * Time: 10:20 PM
 */
public class CandidateMention {
    public final String surface;
    public final String[] tokenIds;
    public final String docId;
    public final String headTokenId;

    public CandidateMention(String surface, String[] tokenIds, String headTokenId, String docId) {
        this.surface = surface;
        this.tokenIds = tokenIds;
        this.docId = docId;
        this.headTokenId = headTokenId;
    }
}