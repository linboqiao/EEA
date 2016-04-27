package edu.cmu.cs.lti.learning.feature.sequence.sentence.functions;

import com.google.common.collect.Table;
import edu.cmu.cs.lti.learning.feature.sequence.base.SequenceFeatureWithFocus;
import edu.cmu.cs.lti.script.type.Dependency;
import edu.cmu.cs.lti.script.type.StanfordCorenlpToken;
import edu.cmu.cs.lti.script.type.StanfordEntityMention;
import edu.cmu.cs.lti.script.type.Word;
import edu.cmu.cs.lti.utils.Configuration;
import gnu.trove.map.TObjectDoubleMap;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.uima.fit.util.FSCollectionFactory;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.FSList;

import java.util.*;
import java.util.function.Function;

/**
 * Created with IntelliJ IDEA.
 * Date: 9/6/15
 * Time: 10:17 PM
 *
 * @author Zhengzhong Liu
 */
public class DependentWordFeatures extends SequenceFeatureWithFocus<StanfordCorenlpToken> {
    Set<String> featureTemplates;

    public DependentWordFeatures(Configuration generalConfig, Configuration featureConfig) {
        super(generalConfig, featureConfig);
        featureTemplates = new HashSet<>(Arrays.asList(featureConfig.getList(this.getClass().getSimpleName() + "" +
                ".templates")));
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
    public void resetWorkspace(JCas aJCas) {

    }

    @Override
    public void extract(List<StanfordCorenlpToken> sequence, int focus, TObjectDoubleMap<String> nodeFeatures,
                        Table<Pair<Integer, Integer>, String, Double> edgeFeatures) {
        if (featureTemplates.contains("ChildLemma")) {
            addDependentFeatures(sequence, focus, nodeFeatures, Word::getLemma, "ChildLemma");
        }
        if (featureTemplates.contains("ChildNer")) {
            addDependentFeatures(sequence, focus, nodeFeatures, Word::getNerTag, "ChildNer");
        }
        if (featureTemplates.contains("ChildPos")) {
            addDependentFeatures(sequence, focus, nodeFeatures, Word::getPos, "ChildPos");
        }
        if (featureTemplates.contains("ChildDepType")) {
            addDependentLabelFeatures(sequence, focus, nodeFeatures, "ChildDepType");
        }
        if (featureTemplates.contains("HeadLemma")) {
            addGovnerFeatures(sequence, focus, nodeFeatures, Word::getLemma, "HeadLemma");
        }
        if (featureTemplates.contains("HeadNer")) {
            addGovnerFeatures(sequence, focus, nodeFeatures, Word::getNerTag, "HeadNer");
        }
        if (featureTemplates.contains("HeadPos")) {
            addGovnerFeatures(sequence, focus, nodeFeatures, Word::getPos, "HeadPos");
        }
        if (featureTemplates.contains("HeadDepType")) {
            addGovnerLabelFeatures(sequence, focus, nodeFeatures, "HeadDepType");
        }
    }

    public void addDependentLabelFeatures(List<StanfordCorenlpToken> sentence, int focus,
                                          TObjectDoubleMap<String> features, String featureType) {
        if (focus < 0 || focus > sentence.size() - 1) {
            return;
        }
        StanfordCorenlpToken token = sentence.get(focus);
        FSList childDependencies = token.getChildDependencyRelations();

        if (childDependencies == null) {
            return;
        }

        for (Dependency dep : FSCollectionFactory.create(childDependencies, Dependency.class)) {
            String featureVal = dep.getDependencyType();
            if (featureVal != null) {
                addToFeatures(features, String.format("%s=%s", featureType, featureVal), 1);
            }
        }
    }

    public void addGovnerLabelFeatures(List<StanfordCorenlpToken> sentence, int focus,
                                       TObjectDoubleMap<String> features, String featureType) {
        if (focus < 0 || focus > sentence.size() - 1) {
            return;
        }
        StanfordCorenlpToken token = sentence.get(focus);
        FSList headDependencyRelations = token.getHeadDependencyRelations();

        if (headDependencyRelations == null) {
            return;
        }

        for (Dependency dep : FSCollectionFactory.create(headDependencyRelations, Dependency.class)) {
            String featureVal = dep.getDependencyType();
            if (featureVal != null) {
                addToFeatures(features, String.format("%s=%s", featureType, featureVal), 1);
            }
        }
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
                addToFeatures(features, String.format("%s=%s", featureType, featureVal), 1);
            }
        }
    }

    public void addGovnerFeatures(List<StanfordCorenlpToken> sentence, int focus, TObjectDoubleMap<String> features,
                                  Function<Word, String> operator, String featureType) {
        if (focus < 0 || focus > sentence.size() - 1) {
            return;
        }
        StanfordCorenlpToken token = sentence.get(focus);
        FSList headDependencyRelations = token.getHeadDependencyRelations();

        if (headDependencyRelations == null) {
            return;
        }

        for (Dependency dep : FSCollectionFactory.create(headDependencyRelations, Dependency.class)) {
            Word gov = dep.getHead();
            String featureVal = operator.apply(gov);
            if (featureVal != null) {
                addToFeatures(features, String.format("%s=%s", featureType, featureVal), 1);
            }
        }
    }

    @Override
    public void extractGlobal(List<StanfordCorenlpToken> sequence, int focus, TObjectDoubleMap<String>
            globalFeatures, Map<Integer, String> knownStates) {

    }
}
