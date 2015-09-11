package edu.cmu.cs.lti.emd.learn.feature.functions;

import edu.cmu.cs.lti.script.type.*;
import edu.cmu.cs.lti.utils.Configuration;
import gnu.trove.map.TObjectDoubleMap;
import org.apache.uima.fit.util.FSCollectionFactory;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.FSList;

import java.util.List;
import java.util.function.Function;

/**
 * Created with IntelliJ IDEA.
 * Date: 9/6/15
 * Time: 10:17 PM
 *
 * @author Zhengzhong Liu
 */
public class DependentWordFeatures extends SequenceFeatureWithFocus {
    public DependentWordFeatures(Configuration config) {
        super(config);
    }

    @Override
    public void initDocumentWorkspace(JCas context) {
        // Set types to each token for easy feature extraction.
        for (StanfordEntityMention mention : JCasUtil.select(context, StanfordEntityMention.class)) {
            String entityType = mention.getEntityType();
            for (StanfordCorenlpToken token : JCasUtil.selectCovered(StanfordCorenlpToken.class, mention)) {
                token.setNerTag(entityType);
            }
        }
    }

    @Override
    public void resetWorkspace(JCas aJCas, int begin, int end) {

    }

    @Override
    public void extract(List<StanfordCorenlpToken> sequence, int focus, TObjectDoubleMap<String> features,
                        TObjectDoubleMap<String> featuresNeedForState) {
        addDependentFeatures(sequence, focus, features, Word::getLemma, "ChildLemma");
        addDependentFeatures(sequence, focus, features, Word::getNerTag, "ChildNer");
    }

    public void addDependentFeatures(List<StanfordCorenlpToken> sentence, int focus, TObjectDoubleMap<String> features,
                                     Function<Word, String> operator, String featureType) {
        if (focus < 0 || focus > sentence.size() - 1) {
            return;
        }
        StanfordCorenlpToken token = sentence.get(focus);
        FSList childDependencies = token.getChildDependencyRelations();

        if (childDependencies == null) {
            return;
        }

        for (Dependency dep : FSCollectionFactory.create(childDependencies, Dependency.class)) {
            Word dependent = dep.getChild();
            String featureVal = operator.apply(dependent);
            if (featureVal != null) {
                features.put(String.format("%s=%s", featureType, featureVal), 1);
            }
        }
    }
}
