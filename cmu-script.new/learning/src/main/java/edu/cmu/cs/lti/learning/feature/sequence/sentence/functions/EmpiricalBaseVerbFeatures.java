package edu.cmu.cs.lti.learning.feature.sequence.sentence.functions;

import com.google.common.collect.Table;
import edu.cmu.cs.lti.learning.feature.sequence.base.SequenceFeatureWithFocus;
import edu.cmu.cs.lti.learning.model.MentionKey;
import edu.cmu.cs.lti.script.type.CharacterAnnotation;
import edu.cmu.cs.lti.script.type.StanfordCorenlpToken;
import edu.cmu.cs.lti.utils.Configuration;
import gnu.trove.map.TObjectDoubleMap;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;

import java.util.ArrayList;
import java.util.List;

/**
 * Features based on the Base Verb observations from the paper:
 * Employing Compositional Semantics and Discourse Consistency in Chinese Event Extraction
 *
 * @author Zhengzhong Liu
 */
public class EmpiricalBaseVerbFeatures extends SequenceFeatureWithFocus<StanfordCorenlpToken> {
    public EmpiricalBaseVerbFeatures(Configuration generalConfig, Configuration featureConfig) {
        super(generalConfig, featureConfig);
    }

    @Override
    public void initDocumentWorkspace(JCas context) {

    }

    @Override
    public void resetWorkspace(JCas aJCas) {

    }

    @Override
    public void extract(List<StanfordCorenlpToken> sequence, int focus, TObjectDoubleMap<String> nodeFeatures,
                        Table<Pair<Integer, Integer>, String, Double> edgeFeatures) {

        if (focus > 0 && focus < sequence.size()) {
            StanfordCorenlpToken token = sequence.get(focus);
            for (String bvs : getBaseVerbStructures(token)) {
                addToFeatures(nodeFeatures, String.format("VerbStructure_%s_%s", bvs, token.getPos()), 1);
            }
        }
    }

    @Override
    public void extractGlobal(List<StanfordCorenlpToken> sequence, int focus, TObjectDoubleMap<String>
            globalFeatures, List<MentionKey> knownStates, MentionKey currentState) {

    }

    private List<String> getBaseVerbStructures(StanfordCorenlpToken token) {
        List<CharacterAnnotation> characters = JCasUtil.selectCovered(CharacterAnnotation.class, token);

        List<String> posSeq = new ArrayList<>();
        List<String> verbStructures = new ArrayList<>();

        for (CharacterAnnotation character : characters) {
//            if (character.getPos() == null) {
//                logger.info("Cannot find pos in " + character.getCoveredText() + " " + UimaConvenience.getDocId
//                        (character.getCAS()));
//                logger.info(character.getBegin() + " " + character.getEnd());
//                DebugUtils.pause();
//            }
            if (character.getPos() == null){
                logger.warn("No POS tagging for the characters available");
                return verbStructures;
            }
            posSeq.add(character.getPos().toLowerCase());
        }


        if (posSeq.size() == 1) {
            if (posSeq.get(0).startsWith("v")) {
                verbStructures.add("SingleBV_" + characters.get(0).getCoveredText());
            }
        } else {
            if (posSeq.get(0).startsWith("v")) {
                String firstVerb = characters.get(0).getCoveredText();
                if (posSeq.get(1).startsWith("v")) {
                    verbStructures.add("BV_verb_" + firstVerb);
                    verbStructures.add("verb_BV_" + characters.get(1).getCoveredText());
                } else if (posSeq.get(1).equals("as")) {
                    verbStructures.add("BV_complementation" + firstVerb);
                } else if (posSeq.get(1).startsWith("n") || posSeq.get(1).equals("JJ")) {
                    verbStructures.add("BV_noun/adj" + firstVerb);
                } else {
                    verbStructures.add("BV_other" + firstVerb);
                }
            } else if (posSeq.get(1).startsWith("v")) {
                String secondVerb = characters.get(1).getCoveredText();
                if (posSeq.get(0).startsWith("n") || posSeq.get(0).equals("JJ")) {
                    verbStructures.add("noun/adj_BV" + secondVerb);
                } else {
                    verbStructures.add("other_BV" + secondVerb);
                }
            } else {
                boolean hasVerb = false;
                String verb = null;

                for (CharacterAnnotation character : characters) {
                    if (character.getPos().startsWith("v")) {
                        hasVerb = true;
                        verb = character.getCoveredText();
                        break;
                    }
                }

                if (hasVerb) {
                    verbStructures.add("lateBV_" + verb);
                } else {
                    verbStructures.add("NoBV");
                }
            }
        }

        return verbStructures;
    }
}
