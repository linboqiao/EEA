package edu.cmu.cs.lti.emd.learn.feature.extractor;

import edu.cmu.cs.lti.learning.model.Alphabet;
import edu.cmu.cs.lti.learning.model.ChainFeatureExtractor;
import edu.cmu.cs.lti.script.type.StanfordCorenlpSentence;
import org.apache.uima.jcas.JCas;

/**
 * Created with IntelliJ IDEA.
 * Date: 8/22/15
 * Time: 5:57 PM
 *
 * @author Zhengzhong Liu
 */
public abstract class UimaSentenceFeatureExtractor extends ChainFeatureExtractor {

    protected JCas context;

    protected StanfordCorenlpSentence sentence;

    public UimaSentenceFeatureExtractor(Alphabet alphabet) {
        super(alphabet);
    }

    /**
     * Called once per document.
     * @param context
     */
    public void initWorkspace(JCas context) {
        this.context = context;
    }

    /**
     * Called once per sentence.
     * @param sentence
     */
    public void resetWorkspace(StanfordCorenlpSentence sentence) {
        this.sentence = sentence;
    }
}
