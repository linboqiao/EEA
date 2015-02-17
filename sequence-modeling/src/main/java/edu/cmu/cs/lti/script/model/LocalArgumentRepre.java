package edu.cmu.cs.lti.script.model;

public class LocalArgumentRepre {
    private final int entityId;
    private final String headWord;
    Integer rewrittenId;
    private final boolean isConcrete;

    public static final String UNKNOWN_HEAD = "UNKNOWN";

    public LocalArgumentRepre(int entityId, String headWord, int rewrittenId, boolean isConcrete) {
        this.entityId = entityId;
        this.headWord = headWord;
        this.rewrittenId = rewrittenId;
        this.isConcrete = isConcrete;
    }

    /**
     * Used when rewriting a real mention
     *
     * @param entityId
     * @param rewrittenId
     */
    public LocalArgumentRepre(int entityId, int rewrittenId) {
        this(entityId, UNKNOWN_HEAD, rewrittenId, true);
    }

    /**
     * Used to directly transform from event mentions
     *
     * @param entityId
     * @param headWord
     */
    public LocalArgumentRepre(int entityId, String headWord) {
        this(entityId, headWord, -1, true);
    }

    public int getEntityId() {
        return entityId;
    }

    public int getRewrittenId() {
        return rewrittenId;
    }

    public String getHeadWord() {
        return headWord;
    }

    public void setRewrittenId(int rewrittenId) {
        this.rewrittenId = rewrittenId;
    }

    public String toString() {
        return headWord + "_" + entityId + ":" + rewrittenId;
    }

    public boolean isConcrete() {
        return isConcrete;
    }

    public boolean isOther() {
        return rewrittenId == KmTargetConstants.otherMarker;
    }

    public boolean mooneyMatch(LocalArgumentRepre repre) {
        return repre.getRewrittenId() == this.rewrittenId;
    }
}