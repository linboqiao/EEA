package edu.cmu.cs.lti.learning.feature.sentence.functions;

import edu.cmu.cs.lti.script.type.FanseToken;
import edu.cmu.cs.lti.script.type.StanfordCorenlpToken;
import edu.cmu.cs.lti.uima.util.TokenAlignmentHelper;
import edu.cmu.cs.lti.utils.Configuration;
import gnu.trove.map.TObjectDoubleMap;
import org.apache.uima.jcas.JCas;

import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * Date: 9/12/15
 * Time: 3:10 PM
 *
 * @author Zhengzhong Liu
 */
public class FanseFeatures extends SequenceFeatureWithFocus{
    TokenAlignmentHelper align;

    public FanseFeatures(Configuration config) {
        super(config);


    }

    @Override
    public void initDocumentWorkspace(JCas context) {
        align.loadStanford2Fanse(context);
    }

    @Override
    public void resetWorkspace(JCas aJCas, int begin, int end) {

    }

    @Override
    public void extract(List<StanfordCorenlpToken> sequence, int focus, TObjectDoubleMap<String> features,
                        TObjectDoubleMap<String> featuresNeedForState) {
        if (focus > sequence.size() - 1 || focus < 0) {
            return;
        }
        StanfordCorenlpToken token = sequence.get(focus);

        FanseToken fanseToken = align.getFanseToken(token);


    }

    private void getFanseHeadWord(){

    }
}
