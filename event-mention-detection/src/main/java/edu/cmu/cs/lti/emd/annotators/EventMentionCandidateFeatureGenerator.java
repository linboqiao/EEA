package edu.cmu.cs.lti.emd.annotators;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import edu.cmu.cs.lti.collection_reader.EventMentionDetectionDataReader;
import edu.cmu.cs.lti.ling.FrameDataReader;
import edu.cmu.cs.lti.script.type.*;
import edu.cmu.cs.lti.uima.annotator.AbstractLoggingAnnotator;
import edu.cmu.cs.lti.uima.util.UimaConvenience;
import edu.cmu.cs.lti.uima.util.UimaNlpUtils;
import edu.cmu.cs.lti.utils.TokenAlignmentHelper;
import gnu.trove.map.TIntDoubleMap;
import gnu.trove.map.hash.TIntDoubleHashMap;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.util.FSCollectionFactory;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.FSArray;
import org.apache.uima.jcas.cas.FSList;
import org.apache.uima.resource.ResourceInitializationException;
import org.javatuples.Triplet;

import java.util.*;

/**
 * Created with IntelliJ IDEA.
 * User: zhengzhongliu
 * Date: 1/27/15
 * Time: 12:03 PM
 */
public class EventMentionCandidateFeatureGenerator extends AbstractLoggingAnnotator {

    public static final String PARAM_SEM_LINK_DIR = "semLinkDir";

    public static final String PARAM_HAS_GOLD = "hasGoldStandard";

    public static BiMap<String, Integer> featureNameMap = HashBiMap.create();

    public static List<Triplet<String, TIntDoubleMap, String>> instances;

    public static Set<String> allTypes;

    @ConfigurationParameter(name = PARAM_SEM_LINK_DIR)
    String semLinkDirPath;

    @ConfigurationParameter(name = PARAM_HAS_GOLD)
    boolean hasGold;

    Map<String, String> vn2Fn;

    Map<String, String> pb2Vn;

    TokenAlignmentHelper align = new TokenAlignmentHelper();

    double averageCoverage = 0;
    int docCount = 0;

    int nextFeatureId = 0;

    Map<Word, Integer> wordIds;
    ArrayList<StanfordCorenlpToken> allWords;

    public static final String OTHER_TYPE = "other_event";

    @Override
    public void initialize(UimaContext aContext) throws ResourceInitializationException {
        super.initialize(aContext);
        vn2Fn = FrameDataReader.getFN2VNFrameMap(semLinkDirPath + "/vn-fn/VN-FNRoleMapping.txt", true);
        pb2Vn = FrameDataReader.getFN2VNFrameMap(semLinkDirPath + "/vn-pb/vnpbMappings", true);
        allTypes = new HashSet<>();
        allTypes.add(OTHER_TYPE);
    }

    @Override
    public void process(JCas aJCas) throws AnalysisEngineProcessException {
        UimaConvenience.printProcessLog(aJCas, logger);

        indexWords(aJCas);

        align.loadWord2Stanford(aJCas, EventMentionDetectionDataReader.componentId);
        align.loadFanse2Stanford(aJCas);

        Map<Word, TIntDoubleMap> semaforCandidates = semaforCandidateFinder(aJCas);
        Map<Word, TIntDoubleMap> fanseCandidates = fanseCandidateFinder(aJCas);

        Map<Word, TIntDoubleMap> allCandidates = mergeCandidates(fanseCandidates, semaforCandidates);

        for (Map.Entry<Word, TIntDoubleMap> candidate : allCandidates.entrySet()) {
            Word headWord = candidate.getKey();
            TIntDoubleMap f = candidate.getValue();
            addHeadWordFeatures(headWord, f);
//            addSurroundingWordFeatures(headWord, 2, f);
        }

        instances = new ArrayList<>();

        logger.info("Candidates " + allCandidates.size() + " " + fanseCandidates.size() + " " + semaforCandidates.size());

        if (hasGold) {
            Map<Word, String> goldHeads = calCoverage(aJCas, allCandidates.keySet());

            for (Map.Entry<Word, TIntDoubleMap> candidate : allCandidates.entrySet()) {
                Word head = candidate.getKey();
                TIntDoubleMap f = candidate.getValue();
                if (goldHeads.containsKey(head)) {
                    String eventType = goldHeads.get(head);
                    instances.add(Triplet.with(head.getCoveredText(), f, eventType));
                    allTypes.add(eventType);
                } else {
                    instances.add(Triplet.with(head.getCoveredText(), f, OTHER_TYPE));
                }
            }
        }
        docCount++;
    }

    private void addHeadWordFeatures(Word triggerWord, TIntDoubleMap features) {
        addFeature("TriggerHeadLemma_" + triggerWord, features);
        addFeature("HeadPOS_" + triggerWord.getPos(), features);

//        if (triggerWord.getHeadDependencyRelations() != null) {
//            for (Dependency dep : FSCollectionFactory.create(triggerWord.getHeadDependencyRelations(), Dependency.class)) {
//                addFeature("HeadDepType_" + dep.getDependencyType(), features);
//                addFeature("HeadDepLemma_" + dep.getHead().getLemma(), features);
//            }
//        }
//
//        if (triggerWord.getChildDependencyRelations() != null) {
//            for (Dependency dep : FSCollectionFactory.create(triggerWord.getChildDependencyRelations(), Dependency.class)) {
//                addFeature("ChildDepType_" + dep.getDependencyType(), features);
//                addFeature("ChildDepLemma_" + dep.getHead().getLemma(), features);
//            }
//        }
    }

    private void addSurroundingWordFeatures(Word word, int windowSize, TIntDoubleMap features) {
        int centerId = wordIds.get(word);
        int leftLimit = centerId - windowSize > 0 ? centerId - windowSize : 0;
        int rightLimit = centerId + windowSize < allWords.size() - 1 ? centerId + windowSize : allWords.size() - 1;
        for (int i = centerId; i >= leftLimit; i--) {
            addWordFeature(allWords.get(i), features);
        }
        for (int i = centerId; i <= rightLimit; i++) {
            addWordFeature(allWords.get(i), features);
        }
    }

    private void addWordFeature(Word word, TIntDoubleMap features) {
        addFeature("WindowPOS_" + word.getPos(), features);
        addFeature("WindowLemma_" + word.getLemma(), features);
    }


    private void indexWords(JCas aJCas) {
        allWords = new ArrayList<>(JCasUtil.select(aJCas, StanfordCorenlpToken.class));
        wordIds = new HashMap<>();
        int i = 0;
        for (Word word : allWords) {
            wordIds.put(word, i++);
        }
    }


    public void collectionProcessComplete() throws AnalysisEngineProcessException {
        System.err.println("Average coverage " + averageCoverage / docCount);
    }

    private ArrayListMultimap<EventMention, Word> getGoldAnnotations(JCas aJCas) {
        JCas goldStandard = UimaConvenience.getView(aJCas, "goldStandard");

        ArrayListMultimap<EventMention, Word> eid2Wid = ArrayListMultimap.create();

        for (EventMention mention : JCasUtil.select(goldStandard, EventMention.class)) {
//            System.err.println(mention.getCoveredText() + " " + mention.getId());
            for (Word word : FSCollectionFactory.create(mention.getMentionTokens(), Word.class)) {
//                StanfordCorenlpToken goldWord = UimaNlpUtils.findHeadFromTreeAnnotation(aJCas, word);
                StanfordCorenlpToken goldWord = align.getStanfordToken(word);
                eid2Wid.put(mention, goldWord);
//                System.err.println("Gold word : " + goldWord.getCoveredText());
            }
        }

        return eid2Wid;
    }

    private Map<Word, TIntDoubleMap> mergeCandidates(Map<Word, TIntDoubleMap>... candidates) {
        Map<Word, TIntDoubleMap> merged = new HashMap<>();
        for (Map<Word, TIntDoubleMap> candidateMap : candidates) {
            for (Map.Entry<Word, TIntDoubleMap> candidateFeatures : candidateMap.entrySet()) {
                Word candidateHead = candidateFeatures.getKey();
                TIntDoubleMap features = candidateFeatures.getValue();
                if (merged.containsKey(candidateHead)) {
                    merged.get(candidateHead).putAll(features);
                } else {
                    merged.put(candidateHead, features);
                }
            }
        }
        return merged;
    }

    private Map<Word, TIntDoubleMap> semaforCandidateFinder(JCas aJCas) {
        Map<Word, TIntDoubleMap> word2Features = new HashMap<>();

        TIntDoubleMap features = new TIntDoubleHashMap();

        for (SemaforAnnotationSet annoSet : JCasUtil.select(aJCas, SemaforAnnotationSet.class)) {
            String frameName = annoSet.getFrameName();
            Map<String, SemaforLabel> sAnnos = getSemaforAnnotations(annoSet);
            StanfordCorenlpToken labelHeadWord = UimaNlpUtils.findHeadFromTreeAnnotation(aJCas, sAnnos.get("Target"));
            getSemaforFeatures(aJCas, sAnnos, frameName, features);
            word2Features.put(labelHeadWord, features);
        }
        return word2Features;
    }

    private int getFeatureId(String featureName) {
        int featureId;
        if (featureNameMap.containsKey(featureName)) {
            featureId = featureNameMap.get(featureName);
        } else {
            featureId = nextFeatureId;
            nextFeatureId++;
            featureNameMap.put(featureName, featureId);
        }
        return featureId;
    }

    private void addFeature(String featureName, double featureVal, TIntDoubleMap features) {
        features.adjustOrPutValue(getFeatureId(featureName), featureVal, featureVal);
    }

    private void addFeature(String featureName, TIntDoubleMap features) {
        features.adjustOrPutValue(getFeatureId(featureName), 1.0, 1.0);
    }

    private void getSemaforFeatures(JCas aJCas, Map<String, SemaforLabel> labels, String frameName, TIntDoubleMap features) {
        addFeature("FrameName_" + frameName, 1.0, features);

        for (Map.Entry<String, SemaforLabel> label : labels.entrySet()) {
            String name = label.getKey();
            SemaforLabel l = label.getValue();
            String labelHeadLemma = UimaNlpUtils.findHeadFromTreeAnnotation(aJCas, l).getLemma().toLowerCase();
            if (!name.equals("Target")) {
                addFeature("FrameArgument_" + labelHeadLemma, 1.0, features);
                addFeature("FramArgumentName_" + name, 1.0, features);
            }
        }
    }

    private Map<String, SemaforLabel> getSemaforAnnotations(SemaforAnnotationSet annoSet) {
        Map<String, SemaforLabel> sLabels = new HashMap<>();
        for (SemaforLayer layer : FSCollectionFactory.create(annoSet.getLayers(), SemaforLayer.class)) {
            String layerName = layer.getName();

            if (layerName.equals("Target")) {// Target that invoke the frame
                sLabels.put("Target", layer.getLabels(0));

            } else if (layerName.equals("FE")) {// Frame element
                FSArray elements = layer.getLabels();
                if (elements != null) {
                    for (SemaforLabel element : FSCollectionFactory.create(elements, SemaforLabel.class)) {
                        sLabels.put(element.getName(), element);
                    }
                }
            }
        }
        return sLabels;
    }


    private Map<Word, TIntDoubleMap> fanseCandidateFinder(JCas aJCas) {
        Map<Word, TIntDoubleMap> word2Frame = new HashMap<>();

        TIntDoubleMap fanseFeatures = new TIntDoubleHashMap();

        for (FanseToken token : JCasUtil.select(aJCas, FanseToken.class)) {
            String propbankSense = token.getLexicalSense();
            String frameName = FrameDataReader.getFrameFromPropBankSense(propbankSense, pb2Vn, vn2Fn);

            if (frameName != null) {
                addFeature("FrameName_" + frameName, fanseFeatures);
            }

            FSList childSemantics = token.getChildSemanticRelations();

            if (childSemantics != null) {
                for (FanseSemanticRelation relation : FSCollectionFactory.create(childSemantics, FanseSemanticRelation.class)) {
                    String labelHeadLemma = align.getLowercaseWordLemma(relation.getChild());
                    addFeature("FrameArgument_" + labelHeadLemma, fanseFeatures);
                }
            }
        }
        return word2Frame;
    }

    private Map<Word, String> calCoverage(JCas aJCas, Set<Word> candidateHeads) {
        Map<Word, String> goldCandidates = new HashMap<>();

        int coverage = 0;
        Map<EventMention, Collection<Word>> goldAnnos = getGoldAnnotations(aJCas).asMap();
        for (Map.Entry<EventMention, Collection<Word>> gold : goldAnnos.entrySet()) {
            EventMention goldEventMention = gold.getKey();
            Collection<Word> goldWords = gold.getValue();

            boolean found = false;

            for (Word candidateHeadWord : candidateHeads) {
                if (goldWords.contains(candidateHeadWord)) {
                    found = true;
                    coverage++;
                    goldCandidates.put(candidateHeadWord, goldEventMention.getEventType());
                    break;
                }
            }

            if (!found) {
                logger.info(String.format("Mention [%s]-%s is not covered, it has type [%s]",
                        goldEventMention.getCoveredText(), goldEventMention.getId(), goldEventMention.getEventType()));

                for (Word w : goldWords) {
                    System.err.println(w.getCoveredText() + " " + w.getBegin() + " " + w.getEnd() + " " + w.getId());
                }
            }
        }
        logger.info("Coverage : " + coverage * 1.0 / goldAnnos.size());
        averageCoverage += coverage * 1.0 / goldAnnos.size();

        return goldCandidates;
    }

}
