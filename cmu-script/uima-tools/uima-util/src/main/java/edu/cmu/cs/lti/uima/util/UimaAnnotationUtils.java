package edu.cmu.cs.lti.uima.util;

import edu.cmu.cs.lti.model.Span;
import edu.cmu.cs.lti.script.type.ComponentAnnotation;
import edu.cmu.cs.lti.script.type.ComponentTOP;
import edu.cmu.cs.lti.script.type.Entity;
import edu.cmu.cs.lti.script.type.EntityMention;
import org.apache.uima.examples.SourceDocumentInformation;
import org.apache.uima.fit.util.FSCollectionFactory;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class UimaAnnotationUtils {
    public static void finishAnnotation(ComponentAnnotation anno, int begin, int end,
                                        String componentId, String id, JCas aJCas) {
        anno.setBegin(begin);
        anno.setEnd(end);
        anno.setComponentId(componentId);
        anno.setId(id);
        anno.addToIndexes(aJCas);
    }

    public static void finishAnnotation(ComponentAnnotation anno, String componentId, String id,
                                        JCas aJCas) {
        anno.setComponentId(componentId);
        anno.setId(id);
        anno.addToIndexes(aJCas);
    }

    public static void finishAnnotation(ComponentAnnotation anno, int begin, int end,
                                        String componentId, int id, JCas aJCas) {
        anno.setBegin(begin);
        anno.setEnd(end);
        anno.setComponentId(componentId);
        anno.setId(Integer.toString(id));
        anno.addToIndexes();
    }

    public static void finishAnnotation(ComponentAnnotation anno, String componentId, int id,
                                        JCas aJCas) {
        anno.setComponentId(componentId);
        anno.setId(Integer.toString(id));
        anno.addToIndexes(aJCas);
    }

    public static void finishTop(ComponentTOP anno, String componentId, String id, JCas aJCas) {
        anno.setComponentId(componentId);
        anno.setId(id);
        anno.addToIndexes(aJCas);
    }

    public static void finishTop(ComponentTOP anno, String componentId, int id, JCas aJCas) {
        anno.setComponentId(componentId);
        anno.setId(Integer.toString(id));
        anno.addToIndexes(aJCas);
    }

    public static <T extends Annotation> T selectCoveredSingle(Annotation anno, Class<T> clazz) {
        T singleAnno = null;
        List<T> coveredAnnos = JCasUtil.selectCovered(clazz, anno);
        if (coveredAnnos.size() > 1) {
            System.err.println(String.format(
                    "Annotation [%s] contains more than one subspan of type [%s]", anno.getCoveredText(),
                    clazz.getSimpleName()));
        }

        if (coveredAnnos.size() == 0) {
            System.err.println(String.format(
                    "Annotation [%s] contains does not have subspans of type [%s]",
                    anno.getCoveredText(), clazz.getSimpleName()));
        } else {
            singleAnno = coveredAnnos.get(0);
        }

        return singleAnno;
    }

    public static <T extends ComponentAnnotation> void assignAnnotationIds(Collection<T> annos) {
        int id = 0;
        for (ComponentAnnotation anno : annos) {
            anno.setId(Integer.toString(id++));
        }
    }

    public static <T extends ComponentTOP> void assignTopIds(Collection<T> annos) {
        int id = 0;
        for (ComponentTOP anno : annos) {
            anno.setId(Integer.toString(id++));
        }
    }

    public static void removeEntityMention(JCas aJCas, EntityMention m) {
        Entity e = m.getReferingEntity();

        if (e.getEntityMentions().size() == 1) {
            System.out.println("Remove a singleton " + m.getCoveredText() + " " + m.getId());
            e.removeFromIndexes(aJCas);
        } else {
            System.out.println("Remove a non-singleton");
            List<EntityMention> remainingMentions = new ArrayList<EntityMention>();
            for (int i = 0; i < e.getEntityMentions().size(); i++) {
                EntityMention otherMention = e.getEntityMentions(i);
                if (!otherMention.equals(m)) {
                    remainingMentions.add(otherMention);
                }
            }
            e.setEntityMentions(FSCollectionFactory.createFSArray(aJCas, remainingMentions));
        }

        m.removeFromIndexes(aJCas);
    }

    public static Span toSpan(ComponentAnnotation anno) {
        return new Span(anno.getBegin(), anno.getEnd());
    }

    public static int entityIdToInteger(String eid) {
        return Integer.parseInt(eid);
    }


    public static void setSourceDocumentInformation(JCas aJCas, String uri, int size, int offsetInSource, boolean isLastSegment) {
        SourceDocumentInformation srcDocInfo = new SourceDocumentInformation(aJCas);
        srcDocInfo.setUri(uri);
        srcDocInfo.setOffsetInSource(offsetInSource);
        srcDocInfo.setDocumentSize(size);
        srcDocInfo.setLastSegment(isLastSegment);
        srcDocInfo.addToIndexes();
    }
}
