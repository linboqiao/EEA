package edu.cmu.cs.lti.learning.feature.extractor;

import edu.cmu.cs.lti.learning.model.FeatureAlphabet;
import edu.cmu.cs.lti.script.type.StanfordCorenlpSentence;
import edu.cmu.cs.lti.script.type.StanfordCorenlpToken;
import edu.cmu.cs.lti.script.type.Word;
import edu.cmu.cs.lti.uima.util.UimaNlpUtils;
import edu.cmu.cs.lti.utils.Configuration;
import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.hash.TObjectIntHashMap;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * While modeling mention features above the sentence level, we need to deal with the offset a little different.
 *
 * @author Zhengzhong Liu
 */
public class MultiSentenceFeatureExtractor<T extends Annotation> extends UimaSequenceFeatureExtractor<T> {
    protected final Logger logger = LoggerFactory.getLogger(getClass());

    // Use to locate the headword and token for mention to sentence transition.
    private Map<T, List<StanfordCorenlpToken>> elements2SentenceTokens;
    private TObjectIntMap<T> headWordOffsets;

    public MultiSentenceFeatureExtractor(FeatureAlphabet alphabet, Configuration generalConfig, Configuration
            sentenceFeatureConfig, Configuration mentionFeatureConfig, boolean useStateFeatures, Class<T>
                                                 elementClass)
            throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException,
            InstantiationException, IllegalAccessException {
        super(alphabet, generalConfig, sentenceFeatureConfig, mentionFeatureConfig, useStateFeatures, elementClass);
    }

    @Override
    public void initWorkspace(JCas context) {
        super.initWorkspace(context);

        elements2SentenceTokens = new HashMap<>();
        headWordOffsets = new TObjectIntHashMap<>();

        for (StanfordCorenlpSentence sentence : JCasUtil.select(context, StanfordCorenlpSentence.class)) {
            List<StanfordCorenlpToken> sentenceTokens = JCasUtil.selectCovered(StanfordCorenlpToken.class, sentence);
            for (T mention : JCasUtil.selectCovered(clazz, sentence)) {
                int tokenIndex = 0;
                Word headWord = UimaNlpUtils.findHeadFromStanfordAnnotation(mention);
                for (StanfordCorenlpToken token : sentenceTokens) {
                    if (headWord.equals(token)) {
                        headWordOffsets.put(mention, tokenIndex);
                    }
                    tokenIndex++;
                }
                elements2SentenceTokens.put(mention, sentenceTokens);
            }
        }
    }

    @Override
    protected int getSentenceFocus(int sequenceFocus) {
        if (sequenceFocus >= 0 && sequenceFocus < sequenceElements.size()) {
            T mention = sequenceElements.get(sequenceFocus);
            return headWordOffsets.get(mention);
        } else {
            return -1;
        }
    }

    @Override
    protected List<StanfordCorenlpToken> getSentenceTokens(int elementFocus) {
        if (elementFocus >= 0 && elementFocus < sequenceElements.size()) {
            T mention = sequenceElements.get(elementFocus);
            return elements2SentenceTokens.get(mention);
        }
        return new ArrayList<>();
    }
}
