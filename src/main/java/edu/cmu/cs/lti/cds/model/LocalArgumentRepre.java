package edu.cmu.cs.lti.cds.model;

public class LocalArgumentRepre {
    private final int entityId;
    private final String headWord;
    Integer rewritedId;
    private final boolean isConcrete;

    public static final String UNKNOWN_HEAD = "head";

    public LocalArgumentRepre(int entityId, String headWord, int rewritedId, boolean isConcrete) {
        this.entityId = entityId;
        this.headWord = headWord;
        this.rewritedId = rewritedId;
        this.isConcrete = isConcrete;
    }

    public LocalArgumentRepre(int entityId, String headWord) {
        this(entityId, headWord, -1, true);
    }

    public int getEntityId() {
        return entityId;
    }

    public int getRewritedId() {
        return rewritedId;
    }

    public String getHeadWord() {
        return headWord;
    }

    public void setRewritedId(int rewritedId) {
        this.rewritedId = rewritedId;
    }

    public String toString() {
        return headWord + "_" + entityId + ":" + rewritedId;
    }

    public boolean isConcrete() {
        return isConcrete;
    }

    public boolean isRewrite() {
        return rewritedId != null;
    }

    public boolean isOther() {
        return rewritedId == KmTargetConstants.otherMarker;
    }

    public boolean mooneyMatch(LocalArgumentRepre repre) {
        return repre.getRewritedId() == this.rewritedId;
    }
}