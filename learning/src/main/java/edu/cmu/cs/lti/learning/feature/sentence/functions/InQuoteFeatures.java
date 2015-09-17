package edu.cmu.cs.lti.learning.feature.sentence.functions;

import edu.cmu.cs.lti.script.type.QuotedContent;
import edu.cmu.cs.lti.script.type.StanfordCorenlpToken;
import edu.cmu.cs.lti.utils.Configuration;
import gnu.trove.map.TObjectDoubleMap;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Created with IntelliJ IDEA.
 * Date: 9/14/15
 * Time: 2:24 PM
 *
 * @author Zhengzhong Liu
 */
public class InQuoteFeatures extends SequenceFeatureWithFocus {
    private Set<StanfordCorenlpToken> phraseQuotedTokens;

    public InQuoteFeatures(Configuration generalConfig, Configuration featureConfig) {
        super(generalConfig, featureConfig);
    }

    @Override
    public void initDocumentWorkspace(JCas context) {
        phraseQuotedTokens = new HashSet<>();
        JCasUtil.select(context, QuotedContent.class).stream().filter(QuotedContent::getPhraseQuote)
                .forEach(quotedContent -> {
                    phraseQuotedTokens.addAll(JCasUtil.selectCovered(StanfordCorenlpToken.class, quotedContent).stream()
                            .collect(Collectors.toList()));
                });
    }

    @Override
    public void resetWorkspace(JCas aJCas, int begin, int end) {

    }

    @Override
    public void extract(List<StanfordCorenlpToken> sequence, int focus, TObjectDoubleMap<String> features,
                        TObjectDoubleMap<String> featuresNeedForState) {

        isInPhraseQuote(features, sequence.get(focus));
    }

    private void isInPhraseQuote(TObjectDoubleMap<String> features, StanfordCorenlpToken token) {
        if (phraseQuotedTokens.contains(token)) {
            features.put("InPhraseQuote", 1);
        }
    }
}
