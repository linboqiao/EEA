package edu.cmu.cs.lti.event_coref.annotators;

import edu.cmu.cs.lti.learning.utils.MentionTypeUtils;
import edu.cmu.cs.lti.script.type.Event;
import edu.cmu.cs.lti.script.type.EventMention;
import edu.cmu.cs.lti.uima.annotator.AbstractAnnotator;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;

import java.io.*;
import java.util.HashSet;
import java.util.Set;

/**
 * Created with IntelliJ IDEA.
 * Date: 12/27/15
 * Time: 9:02 PM
 *
 * @author Zhengzhong Liu
 */
public class DifferentTypeCorefCollector extends AbstractAnnotator {
    public final static String PARAM_COREFERENCE_ALLOWED_TYPES = "differentTypeCorefRules";

    @ConfigurationParameter(name = PARAM_COREFERENCE_ALLOWED_TYPES)
    private File typeRuleOutput;

    private Set<Pair<String, String>> typePairs = new HashSet<>();

    @Override
    public void process(JCas aJCas) throws AnalysisEngineProcessException {
        for (Event event : JCasUtil.select(aJCas, Event.class)) {
            int numMentions = event.getEventMentions().size();

            Set<String> types = new HashSet<>();

            for (int i = 0; i < numMentions; i++) {
                EventMention mention = event.getEventMentions(i);
                types.add(mention.getEventType());
            }

            if (types.size() > 1) {
                for (String typeI : types) {
                    for (String typeJ : types) {
                        if (!typeI.equals(typeJ)) {
                            if (!MentionTypeUtils.partialEquivalence(typeI, typeJ)) {
                                Pair<String, String> typePair = asSortedPair(typeI, typeJ);
                                typePairs.add(typePair);
                            }
                        }
                    }
                }
            }
        }
    }

    public void collectionProcessComplete() throws AnalysisEngineProcessException {
        super.collectionProcessComplete();
        try {
            Writer writer = new BufferedWriter(new FileWriter(typeRuleOutput));
            for (Pair<String, String> typePair : typePairs) {
                writer.write(typePair.getLeft() + "\t" + typePair.getRight() + "\n");
            }
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private Pair<String, String> asSortedPair(String str1, String str2) {
        if (str1.compareTo(str2) > 0) {
            return Pair.of(str1, str2);
        } else {
            return Pair.of(str2, str1);
        }
    }
}
