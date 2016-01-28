package edu.cmu.cs.lti.learning.feature.mention_pair.functions;

import edu.cmu.cs.lti.learning.feature.sequence.FeatureUtils;
import edu.cmu.cs.lti.script.type.*;
import edu.cmu.cs.lti.utils.Configuration;
import gnu.trove.map.TObjectDoubleMap;
import org.apache.uima.fit.util.FSCollectionFactory;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.FSList;

import java.util.HashMap;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * Date: 9/30/15
 * Time: 4:33 AM
 *
 * @author Zhengzhong Liu
 */
public class SyntacticFeatures extends AbstractMentionPairFeatures {
    Map<StanfordCorenlpToken, StanfordTreeAnnotation> head2LargestSpanTree = new HashMap<>();

    public SyntacticFeatures(Configuration generalConfig, Configuration featureConfig) {
        super(generalConfig, featureConfig);
    }

    @Override
    public void initDocumentWorkspace(JCas context) {
        for (StanfordTreeAnnotation tree : JCasUtil.select(context, StanfordTreeAnnotation.class)) {
            StanfordCorenlpToken head = tree.getHead();
            if (head2LargestSpanTree.containsKey(head)) {
                StanfordTreeAnnotation previousTree = head2LargestSpanTree.get(head);
                if (tree.getCoveredText().length() > previousTree.getCoveredText().length()) {
                    head2LargestSpanTree.put(head, tree);
                }
            }
        }
    }

    @Override
    public void extract(JCas documentContext, TObjectDoubleMap<String> rawFeatures, EventMention firstAnno,
                        EventMention secondAnno) {
        Word firstHead = firstAnno.getHeadWord();
        Word secondHead = secondAnno.getHeadWord();

        if (isSubtreeOf(firstHead, secondHead) || isSubtreeOf(secondHead, firstHead)) {
            addBoolean(rawFeatures, "IsSubtreeOf");
        }

        String directDep = directDependency(firstHead, secondHead);
        if (directDep != null) {
            addBoolean(rawFeatures, FeatureUtils.formatFeatureName("DirectDependency", directDep));
        }
    }

    @Override
    public void extract(JCas documentContext, TObjectDoubleMap<String> rawFeatures, EventMention secondAnno) {

    }

    private String directDependency(Word token1, Word token2) {
        String dep = directDirectedDependency(token1, token2);

        if (dep == null) {
            dep = directDirectedDependency(token2, token1);
        }

        return dep;
    }

    private String directDirectedDependency(Word token1, Word token2) {
        FSList token1Children = token1.getChildDependencyRelations();
        if (token1Children != null) {
            for (StanfordDependencyRelation relation : FSCollectionFactory.create(token1Children,
                    StanfordDependencyRelation.class)) {
                if (relation.getChild().equals(token2)) {
                    return relation.getDependencyType();
                }
            }
        }
        return null;
    }

    private boolean isSubtreeOf(Word parent, Word child) {
        if (head2LargestSpanTree.containsKey(parent)) {
            StanfordTreeAnnotation parentTree = head2LargestSpanTree.get(parent);

            if (parentTree.getBegin() < child.getBegin() && parentTree.getEnd() > child.getEnd()) {
                return true;
            }
        }
        return false;
    }
}
