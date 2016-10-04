package edu.cmu.cs.lti.learning.feature.sequence.sentence.functions;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Table;
import edu.cmu.cs.lti.learning.feature.sequence.FeatureUtils;
import edu.cmu.cs.lti.learning.feature.sequence.base.SequenceFeatureWithFocus;
import edu.cmu.cs.lti.learning.model.MentionKey;
import edu.cmu.cs.lti.script.type.*;
import edu.cmu.cs.lti.uima.util.UimaConvenience;
import edu.cmu.cs.lti.uima.util.UimaNlpUtils;
import edu.cmu.cs.lti.utils.Configuration;
import gnu.trove.map.TObjectDoubleMap;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.uima.fit.util.FSCollectionFactory;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.FSArray;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

/**
 * Oh, god, what did I wrote here...
 *
 * @author Zhengzhong Liu
 */
public class FrameFeatures extends SequenceFeatureWithFocus<StanfordCorenlpToken> {
    private Map<StanfordCorenlpToken, String> triggerToFrameName;
    private ArrayListMultimap<StanfordCorenlpToken, String> triggerToArgRole;
    private ArrayListMultimap<StanfordCorenlpToken, String> triggerToArgLemma;
    private ArrayListMultimap<StanfordCorenlpToken, String> triggerToArgNer;

    private List<BiConsumer<TObjectDoubleMap<String>, StanfordCorenlpToken>> featureTemplates;

    private boolean useContextFrames;

    public FrameFeatures(Configuration generalConfig, Configuration featureConfig) {
        super(generalConfig, featureConfig);

        featureTemplates = new ArrayList<>();
        for (String templateName : featureConfig.getList(featureConfigKey("templates"))) {
            switch (templateName) {
                case "FrameArgumentLemma":
                    featureTemplates.add(this::frameArgumentLemma);
                    break;
                case "FrameArgumentRole":
                    featureTemplates.add(this::frameArgumentRole);
                    break;
                case "FrameName":
                    featureTemplates.add(this::frameNameFeature);
                    break;
                case "FrameArgumentNer":
                    featureTemplates.add(this::frameArgumentNer);
                    break;
                case "ContextFrame":
                    useContextFrames = true;
                    break;
                default:
                    logger.warn(String.format("Template [%s] not recognized.", templateName));
            }
        }
    }

    @Override
    public void initDocumentWorkspace(JCas context) {
        readFrames(context);

        // Make sure ner exists.
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
        if (focus > sequence.size() - 1 || focus < 0) {
            return;
        }
        StanfordCorenlpToken token = sequence.get(focus);

        for (BiConsumer<TObjectDoubleMap<String>, StanfordCorenlpToken> template : featureTemplates) {
            template.accept(nodeFeatures, token);
        }

        if (useContextFrames) {
            contextFrame(sequence, nodeFeatures, focus);
        }
    }

    private void frameNameFeature(TObjectDoubleMap<String> features, StanfordCorenlpToken token) {
        addFeature(features, "FrameName", triggerToFrameName.get(token));
    }

    private void frameArgumentRole(TObjectDoubleMap<String> features, StanfordCorenlpToken token) {
        addFeature(features, "FrameArgumentRole", triggerToArgRole.get(token));
    }

    private void frameArgumentLemma(TObjectDoubleMap<String> features, StanfordCorenlpToken token) {
        addPairFeature(features, "FrameArgumentLemma", triggerToArgRole.get(token), triggerToArgLemma.get(token));
    }

    private void frameArgumentNer(TObjectDoubleMap<String> features, StanfordCorenlpToken token) {
        addPairFeature(features, "FrameArgumentNer", triggerToArgRole.get(token), triggerToArgNer.get(token));
    }

    private void contextFrame(List<StanfordCorenlpToken> sequence, TObjectDoubleMap<String> features, int focus) {
        for (int i = 0; i < sequence.size(); i++) {
            if (i != focus) {
                String contextFrame = triggerToFrameName.get(sequence.get(i));
                if (contextFrame != null) {
                    addFeature(features, "ContextFrame", contextFrame);
                }
            }
        }
    }

    private void addFeature(TObjectDoubleMap<String> features, String name, String value) {
        addToFeatures(features, FeatureUtils.formatFeatureName(name, value), 1);
    }

    private void addFeature(TObjectDoubleMap<String> features, String name, List<String> value) {
        if (value != null) {
            for (String v : value) {
                if (v != null) {
                    addToFeatures(features, FeatureUtils.formatFeatureName(name, v), 1);
                }
            }
        }
    }

    private void addPairFeature(TObjectDoubleMap<String> features, String name, List<String> firstValue, List<String>
            secondValue) {
        for (int i = 0; i < firstValue.size(); i++) {
            String f = firstValue.get(i);
            String s = secondValue.get(i);
            if (f != null && s != null) {
                addToFeatures(features, FeatureUtils.formatFeatureName(name, f + "_" + s), 1);
            }
        }
    }

    // Prepare frames.
    private void readFrames(JCas jCas) {
        triggerToArgRole = ArrayListMultimap.create();
        triggerToArgLemma = ArrayListMultimap.create();
        triggerToArgNer = ArrayListMultimap.create();

        triggerToFrameName = new HashMap<>();

        for (SemaforAnnotationSet annoSet : JCasUtil.select(jCas, SemaforAnnotationSet.class)) {
            String frameName = annoSet.getFrameName();

            SemaforLabel trigger = null;
            List<SemaforLabel> frameElements = new ArrayList<>();


            for (SemaforLayer layer : FSCollectionFactory.create(annoSet.getLayers(), SemaforLayer.class)) {
                String layerName = layer.getName();
                if (layerName.equals("Target")) {// Target that invoke the frame
                    trigger = layer.getLabels(0);
                } else if (layerName.equals("FE")) {// Frame element
                    FSArray elements = layer.getLabels();
                    if (elements != null) {
                        frameElements.addAll(FSCollectionFactory.create(elements, SemaforLabel.class).stream()
                                .collect(Collectors.toList()));
                    }
                }
            }

            StanfordCorenlpToken triggerHead = UimaNlpUtils.findHeadFromStanfordAnnotation(trigger);

            if (triggerHead != null) {
                triggerToFrameName.put(triggerHead, frameName);
            }

            for (SemaforLabel label : frameElements) {
                StanfordCorenlpToken elementHead = UimaNlpUtils.findHeadFromStanfordAnnotation(label);
                if (elementHead == null) {
                    elementHead = UimaConvenience.selectCoveredFirst(label, StanfordCorenlpToken.class);
                }
                if (elementHead != null) {
                    StanfordCorenlpToken labelHead = UimaNlpUtils.findHeadFromStanfordAnnotation(label);
                    triggerToArgRole.put(triggerHead, label.getName());
                    triggerToArgLemma.put(triggerHead, labelHead.getLemma());
                    triggerToArgNer.put(triggerHead, labelHead.getNerTag());
                }
            }
        }
    }

    @Override
    public void extractGlobal(List<StanfordCorenlpToken> sequence, int focus, TObjectDoubleMap<String>
            globalFeatures, List<MentionKey> knownStates, MentionKey currentState) {

    }
}
