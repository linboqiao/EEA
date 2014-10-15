package edu.cmu.cs.lti.cds.annotators;

import edu.cmu.cs.lti.model.Span;
import edu.cmu.cs.lti.script.type.Entity;
import edu.cmu.cs.lti.script.type.EntityMention;
import edu.cmu.cs.lti.script.type.StanfordDependencyRelation;
import edu.cmu.cs.lti.script.type.Word;
import edu.cmu.cs.lti.uima.annotator.AbstractLoggingAnnotator;
import edu.cmu.cs.lti.uima.model.AnnotationCondition;
import edu.cmu.cs.lti.uima.util.UimaAnnotationUtils;
import edu.cmu.cs.lti.uima.util.UimaConvenience;
import edu.cmu.cs.lti.uima.util.UimaNlpUtils;
import edu.cmu.cs.lti.utils.Utils;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.util.FSCollectionFactory;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.FSList;
import org.apache.uima.jcas.cas.TOP;

import java.util.*;

/**
 * Created with IntelliJ IDEA.
 * User: zhengzhongliu
 * Date: 10/8/14
 * Time: 12:24 AM
 */
public class WhPronounResoluter extends AbstractLoggingAnnotator {
    public final String WH_WORD_LABEL_PREFIX = "W";
    private HashMap<Span, EntityMention> head2EntityMention;

    public static final String COMPONENT_ID = WhPronounResoluter.class.getSimpleName();

    @Override
    public void process(JCas aJCas) throws AnalysisEngineProcessException {
        head2EntityMention = new HashMap<>();
        Collection<EntityMention> entityMentions = JCasUtil.select(aJCas, EntityMention.class);
        for (EntityMention mention : entityMentions) {
            head2EntityMention.put(Utils.toSpan(mention.getHead()), mention);
        }

        for (Map.Entry<EntityMention, EntityMention> entry : findWhAndNounPair(aJCas).entrySet()) {
            EntityMention rcmodEm = entry.getKey();
            EntityMention whEm = entry.getValue();

            Entity entity = rcmodEm.getReferingEntity();
            if (entity != null) {
                //adding to existing entity
                Collection<EntityMention> mentions = FSCollectionFactory.create(entity.getEntityMentions(), EntityMention.class);
                mentions.add(whEm);

                entity.setEntityMentions(FSCollectionFactory.createFSArray(aJCas, mentions));
            } else {
                //create new entity
                entity = new Entity(aJCas);
                List<EntityMention> mentions = new ArrayList<>();
                mentions.add(rcmodEm);
                mentions.add(whEm);
                entity.setEntityMentions(FSCollectionFactory.createFSArray(aJCas, mentions));
                entity.setRepresentativeMention(rcmodEm);
                UimaAnnotationUtils.finishTop(entity, COMPONENT_ID, null, aJCas);
            }
        }
    }

    private Map<EntityMention, EntityMention> findWhAndNounPair(JCas aJCas) {
        AnnotationCondition rcFilter = new AnnotationCondition() {
            @Override
            public Boolean check(TOP aAnnotation) {
                StanfordDependencyRelation sdr = (StanfordDependencyRelation) aAnnotation;
                return sdr.getDependencyType().equals("rcmod");
            }
        };

        Map<EntityMention, EntityMention> pairwiseCoreferences = new HashMap<>();

        List<StanfordDependencyRelation> rcmodRelations = UimaConvenience.getAnnotationListWithFilter(
                aJCas, StanfordDependencyRelation.class, rcFilter);
        for (StanfordDependencyRelation rcmodRel : rcmodRelations) {
            Word modifier = rcmodRel.getChild();
            FSList childRelations = modifier.getChildDependencyRelations();
            if (childRelations != null) {
                for (StanfordDependencyRelation childRelation : FSCollectionFactory.create(
                        modifier.getChildDependencyRelations(), StanfordDependencyRelation.class)) {
                    String childRelationType = childRelation.getDependencyType();
                    if (childRelationType.equals("nsubj") || childRelationType.equals("rel")) {
                        Word modifierChild = childRelation.getChild();
                        if (modifierChild.getPos().startsWith(WH_WORD_LABEL_PREFIX)) {
                            Word rcmodHead = rcmodRel.getHead();
                            EntityMention rcmodHeadMention = getOrCreateEntityMention(aJCas, rcmodHead);
                            EntityMention whMention = getOrCreateEntityMention(aJCas, modifierChild);
                            pairwiseCoreferences.put(rcmodHeadMention, whMention);
                        }
                    }
                }
            }
        }
        return pairwiseCoreferences;
    }

    private EntityMention getOrCreateEntityMention(JCas jcas, Word headWord) {
        EntityMention mention = head2EntityMention.get(Utils.toSpan(headWord));
        if (mention == null) {
            mention = UimaNlpUtils.createEntityMention(jcas, headWord.getBegin(), headWord.getEnd(),
                    COMPONENT_ID);
            UimaAnnotationUtils.finishAnnotation(mention, headWord.getBegin(), headWord.getEnd(), COMPONENT_ID, null, jcas);
            head2EntityMention.put(Utils.toSpan(headWord), mention);
        }
        return mention;
    }

}
