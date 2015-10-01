package edu.cmu.cs.lti.learning.feature.mention_pair.functions;

import edu.cmu.cs.lti.script.type.EventMention;
import edu.cmu.cs.lti.script.type.StanfordCorenlpSentence;
import edu.cmu.cs.lti.utils.Configuration;
import gnu.trove.map.TObjectDoubleMap;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;

import java.util.HashMap;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * Date: 9/30/15
 * Time: 5:26 AM
 *
 * @author Zhengzhong Liu
 */
public class ForumRepeatFeature extends AbstractMentionPairFeatures {
    Map<EventMention, StanfordCorenlpSentence> mention2Sentence;

    public ForumRepeatFeature(Configuration generalConfig, Configuration featureConfig) {
        super(generalConfig, featureConfig);
    }

    @Override
    public void initDocumentWorkspace(JCas context) {
        mention2Sentence = new HashMap<>();
        for (StanfordCorenlpSentence sentence : JCasUtil.select(context, StanfordCorenlpSentence.class)) {
            for (EventMention mention : JCasUtil.selectCovered(EventMention.class, sentence)) {
                mention2Sentence.put(mention, sentence);
            }
        }
    }

    @Override
    public void extract(JCas documentContext, TObjectDoubleMap<String> rawFeatures, EventMention firstAnno,
                        EventMention secondAnno) {
        StanfordCorenlpSentence sentence1 = mention2Sentence.get(firstAnno);
        StanfordCorenlpSentence sentence2 = mention2Sentence.get(secondAnno);

        if (sentence1 != null && sentence2 != null) {
            String sentStr1 = sentence1.getCoveredText().trim();
            String sentStr2 = sentence2.getCoveredText().trim();

            if (sentStr1.equals(sentStr2)) {
                int firstOffset = firstAnno.getBegin() - sentence1.getBegin();
                int secondOffset = secondAnno.getBegin() - sentence2.getBegin();
                if (firstOffset == secondOffset) {
                    addBoolean(rawFeatures, "ForumRepeat");
                }
            }
        }

    }

    @Override
    public void extract(JCas documentContext, TObjectDoubleMap<String> rawFeatures, EventMention secondAnno) {

    }
}
