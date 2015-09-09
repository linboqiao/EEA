package edu.cmu.cs.lti.emd.learn.feature.extractor;

import edu.cmu.cs.lti.learning.model.Alphabet;
import edu.cmu.cs.lti.learning.model.ChainFeatureExtractor;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;

/**
 * Created with IntelliJ IDEA.
 * Date: 8/22/15
 * Time: 5:57 PM
 *
 * @author Zhengzhong Liu
 */
public abstract class UimaSequenceFeatureExtractor extends ChainFeatureExtractor {

    protected JCas context;

    public UimaSequenceFeatureExtractor(Alphabet alphabet) {
        super(alphabet);
    }

    /**
     * Called once per document.
     *
     * @param context The underlying context JCas index.
     */
    public void initWorkspace(JCas context) {
        this.context = context;
    }

    /**
     * Called once per sequence.
     *
     * @param aJCas The underlying JCas index.
     * @param begin The begin of the sequence.
     * @param end The end of the sequence.
     */
    public abstract void resetWorkspace(JCas aJCas, int begin, int end);

    /**
     * Called once per sequence.
     *
     * @param annotation The annotation representing the sequence.
     */
    public void resetWorkspace(Annotation annotation) {
        annotation.addToIndexes();
        resetWorkspace((JCas) annotation.getCAS(), annotation.getBegin(), annotation.getEnd());
    }

}
