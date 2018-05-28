package edu.cmu.cs.lti.script.annotators;

import edu.cmu.cs.lti.ling.PennTreeTagSet;
import edu.cmu.cs.lti.script.type.Dependency;
import edu.cmu.cs.lti.script.type.EventMention;
import edu.cmu.cs.lti.script.type.StanfordCorenlpToken;
import edu.cmu.cs.lti.script.type.Word;
import edu.cmu.cs.lti.uima.util.UimaAnnotationUtils;
import org.apache.uima.fit.util.FSCollectionFactory;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.FSList;

/**
 * Created with IntelliJ IDEA.
 * Date: 4/22/15
 * Time: 6:23 PM
 *
 * @author Zhengzhong Liu
 */
public class SyntacticBasedEventMentionTupleExtractor extends AbstractEntityMentionCreator {
    @Override
    public String getComponentId() {
        return SyntacticBasedEventMentionTupleExtractor.class.getSimpleName();
    }

    @Override
    public void subprocess(JCas aJCas) {
        int mentionId = 0;
        for (StanfordCorenlpToken token : JCasUtil.select(aJCas, StanfordCorenlpToken.class)) {
            if (token.getPos().startsWith(PennTreeTagSet.VERB_PREFIX) && !isCopula(token)) {
                createVerbBasedEvetnMention(aJCas, token, mentionId++);
            }
        }
    }

    private void createVerbBasedEvetnMention(JCas aJCas, StanfordCorenlpToken predicate, int mentionId) {
        EventMention mention = new EventMention(aJCas);
        FSList childDependenciesFs = predicate.getChildDependencyRelations();
        for (Dependency dep : FSCollectionFactory.create(predicate.getChildDependencyRelations(), Dependency.class)) {
        }

        UimaAnnotationUtils.finishAnnotation(mention, predicate.getBegin(), predicate.getEnd(), getComponentId(), mentionId, aJCas);
    }


    private boolean isCopula(Word word) {
        return word.getLemma().toLowerCase().equals("be");
    }
}
