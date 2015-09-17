package edu.cmu.cs.lti.learning.feature.sentence.functions;

import edu.cmu.cs.lti.learning.feature.sentence.FeatureUtils;
import edu.cmu.cs.lti.ling.WordNetSearcher;
import edu.cmu.cs.lti.script.type.Dependency;
import edu.cmu.cs.lti.script.type.JobTitle;
import edu.cmu.cs.lti.script.type.StanfordCorenlpToken;
import edu.cmu.cs.lti.utils.Configuration;
import gnu.trove.map.TObjectDoubleMap;
import org.apache.uima.fit.util.FSCollectionFactory;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.FSList;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

/**
 * Created with IntelliJ IDEA.
 * Date: 9/15/15
 * Time: 1:42 AM
 *
 * @author Zhengzhong Liu
 */
public class WordNetSenseFeatures extends SequenceFeatureWithFocus {
    Set<StanfordCorenlpToken> jobTitleWords;
    WordNetSearcher searcher;

    List<BiConsumer<TObjectDoubleMap<String>, StanfordCorenlpToken>> featureTemplates;

    public WordNetSenseFeatures(Configuration generalConfig, Configuration featureConfig) throws IOException {
        super(generalConfig, featureConfig);
        searcher = new WordNetSearcher(generalConfig.get("edu.cmu.cs.lti.wndict.path"));

        featureTemplates = new ArrayList<>();

        for (String templateName : featureConfig.getList(this.getClass().getSimpleName() + ".templates")) {
            switch (templateName) {
                case "JobTitle":
                    featureTemplates.add(this::modifyingJobTitle);
                    break;
                case "Synonym":
                    featureTemplates.add(this::synonymFeatures);
                    break;
                default:
                    logger.warn(String.format("Template [%s] not recognized.", templateName));
            }
        }
    }

    @Override
    public void initDocumentWorkspace(JCas context) {
        jobTitleWords = new HashSet<>();
        for (JobTitle title : JCasUtil.select(context, JobTitle.class)) {
            jobTitleWords.addAll(JCasUtil.selectCovered(StanfordCorenlpToken.class, title).stream().collect
                    (Collectors.toList()));
        }
    }

    @Override
    public void resetWorkspace(JCas aJCas, int begin, int end) {

    }

    @Override
    public void extract(List<StanfordCorenlpToken> sequence, int focus, TObjectDoubleMap<String> features,
                        TObjectDoubleMap<String> featuresNeedForState) {
        if (focus > 0 && focus < sequence.size()) {
            for (BiConsumer<TObjectDoubleMap<String>, StanfordCorenlpToken> featureTemplate : featureTemplates) {
                featureTemplate.accept(features, sequence.get(focus));
            }
        }
    }

    private void modifyingJobTitle(TObjectDoubleMap<String> features, StanfordCorenlpToken token) {
        FSList headDeps = token.getHeadDependencyRelations();
        if (headDeps != null) {
            FSCollectionFactory.create(headDeps, Dependency.class).stream().filter(
                    dependency -> dependency.getDependencyType().endsWith("mod")).filter(
                    dependency -> jobTitleWords.contains(dependency.getChild())).forEach(
                    dependency -> features.put("ModifyingJobTitle", 1));
        }
    }

    private void synonymFeatures(TObjectDoubleMap<String> features, StanfordCorenlpToken token) {
        for (String synonym : searcher.getAllSynonyms(token.getLemma().toLowerCase(), token.getPos())) {
            features.put(FeatureUtils.formatFeatureName("LemmaSynonym", synonym), 1);
        }
    }
}
