package edu.cmu.cs.lti.emd.learn.feature.sentence;

import edu.cmu.cs.lti.script.type.StanfordCorenlpToken;
import edu.cmu.cs.lti.utils.Configuration;
import gnu.trove.map.TObjectDoubleMap;
import org.apache.uima.jcas.JCas;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.function.Function;

/**
 * Created with IntelliJ IDEA.
 * Date: 8/22/15
 * Time: 10:09 PM
 *
 * @author Zhengzhong Liu
 */
public abstract class SentenceFeatureWithFocus {
    protected final Logger logger;

    Configuration config;

    public static String outsideValue = "<OUTSIDE>";
    public static String startPlaceholder = "<START>";
    public static String endPlaceholder = "<END>";

    /**
     * Evaluate the function on one of the token. It guard against out of boundary values. Special outsideValue will be
     * returned no matter what the function is.
     *
     * @param sequence The list of tokens to evaluate on.
     * @param operator The operator on the token.
     * @param index The index of the token in interested relative to the sequence.
     * @return
     */
    protected String operateWithOutside(List<StanfordCorenlpToken> sequence, Function<StanfordCorenlpToken, String>
            operator, int index) {
        if (index < -1) {
            return outsideValue;
        } else if (index > sequence.size()) {
            return outsideValue;
        }

        if (index == -1) {
            return startPlaceholder;
        }

        if (index == sequence.size()) {
            return endPlaceholder;
        }

        String operatedValue = operator.apply(sequence.get(index));

        return operatedValue == null ? outsideValue : operatedValue;
    }

    public SentenceFeatureWithFocus(Configuration config) {
        this.config = config;
        this.logger = LoggerFactory.getLogger(this.getClass());
        logger.info("Register feature extractor : " + featureName());
    }

    /**
     * A name to describe this feature, it use the full class name by default
     *
     * @return Name of a feature function.
     */
    public String featureName() {
        return this.getClass().getName();
    }

    /**
     * Run once for each document, which does expensive document level preparation.
     *
     * @param context The JCas associated with the document.
     */
    public abstract void initDocumentWorkspace(JCas context);

    /**
     * Run once for each instance (for example, a sentence), which does expensive operation on the instance.
     *
     * @param aJCas The JCas associated with the document.
     * @param begin Specify the sequence range start.
     * @param end   Specify the sequence range end.
     */
    public abstract void resetWorkspace(JCas aJCas, int begin, int end);

    /**
     * The extractor function, which may be run multiple times on each of the focus.
     *
     * @param sequence             The sequence represented as token.
     * @param focus                The focus to evaluate the features.
     * @param features             The local feature vector to be filled.
     * @param featuresNeedForState The feature vector that should depend on a previous state.
     */
    public abstract void extract(List<StanfordCorenlpToken> sequence, int focus,
                                 TObjectDoubleMap<String> features,
                                 TObjectDoubleMap<String> featuresNeedForState);
}
