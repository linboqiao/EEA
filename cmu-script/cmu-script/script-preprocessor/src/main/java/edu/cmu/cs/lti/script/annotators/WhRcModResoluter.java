package edu.cmu.cs.lti.script.annotators;

import com.google.common.collect.ArrayListMultimap;
import edu.cmu.cs.lti.model.Span;
import edu.cmu.cs.lti.script.type.Entity;
import edu.cmu.cs.lti.script.type.EntityMention;
import edu.cmu.cs.lti.script.type.FanseDependencyRelation;
import edu.cmu.cs.lti.script.type.Word;
import edu.cmu.cs.lti.uima.model.AnnotationCondition;
import edu.cmu.cs.lti.uima.util.UimaAnnotationUtils;
import edu.cmu.cs.lti.uima.util.UimaConvenience;
import org.apache.uima.fit.util.FSCollectionFactory;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.FSList;
import org.apache.uima.jcas.cas.TOP;

import java.util.*;

/**
 * This seems to work well on recent version of Stanford Corenlp parser, the dependency
 * structure is a little different in the annotated_gigaword version, so we use the Fanse
 * dependency instead
 * <p/>
 * Created with IntelliJ IDEA.
 * User: zhengzhongliu
 * Date: 10/8/14
 * Time: 12:24 AM
 */
public class WhRcModResoluter extends AbstractEntityMentionCreator {
    public final String WH_WORD_LABEL_PREFIX = "W";
    private HashMap<Span, EntityMention> head2EntityMention;

    public static final String COMPONENT_ID = WhRcModResoluter.class.getSimpleName();

    @Override
    public String getComponentId() {
        return COMPONENT_ID;
    }

    @Override
    public void subprocess(JCas aJCas) {
        logger.info(progressInfo(aJCas));

        head2EntityMention = new HashMap<>();
        Collection<EntityMention> entityMentions = JCasUtil.select(aJCas, EntityMention.class);
        for (EntityMention mention : entityMentions) {
            head2EntityMention.put(UimaAnnotationUtils.toSpan(mention.getHead()), mention);
        }

        for (Map.Entry<EntityMention, Collection<EntityMention>> entry : findWhAndNounPair(aJCas).asMap().entrySet()) {
            EntityMention rcmodEm = entry.getKey();
            Collection<EntityMention> whEms = entry.getValue();

            Entity entity = rcmodEm.getReferingEntity();
            if (entity != null) {
                //adding to existing entity
                List<EntityMention> mentions = new ArrayList<>(FSCollectionFactory.create(entity.getEntityMentions(), EntityMention.class));
                for (EntityMention whEm : whEms) {
                    mentions.add(whEm);
                    whEm.setReferingEntity(entity);
                }
                entity.setEntityMentions(FSCollectionFactory.createFSArray(aJCas, mentions));
            } else {
                //create new entity
                entity = new Entity(aJCas);
                List<EntityMention> mentions = new ArrayList<>();
                mentions.add(rcmodEm);
                for (EntityMention whEm : whEms) {
                    mentions.add(whEm);
                    whEm.setReferingEntity(entity);
                }
                entity.setEntityMentions(FSCollectionFactory.createFSArray(aJCas, mentions));
                entity.setRepresentativeMention(rcmodEm);
                rcmodEm.setReferingEntity(entity);
                UimaAnnotationUtils.finishTop(entity, COMPONENT_ID, null, aJCas);
            }

//            System.out.println(rcmodEm.getReferingEntity().getRepresentativeMention().getCoveredText());
//            System.out.println(whEm.getReferingEntity().getRepresentativeMention().getCoveredText());
        }
    }

    private ArrayListMultimap<EntityMention, EntityMention> findWhAndNounPair(JCas aJCas) {
        //find relative clause modifiers
        AnnotationCondition rcFilter = new AnnotationCondition() {
            @Override
            public Boolean check(TOP aAnnotation) {
                FanseDependencyRelation sdr = (FanseDependencyRelation) aAnnotation;
                return sdr.getDependencyType().equals("rcmod");
            }
        };

        ArrayListMultimap<EntityMention, EntityMention> relativeClauseCoreferences = ArrayListMultimap.create();

        List<FanseDependencyRelation> rcmodRelations = UimaConvenience.getAnnotationListWithFilter(
                aJCas, FanseDependencyRelation.class, rcFilter);
        for (FanseDependencyRelation rcmodRel : rcmodRelations) {
            Word modifier = rcmodRel.getChild();
            FSList childRelations = modifier.getChildDependencyRelations();
            if (childRelations != null) {
                for (FanseDependencyRelation childRelation : FSCollectionFactory.create(
                        modifier.getChildDependencyRelations(), FanseDependencyRelation.class)) {
                    String childRelationType = childRelation.getDependencyType();
                    if (childRelationType.equals("nsubj") || childRelationType.equals("rel")) {
                        Word modifierChild = childRelation.getChild();
                        if (modifierChild.getPos().startsWith(WH_WORD_LABEL_PREFIX)) {
                            Word rcmodHead = rcmodRel.getHead();
                            EntityMention rcmodHeadMention = getOrCreateEntityMention(aJCas, rcmodHead);
                            EntityMention whMention = getOrCreateEntityMention(aJCas, modifierChild);
                            relativeClauseCoreferences.put(rcmodHeadMention, whMention);
                        }
                    }
                }
            }
        }
        return relativeClauseCoreferences;
    }

//    private EntityMention getOrCreateEntityMention(JCas jcas, Word headWord) {
//        EntityMention mention = head2EntityMention.get(Utils.toSpan(headWord));
//        if (mention == null) {
//            mention = UimaNlpUtils.createEntityMention(jcas, headWord.getBegin(), headWord.getEnd(),
//                    COMPONENT_ID);
//            UimaAnnotationUtils.finishAnnotation(mention, headWord.getBegin(), headWord.getEnd(), COMPONENT_ID, null, jcas);
//            head2EntityMention.put(Utils.toSpan(headWord), mention);
//
//        }
//        return mention;
//    }

}
