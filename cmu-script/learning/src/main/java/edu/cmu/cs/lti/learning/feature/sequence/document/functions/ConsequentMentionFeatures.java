package edu.cmu.cs.lti.learning.feature.sequence.document.functions;

import com.google.common.collect.Table;
import edu.cmu.cs.lti.learning.feature.sequence.base.SequenceFeatureWithFocus;
import edu.cmu.cs.lti.learning.model.MentionKey;
import edu.cmu.cs.lti.script.type.EventMention;
import edu.cmu.cs.lti.script.type.StanfordCorenlpSentence;
import edu.cmu.cs.lti.utils.Configuration;
import gnu.trove.map.TObjectDoubleMap;
import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.hash.TObjectIntHashMap;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;

import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * Date: 12/16/15
 * Time: 10:14 PM
 *
 * @author Zhengzhong Liu
 */
public class ConsequentMentionFeatures extends SequenceFeatureWithFocus<EventMention> {
    TObjectIntMap<EventMention> sentencePosition;

    public ConsequentMentionFeatures(Configuration generalConfig, Configuration featureConfig) {
        super(generalConfig, featureConfig);
    }

    @Override
    public void initDocumentWorkspace(JCas context) {
        sentencePosition = new TObjectIntHashMap<>();

        int sentIndex = 0;
        for (StanfordCorenlpSentence sentence : JCasUtil.select(context, StanfordCorenlpSentence.class)) {
            sentIndex++;
            for (EventMention mention : JCasUtil.selectCovered(EventMention.class, sentence)) {
                sentencePosition.put(mention, sentIndex);
            }
        }
    }

    @Override
    public void resetWorkspace(JCas aJCas) {

    }

    @Override
    public void extract(List<EventMention> sequence, int focus,
                        TObjectDoubleMap<String> nodeFeatures,
                        Table<Pair<Integer, Integer>, String, Double> edgeFeatures) {
        if (focus > 1 && focus < sequence.size()) {
            EventMention thisItem = sequence.get(focus);
            EventMention previousItem = sequence.get(focus - 1);

//            logger.debug(thisItem.getCoveredText() + " " + previousItem.getCoveredText());
//            logger.debug(sentencePosition.get(thisItem) + "," + sentencePosition.get(previousItem));

            if (sentencePosition.get(thisItem) == sentencePosition.get(previousItem)) {
                addToFeatures(edgeFeatures, focus - 1, focus, "SameSentence", 1);
            }
        }
    }

    @Override
    public void extractGlobal(List<EventMention> sequence, int focus, TObjectDoubleMap<String> globalFeatures,
                              List<MentionKey> knownStates, MentionKey currentState) {

    }
}
