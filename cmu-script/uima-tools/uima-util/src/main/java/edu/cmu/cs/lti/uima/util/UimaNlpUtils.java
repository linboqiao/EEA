package edu.cmu.cs.lti.uima.util;

import edu.cmu.cs.lti.script.type.*;
import org.apache.uima.fit.util.FSCollectionFactory;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.FSList;
import org.apache.uima.jcas.tcas.Annotation;

import java.util.ArrayList;
import java.util.List;

public class UimaNlpUtils {
    public static String getLemmatizedAnnotation(Annotation a) {
        StringBuilder builder = new StringBuilder();
        String spliter = "";
        for (StanfordCorenlpToken token : JCasUtil.selectCovered(StanfordCorenlpToken.class, a)) {
            builder.append(spliter);
            builder.append(token.getLemma());
            spliter = " ";
        }
        return builder.toString();
    }

    public static EntityMention createEntityMention(JCas jcas, int begin, int end, String componentId) {
        EntityMention mention = new EntityMention(jcas, begin, end);
        UimaAnnotationUtils.finishAnnotation(mention, componentId, null, jcas);
        mention.setHead(findHeadFromAnnotation(mention));
        return mention;
    }

    public static StanfordCorenlpToken findFirstToken(JCas aJCas, int begin, int end) {
        for (StanfordCorenlpToken token : JCasUtil.selectCovered(aJCas, StanfordCorenlpToken.class, begin, end)) {
            return token;
        }
        return null;
    }


    public static StanfordCorenlpToken findFirstToken(Annotation anno) {
        for (StanfordCorenlpToken token : JCasUtil.selectCovered(StanfordCorenlpToken.class, anno)) {
            return token;
        }
        return null;
    }

    public static Word findFirstWord(JCas jcas, int begin, int end, String targetComponentId) {
        for (Word token : JCasUtil.selectCovered(jcas, Word.class, begin, end)) {
            if (token.getComponentId().equals(targetComponentId)) {
                return token;
            }
        }
        return null;
    }

    public static Word findFirstWord(Annotation anno, String targetComponentId) {
        for (Word token : JCasUtil.selectCovered(Word.class, anno)) {
            if (token.getComponentId().equals(targetComponentId)) {
                return token;
            }
        }
        return null;
    }

    public static StanfordCorenlpToken findHeadFromRange(JCas view, int begin, int end) {
        StanfordTreeAnnotation largestContainingTree = findLargest(JCasUtil.selectCovered(view,
                StanfordTreeAnnotation.class, begin, end));
        return findHeadFromTree(largestContainingTree);
    }

    public static StanfordCorenlpToken findHeadFromAnnotation(Annotation anno) {
        StanfordCorenlpToken headWord = findHeadFromTree(findLargestContainingTree(anno));
        if (headWord == null) {
            headWord = JCasUtil.selectCovering(StanfordCorenlpToken.class, anno).get(0);
        }
        return headWord;
    }

    public static StanfordTreeAnnotation findLargestContainingTree(Annotation anno) {
        return findLargest(JCasUtil.selectCovered(StanfordTreeAnnotation.class, anno));
    }

    public static List<Word> getDependentWords(Word word) {
        List<Word> dependentTokens = new ArrayList<>();

        FSList childDeps = word.getChildDependencyRelations();
        if (childDeps != null) {
            for (Dependency dep : FSCollectionFactory.create(childDeps, StanfordDependencyRelation.class)) {
                dependentTokens.add(dep.getChild());
            }
        }

        return dependentTokens;
    }

    /**
     * Get dependent words with a specific word type.
     *
     * @param word      The head word.
     * @param wordClass The word type class.
     * @param <T>       The word type class name.
     * @return
     */
    public static <T extends Word> List<T> getDependentWords(T word, Class<T> wordClass) {
        List<T> dependentTokens = new ArrayList<>();

        FSList childDeps = word.getChildDependencyRelations();
        if (childDeps != null) {
            for (Dependency dep : FSCollectionFactory.create(childDeps, Dependency.class)) {
                dependentTokens.add((T) dep.getChild());
            }
        }
        return dependentTokens;
    }

    public static StanfordCorenlpToken findHeadFromTree(StanfordTreeAnnotation tree) {
        if (tree != null) {
            if (tree.getIsLeaf()) {
                return findFirstToken(tree);
            } else {
                return tree.getHead();
            }
        } else {
            return null;
        }
    }

    public static <T extends Annotation> T findLargest(List<T> annos) {
        T largestAnno = null;
        for (T anno : annos) {
            if (largestAnno == null) {
                largestAnno = anno;
            } else if (largestAnno.getEnd() - largestAnno.getBegin() < anno.getEnd() - anno
                    .getBegin()) {
                largestAnno = anno;
            }
        }
        return largestAnno;
    }
}