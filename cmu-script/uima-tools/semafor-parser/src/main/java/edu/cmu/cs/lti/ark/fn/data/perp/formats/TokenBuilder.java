package edu.cmu.cs.lti.ark.fn.data.perp.formats;

import edu.cmu.cs.lti.ark.fn.data.prep.formats.Token;

/**
 * Created with IntelliJ IDEA.
 * Date: 3/29/15
 * Time: 11:12 PM
 *
 * @author Zhengzhong Liu
 */
public class TokenBuilder {
    private
    String lemma;
    private
    Integer id;
    private String form;
    private
    String cpostag;
    private
    String postag;
    private
    String feats;
    private
    Integer head;
    private
    String deprel;
    private
    Integer phead;
    private
    String pdeprel;

    private TokenBuilder() {
    }

    public static TokenBuilder aToken(Token t) {
        return aToken().withLemma(t.getLemma()).withId(t.getId()).withForm(t.getForm()).withCpostag(t.getCpostag()).withPostag(t.getPostag()).
                withFeats(t.getFeats()).withHead(t.getHead()).withDeprel(t.getDeprel()).withPhead(t.getPhead()).withPdeprel(t.getPdeprel());
    }

    public static TokenBuilder aToken() {
        return new TokenBuilder();
    }

    public TokenBuilder withLemma(String lemma) {
        this.lemma = lemma;
        return this;
    }

    public TokenBuilder withId(Integer id) {
        this.id = id;
        return this;
    }

    public TokenBuilder withForm(String form) {
        this.form = form;
        return this;
    }

    public TokenBuilder withCpostag(String cpostag) {
        this.cpostag = cpostag;
        return this;
    }

    public TokenBuilder withPostag(String postag) {
        this.postag = postag;
        return this;
    }

    public TokenBuilder withFeats(String feats) {
        this.feats = feats;
        return this;
    }

    public TokenBuilder withHead(Integer head) {
        this.head = head;
        return this;
    }

    public TokenBuilder withDeprel(String deprel) {
        this.deprel = deprel;
        return this;
    }

    public TokenBuilder withPhead(Integer phead) {
        this.phead = phead;
        return this;
    }

    public TokenBuilder withPdeprel(String pdeprel) {
        this.pdeprel = pdeprel;
        return this;
    }

    public TokenBuilder but() {
        return aToken().withLemma(lemma).withId(id).withForm(form).withCpostag(cpostag).withPostag(postag).withFeats(feats).withHead(head).withDeprel(deprel).withPhead(phead).withPdeprel(pdeprel);
    }

    public Token build() {
        Token token = new Token(id, form, lemma, cpostag, postag, feats, head, deprel, phead, pdeprel);
        return token;
    }
}
