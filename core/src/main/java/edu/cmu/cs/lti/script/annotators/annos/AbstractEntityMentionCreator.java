package edu.cmu.cs.lti.script.annotators.annos;

import edu.cmu.cs.lti.model.Span;
import edu.cmu.cs.lti.script.type.*;
import edu.cmu.cs.lti.uima.annotator.AbstractLoggingAnnotator;
import edu.cmu.cs.lti.uima.util.UimaAnnotationUtils;
import edu.cmu.cs.lti.uima.util.UimaConvenience;
import edu.cmu.cs.lti.uima.util.UimaNlpUtils;
import edu.cmu.cs.lti.utils.Utils;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.FSArray;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: zhengzhongliu
 * Date: 10/16/14
 * Time: 10:07 PM
 */
public abstract class AbstractEntityMentionCreator extends AbstractLoggingAnnotator {

    private Map<Span, EntityMention> head2EntityMention;

    public abstract String getComponentId();


    public void process(JCas aJCas) throws AnalysisEngineProcessException {
        head2EntityMention = new HashMap<>();
        Collection<EntityMention> entityMentions = JCasUtil.select(aJCas, EntityMention.class);
        for (EntityMention mention : entityMentions) {
            head2EntityMention.put(Utils.toSpan(mention.getHead()), mention);
        }

        subprocess(aJCas);
    }

    public abstract void subprocess(JCas aJCas);


    protected EventMentionArgumentLink createArgumentLink(JCas aJCas, EventMention evm, String roleName, Word argument, Word prep) {
        EntityMention argumentEntity = getOrCreateSingletonEntityMention(aJCas, argument);
        return createArgumentLink(aJCas, evm, roleName, argumentEntity, prep);
    }


    protected EventMentionArgumentLink createArgumentLink(JCas aJCas, EventMention evm, String roleName, EntityMention argumentEntity, Word prep) {
        EventMentionArgumentLink link = new EventMentionArgumentLink(aJCas);
        link.setVerbPreposition(prep);
        link.setArgument(argumentEntity);
        link.setArgumentRole(roleName);
        link.setEventMention(evm);
        UimaAnnotationUtils.finishTop(link, getComponentId(), null, aJCas);
        argumentEntity.setArgumentLinks(UimaConvenience.appendFSList(aJCas, argumentEntity.getArgumentLinks(), link, EventMentionArgumentLink.class));
        return link;
    }

    protected EntityMention getOrCreateSingletonEntityMention(JCas jcas, Word headWord) {
        EntityMention mention = head2EntityMention.get(Utils.toSpan(headWord));
        if (mention == null) {
            mention = UimaNlpUtils.createEntityMention(jcas, headWord.getBegin(), headWord.getEnd(),
                    getComponentId());
            Entity entity = new Entity(jcas);
            entity.setEntityMentions(new FSArray(jcas, 1));
            entity.setEntityMentions(0, mention);
            entity.setRepresentativeMention(mention);
            mention.setReferingEntity(entity);
            UimaAnnotationUtils.finishTop(entity, getComponentId(), null, jcas);
            head2EntityMention.put(Utils.toSpan(headWord), mention);
        }
        return mention;
    }

    protected EntityMention getOrCreateEntityMention(JCas jcas, Word headWord) {
        EntityMention mention = head2EntityMention.get(Utils.toSpan(headWord));
        if (mention == null) {
            mention = UimaNlpUtils.createEntityMention(jcas, headWord.getBegin(), headWord.getEnd(),
                    getComponentId());
            UimaAnnotationUtils.finishAnnotation(mention, headWord.getBegin(), headWord.getEnd(), getComponentId(), null, jcas);
            head2EntityMention.put(Utils.toSpan(headWord), mention);
        }
        return mention;
    }

    //conj: this should go last so it can make use of what we did before
    protected EventMentionArgumentLink addNewArgument(JCas aJCas, EventMention evm, EntityMention newArgument, String roleName) {
        EventMentionArgumentLink link = createArgumentLink(aJCas, evm, roleName, newArgument, null);
        evm.setArguments(UimaConvenience.appendFSList(aJCas, evm.getArguments(), link, EventMentionArgumentLink.class));
        return link;
    }

    protected EventMentionArgumentLink addNewArgument(JCas aJCas, EventMention evm, Word newArgumentHead, String roleName) {
        EventMentionArgumentLink link = createArgumentLink(aJCas, evm, roleName, newArgumentHead, null);
        evm.setArguments(UimaConvenience.appendFSList(aJCas, evm.getArguments(), link, EventMentionArgumentLink.class));
        return link;
    }
}
