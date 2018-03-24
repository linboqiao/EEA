package edu.cmu.cs.lti.script.annotators.writer;

import edu.cmu.cs.lti.script.type.*;
import edu.cmu.cs.lti.uima.annotator.AbstractLoggingAnnotator;
import edu.cmu.cs.lti.uima.util.UimaNlpUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.util.FSCollectionFactory;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Given dependency parses and coreference chains, create argument
 * cloze tasks.
 * Date: 3/24/18
 * Time: 4:32 PM
 *
 * @author Zhengzhong Liu
 */
public class ArgumentClozeTaskWriter extends AbstractLoggingAnnotator {

    @Override
    public void process(JCas aJCas) throws AnalysisEngineProcessException {
        Map<Word, Integer> headToCoref = loadCoref(aJCas);

        for (StanfordCorenlpToken token : JCasUtil.select(aJCas, StanfordCorenlpToken.class)) {
            if (!token.getPos().startsWith("V")) {
                continue;
            }

            for (StanfordDependencyRelation dep : FSCollectionFactory.create(token
                    .getChildDependencyRelations(), StanfordDependencyRelation.class)) {
                Pair<String, Word> child = takeDep(dep);
                if (child == null) {
                    continue;
                }

                Word word = child.getRight();
                String role = child.getLeft();

                String arg;

                if (headToCoref.containsKey(word)) {
                    arg = String.valueOf(headToCoref.get(word));
                } else {
                    arg = word.getLemma();
                }
            }
        }
    }

    private void takeVerb(StanfordCorenlpToken verb) {

    }

    private Pair<String, Word> takeDep(StanfordDependencyRelation dep) {
        String depType = dep.getDependencyType();
        Word depWord = dep.getChild();
        if (depType.equals("nsubj") || depType.equals("agent")) {
            return Pair.of("subj", depWord);
        } else if (depType.equals("dobj") || depType.equals("nsubjpass")) {
            return Pair.of("dobj", depWord);
        } else if (depType.equals("iobj")) {
            return Pair.of("iobj", depWord);
        } else if (depType.startsWith("prep_")) {
            return Pair.of(depType, depWord);
        }

        return null;
    }

    private Map<Word, Integer> loadCoref(JCas aJCas) {
        Map<Word, Integer> headToCoref = new HashMap<>();

        int corefId = 0;
        for (Entity entity : JCasUtil.select(aJCas, Entity.class)) {
            Collection<EntityMention> mentions = FSCollectionFactory.create(entity
                    .getEntityMentions(), EntityMention.class);

            if (mentions.size() > 1) {
                for (EntityMention mention : mentions) {
                    StanfordCorenlpToken head = UimaNlpUtils.findHeadFromStanfordAnnotation(mention);
                    headToCoref.put(head, corefId);
                }
                corefId += 1;
            }
        }
        return headToCoref;
    }
}
