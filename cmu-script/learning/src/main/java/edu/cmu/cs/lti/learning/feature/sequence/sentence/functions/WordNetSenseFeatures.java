package edu.cmu.cs.lti.learning.feature.sequence.sentence.functions;

import com.google.common.collect.Table;
import edu.cmu.cs.lti.learning.feature.sequence.FeatureUtils;
import edu.cmu.cs.lti.learning.feature.sequence.base.SequenceFeatureWithFocus;
import edu.cmu.cs.lti.learning.model.MentionKey;
import edu.cmu.cs.lti.ling.WordNetSearcher;
import edu.cmu.cs.lti.script.type.Dependency;
import edu.cmu.cs.lti.script.type.StanfordCorenlpToken;
import edu.cmu.cs.lti.script.type.WordNetBasedEntity;
import edu.cmu.cs.lti.utils.Configuration;
import edu.cmu.cs.lti.utils.FileUtils;
import gnu.trove.map.TObjectDoubleMap;
import org.apache.uima.fit.util.FSCollectionFactory;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.FSList;

import java.io.IOException;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

/**
 * Created with IntelliJ IDEA.
 * Date: 9/15/15
 * Time: 1:42 AM
 *
 * @author Zhengzhong Liu
 */
public class WordNetSenseFeatures extends SequenceFeatureWithFocus<StanfordCorenlpToken>  {
    Set<StanfordCorenlpToken> jobTitleWords;
    WordNetSearcher searcher;

    List<BiConsumer<TObjectDoubleMap<String>, StanfordCorenlpToken>> featureTemplates;

    public WordNetSenseFeatures(Configuration generalConfig, Configuration featureConfig) throws IOException {
        super(generalConfig, featureConfig);
        searcher = new WordNetSearcher(
                FileUtils.joinPaths(generalConfig.get("edu.cmu.cs.lti.resource.dir"),
                        generalConfig.get("edu.cmu.cs.lti.wndict.path"))
        );

        featureTemplates = new ArrayList<>();

        for (String templateName : featureConfig.getList(featureConfigKey("templates"))) {
            switch (templateName) {
                case "JobTitle":
                    featureTemplates.add(this::modifyingJobTitle);
                    break;
                case "Synonym":
                    featureTemplates.add(this::synonymFeatures);
                    break;
                case "Derivation":
                    featureTemplates.add(this::derivationFeatures);
                    break;
                default:
                    logger.warn(String.format("Template [%s] not recognized.", templateName));
            }
        }
    }

    @Override
    public void initDocumentWorkspace(JCas context) {
        jobTitleWords = new HashSet<>();
        for (WordNetBasedEntity title : JCasUtil.select(context, WordNetBasedEntity.class)) {
            if (title.getSense().equals("JobTitle")) {
                jobTitleWords.addAll(JCasUtil.selectCovered(StanfordCorenlpToken.class, title).stream().collect
                        (Collectors.toList()));
            }
        }
    }

    @Override
    public void resetWorkspace(JCas aJCas) {

    }

    @Override
    public void extract(List<StanfordCorenlpToken> sequence, int focus, TObjectDoubleMap<String> nodeFeatures,
                        Table<org.apache.commons.lang3.tuple.Pair<Integer, Integer>, String, Double> edgeFeatures) {
        if (focus > 0 && focus < sequence.size()) {
            for (BiConsumer<TObjectDoubleMap<String>, StanfordCorenlpToken> featureTemplate : featureTemplates) {
                featureTemplate.accept(nodeFeatures, sequence.get(focus));
            }
        }
    }

    @Override
    public void extractGlobal(List<StanfordCorenlpToken> sequence, int focus, TObjectDoubleMap<String>
            globalFeatures, List<MentionKey> knownStates, MentionKey currentState) {

    }

    private void modifyingJobTitle(TObjectDoubleMap<String> features, StanfordCorenlpToken token) {
        FSList headDeps = token.getHeadDependencyRelations();
        if (headDeps != null) {
            FSCollectionFactory.create(headDeps, Dependency.class).stream().forEach(dep -> {
                if (dep.getDependencyType().endsWith("mod") && jobTitleWords.contains(dep.getHead())) {
                    addToFeatures(features, "TriggerModifyingJobTitle", 1);
                }
            });
        }
    }

    private void synonymFeatures(TObjectDoubleMap<String> features, StanfordCorenlpToken token) {
        for (String synonym : searcher.getAllSynonyms(token.getLemma().toLowerCase(), token.getPos())) {
            addToFeatures(features, FeatureUtils.formatFeatureName("TriggerLemmaSynonym", synonym), 1);
        }
    }

    private void derivationFeatures(TObjectDoubleMap<String> features, StanfordCorenlpToken token) {
        Set<String> derivedWordType = new HashSet<>();

        for (Map.Entry<String, String> der : searcher.getDerivations(token.getLemma().toLowerCase(), token.getPos()).entries()) {
            derivedWordType.add(der.getValue());
        }

        if (!derivedWordType.isEmpty()) {
            derivedWordType.add(token.getLemma().toLowerCase());
        }

        for (String s : derivedWordType) {
            addToFeatures(features, FeatureUtils.formatFeatureName("TriggerDerivationForm", s), 1);
        }
    }
}
